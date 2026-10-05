package dev.handyshulkers.forge;

import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.ScrollingMenu;
import dev.handyshulkers.ScrollingScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-side registration for the scrolling container screen. */
@Mod.EventBusSubscriber(modid = HandyShulkers.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HandyShulkersForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(HandyShulkersForge.SCROLLING_MENU.get(), ScrollingScreen::new));
    }

    private HandyShulkersForgeClient() {
    }
}
