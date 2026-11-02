package org.chimeramc.client.core.telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds exactly what the anonymous usage counter would send (item 281).
 *
 * <p>The list of fields is short on purpose and is a property of this class rather than of a
 * request builder, so the in-app preview and the upload cannot disagree about what leaves the
 * device. It is pure and Android-free, so the shape is unit-tested without a device.
 *
 * <p>What is <em>not</em> here is the point: no account id, no gamertag, no device serial, no
 * advertising id, no server address, no world name, no mod list. The device is described by a
 * coarse class (its RAM bucket and ABI), which is enough to spot "crashes only on 2 GB devices"
 * and cannot single anyone out.
 */
public final class UsageStatsPayload {

    /** The schema version, so a future field addition is distinguishable server-side. */
    public static final int SCHEMA_VERSION = 1;

    private UsageStatsPayload() {
    }

    /** One field the report carries, for the preview. */
    public static final class Field {
        public final String name;
        public final String value;

        public Field(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * The fields a report contains, in a stable order.
     *
     * @param appVersion the launcher version name
     * @param deviceClass a coarse device class ("2gb", "4gb", "8gb+"), never a model name
     * @param abi the process ABI ("arm64-v8a")
     * @param launches how many launches this install has made
     * @param successes how many of those reached the game
     * @param crashes how many crash reports this install has recorded
     */
    public static Map<String, String> fields(String appVersion, String deviceClass, String abi,
                                             long launches, long successes, long crashes) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("schema", String.valueOf(SCHEMA_VERSION));
        map.put("version", safe(appVersion));
        map.put("device_class", safe(deviceClass));
        map.put("abi", safe(abi));
        map.put("launches", String.valueOf(Math.max(0L, launches)));
        map.put("launch_successes", String.valueOf(Math.max(0L, successes)));
        map.put("crash_reports", String.valueOf(Math.max(0L, crashes)));
        return map;
    }

    /**
     * The coarse device class for a total-RAM byte count.
     *
     * <p>Buckets rather than a number: an exact figure plus a version plus an ABI narrows a user
     * down more than an anonymous counter should. The buckets are the ones that matter for a
     * Minecraft launcher (2 GB devices are the ones that struggle).
     */
    public static String deviceClassFor(long totalRamBytes) {
        long mb = totalRamBytes / (1024L * 1024L);
        if (mb <= 0L) return "unknown";
        if (mb < 2560L) return "2gb";
        if (mb < 5120L) return "4gb";
        if (mb < 9216L) return "8gb";
        return "12gb+";
    }

    /** The JSON body the request carries. Hand-rolled so the payload has one definition. */
    public static String toJson(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (entry.getKey() == null) continue;
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escape(entry.getKey())).append("\":\"")
                        .append(escape(entry.getValue())).append('"');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * The human-readable preview shown before the user opts in.
     *
     * <p>Names every field and its value, then states what is absent. A preview that only listed
     * the field names would let a later addition slip in unseen, so the values are included too.
     */
    public static String describe(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("An anonymous usage report would contain:\n");
        if (fields == null || fields.isEmpty()) {
            sb.append("  (nothing)\n");
        } else {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append('\n');
            }
        }
        sb.append("\nIt contains no account name, gamertag, device serial, advertising id, ")
                .append("server address, world name or mod list.");
        return sb.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escape(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
