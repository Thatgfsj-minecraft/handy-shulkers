package dev.handyshulkers.neoforge;

import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.ShulkerOpenLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(HandyShulkers.MOD_ID)
public class HandyShulkersNeoForge {

    public HandyShulkersNeoForge() {
        HandyShulkersConfig.init(FMLPaths.CONFIGDIR.get().resolve("handyshulkers.json"));
        NeoForge.EVENT_BUS.register(HandyShulkersNeoForge.class);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!ShulkerOpenLogic.isOpenable(stack)) {
            return;
        }
        BlockPos pos = event.getPos();
        InteractionResult result = ShulkerOpenLogic.tryOpenOnBlock(event.getEntity(), pos, stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (!ShulkerOpenLogic.isOpenable(stack)) {
            return;
        }
        if (!HandyShulkersConfig.get().openInAir) {
            return;
        }
        InteractionResult result = ShulkerOpenLogic.tryOpen(event.getEntity(), stack);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
