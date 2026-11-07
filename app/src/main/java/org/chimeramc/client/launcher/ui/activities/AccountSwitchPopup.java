package org.chimeramc.client.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.chimeramc.client.R;
import org.chimeramc.client.util.AccountTextUtils;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.auth.MsftAuthManager;
import org.chimeramc.client.settings.LowLatencyNetworkManager;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.util.DialogUtils;
import org.chimeramc.client.ui.dialogs.LoadingDialog;
import org.chimeramc.client.util.ApiLevels;
import org.chimeramc.client.util.AppLog;

import java.util.ArrayList;
import java.util.List;

/**
 * The account switcher popup anchored under the Launch-tab header avatar.
 *
 * <p>Extracted from {@code MainActivity} so that screen keeps real headroom under the file-size
 * budget. The popup is a self-contained unit: it lists the accounts other than the active one,
 * switches to one on tap (silently when its token is still fresh, through a re-auth otherwise), and
 * offers a route to the full accounts screen.
 *
 * <p>The host supplies the few side effects the popup cannot perform itself, because they belong to
 * the Activity: the background executor, the "signing in" dialog, and the header refresh after a
 * switch.
 */
final class AccountSwitchPopup {

    /** The Activity-side effects the popup depends on. */
    interface Host {
        /** Runs account work off the UI thread, tolerating a torn-down Activity. */
        void runOnAccountExecutor(Runnable work);

        /** The shared "signing in" dialog, created on first use. */
        LoadingDialog loadingDialog();

        /** Stores the shared "signing in" dialog after {@code DialogUtils.ensure} replaced it. */
        void setLoadingDialog(LoadingDialog dialog);

        /** Refreshes the header avatar, name and sign-in button after a switch. */
        void refreshHeader();

        /** The avatar the header is currently showing, or null; used to seed the popup header. */
        android.graphics.drawable.Drawable headerAvatarDrawable();
    }

    private final Activity activity;
    private final Host host;

    AccountSwitchPopup(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    /**
     * Shows the popup anchored under {@code anchor}.
     *
     * <p>The popup dims the screen behind it with a scrim that is removed on dismiss, and every row
     * is staggered in so the list reads as opening rather than appearing.
     */
    void show(View anchor) {
        List<MsftAccountStore.MsftAccount> list = MsftAccountStore.list(activity);

        View content = LayoutInflater.from(activity).inflate(R.layout.popup_account_switch, null);
        androidx.recyclerview.widget.RecyclerView recyclerAccounts =
                content.findViewById(R.id.recycler_accounts);
        TextView manageAction = content.findViewById(R.id.manage_action);
        com.microsoft.xbox.idp.toolkit.CircleImageView headerAvatar =
                content.findViewById(R.id.header_avatar);
        View headerContainer = content.findViewById(R.id.header_container);
        TextView headerName = content.findViewById(R.id.header_name);

        TypedValue outValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        int selectableRes = outValue.resourceId;

        float density = activity.getResources().getDisplayMetrics().density;
        int paddingH = (int) (16 * density);
        int paddingV = (int) (12 * density);
        int paddingR = (int) (12 * density);

        MsftAccountStore.MsftAccount active = activeAccount();
        headerName.setText(AccountTextUtils.displayNameOrNotSigned(activity, active));
        bindHeaderAvatar(headerAvatar, active);

        final PopupWindow popup = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        content.setAlpha(0f);
        content.setTranslationY(24f);
        if (headerContainer != null) {
            headerContainer.setAlpha(0f);
            headerContainer.setTranslationY(8f * density);
        }
        if (headerAvatar != null) {
            headerAvatar.setAlpha(0f);
            headerAvatar.setScaleX(0.94f);
            headerAvatar.setScaleY(0.94f);
        }
        if (headerName != null) {
            headerName.setAlpha(0f);
            headerName.setTranslationY(6f * density);
        }
        if (manageAction != null) {
            manageAction.setAlpha(0f);
            manageAction.setTranslationX(6f * density);
        }
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (ApiLevels.atLeast(21)) popup.setElevation(8f);

        final ViewGroup root = activity.findViewById(android.R.id.content);
        final View scrim = new View(activity);
        scrim.setBackgroundColor(ContextCompat.getColor(activity, R.color.scrim));
        scrim.setClickable(true);
        scrim.setOnClickListener(v -> popup.dismiss());
        root.addView(scrim, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(120).start();

        final List<MsftAccountStore.MsftAccount> displayList = new ArrayList<>();
        for (MsftAccountStore.MsftAccount a : list) {
            if (active == null || !android.text.TextUtils.equals(a.id, active.id)) displayList.add(a);
        }

        recyclerAccounts.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(activity));
        recyclerAccounts.setAdapter(new AccountAdapter(popup, displayList, paddingH, paddingV, paddingR, selectableRes));

        if (displayList.size() > 2) {
            recyclerAccounts.getLayoutParams().height = (int) ((48 * 2 + 16) * density);
        } else {
            recyclerAccounts.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        manageAction.setOnClickListener(v -> {
            popup.dismiss();
            activity.startActivity(new Intent(activity, AccountsActivity.class));
        });
        DynamicAnim.applyPressScale(manageAction);

        popup.setOnDismissListener(() -> {
            if (root != null && scrim != null) {
                scrim.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                    try {
                        root.removeView(scrim);
                    } catch (Exception ignored) {
                        AppLog.warnOnce("GlowberryClient", "AccountSwitchPopup:scrim",
                                "Ignored a failure removing the scrim", ignored);
                    }
                }).start();
            }
        });

        int edgeMargin = (int) (4 * density);
        popup.showAsDropDown(anchor, -edgeMargin, edgeMargin / 4, Gravity.END);

        DynamicAnim.springAlphaTo(content, 1f).start();
        DynamicAnim.springTranslationYTo(content, 0f).start();
        recyclerAccounts.post(() -> DynamicAnim.staggerRecyclerChildren(recyclerAccounts));
        if (headerContainer != null) {
            DynamicAnim.springAlphaTo(headerContainer, 1f).start();
            DynamicAnim.springTranslationYTo(headerContainer, 0f).start();
        }
        if (headerAvatar != null) {
            DynamicAnim.springAlphaTo(headerAvatar, 1f).start();
            DynamicAnim.springScaleXTo(headerAvatar, 1f).start();
            DynamicAnim.springScaleYTo(headerAvatar, 1f).start();
        }
        if (headerName != null) {
            DynamicAnim.springAlphaTo(headerName, 1f).start();
            DynamicAnim.springTranslationYTo(headerName, 0f).start();
        }
        if (manageAction != null) {
            DynamicAnim.springAlphaTo(manageAction, 1f).start();
            DynamicAnim.springTranslationXTo(manageAction, 0f).start();
        }
    }

    /** The account flagged active, or null when nobody is signed in. */
    private MsftAccountStore.MsftAccount activeAccount() {
        for (MsftAccountStore.MsftAccount a : MsftAccountStore.list(activity)) {
            if (a.active) return a;
        }
        return null;
    }

    /**
     * Seeds the popup header avatar from the header's already-loaded drawable, or fetches it.
     *
     * <p>Reusing the loaded drawable avoids a second network round-trip for an image the header is
     * already showing; the fetch is the fallback for the moment before the header has one.
     */
    private void bindHeaderAvatar(com.microsoft.xbox.idp.toolkit.CircleImageView headerAvatar,
                                  MsftAccountStore.MsftAccount active) {
        android.graphics.drawable.Drawable shown = host.headerAvatarDrawable();
        if (shown != null) {
            headerAvatar.setImageDrawable(shown);
            return;
        }
        if (active == null) return;
        final String url = AccountTextUtils.sanitizeUrl(active.xboxAvatarUrl);
        if (url == null) return;
        host.runOnAccountExecutor(() -> {
            try {
                okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder();
                LowLatencyNetworkManager.configure(clientBuilder);
                okhttp3.OkHttpClient client = clientBuilder.build();
                final android.graphics.Bitmap bmp;
                try (okhttp3.Response imgResp = client.newCall(
                        new okhttp3.Request.Builder().url(url).build()).execute()) {
                    bmp = (imgResp.isSuccessful() && imgResp.body() != null)
                            ? android.graphics.BitmapFactory.decodeStream(imgResp.body().byteStream())
                            : null;
                }
                activity.runOnUiThread(() -> {
                    if (bmp != null) headerAvatar.setImageBitmap(bmp);
                });
            } catch (Throwable ignored) {
                AppLog.warnOnce("GlowberryClient", "AccountSwitchPopup:avatar",
                        "Ignored a failure loading the popup avatar", ignored);
            }
        });
    }

    /** The account rows: tap switches, silently when the token is fresh, else re-auths. */
    private final class AccountAdapter
            extends androidx.recyclerview.widget.RecyclerView.Adapter<AccountAdapter.RowHolder> {

        private final PopupWindow popup;
        private final List<MsftAccountStore.MsftAccount> accounts;
        private final int paddingH;
        private final int paddingV;
        private final int paddingR;
        private final int selectableRes;

        AccountAdapter(PopupWindow popup, List<MsftAccountStore.MsftAccount> accounts,
                       int paddingH, int paddingV, int paddingR, int selectableRes) {
            this.popup = popup;
            this.accounts = accounts;
            this.paddingH = paddingH;
            this.paddingV = paddingV;
            this.paddingR = paddingR;
            this.selectableRes = selectableRes;
        }

        final class RowHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final TextView tv;

            RowHolder(TextView t) {
                super(t);
                this.tv = t;
            }
        }

        @Override
        public RowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView row = new TextView(parent.getContext());
            row.setLayoutParams(new androidx.recyclerview.widget.RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.on_surface));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.setPadding(paddingH, paddingV, paddingR, paddingV);
            row.setBackgroundResource(selectableRes);
            return new RowHolder(row);
        }

        @Override
        public void onBindViewHolder(RowHolder holder, int position) {
            MsftAccountStore.MsftAccount account = accounts.get(position);
            holder.tv.setText(AccountTextUtils.titleOrUnknown(account));
            holder.tv.setOnClickListener(v -> switchTo(account));
        }

        @Override
        public int getItemCount() {
            return accounts.size();
        }

        /**
         * Switches the active account.
         *
         * <p>A token younger than seven days is still valid, so the switch is instant; an older one
         * is re-authenticated first, with the "signing in" dialog up for the duration.
         */
        private void switchTo(MsftAccountStore.MsftAccount account) {
            popup.dismiss();

            MsftAccountStore.setActive(activity, account.id);

            if (AccountTextUtils.isRecentlyUpdated(account, 7)) {
                activity.runOnUiThread(() -> {
                    DialogUtils.dismissQuietly(host.loadingDialog());
                    String statusName = AccountTextUtils.displayNameOrNotSigned(activity, account);
                    Toast.makeText(activity, activity.getString(R.string.ms_login_success, statusName),
                            Toast.LENGTH_SHORT).show();
                    host.refreshHeader();
                });
                return;
            }

            host.setLoadingDialog(DialogUtils.ensure(activity, host.loadingDialog()));
            DialogUtils.showWithMessage(host.loadingDialog(),
                    activity.getString(R.string.ms_login_auth_xbox_device));

            host.runOnAccountExecutor(() -> {
                try {
                    BedrockAuthManager authManager = MsftAuthManager.refreshAndAuth(account);
                    MsftAuthManager.saveAccountOrThrow(activity, authManager);
                    MsftAccountStore.setActive(activity, account.id);
                    String minecraftUsername = authManager.getMinecraftCertificateChain()
                            .getUpToDate().getIdentityDisplayName();

                    activity.runOnUiThread(() -> {
                        DialogUtils.dismissQuietly(host.loadingDialog());
                        String statusName = minecraftUsername != null
                                ? minecraftUsername : activity.getString(R.string.not_signed_in);
                        Toast.makeText(activity, activity.getString(R.string.ms_login_success, statusName),
                                Toast.LENGTH_SHORT).show();
                        host.refreshHeader();
                    });
                } catch (Throwable e) {
                    // Throwable, not Exception: this handler is the only thing that dismisses the
                    // "signing in" dialog, so an Error escaping it left that dialog up forever with
                    // no sign-in and no message.
                    activity.runOnUiThread(() -> {
                        DialogUtils.dismissQuietly(host.loadingDialog());
                        Toast.makeText(activity,
                                activity.getString(R.string.ms_login_failed_detail, MsftAuthManager.describeError(e)),
                                Toast.LENGTH_LONG).show();
                        host.refreshHeader();
                    });
                }
            });
        }
    }
}
