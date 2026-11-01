package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The overlay draw-time budget (item 214). */
public class OverlayDrawBudgetTest {

    @Test
    public void aDrawInsideTheBudgetIsNotReported() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertFalse(budget.record("fps", 0.4f, 1000L));
        assertFalse(budget.record("fps", OverlayDrawBudget.BUDGET_MS - 0.1f, 1000L));
    }

    @Test
    public void aDrawAtTheBudgetIsReported() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertTrue(budget.record("fps", OverlayDrawBudget.BUDGET_MS, 1000L));
    }

    @Test
    public void theSameOverlayIsNotReportedTwiceInsideTheCooldown() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertTrue(budget.record("fps", 5f, 1000L));
        assertFalse(budget.record("fps", 5f, 1000L + OverlayDrawBudget.REPORT_COOLDOWN_MS - 1));
        assertTrue(budget.record("fps", 5f, 1000L + OverlayDrawBudget.REPORT_COOLDOWN_MS));
    }

    @Test
    public void theCooldownIsPerOverlay() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertTrue(budget.record("fps", 5f, 1000L));
        assertTrue("a different overlay has its own cooldown", budget.record("cps", 5f, 1000L));
    }

    @Test
    public void aNullIdIsIgnored() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertFalse(budget.record(null, 50f, 1000L));
    }

    @Test
    public void aCustomBudgetIsHonoured() {
        OverlayDrawBudget budget = new OverlayDrawBudget(8f);
        assertEquals(8f, budget.budgetMs(), 0.0001f);
        assertFalse(budget.record("fps", 4f, 1000L));
        assertTrue(budget.record("fps", 9f, 1000L));
    }

    @Test
    public void resetClearsEveryCooldown() {
        OverlayDrawBudget budget = new OverlayDrawBudget();
        assertTrue(budget.record("fps", 5f, 1000L));
        budget.reset();
        assertTrue(budget.record("fps", 5f, 1001L));
    }
}
