package dev.handyshulkers.neoforge;

import dev.handyshulkers.HandItemUse;
import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.SelfTest;
import dev.handyshulkers.TempBedTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(HandyShulkers.MOD_ID)
public class HandyShulkersNeoForge {

    public HandyShulkersNeoForge() {
        HandyShulkersConfig.init(FMLPaths.CONFIGDIR.get().resolve("handyshulkers.json"));
        SelfTest.run();
        NeoForge.EVENT_BUS.register(HandyShulkersNeoForge.class);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) ->
                TempBedTracker.tick(event.getServer()));
    }

    /**
     * Plain right-click on a block: a block with its own menu (chest, crafting
     * table, ...) always wins; otherwise the held functional item is used
     * instead of placing it. Sneaking stays vanilla (place).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (HandyShulkersConfig.get().requireSneak != event.getEntity().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).getMenuProvider(level, pos) != null) {
            return;
        }
        InteractionResult result = HandItemUse.useFromHand(event.getEntity(), stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (HandyShulkersConfig.get().requireSneak != event.getEntity().isShiftKeyDown()) {
            return;
        }
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!HandItemUse.isFunctional(stack)) {
            return;
        }
        InteractionResult result = HandItemUse.useFromHand(event.getEntity(), stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
