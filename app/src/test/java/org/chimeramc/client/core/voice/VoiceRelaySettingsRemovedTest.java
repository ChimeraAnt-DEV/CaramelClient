package org.chimeramc.client.core.voice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The voice settings no longer expose a relay server.
 *
 * <p>The launcher grew its own relay address, password and token-secret fields — three settings that
 * only made sense for a server the launcher itself had to be pointed at. BerryAPI owns a newer voice
 * system that carries its own relay and end-to-end encryption, so asking the player to type a relay
 * host is both redundant and a worse default. The fields are removed; the transport still resolves a
 * relay from the build default when one is configured, so an existing deployment is not broken.
 *
 * <p>A build machine cannot inflate the screen, so this reads the real layout and the real activity.
 */
public class VoiceRelaySettingsRemovedTest {

    @Test
    public void theRelayFieldsAreGoneFromTheVoiceLayout() throws IOException {
        String xml = read("app/src/main/res/layout/activity_voice_chat.xml");
        assertFalse("the relay switch must be removed", xml.contains("voice_relay_switch"));
        assertFalse("the relay address field must be removed", xml.contains("voice_relay_address"));
        assertFalse("the relay password field must be removed", xml.contains("voice_relay_password"));
        assertFalse("the relay token field must be removed",
                xml.contains("voice_relay_token_secret"));
        assertFalse("the relay save button must be removed",
                xml.contains("voice_relay_save_button"));
        assertFalse("the relay status line must be removed", xml.contains("voice_relay_status"));
    }

    @Test
    public void theActivityNoLongerWiresTheRelayFields() throws IOException {
        String src = read("app/src/main/java/org/chimeramc/client/launcher/ui/activities/"
                + "VoiceChatActivity.java");
        assertFalse("the activity must not reference the relay switch",
                src.contains("voice_relay_switch"));
        assertFalse("the activity must not reference the relay address field",
                src.contains("voice_relay_address"));
        assertFalse("the activity must not apply relay settings",
                src.contains("applyRelaySettings"));
    }

    @Test
    public void theTransportStillResolvesABuildDefaultRelay() throws IOException {
        // Removing the fields must not remove the ability to reach a relay the operator configured:
        // the module still reads the address when relay mode is on.
        String module = read("app/src/main/java/org/chimeramc/client/core/voice/"
                + "VoiceChatModule.java");
        assertTrue("the module must still resolve a relay address when relay mode is on",
                module.contains("isVoiceRelayEnabled") && module.contains("getVoiceRelayAddress"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}
