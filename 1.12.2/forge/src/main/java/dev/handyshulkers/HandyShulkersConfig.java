package dev.handyshulkers;

import net.minecraftforge.common.config.Configuration;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Forge .cfg config, same fields as the 1.21.x JSON config plus two
 * 1.12.2-specific whitelists. Differences from 1.21.x are documented per
 * field; see also PORTING.md in the repo root.
 */
public final class HandyShulkersConfig {

    /**
     * False (default): plain right-click (in the air) uses the held functional
     * item (shulker box opens, held bed sleeps in place, held crafting table
     * or enchanting table shows its screen); sneak + right-click keeps vanilla
     * behaviour. Blocks aimed at always behave vanilla (they win).
     * True: reversed - plain right-click stays vanilla and sneak + right-click
     * uses the item instead.
     */
    public boolean requireSneak = false;

    /**
     * When false, items whose storage is not the vanilla shulker box tile
     * entity are refused, so nothing can ever be written into a container the
     * owning mod does not read back. 1.12.2 has no container data component;
     * "known storage" here means an {@code ItemBlock} whose block is a
     * vanilla {@code BlockShulkerBox}.
     */
    public boolean allowUnknownStorage = true;

    /** Let automation fake players open boxes. */
    public boolean allowFakePlayers = false;

    /** Force the menu row count (1-6). -1 detects it from the item. */
    public int forceRows = -1;

    /**
     * 1.12.2 replacement for the 1.21.x tag model: item ids listed here are
     * treated exactly like vanilla shulker boxes (openable from the hand,
     * no-nesting rule applies) without being one of the 17 vanilla colors.
     */
    public String[] extraShulkerBoxes = new String[0];

    /**
     * Item ids treated as shulker-box-like big containers. 1.12.2 keeps only
     * the whitelist semantics ("open it from the hand even though it is not a
     * BlockShulkerBox item") - the 243-slot scrolling UI does not exist here,
     * boxes whose content exceeds 54 slots are refused (see ShulkerOpenLogic).
     */
    public String[] largeBoxes = new String[] {"compressedblocks:compressed_shulker_box"};

    private static HandyShulkersConfig instance;

    private Set<String> extraBoxIds = Collections.emptySet();
    private Set<String> largeBoxIds = Collections.emptySet();

    private HandyShulkersConfig() {
    }

    public static HandyShulkersConfig get() {
        if (instance == null) {
            instance = new HandyShulkersConfig();
        }
        return instance;
    }

    public boolean isExtraBox(String id) {
        return !id.isEmpty() && extraBoxIds.contains(id);
    }

    public boolean isLargeBox(String id) {
        return !id.isEmpty() && largeBoxIds.contains(id);
    }

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        HandyShulkersConfig config = new HandyShulkersConfig();

        config.requireSneak = cfg.getBoolean("requireSneak", "general", false,
                "Require sneak + right-click to use the held item. False: plain right-click uses it, sneak stays vanilla.");
        config.allowUnknownStorage = cfg.getBoolean("allowUnknownStorage", "general", true,
                "Allow opening items whose storage is not the vanilla shulker box tile entity (modded boxes). When false they are refused so edits can never be lost.");
        config.allowFakePlayers = cfg.getBoolean("allowFakePlayers", "general", false,
                "Let automation fake players (e.g. machines) open boxes.");
        config.forceRows = cfg.getInt("forceRows", "general", -1, -1, 6,
                "Force the menu row count (1-6). -1 detects it from the item.");
        config.extraShulkerBoxes = cfg.getStringList("extraShulkerBoxes", "general", new String[0],
                "Item ids treated as shulker boxes (1.12.2 has no tags; this replaces the tag-based extension model of newer versions).");
        config.largeBoxes = cfg.getStringList("largeBoxes", "general",
                new String[] {"compressedblocks:compressed_shulker_box"},
                "Item ids treated as shulker-box-like big containers (whitelist only; content beyond 54 slots is refused because 1.12.2 has no scrolling UI).");

        if (cfg.hasChanged()) {
            cfg.save();
        }

        config.extraBoxIds = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(config.extraShulkerBoxes)));
        config.largeBoxIds = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(config.largeBoxes)));

        if (config.forceRows < 1 || config.forceRows > 6) {
            config.forceRows = -1;
        }
        instance = config;
    }
}
