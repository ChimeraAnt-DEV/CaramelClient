package org.chimeramc.client.core.versions;

public class VersionProfileMetadata {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String profileId;
    public String directoryName;
    public String versionName;
    public String displayName;
    public boolean versionIsolation;
    public boolean launchVertically;
    public boolean shaderCompatEnabled;
    public boolean inGamePackChangerEnabled;
    /** The bound Microsoft account id, or null when the instance uses the active account (F2). */
    public String boundAccountId;
    /** A free-form note the user keeps about this instance (item 278), or null when empty. */
    public String notes;
    public boolean installed;
    public String packageName;
    public long updatedAt;
}
