package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Set;

/**
 * The published Caramel-user list, as the badge rule reads it.
 *
 * <p>The document is hand-edited in the repository, so the parser is the one place a typo can turn
 * into a wrong badge. Every case here is a shape a maintainer might actually write.
 */
public class CaramelUserRegistryTest {

    @Test
    public void theDocumentedShapeIsRead() {
        Set<String> ids = CaramelUserRegistry.parse(
                "{\"schemaVersion\": 1, \"xuids\": [\"2533274812345678\", \"2533274811111111\"]}");
        assertEquals(2, ids.size());
        assertTrue(ids.contains("2533274812345678"));
    }

    @Test
    public void aBareArrayIsReadToo() {
        // A list of ids is small enough that it gets edited by hand and the wrapper does not survive.
        Set<String> ids = CaramelUserRegistry.parse("[\"2533274812345678\"]");
        assertTrue(ids.contains("2533274812345678"));
    }

    @Test
    public void idsAreNormalisedSoMatchingIsCaseInsensitive() {
        Set<String> ids = CaramelUserRegistry.parse("{\"xuids\": [\"  25332748ABCDEF01  \"]}");
        assertTrue(ids.contains("25332748abcdef01"));
        assertEquals(1, ids.size());
    }

    @Test
    public void blankAndBrokenEntriesDoNotBecomeIds() {
        Set<String> ids = CaramelUserRegistry.parse(
                "{\"xuids\": [\"\", \"   \", null, 42, {\"xuid\": \"1\"}]}");
        assertFalse(ids.contains(""));
        assertFalse(ids.contains("42"));
        assertFalse(ids.contains("{\"xuid\": \"1\"}"));
        assertTrue(ids.isEmpty());
    }

    @Test
    public void aMalformedDocumentIsAnEmptyListRatherThanACrash() {
        assertTrue(CaramelUserRegistry.parse("not json").isEmpty());
        assertTrue(CaramelUserRegistry.parse("{\"posts\": []}").isEmpty());
        assertTrue(CaramelUserRegistry.parse("").isEmpty());
        assertTrue(CaramelUserRegistry.parse(null).isEmpty());
    }
}
