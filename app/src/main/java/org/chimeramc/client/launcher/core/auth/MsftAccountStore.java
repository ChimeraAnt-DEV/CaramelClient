package org.chimeramc.client.core.auth;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.chimeramc.client.util.JsonIOUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class MsftAccountStore {

    public static class MsftAccount {
        public String id;
        public String msUserId;
        public String xboxGamertag;
        public String minecraftUsername;
        public String xuid;
        public String xboxAvatarUrl;
        public long lastUpdated;
        public boolean active;
        public String serializedAuthManager;

        public MsftAccount() {}

        public MsftAccount(String id, String msUserId, String xboxGamertag, String minecraftUsername, String xuid, String xboxAvatarUrl, long lastUpdated, boolean active, String serializedAuthManager) {
            this.id = id;
            this.msUserId = msUserId;
            this.xboxGamertag = xboxGamertag;
            this.minecraftUsername = minecraftUsername;
            this.xuid = xuid;
            this.xboxAvatarUrl = xboxAvatarUrl;
            this.lastUpdated = lastUpdated;
            this.active = active;
            this.serializedAuthManager = serializedAuthManager;
        }
    }

    private static final String FILENAME = "Xal.Accounts.json";
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<MsftAccount>>(){}.getType();

    private static File getFile(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "xal");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, FILENAME);
    }

    public static synchronized List<MsftAccount> list(Context ctx) {
        File f = getFile(ctx);
        if (!f.exists()) return new ArrayList<>();
        try {
            String json = JsonIOUtils.read(f);
            if (TextUtils.isEmpty(json)) return new ArrayList<>();
            List<MsftAccount> list = GSON.fromJson(json, LIST_TYPE);
            if (list == null) return new ArrayList<>();
            // The on-disk blob is ciphertext (item 515); every caller wants plaintext, so the
            // decryption happens here rather than at each read site. A value that cannot be
            // decrypted (the Keystore key was invalidated) reads as absent, which the callers
            // already treat as "no auth material" and sign the account out cleanly.
            for (MsftAccount a : list) {
                if (a.serializedAuthManager != null) {
                    a.serializedAuthManager = SecretVault.decrypt(a.serializedAuthManager);
                }
            }
            return list;
        } catch (Exception ex) {
            Log.w("XALExport", "Failed to read " + f.getAbsolutePath(), ex);
            return new ArrayList<>();
        }
    }

    private static synchronized void save(Context ctx, List<MsftAccount> list) {
        File f = getFile(ctx);
        try {
            // Encrypt a copy for disk so the in-memory objects stay plaintext for the callers
            // (item 515). Encrypting the live list in place would leave the process holding
            // ciphertext after the first save.
            List<MsftAccount> onDisk = new ArrayList<>(list.size());
            for (MsftAccount a : list) {
                MsftAccount copy = new MsftAccount(a.id, a.msUserId, a.xboxGamertag,
                        a.minecraftUsername, a.xuid, a.xboxAvatarUrl, a.lastUpdated, a.active,
                        SecretVault.encrypt(a.serializedAuthManager));
                onDisk.add(copy);
            }
            JsonIOUtils.write(f, GSON.toJson(onDisk));
        } catch (Exception ex) {
            Log.w("XALExport", "Failed to write " + f.getAbsolutePath(), ex);
        }
    }

    public static synchronized MsftAccount addOrUpdate(Context ctx, String msUserId, String gamertag, String minecraftUsername, String xuid, String avatarUrl, String serializedAuthManager) {
        List<MsftAccount> list = list(ctx);
        MsftAccount target = null;
        for (MsftAccount a : list) {
            if (msUserId != null && msUserId.equals(a.msUserId)) {
                target = a; break;
            }
        }
        if (target == null) {
            target = new MsftAccount(UUID.randomUUID().toString(), msUserId, gamertag, minecraftUsername, xuid, avatarUrl, System.currentTimeMillis(), list.isEmpty(), serializedAuthManager);
            list.add(target);
        } else {
            if (!TextUtils.isEmpty(gamertag)) target.xboxGamertag = gamertag;
            if (!TextUtils.isEmpty(minecraftUsername)) target.minecraftUsername = minecraftUsername;
            if (!TextUtils.isEmpty(xuid)) target.xuid = xuid;
            if (!TextUtils.isEmpty(avatarUrl)) target.xboxAvatarUrl = avatarUrl;
            if (serializedAuthManager != null) target.serializedAuthManager = serializedAuthManager;
            target.lastUpdated = System.currentTimeMillis();
        }
        save(ctx, list);
        org.chimeramc.client.core.auth.storage.XalExporter.exportActiveAccount(ctx);
        return target;
    }

    public static synchronized void remove(Context ctx, String id) {
        List<MsftAccount> list = list(ctx);
        Iterator<MsftAccount> it = list.iterator();
        while (it.hasNext()) {
            MsftAccount a = it.next();
            if (a.id.equals(id)) {
                it.remove();
            }
        }

        boolean hasActive = false;
        for (MsftAccount a : list) if (a.active) { hasActive = true; break; }
        if (!hasActive && !list.isEmpty()) list.get(0).active = true;
        save(ctx, list);
        if (list.isEmpty()) {
            // No accounts left: drop the Keystore key so a device handed on (or a restored backup)
            // cannot decrypt anything this vault ever wrote (item 515).
            SecretVault.deleteKey();
        }
        org.chimeramc.client.core.auth.storage.XalExporter.exportActiveAccount(ctx);
    }

    public static synchronized void setActive(Context ctx, String id) {
        List<MsftAccount> list = list(ctx);
        for (MsftAccount a : list) {
            a.active = a.id.equals(id);
        }
        save(ctx, list);
        org.chimeramc.client.core.auth.storage.XalExporter.exportActiveAccount(ctx);
    }

    public static synchronized MsftAccount find(Context ctx, String id) {
        for (MsftAccount a : list(ctx)) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }
}