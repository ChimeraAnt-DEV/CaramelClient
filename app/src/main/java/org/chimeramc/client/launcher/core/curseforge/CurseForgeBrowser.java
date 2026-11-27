package org.chimeramc.client.core.curseforge;

import android.content.Context;

import org.chimeramc.client.core.curseforge.models.Content;
import org.chimeramc.client.core.curseforge.models.ContentAuthor;
import org.chimeramc.client.core.curseforge.models.ContentFile;
import org.chimeramc.client.core.curseforge.models.ContentSearchResponse;
import org.chimeramc.client.ui.compose.caramel.CaramelCurseForgeItem;
import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.MainThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The CurseForge tab's data side: search, map to the launcher's row shape, and fetch a file.
 *
 * <p>The API client is the existing {@link CurseForgeClient}; this class only does the two things
 * a screen needs on top of it — turn a response into [CaramelCurseForgeItem]s and write a project's
 * newest file to the cache so the content importer can read it. Callbacks are delivered on the main
 * thread, because every caller is a Compose state update.
 */
public final class CurseForgeBrowser {

    private static final String TAG = "CurseForgeBrowser";
    private static final int PAGE_SIZE = 30;

    private CurseForgeBrowser() {
    }

    public interface SearchListener {
        void onResults(List<CaramelCurseForgeItem> items);
        void onError(String message);
    }

    public interface DownloadListener {
        void onFile(File file);
        void onError(String message);
    }

    /** Searches by free text, newest-download ranking, and reports a mapped list. */
    public static void search(Context context, String query, SearchListener listener) {
        CurseForgeClient.getInstance(context).searchContent(
                query == null ? "" : query,
                0,
                null,
                0,
                PAGE_SIZE,
                CurseForgeClient.SORT_TOTAL_DOWNLOADS,
                "desc",
                new CurseForgeClient.CurseForgeCallback<ContentSearchResponse>() {
                    @Override
                    public void onSuccess(ContentSearchResponse result) {
                        List<CaramelCurseForgeItem> rows = new ArrayList<>();
                        if (result != null && result.data != null) {
                            for (Content content : result.data) {
                                rows.add(toItem(content));
                            }
                        }
                        main(() -> listener.onResults(rows));
                    }

                    @Override
                    public void onError(Throwable t) {
                        String message = t == null ? "" : String.valueOf(t.getMessage());
                        main(() -> listener.onError(message));
                    }
                });
    }

    /** Maps one API project onto the row the tab draws. */
    static CaramelCurseForgeItem toItem(Content content) {
        if (content == null) return new CaramelCurseForgeItem(0, "", "", "", 0);
        String author = "";
        if (content.authors != null && !content.authors.isEmpty()) {
            ContentAuthor first = content.authors.get(0);
            if (first != null && first.name != null) author = first.name;
        }
        ContentFile file = newestFile(content.latestFiles);
        return new CaramelCurseForgeItem(
                content.id,
                content.name == null ? "" : content.name,
                content.summary == null ? "" : content.summary,
                author,
                content.downloadCount,
                content.logo != null && content.logo.thumbnailUrl != null ? content.logo.thumbnailUrl : "",
                file != null ? file.id : 0,
                file != null && file.fileName != null ? file.fileName : "",
                file != null && file.downloadUrl != null ? file.downloadUrl : "",
                content.links != null && content.links.websiteUrl != null ? content.links.websiteUrl : "");
    }

    /** The newest file the API returned; CurseForge lists them newest first. */
    private static ContentFile newestFile(List<ContentFile> files) {
        if (files == null || files.isEmpty()) return null;
        return files.get(0);
    }

    /** Downloads [item]'s file into the cache and hands the file to [listener]. */
    public static void download(Context context, CaramelCurseForgeItem item, DownloadListener listener) {
        if (item == null || !item.getDownloadable()) {
            main(() -> listener.onError("no downloadable file"));
            return;
        }
        Context app = context.getApplicationContext();
        String name = item.getFileName() == null || item.getFileName().isEmpty()
                ? "curseforge_" + item.getId() + ".zip" : item.getFileName();
        File target = new File(new File(app.getCacheDir(), "curseforge"), name);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(item.getDownloadUrl()).build();
        AppExecutors.run(AppExecutors.Kind.NETWORK, "curseforge-download", () -> {
            try {
                File parent = target.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("could not create the download directory");
                }
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("HTTP " + response.code());
                    }
                    try (InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) > 0) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
                main(() -> listener.onFile(target));
            } catch (Throwable error) {
                AppLog.warnOnce(TAG, "download", "The CurseForge download failed", error);
                String message = error.getMessage() == null ? "" : error.getMessage();
                main(() -> listener.onError(message));
            }
        });
    }

    private static void main(Runnable action) {
        MainThread.post(action);
    }
}
