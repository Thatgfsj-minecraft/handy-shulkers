package dev.handyshulkers.fabric;

import dev.handyshulkers.HandItemUse;
import dev.handyshulkers.HandyShulkersConfig;
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

    /**
     * Plain right-click on a block: a block with its own menu (chest, crafting
     * table, ...) always wins; otherwise the held functional item is used
     * instead of placing it. Sneaking stays vanilla (place).
     */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!HandItemUse.isFunctional(stack)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (level.getBlockState(pos).getMenuProvider(level, pos) != null) {
            return InteractionResult.PASS;
        }
        return HandItemUse.useFromHand(player, stack);
    }

    private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!HandItemUse.isFunctional(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = HandItemUse.useFromHand(player, stack);
        if (result != InteractionResult.PASS) {
            return new InteractionResultHolder<>(result, stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
