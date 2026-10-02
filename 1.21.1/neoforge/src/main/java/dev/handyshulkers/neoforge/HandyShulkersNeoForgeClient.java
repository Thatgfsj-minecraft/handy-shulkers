package dev.handyshulkers.neoforge;

import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.ScrollingMenu;
import dev.handyshulkers.ScrollingScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side registration for the scrolling container screen. */
@EventBusSubscriber(modid = HandyShulkers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HandyShulkersNeoForgeClient {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(HandyShulkersNeoForge.SCROLLING_MENU.get(), ScrollingScreen::new);
    }

    private HandyShulkersNeoForgeClient() {
    }
}
