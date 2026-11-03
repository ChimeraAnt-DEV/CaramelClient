package org.chimeramc.client.launcher.core.content;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.chimeramc.client.R;
import org.chimeramc.client.core.content.ScreenshotItem;

import java.io.File;

/**
 * Opens the system share sheet for a screenshot.
 *
 * <p>Saving to the gallery is one step; getting the image to Discord or a chat app from there is
 * several. This hands the file straight to {@code ACTION_SEND} with a {@code FileProvider} content
 * uri, which is what an app on API 24+ needs to pass a private file to another app.
 *
 * <p>Deliberately best-effort, exactly like {@code ReplayShareSheet}: a screenshot with no receiver
 * still opens the chooser, and a failure to build the uri is reported rather than thrown. It is
 * reachable from both the screenshot catalogue row and the in-game pause-menu row, and it shares
 * the pure naming/typing rules in {@link ScreenshotSharing} with the gallery export so the two
 * cannot describe the same file differently.
 */
public final class ScreenshotShareSheet {

    private ScreenshotShareSheet() {
    }

    /**
     * Shares {@code file} through the system chooser.
     *
     * @return true when the chooser was launched, false when there was nothing shareable or the uri
     *         could not be built.
     */
    public static boolean share(Context context, File file) {
        if (context == null || !ScreenshotSharing.isShareable(file)) {
            if (context != null) {
                Toast.makeText(context, R.string.screenshot_share_failed, Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        Uri uri;
        try {
            uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
        } catch (Throwable t) {
            Toast.makeText(context, R.string.screenshot_share_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (uri == null) {
            Toast.makeText(context, R.string.screenshot_share_failed, Toast.LENGTH_SHORT).show();
            return false;
        }

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(ScreenshotSharing.mimeType(file));
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, ScreenshotSharing.shareTitle(file));
        send.putExtra(Intent.EXTRA_TITLE, ScreenshotSharing.shareTitle(file));
        // Without the grant the receiving app cannot read the content uri and the share fails on
        // its side with a security exception.
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            Intent chooser = Intent.createChooser(send,
                    context.getString(R.string.screenshot_share));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            return true;
        } catch (Throwable t) {
            Toast.makeText(context, R.string.screenshot_share_no_app, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /** Convenience for the catalogue row, which carries a {@link ScreenshotItem}. */
    public static boolean shareItem(Context context, ScreenshotItem screenshot) {
        return share(context, screenshot == null ? null : screenshot.file);
    }
}
