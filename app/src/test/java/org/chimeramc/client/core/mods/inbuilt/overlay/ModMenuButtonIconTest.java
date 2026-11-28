package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.R;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The in-game menu button's glyph.
 *
 * <p>The button floats over the game and lands in every screenshot, so its icon decides whether the
 * client announces itself. It wore the Caramel apple; it now wears a plain handle. These tests pin
 * that choice and the three properties the handle needs to be legible on an arbitrary game scene:
 * it is a vector, it draws something, and it is a single flat white so it survives any backdrop.
 */
public class ModMenuButtonIconTest {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private static final String HANDLE_NAME = "app/src/main/res/drawable/mod_menu_handle.xml";

    /** The module's working directory differs between the IDE and Gradle, so walk up to the repo. */
    private static Path handle() throws java.io.IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(HANDLE_NAME);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + HANDLE_NAME);
    }

    @Test
    public void theButtonDoesNotWearTheClientsBrandMark() {
        assertNotEquals("the floating button must not use the apple again",
                R.drawable.mod_menu_icon, ModMenuButtonOverlay.iconResource());
        assertEquals(R.drawable.mod_menu_handle, ModMenuButtonOverlay.iconResource());
    }

    @Test
    public void theHandleIsAVectorWithVisibleStrokes() throws Exception {
        Path handle = handle();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(handle.toFile());
        Element root = document.getDocumentElement();
        assertEquals("vector", root.getTagName());

        NodeList paths = root.getElementsByTagName("path");
        assertTrue("a handle needs at least the three bars, found " + paths.getLength(),
                paths.getLength() >= 3);
        for (int i = 0; i < paths.getLength(); i++) {
            Element path = (Element) paths.item(i);
            String data = android(path, "pathData");
            assertTrue("path " + i + " draws nothing", data.startsWith("M"));
            assertTrue("path " + i + " has no width", !android(path, "strokeWidth").isEmpty());
            assertTrue("path " + i + " has no colour",
                    !android(path, "strokeColor").isEmpty() || !android(path, "fillColor").isEmpty());
        }
    }

    /** The vector attributes are in the android namespace, so a plain getAttribute returns "". */
    private static String android(Element element, String name) {
        String value = element.getAttributeNS(ANDROID_NS, name);
        return value.isEmpty() ? element.getAttribute(name) : value;
    }

    @Test
    public void everyColourIsOpaqueWhite() throws Exception {
        String xml = new String(Files.readAllBytes(handle()), java.nio.charset.StandardCharsets.UTF_8);
        Matcher colours = Pattern.compile("#[0-9A-Fa-f]{6,8}").matcher(xml);
        List<String> found = new ArrayList<>();
        while (colours.find()) found.add(colours.group().toUpperCase());
        assertFalse("the handle states no colour", found.isEmpty());
        for (String colour : found) {
            assertTrue("the handle must stay flat white, found " + colour,
                    colour.equals("#FFFFFFFF"));
        }
    }
}
