package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
 * Shared decision logic for opening a box from the hand.
 *
 * <p>Interaction rules (all server authoritative, client only predicts):
 * <ul>
 *   <li>Right-click the air with a box: opens it.</li>
 *   <li>Right-click a block that has no menu of its own: opens the box —
 *       this is the Quick Shulker style "works against any block" behaviour,
 *       minus the head slot. Nothing is ever equipped, so helmets do not
 *       matter at all.</li>
 *   <li>Right-click a block with a menu (chest, crafting table, ...): the
 *       block wins, the box is not opened.</li>
 *   <li>Sneaking always keeps vanilla behaviour (place the box, use the
 *       block), unless {@code requireSneak} flips the trigger.</li>
 * </ul>
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
     * Opens the box, on the server only. The client side returns success so
     * the hand swings and no placement is predicted.
     */
    public static InteractionResult tryOpen(Player player, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!isOpenable(stack)) {
            return InteractionResult.PASS;
        }
        if (config.requireSneak != player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!config.allowUnknownStorage && !hasKnownStorage(stack)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!config.allowFakePlayers && player.getClass() != ServerPlayer.class) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
            return InteractionResult.PASS;
        }
        int rows = rowsFor(stack);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ShulkerMenu(id, inventory, stack, rows),
                stack.getHoverName()));
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Blocks that open a menu of their own keep their vanilla behaviour.
     */
    public static boolean blockHasOwnMenu(Player player, BlockPos pos) {
        return player.level().getBlockState(pos).getMenuProvider(player.level(), pos) != null;
    }

    /**
     * Shared gate for "right click on a block" handlers.
     */
    public static InteractionResult tryOpenOnBlock(Player player, BlockPos pos, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!config.openOnBlocks) {
            return InteractionResult.PASS;
        }
        if (!config.requireSneak && !player.isShiftKeyDown() && blockHasOwnMenu(player, pos)) {
            return InteractionResult.PASS;
        }
        return tryOpen(player, stack);
    }
}
