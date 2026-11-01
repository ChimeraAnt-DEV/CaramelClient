package org.chimeramc.client.launcher.core.changelog;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the bundled {@code CHANGELOG.md} into a flat list of renderable blocks (item 270).
 *
 * <p>The repo's changelog is the single source of truth, and it is a plain Markdown file rather than
 * a bespoke JSON so contributors can edit it in a normal editor. This class turns the small subset
 * the file actually uses — {@code #} headings, {@code -} bullets and blank lines — into blocks, so
 * the screen needs no Markdown library and the parse is unit-testable without a device.
 *
 * <p>It is deliberately forgiving: an unknown line becomes a body block rather than being dropped,
 * because a changelog that silently loses a line is worse than one that renders it plainly.
 */
public final class ChangelogMarkdown {

    public enum Kind { VERSION, SECTION, BULLET, BODY }

    /** One renderable block. */
    public static final class Block {
        public final Kind kind;
        public final String text;
        /** Heading level: 1 for a version, 2 for a section, 0 otherwise. */
        public final int level;

        Block(Kind kind, String text, int level) {
            this.kind = kind;
            this.text = text;
            this.level = level;
        }
    }

    private ChangelogMarkdown() {
    }

    /**
     * Parses Markdown into blocks.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code # Title} (level 1) is the document title and is skipped; the app supplies its own.
     *   <li>{@code ## X} is a {@link Kind#VERSION} block (a release heading).
     *   <li>{@code ### X} and deeper are {@link Kind#SECTION} blocks.
     *   <li>{@code - x} / {@code * x} / {@code + x} are {@link Kind#BULLET} blocks.
     *   <li>Any other non-blank line is a {@link Kind#BODY} block.
     *   <li>Blank lines are separators and produce nothing.
     * </ul>
     * A null or empty input yields an empty list.
     */
    public static List<Block> parse(String markdown) {
        List<Block> blocks = new ArrayList<>();
        if (markdown == null || markdown.isEmpty()) return blocks;

        for (String raw : markdown.split("\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            int hashes = 0;
            while (hashes < line.length() && line.charAt(hashes) == '#') {
                hashes++;
            }
            if (hashes > 0 && hashes < line.length() && line.charAt(hashes) == ' ') {
                String text = line.substring(hashes + 1).trim();
                if (text.isEmpty()) continue;
                if (hashes == 1) {
                    // Document title; the screen shows its own header.
                    continue;
                }
                blocks.add(new Block(hashes == 2 ? Kind.VERSION : Kind.SECTION, text, hashes));
                continue;
            }

            if (line.length() > 1 && isBulletMarker(line.charAt(0)) && line.charAt(1) == ' ') {
                String text = line.substring(2).trim();
                if (!text.isEmpty()) {
                    blocks.add(new Block(Kind.BULLET, text, 0));
                }
                continue;
            }

            blocks.add(new Block(Kind.BODY, line, 0));
        }
        return blocks;
    }

    private static boolean isBulletMarker(char c) {
        return c == '-' || c == '*' || c == '+';
    }

    /**
     * Whether the parsed blocks contain anything worth rendering.
     *
     * <p>Used to decide between the changelog list and the empty state, so a file that only holds a
     * title does not open a blank screen.
     */
    public static boolean hasContent(List<Block> blocks) {
        if (blocks == null) return false;
        for (Block block : blocks) {
            if (block.kind != Kind.BODY || !block.text.isEmpty()) return true;
        }
        return false;
    }
}
