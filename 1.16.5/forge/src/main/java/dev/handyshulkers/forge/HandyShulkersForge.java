package dev.handyshulkers.forge;

import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Mod entry point. All gameplay wiring lives in
 * {@link HandyShulkersForgeEventHandler} (Forge bus) — this class only makes
 * sure the JSON config exists before the first interaction.
 */
@Mod(HandyShulkers.MOD_ID)
public class HandyShulkersForge {

    public HandyShulkersForge() {
        HandyShulkersConfig.init(FMLPaths.CONFIGDIR.get().resolve("handyshulkers.json"));
    }
}
