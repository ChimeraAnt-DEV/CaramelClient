package org.chimeramc.client.launcher.controller;

/**
 * The "press a button to set the modifier" state machine.
 *
 * <p>Extracted from the settings fragment, which used to hold the pending-layer field and decide
 * inline whether a key press was a capture or an ordinary press. That decision is the mapping
 * model: it is what decides which button a layer ends up bound to, and getting it wrong silently
 * binds the wrong button. It is pure and Android-free, so it can be pinned by tests instead of
 * only being visible on a device with a pad in hand.
 *
 * <p>The rule is deliberately narrow: only a <em>press</em> can complete a capture. A release that
 * arrives while a capture is running must pass through untouched, or the button's own release would
 * be swallowed and the pad would see a stuck press.
 */
public final class LayerModifierCapture {

    /** What the caller should do with a key event it just received. */
    public enum Decision {
        /** The press completed the capture; the layer is now bound and the event is consumed. */
        CAPTURED,
        /** A capture is running but this event is not its completion; the event is consumed. */
        SWALLOWED,
        /** No capture is running; the caller handles the event normally. */
        PASSTHROUGH
    }

    private RemapLayer pending;

    /** Starts a capture. A second call replaces the first, so the newest request wins. */
    public void begin(RemapLayer layer) {
        pending = layer;
    }

    /** True while a capture is waiting for a press. */
    public boolean isCapturing() {
        return pending != null;
    }

    /** The layer waiting for its modifier, or null. */
    public RemapLayer pending() {
        return pending;
    }

    /** Abandons a capture without binding anything. */
    public void cancel() {
        pending = null;
    }

    /**
     * Offers one key event to the capture.
     *
     * @param keyCode the raw key code, before any remap — the capture must see the button the
     *                player actually pressed, not what the current map turns it into
     * @param down    whether this is a press
     * @return what the caller should do with the event
     */
    public Decision offer(int keyCode, boolean down) {
        if (pending == null) {
            return Decision.PASSTHROUGH;
        }
        if (!down) {
            // A release during a capture is not a binding and must not reach the pad either: the
            // press that started this capture was consumed, so forwarding its release would be a
            // release with no press.
            return Decision.SWALLOWED;
        }
        pending.setModifierKeyCode(keyCode);
        pending = null;
        return Decision.CAPTURED;
    }
}
