package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Overlay layouts (item 212): the model's clamps, the store's pure rules and the wheel catalog's
 * layout entries.
 */
public class OverlayLayoutTest {

    @Test
    public void aLayoutRecordsAndReturnsFractionPairs() {
        OverlayLayout layout = new OverlayLayout("pvp", "PvP");
        layout.put("armor_hud", 0.25f, 0.75f);
        float[] value = layout.positionOf("armor_hud");
        assertNotNull(value);
        assertEquals(0.25f, value[0], 0.0001f);
        assertEquals(0.75f, value[1], 0.0001f);
        assertEquals(1, layout.size());
        assertNull(layout.positionOf("cps_display"));
    }

    @Test
    public void aFractionOutsideTheRangeIsClampedOnWrite() {
        OverlayLayout layout = new OverlayLayout("l", "L");
        layout.put("fps_display", -3f, 4f);
        float[] value = layout.positionOf("fps_display");
        assertEquals(0f, value[0], 0.0001f);
        assertEquals(1f, value[1], 0.0001f);
    }

    @Test
    public void aNaNIsWrittenAsZeroRatherThanPropagating() {
        OverlayLayout layout = new OverlayLayout("l", "L");
        layout.put("fps_display", Float.NaN, Float.NaN);
        float[] value = layout.positionOf("fps_display");
        assertEquals(0f, value[0], 0.0001f);
        assertEquals(0f, value[1], 0.0001f);
    }

    @Test
    public void anEmptyKeyIsIgnored() {
        OverlayLayout layout = new OverlayLayout("l", "L");
        layout.put("", 0.5f, 0.5f);
        layout.put(null, 0.5f, 0.5f);
        assertTrue(layout.isEmpty());
    }

    @Test
    public void theEntryCountIsCappedSoACorruptBlobCannotGrowForever() {
        OverlayLayout layout = new OverlayLayout("l", "L");
        for (int i = 0; i < OverlayLayout.MAX_ENTRIES + 20; i++) {
            layout.put("key" + i, 0.1f, 0.1f);
        }
        assertEquals(OverlayLayout.MAX_ENTRIES, layout.size());
        // An existing key can still be rewritten at the cap, so a widget is never frozen.
        layout.put("key0", 0.9f, 0.9f);
        assertEquals(0.9f, layout.positionOf("key0")[0], 0.0001f);
    }

    @Test
    public void aNameBecomesAStableId() {
        assertEquals("pvp", OverlayLayoutStore.slug("PvP"));
        assertEquals("survival", OverlayLayoutStore.slug("Survival!"));
        assertEquals("layout", OverlayLayoutStore.slug("!!!"));
        assertEquals("layout", OverlayLayoutStore.slug(null));
    }

    @Test
    public void aDuplicateNameGetsADistinctId() {
        List<OverlayLayout> layouts = new ArrayList<>();
        layouts.add(new OverlayLayout("pvp", "PvP"));
        assertEquals("pvp2", OverlayLayoutStore.uniqueId(layouts, "PvP"));
    }

    @Test
    public void upsertReplacesByIdAndAppendsANewOne() {
        List<OverlayLayout> layouts = new ArrayList<>();
        OverlayLayout first = new OverlayLayout("pvp", "PvP");
        first.put("a", 0.1f, 0.1f);
        layouts = OverlayLayoutStore.upsert(layouts, first);
        assertEquals(1, layouts.size());

        OverlayLayout replacement = new OverlayLayout("pvp", "PvP 2");
        replacement.put("b", 0.2f, 0.2f);
        layouts = OverlayLayoutStore.upsert(layouts, replacement);
        assertEquals(1, layouts.size());
        assertEquals("PvP 2", layouts.get(0).name);

        layouts = OverlayLayoutStore.upsert(layouts, new OverlayLayout("survival", "Survival"));
        assertEquals(2, layouts.size());
    }

    @Test
    public void upsertRefusesANamelessLayout() {
        List<OverlayLayout> layouts = OverlayLayoutStore.upsert(new ArrayList<>(),
                new OverlayLayout("", ""));
        assertTrue(layouts.isEmpty());
    }

    @Test
    public void sanitizeDropsLayoutsWithoutAnIdAndDuplicates() {
        List<OverlayLayout> raw = new ArrayList<>();
        raw.add(new OverlayLayout("", "no id"));
        raw.add(new OverlayLayout("pvp", "PvP"));
        raw.add(new OverlayLayout("pvp", "PvP again"));
        raw.add(null);
        List<OverlayLayout> safe = OverlayLayoutStore.sanitize(raw);
        assertEquals(1, safe.size());
        assertEquals("pvp", safe.get(0).id);
    }

    @Test
    public void sanitizeNamesALayoutAndTrimsALongName() {
        List<OverlayLayout> raw = new ArrayList<>();
        OverlayLayout unnamed = new OverlayLayout("pvp", "");
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < OverlayLayout.MAX_NAME_LENGTH + 10; i++) longName.append('x');
        raw.add(unnamed);
        raw.add(new OverlayLayout("survival", longName.toString()));
        List<OverlayLayout> safe = OverlayLayoutStore.sanitize(raw);
        assertEquals("an unnamed layout falls back to its id", "pvp", safe.get(0).name);
        assertEquals(OverlayLayout.MAX_NAME_LENGTH, safe.get(1).name.length());
    }

    @Test
    public void sanitizeCapsTheNumberOfLayouts() {
        List<OverlayLayout> raw = new ArrayList<>();
        for (int i = 0; i < OverlayLayout.MAX_LAYOUTS + 5; i++) {
            raw.add(new OverlayLayout("l" + i, "L" + i));
        }
        assertEquals(OverlayLayout.MAX_LAYOUTS, OverlayLayoutStore.sanitize(raw).size());
    }

    @Test
    public void findByIdFindsOnlyAnExactId() {
        List<OverlayLayout> layouts = new ArrayList<>();
        layouts.add(new OverlayLayout("pvp", "PvP"));
        assertNotNull(OverlayLayoutStore.findById(layouts, "pvp"));
        assertNull(OverlayLayoutStore.findById(layouts, "survival"));
        assertNull(OverlayLayoutStore.findById(layouts, null));
    }

    @Test
    public void theWheelOffersALayoutEntryPerStoredLayout() {
        List<OverlayLayout> layouts = new ArrayList<>();
        layouts.add(new OverlayLayout("pvp", "PvP"));
        layouts.add(new OverlayLayout("survival", "Survival"));
        List<WheelEntryCatalog.Choice> choices =
                WheelEntryCatalog.forProfile(new ArrayList<>(), "root", layouts);
        int layoutChoices = 0;
        for (WheelEntryCatalog.Choice choice : choices) {
            if (choice.action == WheelEntry.Action.SWITCH_LAYOUT) {
                layoutChoices++;
                assertTrue("a layout entry names its layout",
                        "pvp".equals(choice.target) || "survival".equals(choice.target));
            }
        }
        assertEquals(2, layoutChoices);
        // The plain catalog is unchanged, so a caller without layouts sees the same set as before.
        assertEquals(WheelEntryCatalog.all().size(),
                WheelEntryCatalog.forProfile(new ArrayList<>(), "root", null).size());
    }

    @Test
    public void aLayoutEntryWithoutANameUsesItsId() {
        List<OverlayLayout> layouts = new ArrayList<>();
        layouts.add(new OverlayLayout("pvp", ""));
        List<WheelEntryCatalog.Choice> choices =
                WheelEntryCatalog.forProfile(new ArrayList<>(), "root", layouts);
        WheelEntryCatalog.Choice layoutChoice = null;
        for (WheelEntryCatalog.Choice choice : choices) {
            if (choice.action == WheelEntry.Action.SWITCH_LAYOUT) layoutChoice = choice;
        }
        assertNotNull(layoutChoice);
        assertEquals("pvp", layoutChoice.literalLabel);
    }

    @Test
    public void aLayoutEntryIsNotASubWheel() {
        assertFalse(new WheelEntry(WheelEntry.Action.SWITCH_LAYOUT, "pvp", "PvP").isSubWheel());
    }
}
