package dev.handyshulkers.forge;

import dev.handyshulkers.HandItemUse;
import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.SelfTest;
import dev.handyshulkers.TempBedTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

/**
 * Plain right-click on a block: a block with its own menu (chest, crafting
 * table, ...) always wins; otherwise the held functional item is used instead
 * of placing it. Sneaking stays vanilla (place). Air right-clicks go through
 * {@link PlayerInteractEvent.RightClickItem} the same way.
 *
 * <p>1.16.5 event note: {@code RightClickBlock} fires before the aimed
 * block's own use, so the "block first" rule is enforced inside the handler
 * by checking the block's container provider. There is no useOn/use result
 * split in this version; PASS always falls through to vanilla. The handlers
 * only act server side (useFromHand returns PASS on the client), so vanilla
 * packets keep flowing and no interaction is ever swallowed.
 */
@Mod.EventBusSubscriber(modid = HandyShulkers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HandyShulkersForgeEventHandler {

    private HandyShulkersForgeEventHandler() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = (PlayerEntity) event.getEntity();
        if (HandyShulkersConfig.get().requireSneak != player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        net.minecraft.world.World level = event.getWorld();
        net.minecraft.util.math.BlockPos pos = event.getPos();
        if (!level.isClientSide && level.getBlockState(pos).getMenuProvider(level, pos) != null) {
            return;
        }
        ActionResultType result = HandItemUse.useFromHand(player, stack);
        if (result != ActionResultType.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        PlayerEntity player = (PlayerEntity) event.getEntity();
        if (HandyShulkersConfig.get().requireSneak != player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        ActionResultType result = HandItemUse.useFromHand(player, stack);
        if (result != ActionResultType.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /** Startup self check: container read/write round-trip. */
    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        SelfTest.run();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TempBedTracker.tick();
        }
    }
}
