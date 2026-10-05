package dev.handyshulkers.forge;

import dev.handyshulkers.HandItemUse;
import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.ScrollingMenu;
import dev.handyshulkers.SelfTest;
import dev.handyshulkers.TempBedTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(HandyShulkers.MOD_ID)
public class HandyShulkersForge {

    public static final DeferredRegister<MenuType<?>> MENUS =
            // 1.18.2 note: the menu registry is ForgeRegistries.CONTAINERS here
            // (renamed to MENU_TYPES only in 1.19.x).
            DeferredRegister.create(ForgeRegistries.CONTAINERS, HandyShulkers.MOD_ID);
    // 1.18.2: plain vanilla MenuType — no feature flags, no extended factory.
    public static final RegistryObject<MenuType<ScrollingMenu>> SCROLLING_MENU =
            MENUS.register("scrolling", () -> new MenuType<>(ScrollingMenu::clientCreate));

    public HandyShulkersForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        HandyShulkersConfig.init(FMLPaths.CONFIGDIR.get().resolve("handyshulkers.json"));
        MENUS.register(modEventBus);
        modEventBus.addListener((FMLCommonSetupEvent event) ->
                ScrollingMenu.TYPE = SCROLLING_MENU.get());
        SelfTest.run();
        MinecraftForge.EVENT_BUS.register(HandyShulkersForge.class);
        MinecraftForge.EVENT_BUS.addListener(HandyShulkersForge::onServerTick);
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                TempBedTracker.tick(server);
            }
        }
    }

    /**
     * Plain right-click on a block: a block with its own menu (chest, crafting
     * table, ...) always wins; otherwise the held functional item is used
     * instead of placing it. Sneaking stays vanilla (place).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 1.18.2 note: the event exposes getPlayer()/getWorld() in this era.
        if (HandyShulkersConfig.get().requireSneak != event.getPlayer().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getPlayer().getItemInHand(event.getHand());
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        Level level = event.getWorld();
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).getMenuProvider(level, pos) != null) {
            return;
        }
        InteractionResult result = HandItemUse.useFromHand(event.getPlayer(), stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (HandyShulkersConfig.get().requireSneak != event.getPlayer().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getPlayer().getItemInHand(event.getHand());
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        InteractionResult result = HandItemUse.useFromHand(event.getPlayer(), stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
