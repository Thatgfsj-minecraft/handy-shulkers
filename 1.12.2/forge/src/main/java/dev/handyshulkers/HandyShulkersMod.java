package dev.handyshulkers;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.io.File;

/**
 * Mod entry point. Wires config, the {@link net.minecraftforge.fml.common.network.IGuiHandler}
 * (1.12.2 has no ContainerType/MenuType — vanilla GUI ids plus the FML gui
 * handler registry are the only way to open arbitrary containers) and the
 * Forge event bus.
 */
@Mod(modid = HandyShulkers.MOD_ID,
        name = HandyShulkers.MOD_NAME,
        version = HandyShulkers.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)",
        dependencies = "after:forge")
public class HandyShulkersMod {

    @Mod.Instance(HandyShulkers.MOD_ID)
    public static HandyShulkersMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        HandyShulkers.LOGGER = event.getModLog();
        File config = event.getSuggestedConfigurationFile();
        File dir = config.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        HandyShulkersConfig.load(config);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new HandyShulkersGuiHandler());
        MinecraftForge.EVENT_BUS.register(new HandyShulkersEvents());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    /**
     * 1.12.2 delivers the server lifecycle events only to @Mod.EventHandler
     * methods - nothing posts FMLServerAboutToStartEvent on the Forge event
     * bus, so the self-test hook lives here instead of a @SubscribeEvent.
     */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        SelfTest.run();
    }
}
