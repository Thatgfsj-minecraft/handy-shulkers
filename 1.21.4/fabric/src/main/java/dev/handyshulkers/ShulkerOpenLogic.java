package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shulker box specific logic: tag membership, storage safety and row sizing.
 * The actual hand-use dispatch lives in {@link HandItemUse}.
 */
public final class ShulkerOpenLogic {

    private ShulkerOpenLogic() {
    }

    public static boolean isOpenable(ItemStack stack) {
        return !stack.isEmpty() && stack.is(HandyShulkers.SHULKER_BOXES);
    }

    /**
     * True when the item certainly keeps its contents in the vanilla container
     * component, meaning opening it can never desync from the owning mod.
     */
    public static boolean hasKnownStorage(ItemStack stack) {
        if (stack.has(DataComponents.CONTAINER)) {
            return true;
        }
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static int rowsFor(ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (config.forceRows >= 1 && config.forceRows <= 6) {
            return config.forceRows;
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock entityBlock) {
            try {
                BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
                if (blockEntity instanceof net.minecraft.world.Container container && container.getContainerSize() > 0) {
                    return Mth.clamp((container.getContainerSize() + 8) / 9, 1, 6);
                }
            } catch (Throwable ignored) {
                // Some modded block entities are not safe to construct headless; fall through.
            }
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            int slots = (int) contents.stream().count();
            if (slots > 0) {
                return Mth.clamp((slots + 8) / 9, 1, 6);
            }
        }
        return 3;
    }

    /**
     * Opens the box inventory. Server side only; every gate (sneak, fake
     * players, spectator, open menus) has already been applied by
     * {@link HandItemUse}.
     */
    public static InteractionResult openMenu(ServerPlayer serverPlayer, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!config.allowUnknownStorage && !hasKnownStorage(stack)) {
            return InteractionResult.PASS;
        }
        int rows = rowsFor(stack);
        // Menus top out at 6 rows (54 slots). A bigger box would silently lose
        // everything beyond slot 54 on the first write-back, so refuse it.
        if (config.forceRows < 1 && capacityOverLimit(stack)) {
            serverPlayer.displayClientMessage(Component.translatable("message.handyshulkers.box_too_large"), true);
            return InteractionResult.SUCCESS_SERVER;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ShulkerMenu(id, inventory, stack, rows),
                stack.getHoverName()));
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean capacityOverLimit(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock entityBlock) {
            try {
                BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
                if (blockEntity instanceof net.minecraft.world.Container container && container.getContainerSize() > 54) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        return contents != null && contents.stream().count() > 54;
    }

    /**
     * Blocks that open a menu of their own (chests, crafting tables, looms,
     * furnaces, ...) always keep their vanilla behaviour.
     */
    public static boolean blockHasOwnMenu(Player player, BlockPos pos) {
        return player.level().getBlockState(pos).getMenuProvider(player.level(), pos) != null;
    }
}
