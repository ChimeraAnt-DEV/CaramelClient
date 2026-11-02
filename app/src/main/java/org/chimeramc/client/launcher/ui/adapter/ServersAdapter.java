package org.chimeramc.client.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ItemServerBinding;
import org.chimeramc.client.core.content.ServerItem;
import org.chimeramc.client.core.content.ServerFavourites;
import org.chimeramc.client.core.content.ServerPongInfo;
import org.chimeramc.client.ui.components.ListDiff;
import org.chimeramc.client.ui.components.ListDiffDispatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server rows carry a live ping line (G14). Ping results arrive asynchronously (from a background
 * executor in the host activity) keyed by {@code ip:port}, so a row can redraw without rebuilding
 * the whole list and a result does not get lost when the adapter is rebound.
 */
public class ServersAdapter extends RecyclerView.Adapter<ServersAdapter.ViewHolder> {

    private List<ServerItem> servers;
    private final OnServerClickListener listener;
    /** Ping outcomes per server key ("ip:port"); absent means "yet to ping". */
    private final Map<String, ServerPongInfo> pongs = new HashMap<>();
    private final Map<String, Boolean> pingFailures = new HashMap<>();
    /** The selected instance's version code, used for the compatibility verdict. */
    private String selectedVersionCode;
    /** Favourite server keys ("ip:port"), read from {@link ServerFavourites} (item 254). */
    private java.util.Set<String> favourites = java.util.Collections.emptySet();

    public interface OnServerClickListener {
        void onDeleteClick(ServerItem server);

        /** The star was tapped on this row; the host persists the change. */
        void onFavouriteToggle(ServerItem server);
    }

    public ServersAdapter(List<ServerItem> servers, OnServerClickListener listener) {
        this.servers = servers;
        this.listener = listener;
    }

    /**
     * Sets the favourite keys and re-sorts, favourites first (item 254).
     *
     * <p>The order is a presentation rule, so it is applied where the list is rendered rather than
     * where it is stored: the game's own {@code external_servers.txt} keeps the order it was written
     * in, and a user who un-favourites a server sees it return to that order instead of to the
     * bottom. The sort is stable, so the stored order is preserved inside each group.
     */
    public void setFavourites(java.util.Set<String> favouriteKeys) {
        this.favourites = favouriteKeys != null
                ? java.util.Collections.unmodifiableSet(new java.util.HashSet<>(favouriteKeys))
                : java.util.Collections.emptySet();
        if (servers != null && !servers.isEmpty()) {
            List<ServerItem> sorted = ServerFavourites.order(servers, this.favourites);
            if (!sorted.equals(servers)) {
                servers = sorted;
                notifyItemRangeChanged(0, servers.size());
            }
        }
    }

    /**
     * Replaces the list, animating only what changed.
     *
     * <p>Pongs are keyed by ip:port and stay valid across re-binds: a filter or a refresh does not
     * justify losing every ping line. A server genuinely deleted and re-added with the same address
     * re-uses the old reading, which is the better failure mode. The diff preserves that, because a
     * row that survives the update is a change rather than a remove and an insert.
     */
    public void updateData(List<ServerItem> newServers) {
        List<ServerItem> raw = newServers != null ? newServers : java.util.Collections.emptyList();
        // Favourites first, then the stored order (item 254). Ordering here, before the diff, is
        // what keeps a starred row from jumping position when a search filter is applied.
        List<ServerItem> next = ServerFavourites.order(raw, favourites);
        ListDiff.Result diff = ListDiff.compute(this.servers, next,
                ServersAdapter::keyFor,
                (old, fresh) -> old.name.equals(fresh.name));
        this.servers = next;
        ListDiffDispatch.apply(this, diff);
    }

    /** The compatibility verdict is per row, so every row's status line may have changed. */
    public void setSelectedVersionCode(String versionCode) {
        this.selectedVersionCode = versionCode;
        if (getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    /** Called from the UI thread when a ping lands; redraws only the matching row. */
    public void onPingResult(ServerItem server, ServerPongInfo pong, boolean failed) {
        String key = keyFor(server);
        if (failed || pong == null) {
            pingFailures.put(key, Boolean.TRUE);
            pongs.remove(key);
        } else {
            pongs.put(key, pong);
            pingFailures.remove(key);
        }
        int index = indexOf(server);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServerBinding binding = ItemServerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerItem server = servers.get(position);
        holder.nameText.setText(server.name);
        holder.ipText.setText(server.ip + ":" + server.port);
        renderStatus(holder, server);

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(server);
            }
        });
        boolean favourite = favourites.contains(keyFor(server));
        holder.favouriteButton.setSelected(favourite);
        holder.favouriteButton.setContentDescription(holder.itemView.getContext().getString(
                favourite ? R.string.server_unfavourite : R.string.server_favourite,
                server.name));
        holder.favouriteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavouriteToggle(server);
            }
        });
        org.chimeramc.client.ui.animation.DynamicAnim.applyPressScale(holder.favouriteButton);

        org.chimeramc.client.util.PersonalizationManager pm =
                new org.chimeramc.client.util.PersonalizationManager(holder.itemView.getContext());
        pm.applyGlassToView(holder.itemView);
        pm.applyAccentToView(holder.itemView, holder.itemView.getContext());
    }

    private void renderStatus(ViewHolder holder, ServerItem server) {
        TextView status = holder.statusText;
        if (status == null) return;
        String key = keyFor(server);

        if (Boolean.TRUE.equals(pingFailures.get(key))) {
            setStatus(status, holder.itemView.getContext()
                    .getString(R.string.server_ping_offline), 0xFF8B949E);
            return;
        }
        ServerPongInfo pong = pongs.get(key);
        if (pong == null) {
            setStatus(status, holder.itemView.getContext()
                    .getString(R.string.server_ping_checking), 0xFF8B949E);
            return;
        }
        if (pong.parseError || !pong.isBedrock()) {
            setStatus(status, holder.itemView.getContext()
                    .getString(R.string.server_ping_offline), 0xFF8B949E);
            return;
        }

        String summary = pong.motd + " · " + pong.playersOnline + "/" + pong.maxPlayers;
        ServerPongInfo.Compatibility compatibility = pong.compatibilityWith(selectedVersionCode);
        if (compatibility == ServerPongInfo.Compatibility.COMPATIBLE) {
            setStatus(status, summary, 0xFF35DC84);
        } else if (compatibility == ServerPongInfo.Compatibility.MISMATCH) {
            String line = summary + " · " + holder.itemView.getContext()
                    .getString(R.string.server_ping_version_mismatch, pong.gameVersion);
            setStatus(status, line, 0xFFFFC944);
        } else {
            setStatus(status, summary, 0xFF8B949E);
        }
    }

    private static void setStatus(TextView status, String text, int color) {
        status.setText(text);
        status.setTextColor(color);
    }

    private int indexOf(ServerItem server) {
        if (servers == null) return -1;
        for (int i = 0; i < servers.size(); i++) {
            if (sameServer(servers.get(i), server)) return i;
        }
        return -1;
    }

    private static boolean sameServer(ServerItem a, ServerItem b) {
        return a.ip.equals(b.ip) && a.port == b.port && a.name.equals(b.name);
    }

    public static String keyFor(ServerItem server) {
        return server.ip + ":" + server.port;
    }

    @Override
    public int getItemCount() {
        return servers == null ? 0 : servers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView ipText;
        TextView statusText;
        Button deleteButton;
        android.widget.ImageButton favouriteButton;

        ViewHolder(ItemServerBinding binding) {
            super(binding.getRoot());
            nameText = binding.serverName;
            ipText = binding.serverIp;
            statusText = binding.serverStatus;
            deleteButton = binding.serverDeleteButton;
            favouriteButton = binding.serverFavouriteButton;
        }
    }
}
