package org.chimeramc.client.core.minecraft;

/**
 * The one rule for turning a library's short name into its file name and back (item 301).
 *
 * <p>Split out of {@code GamePackageManager} so the extraction cache, the loader and the repair path
 * all agree on the name. A drift here is silent: a cache check that normalises differently from the
 * loader simply never finds its own file and re-extracts on every launch. Pure and Android-free, so
 * {@code LibraryNamesTest} pins every case.
 */
public final class LibraryNames {

    private LibraryNames() {
    }

    /** {@code "c++_shared"} to {@code "libc++_shared.so"}; an already-complete name is unchanged. */
    public static String fileName(String name) {
        if (name == null || name.isEmpty()) return "lib.so";
        if (name.startsWith("lib") && name.endsWith(".so")) return name;
        return "lib" + normalize(name) + ".so";
    }

    /** {@code "libc++_shared.so"} to {@code "c++_shared"}; the inverse of {@link #fileName}. */
    public static String normalize(String name) {
        if (name == null) return "";
        String stripped = name;
        if (stripped.startsWith("lib")) stripped = stripped.substring(3);
        if (stripped.endsWith(".so")) stripped = stripped.substring(0, stripped.length() - 3);
        return stripped;
    }

    /**
     * True when {@code name} already carries the {@code lib} prefix and {@code .so} suffix.
     *
     * <p>Used by the loader's "is this the launcher's own bundled library" test, which must not
     * double-prefix a name that is already complete.
     */
    public static boolean isCompleteFileName(String name) {
        return name != null && name.startsWith("lib") && name.endsWith(".so");
    }
}
