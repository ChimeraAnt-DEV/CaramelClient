package org.chimeramc.client.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ItemSettingsContainerBinding;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.ContainerViewHolder> {

    public interface ContentBuilder {
        void build(LinearLayout container);
    }

    private final ContentBuilder builder;

    public SettingsAdapter(ContentBuilder builder) {
        this.builder = builder;
    }

    @NonNull
    @Override
    public ContainerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSettingsContainerBinding binding = ItemSettingsContainerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ContainerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContainerViewHolder holder, int position) {
        if (builder != null) {
            holder.container.removeAllViews();
            builder.build(holder.container);
        }
        android.content.Context ctx = holder.itemView.getContext();
        org.chimeramc.client.util.PersonalizationManager pm = new org.chimeramc.client.util.PersonalizationManager(ctx);
        pm.applyAccentToView(holder.itemView, ctx);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ContainerViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout container;
        public ContainerViewHolder(@NonNull ItemSettingsContainerBinding binding) {
            super(binding.getRoot());
            container = binding.settingsItems;
        }
    }
}