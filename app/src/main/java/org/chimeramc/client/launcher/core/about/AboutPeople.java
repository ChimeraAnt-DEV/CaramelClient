package org.chimeramc.client.launcher.core.about;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The people credited on the About screen.
 *
 * <p>Pure data, so the roster, the roles and the links are unit-tested without a device and the
 * screen cannot quietly credit the wrong account. The GitHub avatar URL is derived from the handle
 * rather than a hardcoded numeric id, so it keeps working if a profile is renamed and needs no
 * network call to resolve.
 */
public final class AboutPeople {

    /** The two roles the screen names. */
    public enum Role { OWNER, LEAD_DEVELOPER }

    /** One credited person. {@code youtubeUrl} is null when the person has no channel to link. */
    public static final class Person {
        public final String name;
        public final String avatarUrl;
        public final String githubUrl;
        public final String youtubeUrl;
        public final Role role;

        Person(String name, String githubUrl, String youtubeUrl, Role role) {
            this.name = name;
            this.githubUrl = githubUrl;
            this.youtubeUrl = youtubeUrl;
            this.role = role;
            this.avatarUrl = "https://github.com/" + name + ".png?size=160";
        }

        public boolean hasGithub() {
            return githubUrl != null && !githubUrl.isEmpty();
        }

        public boolean hasYoutube() {
            return youtubeUrl != null && !youtubeUrl.isEmpty();
        }
    }

    /** The owner, then the lead developer: the order the screen shows them in. */
    private static final List<Person> ROSTER = Collections.unmodifiableList(Arrays.asList(
            new Person("ChimeraAnt-DEV",
                    "https://github.com/ChimeraAnt-DEV",
                    "https://youtube.com/@caramelclient",
                    Role.OWNER),
            new Person("Lynxifyy",
                    "https://github.com/Lynxifyy",
                    null,
                    Role.LEAD_DEVELOPER)));

    private AboutPeople() {
    }

    public static List<Person> all() {
        return new ArrayList<>(ROSTER);
    }

    /** The first person with {@code role}, or null when the roster does not carry it. */
    public static Person of(Role role) {
        for (Person person : ROSTER) {
            if (person.role == role) return person;
        }
        return null;
    }
}
