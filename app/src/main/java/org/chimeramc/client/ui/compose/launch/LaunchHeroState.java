package org.chimeramc.client.ui.compose.launch;

import org.chimeramc.client.util.Api28Compat;

/**
 * The Launch-tab hero's content, as plain data.
 *
 * <p>The hero shows the selected instance's identity and three stats beside the live 3D preview.
 * Keeping the values in one immutable object means the Compose screen is a pure function of it, and
 * the two presentation rules that used to live inside a 3,000-line activity — hide the version line
 * when it merely repeats the instance name, and pick the ABI badge — are testable without a device.
 */
public final class LaunchHeroState {

    /** Which ABI badge to show, or {@link #NONE}. The label and colour are chosen by the UI. */
    public enum AbiKind {
        NONE,
        BIT_32,
        BIT_64,
    }

    public final String title;
    public final String badge;
    public final String name;
    /** Empty when the version adds nothing beyond the name. */
    public final String version;
    public final AbiKind abi;
    public final int instancesCount;
    public final int modsCount;
    public final String playtime;
    /** The preview's accessibility description. */
    public final String previewDescription;

    public LaunchHeroState(String title, String badge, String name, String version, AbiKind abi,
                           int instancesCount, int modsCount, String playtime,
                           String previewDescription) {
        this.title = title;
        this.badge = badge;
        this.name = name;
        this.version = version;
        this.abi = abi;
        this.instancesCount = instancesCount;
        this.modsCount = modsCount;
        this.playtime = playtime;
        this.previewDescription = previewDescription;
    }

    /**
     * The version line, or empty when it repeats the name.
     *
     * <p>Instances are commonly named after their version, so the same "26.51" would otherwise appear
     * twice. The name is the identity; the version only earns a line when it adds something.
     */
    public static String versionOrEmpty(String name, String version) {
        if (version == null || version.isEmpty()) return "";
        return version.equals(name) ? "" : version;
    }

    /** The ABI badge kind for a version's {@code abiList}, or {@link AbiKind#NONE} when unknown. */
    public static AbiKind abiKind(String abiList) {
        if (abiList == null || abiList.isEmpty()) return AbiKind.NONE;
        boolean is32Bit = Api28Compat.listOf("armeabi-v7a", "x86").contains(abiList);
        return is32Bit ? AbiKind.BIT_32 : AbiKind.BIT_64;
    }

    /** The empty state shown before a version is selected. */
    public static LaunchHeroState empty(String title, String badge, String fallbackName,
                                        String previewDescription) {
        return new LaunchHeroState(title, badge, fallbackName, "", AbiKind.NONE, 0, 0, "0m",
                previewDescription);
    }
}
