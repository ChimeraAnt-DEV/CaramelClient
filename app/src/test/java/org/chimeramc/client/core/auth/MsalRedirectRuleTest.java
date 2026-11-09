package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The MSAL redirect URI the app asks Microsoft to return to must be exactly the one the manifest
 * registers, or the interactive sign-in silently completes at Microsoft and never comes back.
 *
 * <p>This is the pure half of that rule, so it is pinned without a device: the URI is
 * {@code msauth://<package>/<signature-hash>}, and the manifest filter's host and path are derived
 * from the same two inputs. The two must agree — a build whose manifest placeholder drifted from the
 * value the runtime sends is a sign-in that hangs forever, which is the failure this test prevents.
 */
public class MsalRedirectRuleTest {

    @Test
    public void theUriIsTheMsauthSchemeWithThePackageAsHost() {
        assertEquals("msauth://org.chimeramc.client/abc123",
                MsalRedirectRule.redirectUri("org.chimeramc.client", "abc123"));
    }

    @Test
    public void theManifestFilterMatchesTheUri() {
        String pkg = "org.chimeramc.client";
        String hash = "aBc123=";
        assertEquals("msauth", MsalRedirectRule.manifestScheme());
        assertEquals(pkg, MsalRedirectRule.manifestHost(pkg));
        assertEquals("/" + hash, MsalRedirectRule.manifestPath(hash));
    }

    @Test
    public void aBlankSignatureHashStillProducesAUsableUri() {
        // The debug keystore hash can be empty before a signature is computed; the URI must still be
        // well-formed (host set, path at least "/") rather than "msauth://pkg/null".
        String uri = MsalRedirectRule.redirectUri("org.chimeramc.client", null);
        assertTrue("uri must not contain a null literal: " + uri, !uri.contains("null"));
        assertTrue(uri.startsWith("msauth://org.chimeramc.client/"));
        assertEquals("/", MsalRedirectRule.manifestPath(null));
    }

    @Test
    public void theSchemeIsTheOneMsalRequires() {
        // MSAL's Android redirect always uses the msauth scheme; anything else is not delivered.
        assertEquals("msauth", MsalRedirectRule.manifestScheme());
    }
}
