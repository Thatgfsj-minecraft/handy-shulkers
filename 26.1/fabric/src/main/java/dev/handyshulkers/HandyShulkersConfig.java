package dev.handyshulkers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HandyShulkersConfig {

    /**
     * False (default): plain right-click uses the held functional item
     * (shulker box opens, held bed sleeps in place, held crafting table or
     * loom shows its screen); sneak + right-click places it. Aimed blocks
     * with a menu of their own (chests, furnaces, ...) always win.
     * True: reversed - plain right-click stays fully vanilla and sneak +
     * right-click uses the item instead.
     */
    public boolean requireSneak = false;
    /**
     * When false, items whose storage is not the vanilla container component
     * are refused, so nothing can ever be written into a component the owning
     * mod does not read back.
     */
    public boolean allowUnknownStorage = true;
    /** Let automation fake players open boxes. */
    public boolean allowFakePlayers = false;
    /** Force the menu row count (1-6). -1 detects it from the item. */
    public int forceRows = -1;
    /**
     * Item id -&gt; backing capacity in slots for boxes larger than the 6-row
     * menus. Listed boxes open in the scrolling UI even while their container
     * component is still empty, because an empty component carries no capacity
     * information. Shipped with the compressed shulker box of the
     * compressed-blocks mod; edit freely.
     */
    public java.util.Map<String, Integer> largeBoxes = new java.util.LinkedHashMap<>(
            java.util.Map.of("compressedblocks:compressed_shulker_box", 243));

    private static HandyShulkersConfig instance;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HandyShulkersConfig() {
    }

    public static HandyShulkersConfig get() {
        if (instance == null) {
            instance = new HandyShulkersConfig();
        }
        return instance;
    }

    public static void init(Path path) {
        instance = load(path);
        save(path);
    }

    private static HandyShulkersConfig load(Path path) {
        HandyShulkersConfig config = new HandyShulkersConfig();
        if (Files.exists(path)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                // Read field by field instead of GSON.fromJson: Gson allocates
                // the instance without running constructors, so a key missing
                // from the file would silently reset a declared default of
                // true back to false.
                // Each field gets its own guard: one malformed value keeps its
                // default instead of wiping every user customization on the
                // following save.
                if (json.has("requireSneak")) {
                    try {
                        config.requireSneak = json.get("requireSneak").getAsBoolean();
                    } catch (Exception ignored) {
                    }
                }
                if (json.has("allowUnknownStorage")) {
                    try {
                        config.allowUnknownStorage = json.get("allowUnknownStorage").getAsBoolean();
                    } catch (Exception ignored) {
                    }
                }
                if (json.has("allowFakePlayers")) {
                    try {
                        config.allowFakePlayers = json.get("allowFakePlayers").getAsBoolean();
                    } catch (Exception ignored) {
                    }
                }
                if (json.has("forceRows")) {
                    try {
                        config.forceRows = json.get("forceRows").getAsInt();
                    } catch (Exception ignored) {
                    }
                }
                if (json.has("largeBoxes") && json.get("largeBoxes").isJsonObject()) {
                    config.largeBoxes.clear();
                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry
                            : json.getAsJsonObject("largeBoxes").entrySet()) {
                        try {
                            config.largeBoxes.put(entry.getKey(), entry.getValue().getAsInt());
                        } catch (Exception ignored) {
                            // a malformed capacity line must not kill the whole config
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[" + HandyShulkers.MOD_ID + "] Failed to read config, using defaults: " + e);
                config = new HandyShulkersConfig();
            }
        }
        if (config.forceRows < 1 || config.forceRows > 6) {
            config.forceRows = -1;
        }
        return config;
    }

    private static void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(get()));
        } catch (IOException e) {
            System.err.println("[" + HandyShulkers.MOD_ID + "] Failed to write config: " + e);
        }
    }
}
