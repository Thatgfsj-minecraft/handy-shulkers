package dev.handyshulkers.fabric;

import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.ShulkerOpenLogic;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class HandyShulkersFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        HandyShulkersConfig.init(FabricLoader.getInstance().getConfigDir().resolve("handyshulkers.json"));
        UseBlockCallback.EVENT.register(HandyShulkersFabric::onUseBlock);
        UseItemCallback.EVENT.register(HandyShulkersFabric::onUseItem);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ShulkerOpenLogic.isOpenable(stack)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        return ShulkerOpenLogic.tryOpenOnBlock(player, pos, stack);
    }

    private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (HandyShulkersConfig.get().openInAir && ShulkerOpenLogic.isOpenable(stack)) {
            InteractionResult result = ShulkerOpenLogic.tryOpen(player, stack);
            if (result != InteractionResult.PASS) {
                return new InteractionResultHolder<>(result, stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
