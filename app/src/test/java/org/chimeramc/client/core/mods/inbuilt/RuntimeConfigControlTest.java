package org.chimeramc.client.core.mods.inbuilt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The schema-to-control rules.
 *
 * <p>A slider whose bounds disagree with the schema writes a value the mod clamps or ignores, and
 * the user sees a control that does nothing. These pin the bounds, the step arithmetic and the
 * choice style so the view cannot drift from the schema.
 */
public class RuntimeConfigControlTest {

    private static RuntimeConfigSchema.Node node(String json) {
        RuntimeConfigSchema schema = RuntimeConfigSchema.parse(
                "{\"version\":2,\"categories\":[{\"id\":\"c\"}],\"nodes\":[" + json + "]}");
        return schema.nodes.get(0);
    }

    @Test
    public void anIntSliderReadsItsBoundsAndValue() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"a\",\"type\":\"slider_int\",\"min_value\":\"2\","
                + "\"max_value\":\"10\",\"default_value\":\"6\"}");
        RuntimeConfigControl.IntRange r = RuntimeConfigControl.intRange(n, "");
        assertEquals(2, r.min);
        assertEquals(10, r.max);
        assertEquals(1, r.step);
        assertEquals(6, r.value);
        assertEquals(8, r.steps());
    }

    @Test
    public void aStoredValueBeatsTheDefaultAndIsClamped() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"a\",\"type\":\"slider_int\",\"min_value\":\"0\","
                + "\"max_value\":\"10\",\"default_value\":\"6\"}");
        assertEquals(3, RuntimeConfigControl.intRange(n, "3").value);
        // A stored value outside the range is clamped, not trusted.
        assertEquals(10, RuntimeConfigControl.intRange(n, "999").value);
        assertEquals(0, RuntimeConfigControl.intRange(n, "-4").value);
    }

    @Test
    public void aMissingBoundFallsBackRatherThanThrowing() {
        RuntimeConfigSchema.Node n = node("{\"id\":\"a\",\"type\":\"slider_int\"}");
        RuntimeConfigControl.IntRange r = RuntimeConfigControl.intRange(n, "");
        assertEquals(0, r.min);
        assertEquals(100, r.max);
        assertEquals(1, r.step);
    }

    @Test
    public void aZeroStepStillProducesATrack() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"a\",\"type\":\"slider_int\",\"min_value\":\"5\","
                + "\"max_value\":\"5\",\"step\":\"0\"}");
        RuntimeConfigControl.IntRange r = RuntimeConfigControl.intRange(n, "");
        assertEquals(1, r.steps());
        assertEquals(5, r.valueAt(0));
    }

    @Test
    public void theIntTrackPositionAndValueRoundTrip() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"a\",\"type\":\"slider_int\",\"min_value\":\"0\","
                + "\"max_value\":\"100\",\"step\":\"10\"}");
        RuntimeConfigControl.IntRange r = RuntimeConfigControl.intRange(n, "40");
        assertEquals(10, r.steps());
        assertEquals(4, r.positionOf(40));
        assertEquals(40, r.valueAt(4));
        // Past the end of the track clamps to the maximum.
        assertEquals(100, r.valueAt(99));
    }

    @Test
    public void aFloatSliderClampsAndReadsItsValue() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"f\",\"type\":\"slider_float\",\"min_value\":\"0.0\","
                + "\"max_value\":\"1.0\",\"default_value\":\"0.5\"}");
        RuntimeConfigControl.FloatRange r = RuntimeConfigControl.floatRange(n, "2.0");
        assertEquals(0f, r.min, 0.0001f);
        assertEquals(1f, r.max, 0.0001f);
        assertEquals(1f, r.value, 0.0001f);
    }

    @Test
    public void anInvertedFloatRangeCollapsesRatherThanGoingNegative() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"f\",\"type\":\"slider_float\",\"min_value\":\"5\","
                + "\"max_value\":\"1\"}");
        RuntimeConfigControl.FloatRange r = RuntimeConfigControl.floatRange(n, "");
        assertTrue(r.max >= r.min);
    }

    @Test
    public void theChoiceStyleFollowsTheOptionCountAtFour() {
        RuntimeConfigSchema.Node four = node(
                "{\"id\":\"c\",\"type\":\"choice\",\"options\":["
                + "{\"value\":\"1\"},{\"value\":\"2\"},{\"value\":\"3\"},{\"value\":\"4\"}]}");
        assertEquals("segmented", RuntimeConfigControl.choiceStyle(four));
        RuntimeConfigSchema.Node five = node(
                "{\"id\":\"c\",\"type\":\"choice\",\"options\":["
                + "{\"value\":\"1\"},{\"value\":\"2\"},{\"value\":\"3\"},"
                + "{\"value\":\"4\"},{\"value\":\"5\"}]}");
        assertEquals("dropdown", RuntimeConfigControl.choiceStyle(five));
    }

    @Test
    public void anExplicitStyleBeatsTheOptionCount() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"c\",\"type\":\"choice\",\"style\":\"radio\",\"options\":["
                + "{\"value\":\"1\"},{\"value\":\"2\"},{\"value\":\"3\"},"
                + "{\"value\":\"4\"},{\"value\":\"5\"}]}");
        assertEquals("radio", RuntimeConfigControl.choiceStyle(n));
    }

    @Test
    public void theChoiceIndexFindsTheMatchingOptionOrFallsBackToZero() {
        RuntimeConfigSchema.Node n = node(
                "{\"id\":\"c\",\"type\":\"choice\",\"options\":["
                + "{\"value\":\"a\"},{\"value\":\"b\"}]}");
        assertEquals(1, RuntimeConfigControl.choiceIndex(n.options, "b"));
        assertEquals(0, RuntimeConfigControl.choiceIndex(n.options, "zzz"));
        assertEquals(0, RuntimeConfigControl.choiceIndex(null, "b"));
    }

    @Test
    public void theTextRulesReadLengthAndMultiline() {
        RuntimeConfigSchema.Node single = node(
                "{\"id\":\"t\",\"type\":\"text\",\"max_length\":\"12\"}");
        assertEquals(12, RuntimeConfigControl.maxLength(single));
        assertFalse(RuntimeConfigControl.multiline(single));
        RuntimeConfigSchema.Node multi = node("{\"id\":\"t\",\"type\":\"multiline_text\"}");
        assertTrue(RuntimeConfigControl.multiline(multi));
        assertEquals(0, RuntimeConfigControl.maxLength(multi));
    }

    @Test
    public void theToggleRuleMatchesTheStoredForms() {
        assertTrue(RuntimeConfigControl.toggleValue("true"));
        assertTrue(RuntimeConfigControl.toggleValue("TRUE"));
        assertTrue(RuntimeConfigControl.toggleValue("1"));
        assertTrue(RuntimeConfigControl.toggleValue("yes"));
        assertTrue(RuntimeConfigControl.toggleValue("on"));
        assertFalse(RuntimeConfigControl.toggleValue("false"));
        assertFalse(RuntimeConfigControl.toggleValue("0"));
        assertFalse(RuntimeConfigControl.toggleValue(null));
    }
}
