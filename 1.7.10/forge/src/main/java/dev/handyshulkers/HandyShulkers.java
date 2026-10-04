package dev.handyshulkers;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

/**
 * Handy Shulkers — 1.7.10 根版本（该版本原版没有潜影盒）。
 *
 * <p>移植自 1.21.x 语义：手持右键“拿着就用”——白名单存储盒打开、手持床就地入睡、
 * 手持工作台/附魔台/铁砧显示界面、手持末影箱打开末影空间。潜行+右键保持原版放置，
 * 被瞄准且自带界面的方块始终优先。全部逻辑服务端权威，客户端只透传原版包。
 */
@Mod(modid = HandyShulkers.MOD_ID, name = HandyShulkers.MOD_NAME, version = HandyShulkers.VERSION,
        acceptedMinecraftVersions = "[1.7.10]")
public final class HandyShulkers {

    public static final String MOD_ID = "handyshulkers";
    public static final String MOD_NAME = "Handy Shulkers";
    public static final String VERSION = "1.4.1";

    /** NetworkRegistry 的 IGuiHandler 界面 id。 */
    public static final int GUI_BOX = 0;
    public static final int GUI_CRAFTING = 1;
    public static final int GUI_ENCHANTING = 2;
    public static final int GUI_ANVIL = 3;
    public static final int GUI_ENDER_CHEST = 4;

    @Mod.Instance(MOD_ID)
    public static HandyShulkers instance;

    public static Logger logger;

    private final HandItemUse handItemUse = new HandItemUse();
    private final TempBedTracker bedTracker = new TempBedTracker();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        HandyShulkersConfig.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        // Forge 总线：PlayerInteractEvent / PlayerWakeUpEvent / PlayerLoggedOutEvent
        MinecraftForge.EVENT_BUS.register(handItemUse);
        MinecraftForge.EVENT_BUS.register(bedTracker);
        // FML 总线：ServerTickEvent（1.7.10 双总线惯例）
        FMLCommonHandler.instance().bus().register(bedTracker);
    }

    /** 1.7.10 存在该事件；在开服早期跑一次无头自检（SELF-TEST）。 */
    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        SelfTest.run();
    }
}
