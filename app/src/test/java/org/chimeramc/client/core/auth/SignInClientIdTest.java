package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The sign-in flows must all use one real Bedrock client id.
 *
 * <p>The MSAL flow was handed the literal string {@code YOUR_CLIENT_ID_HERE}, so that button could
 * never complete a sign-in while the device-code and WebView flows worked. These checks pin the
 * single source of truth and that it is a real id rather than a placeholder.
 */
public class SignInClientIdTest {

    @Test
    public void theClientIdIsTheBedrockAndroidTitleId() {
        assertEquals("0000000048183522", MsftAuthManager.bedrockClientId());
    }

    @Test
    public void theClientIdIsNotAPlaceholder() {
        String id = MsftAuthManager.bedrockClientId();
        assertFalse("a placeholder client id can never complete a sign-in",
                id.toUpperCase().contains("YOUR_CLIENT_ID"));
        assertFalse(id.trim().isEmpty());
        assertFalse("a placeholder marker leaked in", id.contains("HERE"));
    }

    @Test
    public void theClientIdLooksLikeAnMsaTitleId() {
        // MSA title ids are 16 hex characters. A UUID-style id here would be an Azure app
        // registration, which is a different thing and would not authenticate a Bedrock title.
        String id = MsftAuthManager.bedrockClientId();
        assertTrue("expected 16 hex chars, got: " + id, id.matches("[0-9a-fA-F]{16}"));
    }
}
