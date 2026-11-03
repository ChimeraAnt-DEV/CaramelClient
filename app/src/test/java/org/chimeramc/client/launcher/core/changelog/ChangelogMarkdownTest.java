package org.chimeramc.client.launcher.core.changelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Pins the CHANGELOG.md parse rules. */
public class ChangelogMarkdownTest {

    @Test
    public void parsesVersionSectionsAndBullets() {
        String md = "# Changelog\n\n## BETA3\n\n### Cosmetics\n\n- Fixed the cape\n- Fixed the hat\n";
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse(md);
        assertEquals(4, blocks.size());
        assertEquals(ChangelogMarkdown.Kind.VERSION, blocks.get(0).kind);
        assertEquals("BETA3", blocks.get(0).text);
        assertEquals(ChangelogMarkdown.Kind.SECTION, blocks.get(1).kind);
        assertEquals("Cosmetics", blocks.get(1).text);
        assertEquals(ChangelogMarkdown.Kind.BULLET, blocks.get(2).kind);
        assertEquals("Fixed the cape", blocks.get(2).text);
    }

    @Test
    public void theDocumentTitleIsSkipped() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("# Changelog\n## BETA3\n");
        assertEquals(1, blocks.size());
        assertEquals(ChangelogMarkdown.Kind.VERSION, blocks.get(0).kind);
    }

    @Test
    public void anUnknownLineBecomesABodyBlockNotDropped() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("just a sentence\n");
        assertEquals(1, blocks.size());
        assertEquals(ChangelogMarkdown.Kind.BODY, blocks.get(0).kind);
        assertEquals("just a sentence", blocks.get(0).text);
    }

    @Test
    public void everyBulletMarkerIsAccepted() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("- a\n* b\n+ c\n");
        assertEquals(3, blocks.size());
        for (ChangelogMarkdown.Block b : blocks) {
            assertEquals(ChangelogMarkdown.Kind.BULLET, b.kind);
        }
    }

    @Test
    public void aHashWithoutASpaceIsNotAHeading() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("#notaheading\n");
        assertEquals(1, blocks.size());
        assertEquals(ChangelogMarkdown.Kind.BODY, blocks.get(0).kind);
    }

    @Test
    public void blankLinesProduceNothing() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("\n\n   \n\n");
        assertTrue(blocks.isEmpty());
    }

    @Test
    public void nullAndEmptyInputsAreSafe() {
        assertTrue(ChangelogMarkdown.parse(null).isEmpty());
        assertTrue(ChangelogMarkdown.parse("").isEmpty());
        assertFalse(ChangelogMarkdown.hasContent(null));
    }

    @Test
    public void aTitleOnlyFileHasNoContent() {
        assertFalse(ChangelogMarkdown.hasContent(ChangelogMarkdown.parse("# Changelog\n")));
    }

    @Test
    public void aRealEntryHasContent() {
        assertTrue(ChangelogMarkdown.hasContent(
                ChangelogMarkdown.parse("# Changelog\n## BETA3\n- one change\n")));
    }

    @Test
    public void deeperHeadingsAreSectionsNotVersions() {
        List<ChangelogMarkdown.Block> blocks = ChangelogMarkdown.parse("#### Deep\n");
        assertEquals(1, blocks.size());
        assertEquals(ChangelogMarkdown.Kind.SECTION, blocks.get(0).kind);
    }
}
