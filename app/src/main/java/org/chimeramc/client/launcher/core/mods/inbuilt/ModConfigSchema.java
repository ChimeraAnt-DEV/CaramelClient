package org.chimeramc.client.core.mods.inbuilt;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.core.mods.inbuilt.overlay.HitTimingSolver;

import java.util.ArrayList;
import java.util.List;

/**
 * The config layer of the inbuilt modules: the config keys, the {@link UnifiedMod.ConfigEntry} list
 * for a module, and the {@link RuntimeConfigSchema} a module's dialog renders.
 *
 * <p>Split out of {@code InbuiltModuleProvider} (the file-size ratchet): the provider declares the
 * modules and applies their settings, while everything about a module's *config shape* lives here.
 * The keys are package-private so the provider's setter switch and this class cannot drift apart.
 */
final class ModConfigSchema {

    private ModConfigSchema() {
    }

    static final String CFG_OVERLAY_SIZE = "overlay_size";
    static final String CFG_OVERLAY_OPACITY = "overlay_opacity";
    static final String CFG_OVERLAY_LOCK = "overlay_lock";
    static final String CFG_OVERLAY_SHOW_EVERYWHERE = "overlay_show_everywhere";
    static final String CFG_FRAME_GRAPH = "overlay_frame_graph";
    static final String CFG_FPS_STATS = "overlay_fps_stats";
    static final String CFG_FPS_STATS_WINDOW = "overlay_fps_stats_window";
    static final String CFG_AUTO_SPRINT_KEYBIND = "auto_sprint_keybind";
    static final String CFG_CURSOR_SENSITIVITY = "cursor_sensitivity";
    static final String CFG_ZOOM_LEVEL = "zoom_level";
    static final String CFG_ZOOM_TRANSITION = "zoom_transition";
    static final String CFG_ZOOM_KEYBIND = "zoom_keybind";
    static final String CFG_ZOOM_SCROLL_SENSITIVITY = "zoom_scroll_sensitivity";
    static final String CFG_ZOOM_SMOOTHING = "zoom_smoothing";
    static final String CFG_SNAPLOOK_KEYBIND = "snaplook_keybind";
    static final String CFG_SNAPLOOK_RETURN_SPEED = "snaplook_return_speed";
    static final String CFG_GYRO_SENSITIVITY_X = "gyro_sensitivity_x";
    static final String CFG_GYRO_SENSITIVITY_Y = "gyro_sensitivity_y";
    static final String CFG_GYRO_INVERT_X = "gyro_invert_x";
    static final String CFG_GYRO_INVERT_Y = "gyro_invert_y";
    static final String CFG_GYRO_DEADZONE = "gyro_deadzone";
    static final String CFG_HOTBAR_ITEM_ICONS = "hotbar_item_icons";
    static final String CFG_OFFHAND_MODE = "offhand_mode";
    static final String CFG_OFFHAND_INVENTORY_KEY = "offhand_inventory_key";
    static final String CFG_OFFHAND_SWAP_KEY = "offhand_swap_key";
    static final String CFG_OFFHAND_USE_KEY = "offhand_use_key";
    static final String CFG_OFFHAND_KEYBIND = "offhand_keybind";
    static final String CFG_HOTBAR_SLOT_PREFIX = "hotbar_slot_";
    static final String CFG_HOTBAR_SLOT_ENABLED = "enabled";
    static final String CFG_HOTBAR_SLOT_SIZE = "size";
    static final String CFG_HOTBAR_SLOT_OPACITY = "opacity";
    static final String CFG_AIM_SMOOTHING = "aim_smoothing";
    static final String CFG_AIM_CROSSHAIR = "aim_crosshair";
    static final String CFG_AIM_FLASH = "aim_flash";
    static final String CFG_AIM_SENSITIVITY = "aim_sensitivity";
    static final String CFG_AIM_CROSSHAIR_STYLE = "aim_crosshair_style";
    static final String CFG_AIM_CROSSHAIR_COLOR = "aim_crosshair_color";
    static final String CFG_ARMOR_HUD_SHOW_TARGET = "armor_hud_show_target";
    static final String CFG_ARMOR_HUD_SHOW_ENCHANTS = "armor_hud_show_enchants";
    static final String CFG_ARMOR_HUD_STACKED = "armor_hud_stacked";
    static final String CFG_ARMOR_HUD_REFRESH_MS = "armor_hud_refresh_ms";
    static final String CFG_CRYSTAL_MIN_SELF_HP = "crystal_min_self_hp";
    static final String CFG_CRYSTAL_MAX_RANGE = "crystal_max_range";
    static final String CFG_CRYSTAL_PLACEMENT_DELAY_MS = "crystal_placement_delay_ms";
    static final String CFG_CRYSTAL_MANUAL_ASSIST = "crystal_manual_assist";
    static final String CFG_CRYSTAL_KEYBIND = "crystal_keybind";
    static final String CFG_HITREG_SENSITIVITY = "hitreg_sensitivity";
    static final String CFG_HITREG_SMOOTHING = "hitreg_smoothing";
    static final String CFG_HITREG_PREDICTION = "hitreg_prediction";
    static final String CFG_HITREG_HAPTIC = "hitreg_haptic";
    static final String CFG_HIT_TIMING_COOLDOWN_MS = "hit_timing_cooldown_ms";
    static final String CFG_HIT_TIMING_SHOW_COMBO = "hit_timing_show_combo";
    static final String CFG_HIT_TIMING_SHOW_BAR = "hit_timing_show_bar";
    static final String CFG_HITBOX_SHOW_PLAYERS = "hitbox_show_players";
    static final String CFG_HITBOX_SHOW_MOBS = "hitbox_show_mobs";
    static final String CFG_HITBOX_SHOW_ITEMS = "hitbox_show_items";
    static final String CFG_HITBOX_SHOW_PROJECTILES = "hitbox_show_projectiles";
    static final String CFG_HITBOX_SHOW_THROWN_ITEMS = "hitbox_show_thrown_items";
    static final String CFG_HITBOX_SHOW_LOOK_LINE = "hitbox_show_look_line";
    static final String CFG_HITBOX_SHOW_CRIT_LINE = "hitbox_show_crit_line";
    static final String CFG_HITBOX_SHOW_COMBO_BOX = "hitbox_show_combo_box";
    static final String CFG_HITBOX_SHOW_TARGET_BOX = "hitbox_show_target_box";
    static final String CFG_VOICE_RANGE = "voice_range_blocks";
    static final String CFG_VOICE_VOLUME = "voice_volume_percent";
    static final String CFG_VOICE_CHANNEL = "voice_channel";
    static final String CFG_VOICE_MIC = "voice_mic_enabled";
    static final String CFG_VOICE_ICON_STYLE = "voice_icon_style";
    static final String CFG_VOICE_ICON_ANIMATE = "voice_icon_animate";
    static final String CFG_VOICE_ICON_NAMETAG = "voice_icon_show_nametag";
    // PvP Suite (V1.1): overlay-only visual modules, filed under the General tab.
    static final String CFG_REACH_POSITION = "reach_indicator_position";
    static final String CFG_TRAJECTORY_COLOR = "trajectory_color";
    static final String CFG_HIT_PREDICTION_LOOKAHEAD = "hit_prediction_lookahead_ms";
    static final String CFG_KILL_EFFECT_STYLE = "kill_effect_style";
    static final String CFG_KILL_EFFECT_COLOR = "kill_effect_color";
    static final String CFG_CATALOG_CHAT_MACRO = "catalog_chat_macro";
    static final String CFG_CATALOG_COMMAND_BIND = "catalog_command_bind";

    /** A catalogue module's own options: macro text, or the readout's overlay appearance. */
    static List<UnifiedMod.ConfigEntry> createCatalogConfigs(Context context,
                                                                     InbuiltModManager manager,
                                                                     String modId) {
        List<UnifiedMod.ConfigEntry> configs = new ArrayList<>();
        if ("util_chat_macro".equals(modId)) {
            configs.add(config(CFG_CATALOG_CHAT_MACRO,
                    context.getString(R.string.catalog_macro_text),
                    UnifiedMod.ConfigType.TEXT, "", "", "",
                    manager.getCatalogChatMacro()));
        } else if ("util_command_bind".equals(modId)) {
            configs.add(config(CFG_CATALOG_COMMAND_BIND,
                    context.getString(R.string.catalog_command_text),
                    UnifiedMod.ConfigType.TEXT, "", "", "",
                    manager.getCatalogCommandBind()));
        }
        if (org.chimeramc.client.core.mods.inbuilt.overlay.CatalogModuleHost
                .isReadout(modId)) {
            configs.add(config(CFG_OVERLAY_SIZE,
                    context.getString(R.string.mod_config_overlay_button_size_dp),
                    UnifiedMod.ConfigType.SLIDER_INT, "56", "20", "100",
                    String.valueOf(manager.getOverlayButtonSize(modId))));
            configs.add(config(CFG_OVERLAY_OPACITY,
                    context.getString(R.string.mod_config_overlay_opacity_percent),
                    UnifiedMod.ConfigType.SLIDER_INT, "100", "0", "100",
                    String.valueOf(manager.getOverlayOpacity(modId))));
            configs.add(config(CFG_OVERLAY_LOCK,
                    context.getString(R.string.overlay_button_lock),
                    UnifiedMod.ConfigType.TOGGLE, "false", "", "",
                    String.valueOf(manager.isOverlayLocked(modId))));
            configs.add(config(CFG_OVERLAY_SHOW_EVERYWHERE,
                    context.getString(R.string.mod_config_overlay_show_everywhere),
                    UnifiedMod.ConfigType.TOGGLE, "false", "", "",
                    String.valueOf(manager.isOverlayShowEverywhere(modId))));
        }
        return configs;
    }

    /**
     * A catalogue module's config dialog: its own content/display options plus a scope note that
     * says which route the module uses, so a working module does not read as a roadmap stub.
     */
    static RuntimeConfigSchema createCatalogConfigSchema(Context context, UnifiedMod mod) {
        String modId = mod.getId();
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            boolean readout = org.chimeramc.client.core.mods.inbuilt.overlay
                    .CatalogModuleHost.isReadout(modId);
            categories.put(configCategory(context, "content", R.string.mod_config_category_behavior));
            if (readout) {
                categories.put(configCategory(context, "display", R.string.mod_config_category_appearance));
            }
            if ("util_chat_macro".equals(modId)) {
                nodes.put(configNode(context, mod, CFG_CATALOG_CHAT_MACRO, "content"));
            } else if ("util_command_bind".equals(modId)) {
                nodes.put(configNode(context, mod, CFG_CATALOG_COMMAND_BIND, "content"));
            }
            if (readout) {
                nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "display"));
            }
            int noteRes = catalogScopeNoteRes(modId);
            if (noteRes != 0) {
                nodes.put(scopeNoteNode(context, mod, noteRes, "content"));
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "content")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build catalogue config schema", e);
        }
    }

    private static int catalogScopeNoteRes(String modId) {
        switch (org.chimeramc.client.core.mods.inbuilt.catalog.PvpModuleScopes.scopeOf(modId)) {
            case GAME_OPTIONS:
                return R.string.catalog_options_scope_note;
            case LOCAL_FEED:
            case PEER_FEED:
                return R.string.catalog_local_scope_note;
            case DEVICE_INPUT:
                return R.string.catalog_scope_note;
            default:
                return 0;
        }
    }

    static RuntimeConfigSchema createLocalConfigSchema(Context context, UnifiedMod mod) {
        String modId = mod.getId();
        if (ModIds.ARMOR_HUD.equals(modId) || ModIds.CRYSTAL_OPTIMIZER.equals(modId)
                || ModIds.HIT_REGISTRATION.equals(modId) || ModIds.HIT_TIMING.equals(modId)
                || ModIds.HITBOX.equals(modId)) {
            return createCombatConfigSchema(context, mod);
        }
        if (ModIds.VOICE_CHAT.equals(modId)) {
            return createVoiceConfigSchema(context, mod);
        }
        if (ModIds.REACH_INDICATOR.equals(modId) || ModIds.TRAJECTORY_PREDICTION.equals(modId)
                || ModIds.HIT_PREDICTION.equals(modId) || ModIds.KILL_EFFECTS.equals(modId)) {
            return createPvpSuiteConfigSchema(context, mod);
        }
        if (ModIds.OFFHAND.equals(modId)) {
            return createOffhandConfigSchema(context, mod);
        }
        if (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.isFpsModule(modId)) {
            return createFpsConfigSchema(context, mod);
        }
        if (org.chimeramc.client.core.mods.render.PresentFrameCatalog.isPresentFrameModule(modId)) {
            return createPresentFrameConfigSchema(context, mod);
        }
        boolean hotbar = ModIds.HOTBAR_SLOT.equals(mod.getId());
        if (!hotbar && !ModIds.GYRO.equals(mod.getId())) return null;
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            if (hotbar) {
                categories.put(configCategory(context, "slots", R.string.mod_config_category_slots));
                categories.put(configCategory(context, "appearance", R.string.mod_config_category_appearance));
                categories.put(configCategory(context, "behavior", R.string.mod_config_category_behavior));
                nodes.put(configNode(context, mod, CFG_HOTBAR_ITEM_ICONS, "slots"));
                JSONArray slots = new JSONArray();
                for (int slot = 1; slot <= 9; slot++) {
                    String key = hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_ENABLED);
                    slots.put(new JSONObject().put("key", key).put("value", key)
                            .put("label", mod.findConfigEntry(key).displayName));
                    String section = "slot_" + slot;
                    nodes.put(new JSONObject().put("id", section).put("type", "section")
                            .put("category", "appearance").put("collapsible", true)
                            .put("title", context.getString(R.string.mod_config_hotbar_slot_section, slot)));
                    nodes.put(configNode(context, mod, hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_SIZE), "appearance")
                            .put("section", section));
                    nodes.put(configNode(context, mod, hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_OPACITY), "appearance")
                            .put("section", section));
                }
                nodes.put(new JSONObject().put("id", "visible_slots").put("type", "toggle_group")
                        .put("category", "slots").put("title", context.getString(R.string.mod_config_visible_slots))
                        .put("options", slots));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "behavior"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "behavior"));
            } else {
                categories.put(configCategory(context, "motion", R.string.mod_config_category_motion));
                categories.put(configCategory(context, "button", R.string.mod_config_category_button));
                nodes.put(configNode(context, mod, CFG_GYRO_SENSITIVITY_X, "motion"));
                nodes.put(configNode(context, mod, CFG_GYRO_SENSITIVITY_Y, "motion"));
                nodes.put(configNode(context, mod, CFG_GYRO_INVERT_X, "motion"));
                nodes.put(configNode(context, mod, CFG_GYRO_INVERT_Y, "motion"));
                nodes.put(configNode(context, mod, CFG_GYRO_DEADZONE, "motion"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "button"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "button"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "button"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "button"));
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", hotbar ? "slots" : "motion")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build inbuilt config schema", e);
        }
    }

    /**
     * Category layout for the three combat modules. They each get an explicit schema so the
     * dialog groups related settings rather than listing one flat column.
     */
    private static RuntimeConfigSchema createCombatConfigSchema(Context context, UnifiedMod mod) {
        String modId = mod.getId();
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            String defaultCategory;
            if (ModIds.ARMOR_HUD.equals(modId)) {
                categories.put(configCategory(context, "display", R.string.mod_config_category_appearance));
                categories.put(configCategory(context, "data", R.string.mod_config_category_behavior));
                defaultCategory = "display";
                nodes.put(configNode(context, mod, CFG_ARMOR_HUD_SHOW_TARGET, "display"));
                nodes.put(configNode(context, mod, CFG_ARMOR_HUD_SHOW_ENCHANTS, "display"));
                nodes.put(configNode(context, mod, CFG_ARMOR_HUD_STACKED, "display"));
                nodes.put(configNode(context, mod, CFG_ARMOR_HUD_REFRESH_MS, "data"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "data"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "data"));
            } else if (ModIds.CRYSTAL_OPTIMIZER.equals(modId)) {
                categories.put(configCategory(context, "safety", R.string.mod_config_category_behavior));
                categories.put(configCategory(context, "tuning", R.string.mod_config_category_motion));
                defaultCategory = "safety";
                nodes.put(configNode(context, mod, CFG_CRYSTAL_MANUAL_ASSIST, "safety"));
                nodes.put(configNode(context, mod, CFG_CRYSTAL_MIN_SELF_HP, "safety"));
                nodes.put(configNode(context, mod, CFG_CRYSTAL_MAX_RANGE, "tuning"));
                nodes.put(configNode(context, mod, CFG_CRYSTAL_PLACEMENT_DELAY_MS, "tuning"));
                nodes.put(configNode(context, mod, CFG_CRYSTAL_KEYBIND, "tuning"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "tuning"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "tuning"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "safety"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "safety"));
            } else if (ModIds.HIT_TIMING.equals(modId)) {
                categories.put(configCategory(context, "timing", R.string.mod_config_category_motion));
                categories.put(configCategory(context, "display", R.string.mod_config_category_appearance));
                defaultCategory = "timing";
                nodes.put(configNode(context, mod, CFG_HIT_TIMING_COOLDOWN_MS, "timing"));
                nodes.put(configNode(context, mod, CFG_HIT_TIMING_SHOW_COMBO, "display"));
                nodes.put(configNode(context, mod, CFG_HIT_TIMING_SHOW_BAR, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "display"));
            } else if (ModIds.HITBOX.equals(modId)) {
                categories.put(configCategory(context, "entities", R.string.mod_config_category_slots));
                categories.put(configCategory(context, "guides", R.string.mod_config_category_appearance));
                defaultCategory = "entities";
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_PLAYERS, "entities"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_MOBS, "entities"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_ITEMS, "entities"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_PROJECTILES, "entities"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_THROWN_ITEMS, "entities"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_LOOK_LINE, "guides"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_TARGET_BOX, "guides"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_CRIT_LINE, "guides"));
                nodes.put(configNode(context, mod, CFG_HITBOX_SHOW_COMBO_BOX, "guides"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "guides"));
            } else {
                categories.put(configCategory(context, "aim", R.string.mod_config_category_motion));
                categories.put(configCategory(context, "feedback", R.string.mod_config_category_button));
                defaultCategory = "aim";
                nodes.put(configNode(context, mod, CFG_HITREG_SENSITIVITY, "aim"));
                nodes.put(configNode(context, mod, CFG_HITREG_SMOOTHING, "aim"));
                nodes.put(configNode(context, mod, CFG_HITREG_PREDICTION, "aim"));
                nodes.put(configNode(context, mod, CFG_HITREG_HAPTIC, "feedback"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "feedback"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "feedback"));
            }
            int noteRes = scopeNoteRes(modId);
            if (noteRes != 0) {
                nodes.put(scopeNoteNode(context, mod, noteRes, defaultCategory));
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", defaultCategory)
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build combat config schema", e);
        }
    }

    /**
     * Category layout for the proximity voice module: the microphone and the icon's appearance.
     *
     * <p><b>Scope is deliberately trimmed to mic-only.</b> Channel name, visibility, capacity and
     * per-member mute all live on the Voice tab now, so this dialog does not duplicate them -- a
     * control in two places is a control that can disagree with itself. What remains is global and
     * genuinely belongs to the module: whether the mic transmits at all, and how the in-world
     * nametag icon looks and behaves.
     *
     * <p>The scope note is placed in the default category so it is the first thing read -- the
     * module is named "Proximity Voice Chat" but only applies distance when a position feed
     * exists, and that distinction has to be on screen, not buried.
     */
    private static RuntimeConfigSchema createVoiceConfigSchema(Context context, UnifiedMod mod) {
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            categories.put(configCategory(context, "mic", R.string.mod_config_category_behavior));
            categories.put(configCategory(context, "icon", R.string.mod_config_category_appearance));
            categories.put(configCategory(context, "overlay", R.string.mod_config_category_button));
            nodes.put(configNode(context, mod, CFG_VOICE_MIC, "mic"));
            nodes.put(configNode(context, mod, CFG_VOICE_ICON_STYLE, "icon"));
            nodes.put(configNode(context, mod, CFG_VOICE_ICON_ANIMATE, "icon"));
            nodes.put(configNode(context, mod, CFG_VOICE_ICON_NAMETAG, "icon"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "overlay"));
            nodes.put(scopeNoteNode(context, mod, R.string.voice_chat_scope_note, "mic"));
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "mic")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build voice config schema", e);
        }
    }

    /**
     * Category layout for the four PvP Suite modules (V1.1).
     *
     * <p>Each gets a short, focused dialog: the module's own display options plus the shared
     * "show everywhere" switch, and an info node carrying its scope note. The note is deliberate -
     * every module in the suite is named after an outcome (Reach Indicator, Hit Prediction) but
     * only works from the peer feed this build can actually reach, so the dialog says what it can
     * and cannot see rather than leaving the limit to read as a broken feature.
     */
    private static RuntimeConfigSchema createFpsConfigSchema(Context context, UnifiedMod mod) {
        org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Module module =
                org.chimeramc.client.core.mods.fps.FpsModuleCatalog.find(mod.getId());
        if (module == null) return null;
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            // The three FPS modules keep only their implemented options, so a category is only
            // created when an option actually uses it.
            categories.put(configCategory(context, "main", R.string.mod_config_category_culling));
            categories.put(configCategory(context, "batching", R.string.mod_config_category_batching));
            categories.put(configCategory(context, "memory", R.string.mod_config_category_memory));
            categories.put(configCategory(context, "timing", R.string.mod_config_category_timing));
            categories.put(configCategory(context, "network", R.string.mod_config_category_network));

            nodes.put(scopeNoteNode(context, mod, R.string.fps_modules_scope_note, "main"));

            for (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Option option : module.options) {
                JSONObject node = new JSONObject()
                        .put("id", option.key)
                        .put("key", option.key)
                        .put("category", fpsCategoryFor(option.key))
                        .put("title", option.label)
                        .put("type", option.isToggle() ? "toggle" : "slider")
                        .put("default_value", option.defaultValue)
                        .put("min_value", String.valueOf(option.min))
                        .put("max_value", String.valueOf(option.max))
                        .put("description", option.description);
                nodes.put(node);
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "main")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build FPS module config schema", e);
        }
    }

    /** The category a kept FPS option belongs to. Only the seven implemented options are present. */
    private static String fpsCategoryFor(String key) {
        if (key.startsWith("tr_")) {
            return key.contains("net") || key.contains("coalesce") ? "network" : "timing";
        }
        if (key.startsWith("vc_")) {
            return key.contains("atlas") ? "memory" : "batching";
        }
        return "main";
    }

    /**
     * The config schema for a present-frame effect module (frame generation, motion blur).
     *
     * <p>Every option is an option {@link org.chimeramc.client.core.mods.render.PresentFramePolicy}
     * reads, so there is no dead switch. The scope note leads the panel because both modules are
     * easy to misread as a frame-rate feature; the note says what they actually do.
     */
    private static RuntimeConfigSchema createPresentFrameConfigSchema(Context context, UnifiedMod mod) {
        org.chimeramc.client.core.mods.render.PresentFrameCatalog.Module module =
                org.chimeramc.client.core.mods.render.PresentFrameCatalog.find(mod.getId());
        if (module == null) return null;
        int noteRes = ModIds.FRAME_GENERATION.equals(mod.getId())
                ? R.string.frame_generation_scope_note
                : R.string.motion_blur_scope_note;
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            categories.put(configCategory(context, "main", R.string.mod_config_category_effect));
            categories.put(configCategory(context, "quality", R.string.mod_config_category_memory));

            nodes.put(scopeNoteNode(context, mod, noteRes, "main"));

            for (org.chimeramc.client.core.mods.render.PresentFrameCatalog.Option option
                    : module.options) {
                JSONObject node = new JSONObject()
                        .put("id", option.key)
                        .put("key", option.key)
                        .put("category", option.key.startsWith("mb_budget") ? "quality" : "main")
                        .put("title", option.label)
                        .put("type", option.isToggle() ? "toggle" : "slider")
                        .put("default_value", option.defaultValue)
                        .put("min_value", String.valueOf(option.min))
                        .put("max_value", String.valueOf(option.max))
                        .put("description", option.description);
                nodes.put(node);
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "main")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build present-frame module config schema", e);
        }
    }

    private static RuntimeConfigSchema createPvpSuiteConfigSchema(Context context, UnifiedMod mod) {
        String modId = mod.getId();
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            categories.put(configCategory(context, "display", R.string.mod_config_category_appearance));
            categories.put(configCategory(context, "behavior", R.string.mod_config_category_behavior));
            if (ModIds.REACH_INDICATOR.equals(modId)) {
                nodes.put(configNode(context, mod, CFG_REACH_POSITION, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "behavior"));
            } else if (ModIds.TRAJECTORY_PREDICTION.equals(modId)) {
                nodes.put(configNode(context, mod, CFG_TRAJECTORY_COLOR, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "behavior"));
            } else if (ModIds.HIT_PREDICTION.equals(modId)) {
                nodes.put(configNode(context, mod, CFG_HIT_PREDICTION_LOOKAHEAD, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "behavior"));
            } else {
                nodes.put(configNode(context, mod, CFG_KILL_EFFECT_STYLE, "display"));
                nodes.put(configNode(context, mod, CFG_KILL_EFFECT_COLOR, "display"));
                nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "behavior"));
            }
            int noteRes = scopeNoteRes(modId);
            if (noteRes != 0) {
                nodes.put(scopeNoteNode(context, mod, noteRes, "display"));
            }
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "display")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build PvP suite config schema", e);
        }
    }

    /**
     * Category layout for the Offhand module.
     *
     * <p>Two categories: what the on-screen button does (mode + overlay look) and the hardware
     * keybinds. The scope note is in the default category because the module is honest about being
     * a key-sender — Bedrock resolves the swap/use, so it works everywhere the game does.
     */
    private static RuntimeConfigSchema createOffhandConfigSchema(Context context, UnifiedMod mod) {
        try {
            JSONArray categories = new JSONArray();
            JSONArray nodes = new JSONArray();
            categories.put(configCategory(context, "action", R.string.mod_config_category_behavior));
            categories.put(configCategory(context, "keys", R.string.mod_config_category_button));
            categories.put(configCategory(context, "overlay", R.string.mod_config_category_appearance));
            nodes.put(configNode(context, mod, CFG_OFFHAND_MODE, "action"));
            nodes.put(configNode(context, mod, CFG_OFFHAND_INVENTORY_KEY, "keys"));
            nodes.put(configNode(context, mod, CFG_OFFHAND_USE_KEY, "keys"));
            nodes.put(configNode(context, mod, CFG_OFFHAND_KEYBIND, "keys"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_SIZE, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_OPACITY, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_LOCK, "overlay"));
            nodes.put(configNode(context, mod, CFG_OVERLAY_SHOW_EVERYWHERE, "overlay"));
            nodes.put(scopeNoteNode(context, mod, R.string.offhand_scope_note, "action"));
            return RuntimeConfigSchema.parse(new JSONObject().put("version", 2)
                    .put("default_category", "action")
                    .put("categories", categories).put("nodes", nodes).toString());
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to build offhand config schema", e);
        }
    }

    private static JSONObject configCategory(Context context, String id, int titleRes) throws JSONException {
        return new JSONObject().put("id", id).put("title", context.getString(titleRes));
    }

    private static JSONObject configNode(Context context, UnifiedMod mod, String key, String category) throws JSONException {
        UnifiedMod.ConfigEntry config = mod.findConfigEntry(key);
        JSONObject node = new JSONObject().put("id", key).put("key", key)
                .put("category", category).put("title", config.displayName)
                .put("type", schemaTypeFor(config.type))
                .put("default_value", config.defaultValue)
                .put("min_value", config.minValue).put("max_value", config.maxValue);
        // A choice node needs its options in the schema: the dialog builds the control from the
        // node alone, so a RADIO without options renders an empty selector. The labels live in
        // min_value and the values are their indices, matching the legacy config renderer.
        if (config.type == UnifiedMod.ConfigType.RADIO) {
            JSONArray options = new JSONArray();
            String[] labels = config.minValue != null ? config.minValue.split(",") : new String[0];
            for (int i = 0; i < labels.length; i++) {
                options.put(new JSONObject().put("value", String.valueOf(i))
                        .put("label", labels[i].trim()));
            }
            node.put("options", options);
        }
        int descRes = configDescriptionRes(key);
        if (descRes != 0) {
            node.put("description", context.getString(descRes));
        }
        if (!config.dependsOn.isEmpty()) {
            node.put("enabled_when", new JSONArray().put(new JSONObject()
                    .put("key", config.dependsOn).put("op", "truthy")));
        }
        // An output the native entity feed would have to supply is disabled rather than hidden:
        // the switch stays visible so the option is discoverable, but it cannot be turned on to
        // no effect. The same rule the module-level badge uses, one level down.
        if (ModIds.outputRequiresGameData(mod.getId(), key)) {
            node.put("disabled", true);
        }
        return node;
    }

    /**
     * The `_desc` string for a config key, or 0 when there is none.
     *
     * <p>Resolved through an explicit table rather than a name lookup so a renamed key cannot
     * silently start matching an unrelated string.
     */
    static int configDescriptionRes(String key) {
        switch (key) {
            case CFG_ARMOR_HUD_SHOW_TARGET: return R.string.mod_config_armor_hud_show_target_desc;
            case CFG_ARMOR_HUD_SHOW_ENCHANTS: return R.string.mod_config_armor_hud_show_enchants_desc;
            case CFG_ARMOR_HUD_STACKED: return R.string.mod_config_armor_hud_stacked_desc;
            case CFG_ARMOR_HUD_REFRESH_MS: return R.string.mod_config_armor_hud_refresh_ms_desc;
            case CFG_HITREG_SENSITIVITY: return R.string.mod_config_hitreg_sensitivity_desc;
            case CFG_HITREG_SMOOTHING: return R.string.mod_config_hitreg_smoothing_desc;
            case CFG_HITREG_PREDICTION: return R.string.mod_config_hitreg_prediction_desc;
            case CFG_HITREG_HAPTIC: return R.string.mod_config_hitreg_haptic_desc;
            case CFG_CRYSTAL_PLACEMENT_DELAY_MS: return R.string.mod_config_crystal_placement_delay_ms_desc;
            case CFG_CRYSTAL_MANUAL_ASSIST: return R.string.mod_config_crystal_manual_assist_desc;
            case CFG_CRYSTAL_MIN_SELF_HP: return R.string.mod_config_crystal_min_self_hp_desc;
            case CFG_CRYSTAL_MAX_RANGE: return R.string.mod_config_crystal_max_range_desc;
            case CFG_CRYSTAL_KEYBIND: return R.string.mod_config_crystal_keybind_desc;
            case CFG_HIT_TIMING_COOLDOWN_MS: return R.string.mod_config_hit_timing_cooldown_ms_desc;
            case CFG_HIT_TIMING_SHOW_COMBO: return R.string.mod_config_hit_timing_show_combo_desc;
            case CFG_HIT_TIMING_SHOW_BAR: return R.string.mod_config_hit_timing_show_bar_desc;
            case CFG_HITBOX_SHOW_PLAYERS: return R.string.mod_config_hitbox_show_players_desc;
            case CFG_HITBOX_SHOW_MOBS: return R.string.mod_config_hitbox_show_mobs_desc;
            case CFG_HITBOX_SHOW_ITEMS: return R.string.mod_config_hitbox_show_items_desc;
            case CFG_HITBOX_SHOW_PROJECTILES: return R.string.mod_config_hitbox_show_projectiles_desc;
            case CFG_HITBOX_SHOW_THROWN_ITEMS: return R.string.mod_config_hitbox_show_thrown_items_desc;
            case CFG_HITBOX_SHOW_LOOK_LINE: return R.string.mod_config_hitbox_show_look_line_desc;
            case CFG_HITBOX_SHOW_CRIT_LINE: return R.string.mod_config_hitbox_show_crit_line_desc;
            case CFG_HITBOX_SHOW_COMBO_BOX: return R.string.mod_config_hitbox_show_combo_box_desc;
            case CFG_HITBOX_SHOW_TARGET_BOX: return R.string.mod_config_hitbox_show_target_box_desc;
            case CFG_VOICE_RANGE: return R.string.mod_config_voice_range_desc;
            case CFG_VOICE_VOLUME: return R.string.mod_config_voice_volume_desc;
            case CFG_VOICE_CHANNEL: return R.string.mod_config_voice_channel_desc;
            case CFG_VOICE_MIC: return R.string.mod_config_voice_mic_desc;
            case CFG_VOICE_ICON_STYLE: return R.string.mod_config_voice_icon_style_desc;
            case CFG_VOICE_ICON_ANIMATE: return R.string.mod_config_voice_icon_animate_desc;
            case CFG_VOICE_ICON_NAMETAG: return R.string.mod_config_voice_icon_nametag_desc;
            case CFG_REACH_POSITION: return R.string.mod_config_reach_position_desc;
            case CFG_TRAJECTORY_COLOR: return R.string.mod_config_trajectory_color_desc;
            case CFG_HIT_PREDICTION_LOOKAHEAD: return R.string.mod_config_hit_prediction_lookahead_desc;
            case CFG_KILL_EFFECT_STYLE: return R.string.mod_config_kill_effect_style_desc;
            case CFG_KILL_EFFECT_COLOR: return R.string.mod_config_kill_effect_color_desc;
            default: return 0;
        }
    }

    /**
     * An informational node carrying the module's scope note, or null when it has none.
     *
     * <p>These modules are named after an outcome ("Hit Registration") but deliberately stop
     * short of it — the server decides whether a hit lands, and the box feed may not exist yet.
     * Saying so in the dialog is what stops the honest limit reading as a broken feature.
     */
    private static JSONObject scopeNoteNode(Context context, UnifiedMod mod,
                                            int noteRes, String category) throws JSONException {
        return new JSONObject()
                .put("id", mod.getId() + "_scope_note")
                .put("type", "info")
                .put("category", category)
                .put("title", context.getString(R.string.mod_config_scope_note_title))
                .put("description", context.getString(noteRes));
    }

    private static int scopeNoteRes(String modId) {
        if (ModIds.HIT_REGISTRATION.equals(modId)) return R.string.hitreg_scope_note;
        if (ModIds.CRYSTAL_OPTIMIZER.equals(modId)) return R.string.crystal_optimizer_fairness_note;
        if (ModIds.HIT_TIMING.equals(modId)) return R.string.hit_timing_scope_note;
        if (ModIds.HITBOX.equals(modId)) return R.string.hitbox_scope_note;
        if (ModIds.ARMOR_HUD.equals(modId)) return R.string.armor_hud_no_data;
        if (ModIds.VOICE_CHAT.equals(modId)) return R.string.voice_chat_scope_note;
        if (ModIds.REACH_INDICATOR.equals(modId)) return R.string.reach_indicator_scope_note;
        if (ModIds.TRAJECTORY_PREDICTION.equals(modId)) return R.string.trajectory_prediction_scope_note;
        if (ModIds.HIT_PREDICTION.equals(modId)) return R.string.hit_prediction_scope_note;
        if (ModIds.KILL_EFFECTS.equals(modId)) return R.string.kill_effects_scope_note;
        if (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.isFpsModule(modId)) {
            return R.string.fps_modules_scope_note;
        }
        return 0;
    }

    /**
     * Maps a config entry's type to the schema node type the dialog renders.
     *
     * This must be the real type, not a toggle/slider default: the dialog builds its control
     * from the node type alone, so a KEYBIND node declared as a slider would show a 0..100
     * range and write a small integer over the stored key code, and a RADIO node would lose
     * its options. Anything the schema cannot express (the legacy colour picker) still falls
     * back to a slider rather than silently dropping the setting.
     */
    static String schemaTypeFor(UnifiedMod.ConfigType type) {
        switch (type) {
            case TOGGLE:
                return "toggle";
            case SLIDER_INT:
                return "slider_int";
            case SLIDER_FLOAT:
                return "slider_float";
            case RADIO:
                return "choice";
            case KEYBIND:
                return "keybind";
            case TEXT:
                return "text";
            case BUTTON:
                return "button";
            case COLOR:
            default:
                return "slider_int";
        }
    }

    static List<UnifiedMod.ConfigEntry> createConfigs(Context context,
                                                              InbuiltModManager manager,
                                                              String modId) {
        List<UnifiedMod.ConfigEntry> configs = new ArrayList<>();
        if (ModIds.POJAV_CONTROLS.equals(modId) || ModIds.MORE_BUTTONS.equals(modId)) return configs;
        if (!ModIds.CHICK_PET.equals(modId)) {
            if (!ModIds.HOTBAR_SLOT.equals(modId)) {
                configs.add(config(CFG_OVERLAY_SIZE,
                        context.getString(R.string.mod_config_overlay_button_size_dp),
                        UnifiedMod.ConfigType.SLIDER_INT,
                        "56", "20", "100",
                        String.valueOf(manager.getOverlayButtonSize(modId))));
                configs.add(config(CFG_OVERLAY_OPACITY,
                        context.getString(R.string.mod_config_overlay_opacity_percent),
                        UnifiedMod.ConfigType.SLIDER_INT,
                        "100", "0", "100",
                        String.valueOf(manager.getOverlayOpacity(modId))));
            }
            configs.add(config(CFG_OVERLAY_LOCK,
                    context.getString(R.string.overlay_button_lock),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isOverlayLocked(modId))));
            configs.add(config(CFG_OVERLAY_SHOW_EVERYWHERE,
                    context.getString(R.string.mod_config_overlay_show_everywhere),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isOverlayShowEverywhere(modId))));
        }

        // The frame-time graph is an FPS-overlay-only option: it needs the FPS meter,
        // which only that overlay runs.
        if (ModIds.FPS_DISPLAY.equals(modId)) {
            configs.add(config(CFG_FRAME_GRAPH,
                    context.getString(R.string.mod_config_fps_frame_graph),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isFrameTimeGraphEnabled(modId))));
            configs.add(config(CFG_FPS_STATS,
                    context.getString(R.string.mod_config_fps_stats),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isFpsStatsEnabled(modId))));
            configs.add(config(CFG_FPS_STATS_WINDOW,
                    context.getString(R.string.mod_config_fps_stats_window),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    String.valueOf(InbuiltModManager.FPS_STATS_WINDOW_DEFAULT),
                    String.valueOf(InbuiltModManager.FPS_STATS_WINDOW_MIN),
                    String.valueOf(InbuiltModManager.FPS_STATS_WINDOW_MAX),
                    String.valueOf(manager.getFpsStatsWindow(modId))));
        }

        if (ModIds.HOTBAR_SLOT.equals(modId)) {
            configs.add(config(CFG_HOTBAR_ITEM_ICONS,
                    context.getString(R.string.mod_config_hotbar_item_icons),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isHotbarItemIconsEnabled())));
            for (int slot = 1; slot <= 9; slot++) {
                String enabledKey = hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_ENABLED);
                String overlayKey = ModIds.HOTBAR_SLOT + ":" + slot;
                configs.add(config(enabledKey,
                        context.getString(R.string.mod_config_hotbar_slot_enabled, slot),
                        UnifiedMod.ConfigType.TOGGLE,
                        "true", "", "",
                        String.valueOf(manager.isHotbarSlotEnabled(slot))));
                configs.add(config(hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_SIZE),
                        context.getString(R.string.mod_config_hotbar_slot_size, slot),
                        UnifiedMod.ConfigType.SLIDER_INT,
                        "56", "20", "100",
                        String.valueOf(manager.getOverlayButtonSize(overlayKey)), enabledKey));
                configs.add(config(hotbarSlotConfigKey(slot, CFG_HOTBAR_SLOT_OPACITY),
                        context.getString(R.string.mod_config_hotbar_slot_opacity, slot),
                        UnifiedMod.ConfigType.SLIDER_INT,
                        "100", "0", "100",
                        String.valueOf(manager.getOverlayOpacity(overlayKey)), enabledKey));
            }
        }

        if (ModIds.OFFHAND.equals(modId)) {
            configs.add(config(CFG_OFFHAND_MODE,
                    context.getString(R.string.mod_config_offhand_mode),
                    UnifiedMod.ConfigType.RADIO,
                    context.getString(R.string.mod_config_offhand_mode_labels), "", "",
                    String.valueOf(manager.getOffhandMode())));
            configs.add(config(CFG_OFFHAND_INVENTORY_KEY,
                    context.getString(R.string.mod_config_offhand_inventory_key),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getOffhandInventoryKey())));
            configs.add(config(CFG_OFFHAND_USE_KEY,
                    context.getString(R.string.mod_config_offhand_use_key),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getOffhandUseKey())));
            configs.add(config(CFG_OFFHAND_KEYBIND,
                    context.getString(R.string.mod_config_offhand_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getOffhandKeybind())));
        }

        if (ModIds.AUTO_SPRINT.equals(modId)) {
            configs.add(config(CFG_AUTO_SPRINT_KEYBIND,
                    context.getString(R.string.mod_config_auto_sprint_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getAutoSprintKeybind())));
        } else if (ModIds.VIRTUAL_CURSOR.equals(modId)) {
            configs.add(config(CFG_CURSOR_SENSITIVITY,
                    context.getString(R.string.mod_config_cursor_sensitivity_percent),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "120", "10", "200",
                    String.valueOf(manager.getCursorSensitivity())));
        } else if (ModIds.ZOOM.equals(modId)) {
            // The keybind is always offered: it is the input that would drive zoom once a backend
            // provides one. The four shaping sliders are hidden while no backend advertises
            // Feature.ZOOM, because they change a field-of-view zoom nothing can apply — offering
            // them would be four controls that visibly do nothing.
            configs.add(config(CFG_ZOOM_KEYBIND,
                    context.getString(R.string.mod_config_zoom_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getZoomKeybind())));
            if (ModIds.requiresBackendFeature(ModIds.ZOOM)) {
                return configs;
            }
            configs.add(config(CFG_ZOOM_LEVEL,
                    context.getString(R.string.mod_config_zoom_level_percent),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "10", "-20", "100",
                    String.valueOf(manager.getZoomLevel())));
            configs.add(config(CFG_ZOOM_TRANSITION,
                    context.getString(R.string.mod_config_zoom_transition),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "150", "0", "1000",
                    String.valueOf(manager.getZoomTransitionDuration())));
            configs.add(config(CFG_ZOOM_SCROLL_SENSITIVITY,
                    context.getString(R.string.mod_config_zoom_scroll_sensitivity),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    String.valueOf(100),
                    String.valueOf(InbuiltModManager.MIN_ZOOM_SCROLL_SENSITIVITY),
                    String.valueOf(InbuiltModManager.MAX_ZOOM_SCROLL_SENSITIVITY),
                    String.valueOf(manager.getZoomScrollSensitivity())));
            configs.add(config(CFG_ZOOM_SMOOTHING,
                    context.getString(R.string.mod_config_zoom_smoothing),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "0", "0",
                    String.valueOf(InbuiltModManager.MAX_ZOOM_SMOOTHING),
                    String.valueOf(manager.getZoomSmoothing())));
        } else if (ModIds.SNAPLOOK.equals(modId)) {
            configs.add(config(CFG_SNAPLOOK_KEYBIND,
                    context.getString(R.string.mod_config_snaplook_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getSnaplookKeybind())));
            configs.add(config(CFG_SNAPLOOK_RETURN_SPEED,
                    context.getString(R.string.mod_config_snaplook_return_speed),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "50", "0",
                    String.valueOf(InbuiltModManager.MAX_SNAPLOOK_RETURN_SPEED),
                    String.valueOf(manager.getSnaplookReturnSpeed())));
        } else if (ModIds.GYRO.equals(modId)) {
            configs.add(config(CFG_GYRO_SENSITIVITY_X,
                    context.getString(R.string.mod_config_gyro_sensitivity_x),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "10", "300",
                    String.valueOf(manager.getGyroSensitivityX())));
            configs.add(config(CFG_GYRO_SENSITIVITY_Y,
                    context.getString(R.string.mod_config_gyro_sensitivity_y),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "10", "300",
                    String.valueOf(manager.getGyroSensitivityY())));
            configs.add(config(CFG_GYRO_INVERT_X,
                    context.getString(R.string.mod_config_gyro_invert_x),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isGyroInvertX())));
            configs.add(config(CFG_GYRO_INVERT_Y,
                    context.getString(R.string.mod_config_gyro_invert_y),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isGyroInvertY())));
            configs.add(config(CFG_GYRO_DEADZONE,
                    context.getString(R.string.mod_config_gyro_deadzone),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "5", "0", "50",
                    String.valueOf(manager.getGyroDeadzone())));
        } else if (ModIds.AIM_SETTINGS.equals(modId)) {
            configs.add(config(CFG_AIM_SENSITIVITY,
                    context.getString(R.string.mod_config_aim_sensitivity),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "10", "300",
                    String.valueOf(manager.getAimSensitivity())));
            configs.add(config(CFG_AIM_SMOOTHING,
                    context.getString(R.string.mod_config_aim_smoothing),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "40", "0", "95",
                    String.valueOf(manager.getAimSmoothing())));
            configs.add(config(CFG_AIM_CROSSHAIR,
                    context.getString(R.string.mod_config_aim_crosshair),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isAimCrosshairEnabled())));
            configs.add(config(CFG_AIM_CROSSHAIR_STYLE,
                    context.getString(R.string.aim_crosshair_style),
                    UnifiedMod.ConfigType.RADIO,
                    "1",
                    context.getString(R.string.aim_crosshair_style_dot) + ","
                            + context.getString(R.string.aim_crosshair_style_cross) + ","
                            + context.getString(R.string.aim_crosshair_style_circle),
                    "",
                    String.valueOf(manager.getAimCrosshairStyle())));
            configs.add(config(CFG_AIM_CROSSHAIR_COLOR,
                    context.getString(R.string.aim_crosshair_color),
                    UnifiedMod.ConfigType.COLOR,
                    "#FF3DDC84", "", "",
                    colorToHex(manager.getAimCrosshairColor())));
            configs.add(config(CFG_AIM_FLASH,
                    context.getString(R.string.mod_config_aim_flash),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isAimFlashEnabled())));
        } else if (ModIds.ARMOR_HUD.equals(modId)) {
            configs.add(config(CFG_ARMOR_HUD_SHOW_TARGET,
                    context.getString(R.string.mod_config_armor_hud_show_target),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isArmorHudShowTarget())));
            configs.add(config(CFG_ARMOR_HUD_SHOW_ENCHANTS,
                    context.getString(R.string.mod_config_armor_hud_show_enchants),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.isArmorHudShowEnchants())));
            configs.add(config(CFG_ARMOR_HUD_STACKED,
                    context.getString(R.string.mod_config_armor_hud_stacked),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isArmorHudStacked())));
            configs.add(config(CFG_ARMOR_HUD_REFRESH_MS,
                    context.getString(R.string.mod_config_armor_hud_refresh_ms),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "50", "1000",
                    String.valueOf(manager.getArmorHudRefreshMs())));
        } else if (ModIds.CRYSTAL_OPTIMIZER.equals(modId)) {
            // Manual assist is listed first and defaults on: it is the variant that does not
            // automate a player action, so it must be the one a fresh profile lands on.
            configs.add(config(CFG_CRYSTAL_MANUAL_ASSIST,
                    context.getString(R.string.mod_config_crystal_manual_assist),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isCrystalManualAssist())));
            configs.add(config(CFG_CRYSTAL_MIN_SELF_HP,
                    context.getString(R.string.mod_config_crystal_min_self_hp),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "14", "1", "20",
                    String.valueOf(manager.getCrystalMinSelfHp())));
            configs.add(config(CFG_CRYSTAL_MAX_RANGE,
                    context.getString(R.string.mod_config_crystal_max_range),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "4", "1", "8",
                    String.valueOf(manager.getCrystalMaxRange())));
            configs.add(config(CFG_CRYSTAL_PLACEMENT_DELAY_MS,
                    context.getString(R.string.mod_config_crystal_placement_delay_ms),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "50", "0", "1000",
                    String.valueOf(manager.getCrystalPlacementDelayMs())));
            configs.add(config(CFG_CRYSTAL_KEYBIND,
                    context.getString(R.string.mod_config_crystal_keybind),
                    UnifiedMod.ConfigType.KEYBIND,
                    "", "", "",
                    String.valueOf(manager.getCrystalKeybind())));
        } else if (ModIds.HIT_REGISTRATION.equals(modId)) {
            configs.add(config(CFG_HITREG_SENSITIVITY,
                    context.getString(R.string.mod_config_hitreg_sensitivity),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "100", "10", "300",
                    String.valueOf(manager.getHitRegSensitivity())));
            configs.add(config(CFG_HITREG_SMOOTHING,
                    context.getString(R.string.mod_config_hitreg_smoothing),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "25", "0", "95",
                    String.valueOf(manager.getHitRegSmoothing())));
            configs.add(config(CFG_HITREG_PREDICTION,
                    context.getString(R.string.mod_config_hitreg_prediction),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    "35", "0", "100",
                    String.valueOf(manager.getHitRegPrediction())));
            configs.add(config(CFG_HITREG_HAPTIC,
                    context.getString(R.string.mod_config_hitreg_haptic),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitRegHapticEnabled())));
        } else if (ModIds.HIT_TIMING.equals(modId)) {
            configs.add(config(CFG_HIT_TIMING_COOLDOWN_MS,
                    context.getString(R.string.mod_config_hit_timing_cooldown_ms),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    String.valueOf(HitTimingSolver.DEFAULT_COOLDOWN_MS), "50", "2000",
                    String.valueOf(manager.getHitTimingCooldownMs())));
            configs.add(config(CFG_HIT_TIMING_SHOW_COMBO,
                    context.getString(R.string.mod_config_hit_timing_show_combo),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitTimingShowCombo())));
            configs.add(config(CFG_HIT_TIMING_SHOW_BAR,
                    context.getString(R.string.mod_config_hit_timing_show_bar),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitTimingShowTimingBar())));
        } else if (ModIds.HITBOX.equals(modId)) {
            configs.add(config(CFG_HITBOX_SHOW_PLAYERS,
                    context.getString(R.string.mod_config_hitbox_show_players),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowPlayers())));
            configs.add(config(CFG_HITBOX_SHOW_MOBS,
                    context.getString(R.string.mod_config_hitbox_show_mobs),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowMobs())));
            configs.add(config(CFG_HITBOX_SHOW_ITEMS,
                    context.getString(R.string.mod_config_hitbox_show_items),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowItems())));
            configs.add(config(CFG_HITBOX_SHOW_PROJECTILES,
                    context.getString(R.string.mod_config_hitbox_show_projectiles),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowProjectiles())));
            configs.add(config(CFG_HITBOX_SHOW_THROWN_ITEMS,
                    context.getString(R.string.mod_config_hitbox_show_thrown_items),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowThrownItems())));
            configs.add(config(CFG_HITBOX_SHOW_LOOK_LINE,
                    context.getString(R.string.mod_config_hitbox_show_look_line),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowLookLine())));
            configs.add(config(CFG_HITBOX_SHOW_CRIT_LINE,
                    context.getString(R.string.mod_config_hitbox_show_crit_line),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowCritLine())));
            configs.add(config(CFG_HITBOX_SHOW_COMBO_BOX,
                    context.getString(R.string.mod_config_hitbox_show_combo_box),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowComboBox())));
            configs.add(config(CFG_HITBOX_SHOW_TARGET_BOX,
                    context.getString(R.string.mod_config_hitbox_show_target_box),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isHitboxShowTargetBox())));
        } else if (ModIds.REACH_INDICATOR.equals(modId)) {
            configs.add(config(CFG_REACH_POSITION,
                    context.getString(R.string.mod_config_reach_position),
                    UnifiedMod.ConfigType.RADIO,
                    "0",
                    context.getString(R.string.mod_config_reach_position_below) + ","
                            + context.getString(R.string.mod_config_reach_position_hotbar),
                    "",
                    String.valueOf(manager.getReachIndicatorPosition())));
        } else if (ModIds.TRAJECTORY_PREDICTION.equals(modId)) {
            configs.add(config(CFG_TRAJECTORY_COLOR,
                    context.getString(R.string.mod_config_trajectory_color),
                    UnifiedMod.ConfigType.COLOR,
                    "#FF7CFC5A", "", "",
                    colorToHex(manager.getTrajectoryColor())));
        } else if (ModIds.HIT_PREDICTION.equals(modId)) {
            configs.add(config(CFG_HIT_PREDICTION_LOOKAHEAD,
                    context.getString(R.string.mod_config_hit_prediction_lookahead),
                    UnifiedMod.ConfigType.SLIDER_INT,
                    String.valueOf(org.chimeramc.client.core.mods.inbuilt.overlay.HitPredictor.DEFAULT_LOOK_AHEAD_MS),
                    String.valueOf(org.chimeramc.client.core.mods.inbuilt.overlay.HitPredictor.MIN_LOOK_AHEAD_MS),
                    String.valueOf(org.chimeramc.client.core.mods.inbuilt.overlay.HitPredictor.MAX_LOOK_AHEAD_MS),
                    String.valueOf(manager.getHitPredictionLookAheadMs())));
        } else if (ModIds.KILL_EFFECTS.equals(modId)) {
            configs.add(config(CFG_KILL_EFFECT_STYLE,
                    context.getString(R.string.mod_config_kill_effect_style),
                    UnifiedMod.ConfigType.RADIO,
                    "0",
                    context.getString(R.string.kill_effect_style_burst) + ","
                            + context.getString(R.string.kill_effect_style_column) + ","
                            + context.getString(R.string.kill_effect_style_ring),
                    "",
                    String.valueOf(manager.getKillEffectStyle())));
            configs.add(config(CFG_KILL_EFFECT_COLOR,
                    context.getString(R.string.mod_config_kill_effect_color),
                    UnifiedMod.ConfigType.COLOR,
                    "#FFFFC24B", "", "",
                    colorToHex(manager.getKillEffectColor())));
        } else if (ModIds.VOICE_CHAT.equals(modId)) {
            // Trimmed to mic-only scope: the mic master switch plus the in-world icon's look and
            // behaviour. Channel, visibility, capacity and per-member mute are owned by the Voice
            // tab, so they are deliberately absent here to avoid a second source of truth.
            configs.add(config(CFG_VOICE_MIC,
                    context.getString(R.string.mod_config_voice_mic),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isVoiceMicEnabled())));
            configs.add(config(CFG_VOICE_ICON_STYLE,
                    context.getString(R.string.mod_config_voice_icon_style),
                    UnifiedMod.ConfigType.TOGGLE,
                    "false", "", "",
                    String.valueOf(manager.getVoiceIconStyle()
                            == org.chimeramc.client.core.mods.inbuilt.overlay.MicIconStyle.STYLE_SMOOTH)));
            configs.add(config(CFG_VOICE_ICON_ANIMATE,
                    context.getString(R.string.mod_config_voice_icon_animate),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isVoiceIconAnimated())));
            configs.add(config(CFG_VOICE_ICON_NAMETAG,
                    context.getString(R.string.mod_config_voice_icon_nametag),
                    UnifiedMod.ConfigType.TOGGLE,
                    "true", "", "",
                    String.valueOf(manager.isVoiceNametagIconEnabled())));
        }
        if (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.isFpsModule(modId)) {
            // The FPS options come straight from the catalogue, so the dialog, the defaults and the
            // stored values read one source. A missing stored value reads the catalogue default.
            org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Module module =
                    org.chimeramc.client.core.mods.fps.FpsModuleCatalog.find(modId);
            for (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Option option : module.options) {
                if (option.isToggle()) {
                    boolean current = manager.getModConfigBool(option.key,
                            Boolean.parseBoolean(option.defaultValue));
                    configs.add(config(option.key, option.label, UnifiedMod.ConfigType.TOGGLE,
                            option.defaultValue, "", "", String.valueOf(current)));
                } else {
                    int def = Integer.parseInt(option.defaultValue);
                    int current = manager.getModConfigInt(option.key, def);
                    configs.add(config(option.key, option.label, UnifiedMod.ConfigType.SLIDER_INT,
                            option.defaultValue, String.valueOf(option.min),
                            String.valueOf(option.max), String.valueOf(current)));
                }
            }
        }
        return configs;
    }

    private static UnifiedMod.ConfigEntry config(String key, String displayName,
                                                 UnifiedMod.ConfigType type,
                                                 String defaultValue, String minValue,
                                                 String maxValue, String currentValue) {
        return config(key, displayName, type, defaultValue, minValue, maxValue, currentValue, "");
    }

    private static UnifiedMod.ConfigEntry config(String key, String displayName,
                                                 UnifiedMod.ConfigType type,
                                                 String defaultValue, String minValue,
                                                 String maxValue, String currentValue,
                                                 String dependsOn) {
        return new UnifiedMod.ConfigEntry(
                key, displayName, type, defaultValue, minValue, maxValue,
                currentValue, dependsOn
        );
    }

    static String hotbarSlotConfigKey(int slot, String setting) {
        return CFG_HOTBAR_SLOT_PREFIX + slot + "_" + setting;
    }

    private static String colorToHex(int color) {
        return String.format(java.util.Locale.US, "#%08X", color);
    }
}
