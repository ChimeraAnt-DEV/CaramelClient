package org.chimeramc.client.ui.compose.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

/**
 * The Compose dimension tokens equal the real {@code dimens.xml}.
 *
 * <p>This is what keeps {@link GlowDimens} from drifting: a change to a spacing or radius that is made
 * in only one of the two places fails the build. The check compares every mirrored token by value,
 * and fails if a token the class carries is not in the XML at all (a typo or a renamed dimen).
 */
public class GlowDimensTest {

    @Test
    public void everyMirroredTokenMatchesTheXml() throws IOException {
        Map<String, Integer> xml = GlowDimens.readDimensXml();
        for (Map.Entry<String, Integer> e : GlowDimens.asMap().entrySet()) {
            Integer actual = xml.get(e.getKey());
            assertTrue("@dimen/" + e.getKey() + " is mirrored in GlowDimens but not in dimens.xml",
                    actual != null);
            assertEquals("GlowDimens." + e.getKey() + " disagrees with dimens.xml",
                    actual, e.getValue());
        }
    }

    @Test
    public void theAliasResolutionResolvesAnIndirectDimen() throws IOException {
        // radius_card = @dimen/radius_large = 16dp; if the alias resolution were broken this would
        // read as absent rather than 16.
        Map<String, Integer> xml = GlowDimens.readDimensXml();
        assertEquals(Integer.valueOf(16), xml.get("radius_large"));
    }

    @Test
    public void theScaleIsMonotonic() {
        assertTrue(GlowDimens.SPACE_1 < GlowDimens.SPACE_2);
        assertTrue(GlowDimens.SPACE_2 < GlowDimens.SPACE_3);
        assertTrue(GlowDimens.SPACE_3 < GlowDimens.SPACE_4);
        assertTrue(GlowDimens.SPACE_4 < GlowDimens.SPACE_6);
        assertTrue(GlowDimens.SPACE_6 < GlowDimens.SPACE_8);
        assertTrue(GlowDimens.RADIUS_SMALL < GlowDimens.RADIUS_MEDIUM);
        assertTrue(GlowDimens.RADIUS_MEDIUM < GlowDimens.RADIUS_LARGE);
        assertTrue(GlowDimens.RADIUS_LARGE < GlowDimens.RADIUS_XLARGE);
    }

    @Test
    public void touchTargetsMeetTheAccessibilityFloor() {
        assertTrue(GlowDimens.MIN_TOUCH_TARGET >= 48);
        assertTrue(GlowDimens.NAV_RAIL_TAB_HEIGHT >= 48);
    }
}
