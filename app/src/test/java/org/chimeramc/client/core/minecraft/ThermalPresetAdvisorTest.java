package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.chimeramc.client.util.ThermalPolicy;

/**
 * Pins the thermal preset suggestion.
 *
 * <p>The rule has two boundaries that matter: a merely warm device must not be interrupted, and a
 * device already on Battery must never be told to go lighter (there is nothing below it).
 */
public class ThermalPresetAdvisorTest {

    @Test
    public void aCoolDeviceGetsNoAdvice() {
        assertEquals(ThermalPresetAdvisor.Advice.NONE,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_NONE, "PERFORMANCE"));
    }

    @Test
    public void aWarmDeviceGetsNoAdvice() {
        assertEquals(ThermalPresetAdvisor.Advice.NONE,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_WARM, "PERFORMANCE"));
    }

    @Test
    public void aHotDeviceOnPerformanceIsAdvised() {
        assertEquals(ThermalPresetAdvisor.Advice.SUGGEST_LIGHTER,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_HOT, "PERFORMANCE"));
    }

    @Test
    public void aHotDeviceOnBalancedIsAdvised() {
        assertEquals(ThermalPresetAdvisor.Advice.SUGGEST_LIGHTER,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_HOT, "BALANCED"));
    }

    @Test
    public void aHotDeviceAlreadyOnBatteryIsNotAdvised() {
        assertEquals(ThermalPresetAdvisor.Advice.NONE,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_HOT, "BATTERY"));
    }

    @Test
    public void anUnknownPresetReadsAsBalanced() {
        assertEquals(ThermalPresetAdvisor.Advice.SUGGEST_LIGHTER,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_HOT, "WHATEVER"));
    }

    @Test
    public void aNullPresetReadsAsBalanced() {
        assertEquals(ThermalPresetAdvisor.Advice.SUGGEST_LIGHTER,
                ThermalPresetAdvisor.advise(ThermalPolicy.SEVERITY_HOT, null));
    }

    @Test
    public void theSuggestionIsBatteryFromPerformance() {
        assertEquals("BATTERY", ThermalPresetAdvisor.suggestedPresetName("PERFORMANCE"));
    }

    @Test
    public void theSuggestionIsBatteryFromBalanced() {
        assertEquals("BATTERY", ThermalPresetAdvisor.suggestedPresetName("BALANCED"));
    }

    @Test
    public void thereIsNoSuggestionFromBattery() {
        assertNull(ThermalPresetAdvisor.suggestedPresetName("BATTERY"));
    }
}
