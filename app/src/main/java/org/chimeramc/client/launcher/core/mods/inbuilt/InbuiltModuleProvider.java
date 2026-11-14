package org.chimeramc.client.core.mods.inbuilt;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.graphics.Color;


import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.core.mods.inbuilt.overlay.InbuiltOverlayManager;
import org.chimeramc.client.core.mods.inbuilt.overlay.HitTimingSolver;
import org.chimeramc.client.core.mods.inbuilt.overlay.MoreButtonsEditor;
import org.chimeramc.pojavcontrols.PojavControls;

import java.util.ArrayList;
import java.util.List;

public final class InbuiltModuleProvider {
    private static final String GROUP_ID = "inbuilt";
    private static final String GROUP_PVP_ID = ModIds.GROUP_PVP;
    private static final String MOD_ID = "inbuilt";

    private InbuiltModuleProvider() {
    }

    public static List<UnifiedMod> load(Activity activity) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        String groupName = activity.getString(R.string.mod_menu_group_inbuilt);
        List<UnifiedMod> mods = new ArrayList<>();

        mods.add(create(activity, manager, overlayManager, ModIds.QUICK_DROP,
                R.string.inbuilt_mod_quick_drop, R.string.inbuilt_mod_quick_drop_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CAMERA_PERSPECTIVE,
                R.string.inbuilt_mod_camera, R.string.inbuilt_mod_camera_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TOGGLE_HUD,
                R.string.inbuilt_mod_hud, R.string.inbuilt_mod_hud_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.AUTO_SPRINT,
                R.string.inbuilt_mod_autosprint, R.string.inbuilt_mod_autosprint_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CHICK_PET,
                R.string.inbuilt_mod_chick_pet, R.string.inbuilt_mod_chick_pet_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.ZOOM,
                R.string.inbuilt_mod_zoom, R.string.inbuilt_mod_zoom_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.FPS_DISPLAY,
                R.string.inbuilt_mod_fps_display, R.string.inbuilt_mod_fps_display_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CPS_DISPLAY,
                R.string.inbuilt_mod_cps_display, R.string.inbuilt_mod_cps_display_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.SNAPLOOK,
                R.string.inbuilt_mod_snaplook, R.string.inbuilt_mod_snaplook_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.VIRTUAL_CURSOR,
                R.string.inbuilt_mod_virtual_cursor, R.string.inbuilt_mod_virtual_cursor_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.GYRO,
                R.string.inbuilt_mod_gyro, R.string.inbuilt_mod_gyro_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.POJAV_CONTROLS,
                R.string.inbuilt_mod_pojav_controls, R.string.inbuilt_mod_pojav_controls_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.MORE_BUTTONS,
                R.string.inbuilt_mod_more_buttons, R.string.inbuilt_mod_more_buttons_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.HOTBAR_SLOT,
                R.string.inbuilt_mod_hotbar_slot, R.string.inbuilt_mod_hotbar_slot_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.OFFHAND,
                R.string.inbuilt_mod_offhand, R.string.inbuilt_mod_offhand_desc,
                groupName));
        // PvP Suite (V1.1): overlay-only visual aids. They sit in the General section, not a
        // separate PvP section - the suite is not gameplay automation and the spec is explicit
        // that no new tab is added.
        mods.add(create(activity, manager, overlayManager, ModIds.REACH_INDICATOR,
                R.string.inbuilt_mod_reach_indicator, R.string.inbuilt_mod_reach_indicator_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TRAJECTORY_PREDICTION,
                R.string.inbuilt_mod_trajectory_prediction,
                R.string.inbuilt_mod_trajectory_prediction_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.HIT_PREDICTION,
                R.string.inbuilt_mod_hit_prediction, R.string.inbuilt_mod_hit_prediction_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.KILL_EFFECTS,
                R.string.inbuilt_mod_kill_effects, R.string.inbuilt_mod_kill_effects_desc,
                groupName));
        // FPS optimization modules: host-side performance only. They reduce the launcher's own work
        // and never touch the game's renderer, so no switch here promises a frame rate.
        mods.add(create(activity, manager, overlayManager, ModIds.HYPERSPATIAL_CULL,
                R.string.inbuilt_mod_hyperspatial_cull,
                R.string.inbuilt_mod_hyperspatial_cull_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.VERTICAL_RENDER_COALESCE,
                R.string.inbuilt_mod_vertical_render_coalesce,
                R.string.inbuilt_mod_vertical_render_coalesce_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TICK_RATE_PREDICTOR,
                R.string.inbuilt_mod_tick_rate_predictor,
                R.string.inbuilt_mod_tick_rate_predictor_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.ADAPTIVE_FRAME_PACING,
                R.string.inbuilt_mod_adaptive_frame_pacing,
                R.string.inbuilt_mod_adaptive_frame_pacing_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.THERMAL_QUALITY_HOLD,
                R.string.inbuilt_mod_thermal_quality_hold,
                R.string.inbuilt_mod_thermal_quality_hold_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.AIM_SETTINGS,
                R.string.inbuilt_mod_aim_settings, R.string.inbuilt_mod_aim_settings_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.ARMOR_HUD,
                R.string.inbuilt_mod_armor_hud, R.string.inbuilt_mod_armor_hud_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CRYSTAL_OPTIMIZER,
                R.string.inbuilt_mod_crystal_optimizer, R.string.inbuilt_mod_crystal_optimizer_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.HIT_REGISTRATION,
                R.string.inbuilt_mod_hit_registration, R.string.inbuilt_mod_hit_registration_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.HIT_TIMING,
                R.string.inbuilt_mod_hit_timing, R.string.inbuilt_mod_hit_timing_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.HITBOX,
                R.string.inbuilt_mod_hitbox, R.string.inbuilt_mod_hitbox_desc,
                groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.VOICE_CHAT,
                R.string.inbuilt_mod_voice_chat, R.string.inbuilt_mod_voice_chat_desc,
                activity.getString(R.string.mod_menu_group_voice)));

        // Visual / HUD / utility modules. The ones that need a native frame or entity feed are
        // badged and refused by ModAvailability; the ones that work through an existing route
        // (Fullbright, Crosshair, List Players) stay usable.
        mods.add(create(activity, manager, overlayManager, ModIds.FULLBRIGHT,
                R.string.inbuilt_mod_fullbright, R.string.inbuilt_mod_fullbright_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TIME_CHANGER,
                R.string.inbuilt_mod_time_changer, R.string.inbuilt_mod_time_changer_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.LOW_FIRE,
                R.string.inbuilt_mod_low_fire, R.string.inbuilt_mod_low_fire_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.BLOCK_OUTLINE,
                R.string.inbuilt_mod_block_outline, R.string.inbuilt_mod_block_outline_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.MOTION_BLUR,
                R.string.inbuilt_mod_motion_blur, R.string.inbuilt_mod_motion_blur_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.FRAME_GENERATION,
                R.string.inbuilt_mod_frame_generation,
                R.string.inbuilt_mod_frame_generation_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.SATURATION,
                R.string.inbuilt_mod_saturation, R.string.inbuilt_mod_saturation_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.CROSSHAIR,
                R.string.inbuilt_mod_crosshair, R.string.inbuilt_mod_crosshair_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.ARMOR_VIEW_BENDING,
                R.string.inbuilt_mod_armor_view_bending,
                R.string.inbuilt_mod_armor_view_bending_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.PROJECTILE_SCALER,
                R.string.inbuilt_mod_projectile_scaler,
                R.string.inbuilt_mod_projectile_scaler_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.FREECAM,
                R.string.inbuilt_mod_freecam, R.string.inbuilt_mod_freecam_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.TNT,
                R.string.inbuilt_mod_tnt, R.string.inbuilt_mod_tnt_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.LIST_PLAYERS,
                R.string.inbuilt_mod_list_players, R.string.inbuilt_mod_list_players_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.DEBUG_MENU,
                R.string.inbuilt_mod_debug_menu, R.string.inbuilt_mod_debug_menu_desc, groupName));
        mods.add(create(activity, manager, overlayManager, ModIds.THIRD_PERSON_NAMETAG,
                R.string.inbuilt_mod_third_person_nametag,
                R.string.inbuilt_mod_third_person_nametag_desc, groupName));

        addPvpCatalogEntries(activity, manager, overlayManager, mods);

        return groupPvpLast(mods);
    }

    /**
     * Adds the PvP catalogue as menu entries.
     *
     * <p>A catalogue entry that links to a shipped module ({@link
     * org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.Module#linksToExisting()}) is a
     * <em>view</em>: its toggle drives the real module's own enable state, so there is one source of
     * truth and the two can never disagree. A roadmap entry (one this build cannot drive) is listed
     * with the shared badge and is not clickable — {@code ModIds.requiresGameData} covers it, so the
     * grey-out, the badge and the refused toggle all come from the same rule the rest of the menu
     * uses.
     */
    private static void addPvpCatalogEntries(Activity activity, InbuiltModManager manager,
                                             InbuiltOverlayManager overlayManager,
                                             List<UnifiedMod> mods) {
        String pvpGroupName = activity.getString(R.string.mod_menu_group_pvp);
        for (org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.Module entry
                : org.chimeramc.client.core.mods.pvp.PvpModuleCatalog.modules()) {
            // A catalogue entry that duplicates an already-listed module by id is skipped; the menu
            // shows each shipped module once.
            if (entry.linksToExisting()) continue;
            UnifiedMod mod = new UnifiedMod(
                    entry.id,
                    entry.name,
                    entry.description,
                    MOD_ID,
                    UnifiedMod.Source.INBUILT,
                    overlayManager != null
                            ? overlayManager.isModActive(entry.id)
                            : manager.resolveInbuiltModEnabled(entry.id, false),
                    ModConfigSchema.createCatalogConfigs(activity, manager, entry.id),
                    false,
                    GROUP_PVP_ID,
                    pvpGroupName,
                    (m, enabled) -> setEnabled(manager, m, enabled),
                    (m, config, value) -> setConfig(manager, m, config, value)
            );
            mod.setLocalConfigSchema(ModConfigSchema.createCatalogConfigSchema(activity, mod));
            mods.add(mod);
        }
    }

    /**
     * Orders the menu: one contiguous run per group id, each sorted alphabetically by display name,
     * with the PvP group last.
     *
     * <p>Two invariants meet here. The menu draws one section header per contiguous run of a group
     * id, so every entry of a group must be adjacent — the voice module is declared in the middle of
     * the inbuilt list, and the PvP catalogue entries are appended apart from the hand-declared PvP
     * modules, so neither may simply be left in declaration order. Grouping by the id (rather than
     * only splitting PvP out) fixes both and keeps each header single.
     *
     * <p>And the player scans a section by name, so each run is sorted case-insensitively by display
     * name, with the id as a tie-break for a stable order. The sort is per run, never across the
     * whole list, or it would interleave the groups and split their headers again.
     */
    static List<UnifiedMod> groupPvpLast(List<UnifiedMod> mods) {
        // First-appearance order for the non-PvP groups, so the menu's section order stays the
        // order the provider declares rather than a hash order.
        java.util.LinkedHashMap<String, List<UnifiedMod>> byGroup = new java.util.LinkedHashMap<>();
        List<UnifiedMod> pvp = new ArrayList<>();
        for (UnifiedMod mod : mods) {
            if (ModIds.isPvpModule(mod.getId())) {
                pvp.add(mod);
                continue;
            }
            String group = mod.getGroupId() == null ? "" : mod.getGroupId();
            List<UnifiedMod> bucket = byGroup.get(group);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byGroup.put(group, bucket);
            }
            bucket.add(mod);
        }
        List<UnifiedMod> ordered = new ArrayList<>(mods.size());
        for (List<UnifiedMod> bucket : byGroup.values()) {
            bucket.sort(UnifiedModuleComparator.BY_NAME);
            ordered.addAll(bucket);
        }
        pvp.sort(UnifiedModuleComparator.BY_NAME);
        ordered.addAll(pvp);
        return ordered;
    }

    /** The name-then-id comparator shared by the section sort. */
    private static final class UnifiedModuleComparator {
        static final java.util.Comparator<UnifiedMod> BY_NAME = (a, b) -> {
            String an = a.getName() == null ? "" : a.getName();
            String bn = b.getName() == null ? "" : b.getName();
            int byName = an.compareToIgnoreCase(bn);
            if (byName != 0) return byName;
            String ai = a.getId() == null ? "" : a.getId();
            String bi = b.getId() == null ? "" : b.getId();
            return ai.compareTo(bi);
        };

        private UnifiedModuleComparator() {
        }
    }

    private static UnifiedMod create(Activity activity, InbuiltModManager manager,
                                     InbuiltOverlayManager overlayManager, String id,
                                     int nameRes, int descRes, String groupName) {
        boolean active = overlayManager != null
                ? overlayManager.isModActive(id)
                : manager.resolveInbuiltModEnabled(id, false);
        boolean customConfig = ModIds.POJAV_CONTROLS.equals(id) || ModIds.MORE_BUTTONS.equals(id)
                || ModIds.ARMOR_HUD.equals(id) || ModIds.CRYSTAL_OPTIMIZER.equals(id)
                || ModIds.HIT_REGISTRATION.equals(id) || ModIds.HIT_TIMING.equals(id)
                || ModIds.HITBOX.equals(id) || ModIds.VOICE_CHAT.equals(id)
                || ModIds.REACH_INDICATOR.equals(id) || ModIds.TRAJECTORY_PREDICTION.equals(id)
                || ModIds.HIT_PREDICTION.equals(id) || ModIds.KILL_EFFECTS.equals(id)
                || ModIds.OFFHAND.equals(id)
                || ModIds.CROSSHAIR.equals(id)
                || ModIds.HYPERSPATIAL_CULL.equals(id)
                || ModIds.VERTICAL_RENDER_COALESCE.equals(id)
                || ModIds.TICK_RATE_PREDICTOR.equals(id)
                || ModIds.ADAPTIVE_FRAME_PACING.equals(id)
                || ModIds.THERMAL_QUALITY_HOLD.equals(id);
        // Combat modules get their own PvP section so the tab is a real destination, not just a
        // filter over the inbuilt list. They remain inbuilt modules, so the Inbuilt filter and
        // the "Inbuilt" grouping still find them.
        boolean pvpModule = ModIds.isPvpModule(id);
        UnifiedMod result = new UnifiedMod(
                id,
                activity.getString(nameRes),
                activity.getString(descRes),
                MOD_ID,
                UnifiedMod.Source.INBUILT,
                active,
                ModConfigSchema.createConfigs(activity, manager, id),
                customConfig,
                pvpModule ? GROUP_PVP_ID : GROUP_ID,
                pvpModule ? activity.getString(R.string.mod_menu_group_pvp) : groupName,
                (mod, enabled) -> setEnabled(manager, mod, enabled),
                (mod, config, value) -> setConfig(manager, mod, config, value),
                ModIds.POJAV_CONTROLS.equals(id)
                        ? mod -> PojavControls.launchEditor(activity)
                        : (ModIds.MORE_BUTTONS.equals(id) ? mod -> MoreButtonsEditor.show(activity) : null)
        );
        result.setLocalConfigSchema(ModConfigSchema.createLocalConfigSchema(activity, result));
        return result;
    }

    private static void setEnabled(InbuiltModManager manager, UnifiedMod mod, boolean enabled) {
        manager.setInbuiltModEnabled(mod.getId(), enabled);
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.handleModToggle(mod.getId(), enabled);
        }
        if (org.chimeramc.client.core.mods.render.PresentFrameCatalog
                .isPresentFrameModule(mod.getId())) {
            // A toggle is as live as an option change: push the plan so enabling the module reaches
            // the present seam immediately, and disabling it stops the effect on the next present.
            pushPresentFramePlan(manager);
        }
    }

    private static void setConfig(InbuiltModManager manager, UnifiedMod mod, UnifiedMod.ConfigEntry config,
                                  String value) {
        if (ModIds.HOTBAR_SLOT.equals(mod.getId()) && setHotbarSlotConfig(manager, config.key, value)) return;
        if (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.isFpsModule(mod.getId())) {
            setFpsConfig(manager, mod, config.key, value);
            return;
        }
        if (org.chimeramc.client.core.mods.render.PresentFrameCatalog
                .isPresentFrameModule(mod.getId())) {
            setPresentFrameConfig(manager, mod, config.key, value);
            return;
        }
        switch (config.key) {
            case ModConfigSchema.CFG_CATALOG_CHAT_MACRO:
                manager.setModConfigString(ModConfigSchema.CFG_CATALOG_CHAT_MACRO, value == null ? "" : value);
                break;
            case ModConfigSchema.CFG_CATALOG_COMMAND_BIND:
                manager.setModConfigString(ModConfigSchema.CFG_CATALOG_COMMAND_BIND, value == null ? "" : value);
                break;
            case ModConfigSchema.CFG_OVERLAY_SIZE:
                manager.setOverlayButtonSize(mod.getId(), parseInt(value, manager.getOverlayButtonSize(mod.getId())));
                break;
            case ModConfigSchema.CFG_OVERLAY_OPACITY:
                manager.setOverlayOpacity(mod.getId(), parseInt(value, manager.getOverlayOpacity(mod.getId())));
                break;
            case ModConfigSchema.CFG_OVERLAY_LOCK:
                manager.setOverlayLocked(mod.getId(), parseBoolean(value));
                break;
            case ModConfigSchema.CFG_OVERLAY_SHOW_EVERYWHERE:
                manager.setOverlayShowEverywhere(mod.getId(), parseBoolean(value));
                break;
            case ModConfigSchema.CFG_FRAME_GRAPH:
                manager.setFrameTimeGraphEnabled(mod.getId(), parseBoolean(value));
                break;
            case ModConfigSchema.CFG_FPS_STATS:
                manager.setFpsStatsEnabled(mod.getId(), parseBoolean(value));
                break;
            case ModConfigSchema.CFG_FPS_STATS_WINDOW:
                manager.setFpsStatsWindow(mod.getId(), parseInt(value, manager.getFpsStatsWindow(mod.getId())));
                break;
            case ModConfigSchema.CFG_AUTO_SPRINT_KEYBIND:
                manager.setAutoSprintKeybind(parseInt(value, manager.getAutoSprintKeybind()));
                break;
            case ModConfigSchema.CFG_CURSOR_SENSITIVITY:
                manager.setCursorSensitivity(parseInt(value, manager.getCursorSensitivity()));
                break;
            case ModConfigSchema.CFG_ZOOM_LEVEL:
                manager.setZoomLevel(parseInt(value, manager.getZoomLevel()));
                break;
            case ModConfigSchema.CFG_ZOOM_TRANSITION:
                manager.setZoomTransitionDuration(parseInt(value, manager.getZoomTransitionDuration()));
                break;
            case ModConfigSchema.CFG_ZOOM_KEYBIND:
                manager.setZoomKeybind(parseInt(value, manager.getZoomKeybind()));
                break;
            case ModConfigSchema.CFG_ZOOM_SCROLL_SENSITIVITY:
                manager.setZoomScrollSensitivity(parseInt(value, manager.getZoomScrollSensitivity()));
                break;
            case ModConfigSchema.CFG_ZOOM_SMOOTHING:
                manager.setZoomSmoothing(parseInt(value, manager.getZoomSmoothing()));
                break;
            case ModConfigSchema.CFG_SNAPLOOK_KEYBIND:
                manager.setSnaplookKeybind(parseInt(value, manager.getSnaplookKeybind()));
                break;
            case ModConfigSchema.CFG_SNAPLOOK_RETURN_SPEED:
                manager.setSnaplookReturnSpeed(parseInt(value, manager.getSnaplookReturnSpeed()));
                break;
            case ModConfigSchema.CFG_GYRO_SENSITIVITY_X:
                manager.setGyroSensitivityX(parseInt(value, manager.getGyroSensitivityX()));
                break;
            case ModConfigSchema.CFG_GYRO_SENSITIVITY_Y:
                manager.setGyroSensitivityY(parseInt(value, manager.getGyroSensitivityY()));
                break;
            case ModConfigSchema.CFG_GYRO_INVERT_X:
                manager.setGyroInvertX(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_GYRO_INVERT_Y:
                manager.setGyroInvertY(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_GYRO_DEADZONE:
                manager.setGyroDeadzone(parseInt(value, manager.getGyroDeadzone()));
                break;
            case ModConfigSchema.CFG_HOTBAR_ITEM_ICONS:
                manager.setHotbarItemIconsEnabled(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_OFFHAND_MODE:
                manager.setOffhandMode(parseInt(value, manager.getOffhandMode()));
                break;
            case ModConfigSchema.CFG_OFFHAND_INVENTORY_KEY:
                manager.setOffhandInventoryKey(parseInt(value, manager.getOffhandInventoryKey()));
                break;
            case ModConfigSchema.CFG_OFFHAND_SWAP_KEY:
                // Legacy key kept so a profile saved before the rename still applies.
                manager.setOffhandInventoryKey(parseInt(value, manager.getOffhandInventoryKey()));
                break;
            case ModConfigSchema.CFG_OFFHAND_USE_KEY:
                manager.setOffhandUseKey(parseInt(value, manager.getOffhandUseKey()));
                break;
            case ModConfigSchema.CFG_OFFHAND_KEYBIND:
                manager.setOffhandKeybind(parseInt(value, manager.getOffhandKeybind()));
                break;
            case ModConfigSchema.CFG_AIM_SMOOTHING:
                manager.setAimSmoothing(parseInt(value, manager.getAimSmoothing()));
                break;
            case ModConfigSchema.CFG_AIM_SENSITIVITY:
                manager.setAimSensitivity(parseInt(value, manager.getAimSensitivity()));
                break;
            case ModConfigSchema.CFG_AIM_CROSSHAIR:
                manager.setAimCrosshairEnabled(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_AIM_CROSSHAIR_STYLE:
                manager.setAimCrosshairStyle(parseInt(value, manager.getAimCrosshairStyle()));
                break;
            case ModConfigSchema.CFG_AIM_CROSSHAIR_COLOR: {
                try {
                    manager.setAimCrosshairColor(Color.parseColor(value));
                } catch (Exception ignored) {
                    AppLog.warnOnce("CaramelClient", "InbuiltModuleProvider:1529", "Ignored a failure at InbuiltModuleProvider:1529", ignored);
                }
                break;
            }
            case ModConfigSchema.CFG_AIM_FLASH:
                manager.setAimFlashEnabled(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_ARMOR_HUD_SHOW_TARGET:
                manager.setArmorHudShowTarget(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_ARMOR_HUD_SHOW_ENCHANTS:
                manager.setArmorHudShowEnchants(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_ARMOR_HUD_STACKED:
                manager.setArmorHudStacked(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_ARMOR_HUD_REFRESH_MS:
                manager.setArmorHudRefreshMs(parseInt(value, manager.getArmorHudRefreshMs()));
                break;
            case ModConfigSchema.CFG_CRYSTAL_MANUAL_ASSIST:
                manager.setCrystalManualAssist(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_CRYSTAL_MIN_SELF_HP:
                manager.setCrystalMinSelfHp(parseInt(value, manager.getCrystalMinSelfHp()));
                break;
            case ModConfigSchema.CFG_CRYSTAL_MAX_RANGE:
                manager.setCrystalMaxRange(parseInt(value, manager.getCrystalMaxRange()));
                break;
            case ModConfigSchema.CFG_CRYSTAL_PLACEMENT_DELAY_MS:
                manager.setCrystalPlacementDelayMs(parseInt(value, manager.getCrystalPlacementDelayMs()));
                break;
            case ModConfigSchema.CFG_CRYSTAL_KEYBIND:
                manager.setCrystalKeybind(parseInt(value, manager.getCrystalKeybind()));
                break;
            case ModConfigSchema.CFG_HITREG_SENSITIVITY:
                manager.setHitRegSensitivity(parseInt(value, manager.getHitRegSensitivity()));
                break;
            case ModConfigSchema.CFG_HITREG_SMOOTHING:
                manager.setHitRegSmoothing(parseInt(value, manager.getHitRegSmoothing()));
                break;
            case ModConfigSchema.CFG_HITREG_PREDICTION:
                manager.setHitRegPrediction(parseInt(value, manager.getHitRegPrediction()));
                break;
            case ModConfigSchema.CFG_HITREG_HAPTIC:
                manager.setHitRegHapticEnabled(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HIT_TIMING_COOLDOWN_MS:
                manager.setHitTimingCooldownMs(parseInt(value, manager.getHitTimingCooldownMs()));
                break;
            case ModConfigSchema.CFG_HIT_TIMING_SHOW_COMBO:
                manager.setHitTimingShowCombo(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HIT_TIMING_SHOW_BAR:
                manager.setHitTimingShowTimingBar(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_PLAYERS:
                manager.setHitboxShowPlayers(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_MOBS:
                manager.setHitboxShowMobs(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_ITEMS:
                manager.setHitboxShowItems(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_PROJECTILES:
                manager.setHitboxShowProjectiles(parseBoolean(value));
                org.chimeramc.client.core.mods.inbuilt.overlay.HitboxMod.onConfigChanged(manager);
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_THROWN_ITEMS:
                manager.setHitboxShowThrownItems(parseBoolean(value));
                org.chimeramc.client.core.mods.inbuilt.overlay.HitboxMod.onConfigChanged(manager);
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_LOOK_LINE:
                manager.setHitboxShowLookLine(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_CRIT_LINE:
                manager.setHitboxShowCritLine(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_COMBO_BOX:
                manager.setHitboxShowComboBox(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_HITBOX_SHOW_TARGET_BOX:
                manager.setHitboxShowTargetBox(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_REACH_POSITION:
                manager.setReachIndicatorPosition(parseInt(value, manager.getReachIndicatorPosition()));
                break;
            case ModConfigSchema.CFG_TRAJECTORY_COLOR:
                try {
                    manager.setTrajectoryColor(Color.parseColor(value));
                } catch (Exception ignored) {
                    AppLog.warnOnce("CaramelClient", "InbuiltModuleProvider:1620", "Ignored a failure at InbuiltModuleProvider:1620", ignored);
                }
                break;
            case ModConfigSchema.CFG_HIT_PREDICTION_LOOKAHEAD:
                manager.setHitPredictionLookAheadMs(parseInt(value, manager.getHitPredictionLookAheadMs()));
                break;
            case ModConfigSchema.CFG_KILL_EFFECT_STYLE:
                manager.setKillEffectStyle(parseInt(value, manager.getKillEffectStyle()));
                break;
            case ModConfigSchema.CFG_KILL_EFFECT_COLOR:
                try {
                    manager.setKillEffectColor(Color.parseColor(value));
                } catch (Exception ignored) {
                    AppLog.warnOnce("CaramelClient", "InbuiltModuleProvider:1633", "Ignored a failure at InbuiltModuleProvider:1633", ignored);
                }
                break;
            case ModConfigSchema.CFG_VOICE_MIC:
                manager.setVoiceMicEnabled(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_VOICE_ICON_STYLE:
                manager.setVoiceIconStyle(parseBoolean(value)
                        ? org.chimeramc.client.core.mods.inbuilt.overlay.MicIconStyle.STYLE_SMOOTH
                        : org.chimeramc.client.core.mods.inbuilt.overlay.MicIconStyle.STYLE_CLASSIC);
                break;
            case ModConfigSchema.CFG_VOICE_ICON_ANIMATE:
                manager.setVoiceIconAnimated(parseBoolean(value));
                break;
            case ModConfigSchema.CFG_VOICE_ICON_NAMETAG:
                manager.setVoiceNametagIconEnabled(parseBoolean(value));
                break;
            // These keys are no longer surfaced in the module dialog (they live on the Voice tab),
            // but a config value restored from an older backup can still carry them. They are
            // handled so an import cannot silently drop a stored preference.
            case ModConfigSchema.CFG_VOICE_RANGE:
                manager.setVoiceRangeBlocks(parseInt(value, (int) manager.getVoiceRangeBlocks()));
                break;
            case ModConfigSchema.CFG_VOICE_VOLUME:
                manager.setVoiceVolumePercent(parseInt(value, manager.getVoiceVolumePercent()));
                break;
            case ModConfigSchema.CFG_VOICE_CHANNEL: {
                String typed = value == null ? "" : value.trim();
                String normalized = org.chimeramc.client.core.voice.VoiceChannel.normalize(typed);
                boolean privateChannel =
                        org.chimeramc.client.core.voice.VoiceChannel.isJoinCode(normalized);
                manager.joinVoiceChannel(normalized, typed, privateChannel);
                break;
            }
            default:
                break;
        }
        if (ModIds.AIM_SETTINGS.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.AimSettingsMod.onConfigChanged(manager);
        } else if (ModIds.ARMOR_HUD.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.ArmorHudMod.onConfigChanged(manager);
        } else if (ModIds.CRYSTAL_OPTIMIZER.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.CrystalOptimizerMod.onConfigChanged(manager);
        } else if (ModIds.HIT_REGISTRATION.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.HitRegistrationMod.onConfigChanged(manager);
        } else if (ModIds.HIT_TIMING.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.HitTimingMod.onConfigChanged(manager);
        } else if (ModIds.HITBOX.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.HitboxMod.onConfigChanged(manager);
        } else if (ModIds.REACH_INDICATOR.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.ReachIndicatorMod.onConfigChanged(manager);
        } else if (ModIds.TRAJECTORY_PREDICTION.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.TrajectoryPredictionMod.onConfigChanged(manager);
        } else if (ModIds.HIT_PREDICTION.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.HitPredictionMod.onConfigChanged(manager);
        } else if (ModIds.KILL_EFFECTS.equals(mod.getId())) {
            org.chimeramc.client.core.mods.inbuilt.overlay.KillEffectsMod.onConfigChanged(manager);
        } else if (ModIds.VOICE_CHAT.equals(mod.getId())) {
            org.chimeramc.client.core.voice.VoiceChatModule module =
                    org.chimeramc.client.core.voice.VoiceChatModule.peek();
            if (module != null) module.applyConfig(manager);
        }
    }

    /**
     * Stores an FPS module option. The type is taken from the catalogue, so a toggle key can never be
     * written as an int (or the reverse) — the exact class-cast-on-read bug the settings backup
     * restore guards against.
     */
    private static void setFpsConfig(InbuiltModManager manager, UnifiedMod mod, String key,
                                     String value) {
        org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Option option = null;
        org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Module module =
                org.chimeramc.client.core.mods.fps.FpsModuleCatalog.find(mod.getId());
        if (module != null) {
            for (org.chimeramc.client.core.mods.fps.FpsModuleCatalog.Option candidate : module.options) {
                if (candidate.key.equals(key)) {
                    option = candidate;
                    break;
                }
            }
        }
        if (option == null) return;
        if (option.isToggle()) {
            manager.setModConfigBool(key, parseBoolean(value));
        } else {
            manager.setModConfigInt(key, parseInt(value, Integer.parseInt(option.defaultValue)));
        }
    }

    /**
     * Writes a present-frame option, then pushes the whole plan to the native seam.
     *
     * <p>The push is what makes an option live: the seam reads the plan on the render thread at the
     * present boundary, so a changed strength reaches the next presented frame without a restart. A
     * no-op when the native seam is absent, which is exactly when the modules are badged anyway.
     */
    private static void setPresentFrameConfig(InbuiltModManager manager, UnifiedMod mod, String key,
                                              String value) {
        org.chimeramc.client.core.mods.render.PresentFrameCatalog.Module module =
                org.chimeramc.client.core.mods.render.PresentFrameCatalog.find(mod.getId());
        if (module == null) return;
        org.chimeramc.client.core.mods.render.PresentFrameCatalog.Option option = null;
        for (org.chimeramc.client.core.mods.render.PresentFrameCatalog.Option candidate
                : module.options) {
            if (candidate.key.equals(key)) {
                option = candidate;
                break;
            }
        }
        if (option == null) return;
        if (option.isToggle()) {
            manager.setModConfigBool(key, parseBoolean(value));
        } else {
            manager.setModConfigInt(key, parseInt(value, Integer.parseInt(option.defaultValue)));
        }
        pushPresentFramePlan(manager);
    }

    /** Pushes the stored present-frame plan to the native seam (fail-closed when it is absent). */
    private static void pushPresentFramePlan(InbuiltModManager manager) {
        try {
            org.chimeramc.client.core.mods.render.PresentFramePolicy.ValueSource values =
                    new org.chimeramc.client.core.mods.render.PresentFramePolicy.ValueSource() {
                        @Override
                        public int integer(String key, int fallback) {
                            return manager.getModConfigInt(key, fallback);
                        }

                        @Override
                        public boolean bool(String key, boolean fallback) {
                            return manager.getModConfigBool(key, fallback);
                        }
                    };
            org.chimeramc.client.core.mods.render.PresentFramePolicy.Plan plan =
                    org.chimeramc.client.core.mods.render.PresentFramePolicy.plan(
                            org.chimeramc.client.core.backend.HookAvailability.isLive(
                                    org.chimeramc.client.core.backend.HookAvailability.Hook.PRESENT),
                            manager.resolveInbuiltModEnabled(ModIds.FRAME_GENERATION, false),
                            manager.resolveInbuiltModEnabled(ModIds.MOTION_BLUR, false),
                            values);
            org.chimeramc.client.preloader.PreloaderInput.setPresentFramePlan(
                    plan.frameGenerationEnabled, plan.interpolationStrength,
                    plan.maxFramesPerRendered, plan.reduceEdgeArtifacts, plan.motionBlurEnabled,
                    plan.blurStrength, plan.blurBudget, plan.weightByViewSpeed);
        } catch (Throwable t) {
            // A missing native seam is expected on a build without the present hook; the modules are
            // badged in that case, so nothing is silently lost.
            AppLog.warnOnce("CaramelClient", "InbuiltModuleProvider:pushPresentFramePlan",
                    "Present-frame plan push skipped", t);
        }
    }

    private static boolean setHotbarSlotConfig(InbuiltModManager manager, String key, String value) {
        for (int slot = 1; slot <= 9; slot++) {
            String overlayKey = ModIds.HOTBAR_SLOT + ":" + slot;
            if (ModConfigSchema.hotbarSlotConfigKey(slot, ModConfigSchema.CFG_HOTBAR_SLOT_ENABLED).equals(key)) {
                manager.setHotbarSlotEnabled(slot, parseBoolean(value));
                return true;
            }
            if (ModConfigSchema.hotbarSlotConfigKey(slot, ModConfigSchema.CFG_HOTBAR_SLOT_SIZE).equals(key)) {
                manager.setOverlayButtonSize(overlayKey, parseInt(value, manager.getOverlayButtonSize(overlayKey)));
                return true;
            }
            if (ModConfigSchema.hotbarSlotConfigKey(slot, ModConfigSchema.CFG_HOTBAR_SLOT_OPACITY).equals(key)) {
                manager.setOverlayOpacity(overlayKey, parseInt(value, manager.getOverlayOpacity(overlayKey)));
                return true;
            }
        }
        return false;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** Delegates to {@link ModConfigSchema}; kept so the schema mapping stays under test here. */
    static String schemaTypeFor(UnifiedMod.ConfigType type) {
        return ModConfigSchema.schemaTypeFor(type);
    }

    /** Delegates to {@link ModConfigSchema}; kept so the description table stays under test here. */
    static int configDescriptionRes(String key) {
        return ModConfigSchema.configDescriptionRes(key);
    }

    private static boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
