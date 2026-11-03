package org.chimeramc.client.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.antegg.ScriptManager;
import org.chimeramc.client.databinding.ItemScriptPackageBinding;
import org.chimeramc.client.ui.animation.DynamicAnim;

import java.util.List;

/**
 * Renders the script manager's rows.
 *
 * <p>A {@link ListAdapter} with a {@link DiffUtil.ItemCallback} so toggling one package animates
 * that row instead of repainting the list, and the row identity is the package id — a package's
 * display name can change with a manifest edit, so the id is the stable key.
 *
 * <p>A row for a package whose manifest cannot be read still renders, with only the remove action,
 * because a broken package the user cannot see is one they cannot delete.
 */
public class ScriptManagerAdapter
        extends ListAdapter<ScriptManager.ScriptInfo, ScriptManagerAdapter.RowHolder> {

    /** Row callbacks the activity supplies, so the adapter holds no Context beyond the view. */
    public interface Listener {
        void onToggle(ScriptManager.ScriptInfo info, boolean enabled);

        void onRun(ScriptManager.ScriptInfo info);

        void onRemove(ScriptManager.ScriptInfo info);
    }

    private final Listener listener;

    public ScriptManagerAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ScriptManager.ScriptInfo> DIFF =
            new DiffUtil.ItemCallback<ScriptManager.ScriptInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull ScriptManager.ScriptInfo oldItem,
                                               @NonNull ScriptManager.ScriptInfo newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ScriptManager.ScriptInfo oldItem,
                                                  @NonNull ScriptManager.ScriptInfo newItem) {
                    return oldItem.name.equals(newItem.name)
                            && oldItem.version.equals(newItem.version)
                            && oldItem.type.equals(newItem.type)
                            && oldItem.readable == newItem.readable
                            && oldItem.loadIndex == newItem.loadIndex
                            && oldItem.allDependenciesMet() == newItem.allDependenciesMet();
                }
            };

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScriptPackageBinding binding = ItemScriptPackageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RowHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        private final ItemScriptPackageBinding binding;

        RowHolder(ItemScriptPackageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ScriptManager.ScriptInfo info, Listener listener) {
            if (!info.readable) {
                binding.scriptRowName.setText(info.id);
                binding.scriptRowMeta.setText(R.string.script_manager_unreadable);
                binding.scriptRowLoadOrder.setVisibility(View.GONE);
                binding.scriptRowDependencies.setVisibility(View.GONE);
                binding.scriptRowToggle.setVisibility(View.GONE);
                binding.scriptRowRun.setVisibility(View.GONE);
                binding.scriptRowRemove.setVisibility(View.VISIBLE);
                binding.scriptRowRemove.setOnClickListener(v -> listener.onRemove(info));
                DynamicAnim.applyPressScale(binding.scriptRowRemove);
                return;
            }

            binding.scriptRowName.setText(info.name.isEmpty() ? info.id : info.name);
            String type = itemView.getContext().getString(info.isScript()
                    ? R.string.script_manager_type_script : R.string.script_manager_type_native);
            binding.scriptRowMeta.setText(itemView.getContext().getString(
                    R.string.script_manager_meta, type, info.version));
            binding.scriptRowLoadOrder.setText(itemView.getContext().getString(
                    R.string.script_manager_load_order, info.loadIndex + 1));
            binding.scriptRowLoadOrder.setVisibility(View.VISIBLE);

            if (info.dependencies.isEmpty()) {
                binding.scriptRowDependencies.setVisibility(View.GONE);
            } else {
                binding.scriptRowDependencies.setVisibility(View.VISIBLE);
                boolean met = info.allDependenciesMet();
                binding.scriptRowDependencies.setTextColor(itemView.getContext().getColor(
                        met ? R.color.text_secondary : R.color.error));
                binding.scriptRowDependencies.setText(buildDependencyLine(info));
            }

            binding.scriptRowToggle.setVisibility(View.VISIBLE);
            binding.scriptRowToggle.setOnCheckedChangeListener(null);
            binding.scriptRowToggle.setChecked(true);
            binding.scriptRowToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                    listener.onToggle(info, isChecked));

            binding.scriptRowRun.setVisibility(info.isScript() ? View.VISIBLE : View.GONE);
            binding.scriptRowRun.setOnClickListener(v -> listener.onRun(info));
            DynamicAnim.applyPressScale(binding.scriptRowRun);

            binding.scriptRowRemove.setVisibility(View.VISIBLE);
            binding.scriptRowRemove.setOnClickListener(v -> listener.onRemove(info));
            DynamicAnim.applyPressScale(binding.scriptRowRemove);
        }

        private String buildDependencyLine(ScriptManager.ScriptInfo info) {
            StringBuilder sb = new StringBuilder();
            List<ScriptManager.DependencyStatus> deps = info.dependencies;
            for (int i = 0; i < deps.size(); i++) {
                if (i > 0) sb.append('\n');
                ScriptManager.DependencyStatus dep = deps.get(i);
                String marker = dep.met ? "\u2713" : "\u2717";
                sb.append(marker).append(' ').append(dep.id);
                if (dep.range != null && !dep.range.isEmpty() && !"*".equals(dep.range)) {
                    sb.append(' ').append(dep.range);
                }
            }
            return sb.toString();
        }
    }
}
