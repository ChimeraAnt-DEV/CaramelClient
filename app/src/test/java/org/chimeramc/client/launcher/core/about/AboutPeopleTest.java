package org.chimeramc.client.launcher.core.about;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The About roster: the owner and the lead developer, their roles and their links.
 *
 * <p>The screen is a screenshot people share, so the two accounts it names and the URLs it opens are
 * pinned here: a wrong handle is a link to someone else's profile.
 */
public class AboutPeopleTest {

    @Test
    public void theRosterNamesTheOwnerThenTheLeadDeveloper() {
        List<AboutPeople.Person> all = AboutPeople.all();
        assertEquals(2, all.size());
        assertEquals("ChimeraAnt-DEV", all.get(0).name);
        assertEquals(AboutPeople.Role.OWNER, all.get(0).role);
        assertEquals("Lynxifyy", all.get(1).name);
        assertEquals(AboutPeople.Role.LEAD_DEVELOPER, all.get(1).role);
    }

    @Test
    public void theOwnerLinksGitHubAndYouTube() {
        AboutPeople.Person owner = AboutPeople.of(AboutPeople.Role.OWNER);
        assertNotNull(owner);
        assertEquals("https://github.com/ChimeraAnt-DEV", owner.githubUrl);
        assertEquals("https://youtube.com/@glowberryclient", owner.youtubeUrl);
        assertTrue(owner.hasGithub());
        assertTrue(owner.hasYoutube());
    }

    @Test
    public void theLeadDeveloperLinksGitHubAndHasNoYouTubeChannel() {
        AboutPeople.Person lead = AboutPeople.of(AboutPeople.Role.LEAD_DEVELOPER);
        assertNotNull(lead);
        assertEquals("https://github.com/Lynxifyy", lead.githubUrl);
        assertTrue(lead.hasGithub());
        assertFalse("the lead developer has no channel to link", lead.hasYoutube());
    }

    @Test
    public void everyAvatarUrlIsTheProfilesOwnHandle() {
        for (AboutPeople.Person person : AboutPeople.all()) {
            assertEquals("https://github.com/" + person.name + ".png?size=160", person.avatarUrl);
        }
    }

    @Test
    public void everyPersonIsFullyPopulated() {
        for (AboutPeople.Person person : AboutPeople.all()) {
            assertFalse(person.name.isEmpty());
            assertTrue(person.hasGithub());
            assertNotNull(person.role);
        }
    }
}
