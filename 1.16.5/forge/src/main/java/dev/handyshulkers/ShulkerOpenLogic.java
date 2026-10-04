package dev.handyshulkers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

/**
 * Shulker box specific logic: tag membership, storage safety and row sizing.
 * The actual hand-use dispatch lives in {@link HandItemUse}.
 *
 * <p>1.16.5 port note: there are no data components and no vanilla shulker box
 * item tag, so detection is item tag ({@code handyshulkers:shulker_boxes},
 * overridable by data packs) plus a hardcoded vanilla-class fallback. Storage
 * is the vanilla {@code BlockEntityTag.Items} convention, and boxes larger
 * than the 54-slot menu limit are refused (1.16.5 has no scrolling menus).
 */
public final class ShulkerOpenLogic {

    private ShulkerOpenLogic() {
    }

    public static boolean isOpenable(ItemStack stack) {
        return !stack.isEmpty()
                && (HandyShulkers.isIn(stack, HandyShulkers.SHULKER_BOXES) || isVanillaShulkerBox(stack));
    }

    /** True for items whose block is a vanilla(-style) shulker box block. */
    public static boolean isVanillaShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem
                && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock;
    }

    /**
     * True when the item certainly keeps its contents in the vanilla
     * {@code BlockEntityTag.Items} NBT, meaning opening it can never desync
     * from the owning mod.
     */
    public static boolean hasKnownStorage(ItemStack stack) {
        if (isVanillaShulkerBox(stack)) {
            return true;
        }
        CompoundNBT tag = stack.getTag();
        return tag != null && tag.contains("BlockEntityTag", 10)
                && tag.getCompound("BlockEntityTag").contains("Items", 9);
    }

    /** Declared capacity from the config's largeBoxes map, or 0. */
    public static int declaredCapacity(ItemStack stack) {
        Integer declared = HandyShulkersConfig.get().largeBoxes.get(
                Registry.ITEM.getKey(stack.getItem()).toString());
        return declared == null ? 0 : declared;
    }

    /**
     * Detected slot count of the box: the declared config capacity wins,
     * otherwise the block's own tile entity reports its size (headless
     * construction; unsafe tiles fall back to the vanilla 27).
     */
    public static int capacityFor(ItemStack stack) {
        int declared = declaredCapacity(stack);
        if (declared > 0) {
            return declared;
        }
        if (stack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block instanceof ContainerBlock) {
                try {
                    TileEntity tileEntity = ((ContainerBlock) block).newBlockEntity(null);
                    if (tileEntity instanceof IInventory) {
                        int size = ((IInventory) tileEntity).getContainerSize();
                        if (size > 0) {
                            return size;
                        }
                    }
                } catch (Throwable ignored) {
                    // Some modded block entities are not safe to construct headless; fall through.
                }
            }
        }
        return 27;
    }

    /**
     * Opens the box inventory. Server side only; every gate (sneak, fake
     * players, spectator, open menus) has already been applied by
     * {@link HandItemUse}. Boxes that exceed the 6-row (54 slot) menu limit of
     * 1.16.5 are refused with an action bar message and a log line instead of
     * silently truncating content.
     */
    public static ActionResultType openMenu(ServerPlayerEntity serverPlayer, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!config.allowUnknownStorage && !hasKnownStorage(stack)) {
            return ActionResultType.PASS;
        }
        int capacity = capacityFor(stack);
        if (capacity > 54) {
            HandyShulkers.LOGGER.info("[handyshulkers] refusing to open '{}' from hand: {} slots exceeds the 54-slot menu limit of 1.16.5",
                    Registry.ITEM.getKey(stack.getItem()), capacity);
            serverPlayer.displayClientMessage(new TranslationTextComponent("message.handyshulkers.box_too_large"), true);
            return ActionResultType.SUCCESS;
        }
        int rows;
        if (config.forceRows >= 1 && config.forceRows <= 6) {
            rows = config.forceRows;
        } else {
            rows = MathHelper.clamp((capacity + 8) / 9, 1, 6);
        }
        serverPlayer.openMenu(new BoxMenuProvider(rows, stack));
        return ActionResultType.SUCCESS;
    }

    /**
     * Blocks that open a menu of their own (chests, crafting tables, looms,
     * furnaces, anvils, ...) always keep their vanilla behaviour. The state
     * level accessor delegates to the block, whose base implementation
     * returns null; every menu block overrides it.
     */
    public static boolean blockHasOwnMenu(ServerPlayerEntity player, BlockPos pos) {
        World level = player.level;
        BlockState state = level.getBlockState(pos);
        return state.getMenuProvider(level, pos) != null;
    }

    /** Menu provider for the held box; named boxes keep their custom name. */
    private static class BoxMenuProvider implements INamedContainerProvider {
        private final int rows;
        private final ItemStack stack;

        BoxMenuProvider(int rows, ItemStack stack) {
            this.rows = rows;
            this.stack = stack;
        }

        @Override
        public ITextComponent getDisplayName() {
            return stack.getDisplayName();
        }

        @Override
        public Container createMenu(int id, PlayerInventory inventory, PlayerEntity player) {
            return ShulkerMenu.create(id, inventory, stack, rows);
        }
    }
}
