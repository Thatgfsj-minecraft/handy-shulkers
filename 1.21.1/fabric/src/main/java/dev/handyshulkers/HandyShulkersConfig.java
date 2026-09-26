package dev.handyshulkers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HandyShulkersConfig {

    /** Right-clicking while aiming at the air opens the box. */
    public boolean openInAir = true;
    /** Right-clicking a block that has no menu of its own opens the box. */
    public boolean openOnBlocks = true;
    /** When true, only sneaking opens boxes; sneaking otherwise never opens. */
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
                config = GSON.fromJson(Files.readString(path), HandyShulkersConfig.class);
            } catch (Exception e) {
                System.err.println("[" + HandyShulkers.MOD_ID + "] Failed to read config, using defaults: " + e);
                config = new HandyShulkersConfig();
            }
        }
        if (config == null) {
            config = new HandyShulkersConfig();
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
