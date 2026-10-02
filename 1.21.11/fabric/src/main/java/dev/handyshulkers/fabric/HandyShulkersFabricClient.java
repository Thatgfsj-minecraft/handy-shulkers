package dev.handyshulkers.fabric;

import dev.handyshulkers.ScrollingMenu;
import dev.handyshulkers.ScrollingScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class HandyShulkersFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ScrollingMenu.TYPE, ScrollingScreen::new);
    }
}
