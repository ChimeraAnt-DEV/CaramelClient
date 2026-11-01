package org.chimeramc.client.launcher.controller;

/**
 * A stable identity for one physical controller model (item 219).
 *
 * <p>A profile list is keyed by {@link ControllerType}, which is a family ("Xbox"), not a device.
 * Two different Xbox pads can want different dead zones and remaps, so a per-device binding needs
 * its own key. Vendor and product id together are what the input framework reports for a model, and
 * they are stable across reconnects and reboots, unlike a device id which is reassigned.
 *
 * <p>Pure and Android-free so the key format is unit-tested; the manager stores bindings under
 * {@link #storageKey()}.
 */
public final class ControllerDeviceKey {

    /** The vendor/product pair the input framework reports for "unknown". */
    public static final int UNKNOWN = -1;

    private final int vendorId;
    private final int productId;

    private ControllerDeviceKey(int vendorId, int productId) {
        this.vendorId = vendorId;
        this.productId = productId;
    }

    /**
     * Builds a key for a device, or null when the ids cannot identify one.
     *
     * <p>Both ids must be present and non-negative. A key built from a missing id would collapse
     * every unidentified pad onto one binding, which is worse than having no per-device profile at
     * all: the user would edit one pad's profile and silently change another's.
     */
    public static ControllerDeviceKey of(int vendorId, int productId) {
        if (vendorId < 0 || productId < 0) return null;
        return new ControllerDeviceKey(vendorId, productId);
    }

    public int vendorId() {
        return vendorId;
    }

    public int productId() {
        return productId;
    }

    /** The SharedPreferences key suffix; hex so it is a single stable token with no separators. */
    public String storageKey() {
        return Integer.toHexString(vendorId) + "_" + Integer.toHexString(productId);
    }

    @Override
    public String toString() {
        return storageKey();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ControllerDeviceKey)) return false;
        ControllerDeviceKey that = (ControllerDeviceKey) other;
        return vendorId == that.vendorId && productId == that.productId;
    }

    @Override
    public int hashCode() {
        return vendorId * 31 + productId;
    }
}
