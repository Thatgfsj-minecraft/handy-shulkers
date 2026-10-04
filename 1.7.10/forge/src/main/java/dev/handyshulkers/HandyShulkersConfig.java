package dev.handyshulkers;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 配置（1.7.10 原生 .cfg，键与 1.21.x 对齐）：
 * requireSneak / allowUnknownStorage / allowFakePlayers / forceRows /
 * extraShulkerBoxes / largeBoxes。
 * 与 1.21.x 的差异：1.7.10 没有 >54 格界面，largeBoxes 只保留“白名单”语义，
 * 不再携带容量（无滚动 UI 可承载）。
 */
public final class HandyShulkersConfig {

    private static final String CATEGORY = "general";

    /** false（默认）：普通右键=使用手中功能物品，潜行+右键=原版放置；true：方向反转。 */
    public boolean requireSneak = false;
    /**
     * false：只打开白名单声明的盒类物品（存储约定已知，绝不错写）。
     * true（默认）：NBT 里已带 Items 列表、且方块形态是 TileEntity 容器的物品也按盒处理。
     */
    public boolean allowUnknownStorage = true;
    /** 允许自动化假玩家触发手持功能。 */
    public boolean allowFakePlayers = false;
    /** 强制菜单行数（1-6）；-1 按盒内已存槽位推断。 */
    public int forceRows = -1;

    /** 模组盒物品 id（domain:name，小写）：内容存于原版约定的 NBT Items 列表。 */
    public final Set<String> extraShulkerBoxes = new HashSet<String>();
    /** 大盒白名单（1.7.10 仅作白名单语义）。 */
    public final Set<String> largeBoxes = new HashSet<String>();

    private static HandyShulkersConfig instance = new HandyShulkersConfig();

    public static HandyShulkersConfig get() {
        return instance;
    }

    public static void init(File file) {
        Configuration config = new Configuration(file);
        config.load();
        HandyShulkersConfig fresh = new HandyShulkersConfig();
        fresh.requireSneak = config.getBoolean("requireSneak", CATEGORY, false,
                "false: plain right-click uses the held functional item, sneak+right-click places it. true: reversed.");
        fresh.allowUnknownStorage = config.getBoolean("allowUnknownStorage", CATEGORY, true,
                "false: only whitelisted box items open from the hand. true: items already carrying an NBT 'Items' list whose block form is a tile-entity container are also treated as boxes.");
        fresh.allowFakePlayers = config.getBoolean("allowFakePlayers", CATEGORY, false,
                "Let automation fake players trigger hand-use.");
        fresh.forceRows = config.getInt("forceRows", CATEGORY, -1, -1, 6,
                "Force the menu row count (1-6). -1 detects it from the item's stored slots.");
        String[] extra = config.getStringList("extraShulkerBoxes", CATEGORY, new String[0],
                "Mod storage-box item ids (domain:name) whose contents live in the vanilla-convention NBT 'Items' list. They open from the hand like shulker boxes.");
        String[] large = config.getStringList("largeBoxes", CATEGORY,
                new String[] {"compressedblocks:compressed_shulker_box"},
                "Item ids treated as shulker-box-like for hand-open and the nesting guard. 1.7.10 has no >54-slot UI, so this is a plain whitelist (no capacity).");
        fill(fresh.extraShulkerBoxes, extra);
        fill(fresh.largeBoxes, large);
        if (config.hasChanged()) {
            config.save();
        }
        instance = fresh;
    }

    private static void fill(Set<String> target, String[] values) {
        target.clear();
        for (int i = 0; i < values.length; i++) {
            String id = values[i];
            if (id != null) {
                id = id.trim().toLowerCase();
                if (id.length() > 0) {
                    target.add(id);
                }
            }
        }
    }
}
