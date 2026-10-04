package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shulker box specific logic: tag membership, storage safety and row sizing.
 * The actual hand-use dispatch lives in {@link HandItemUse}.
 *
 * <p>1.20.1 port note: there are no data components (1.20.5+), so storage is
 * the vanilla {@code BlockEntityTag.Items} NBT convention and the 256-slot
 * clamp is applied to the backing NBT slot count instead of the container
 * component.
 */
public final class ShulkerOpenLogic {

    private ShulkerOpenLogic() {
    }

    public static boolean isOpenable(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(HandyShulkers.SHULKER_BOXES) || isVanillaShulkerBox(stack));
    }

    /** True for items whose block is a vanilla(-style) shulker box block. */
    public static boolean isVanillaShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
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
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)
                && tag.getCompound("BlockEntityTag").contains("Items", Tag.TAG_LIST);
    }

    /** Number of stored stacks in the item's BlockEntityTag.Items, or 0. */
    private static int nbtSlots(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag blockEntityTag = tag.getCompound("BlockEntityTag");
        if (!blockEntityTag.contains("Items", Tag.TAG_LIST)) {
            return 0;
        }
        return blockEntityTag.getList("Items", Tag.TAG_COMPOUND).size();
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
        int slots = nbtSlots(stack);
        if (slots > 0) {
            return Mth.clamp((slots + 8) / 9, 1, 6);
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
        // Boxes bigger than the 6-row menus ALWAYS open in the scrolling UI,
        // even with forceRows set: a rows*9 container would truncate (or crash
        // on) content stored beyond its slots. forceRows only governs the
        // normal-sized menus below.
        int capacity = largeBoxCapacity(stack, config);
        if (capacity > 0) {
            ItemStackContainer container = new ItemStackContainer(stack, ScrollingMenu.STORAGE_SLOTS);
            container.startOpen(serverPlayer);
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new ScrollingMenu(id, inventory, container, capacity, stack),
                    stack.getHoverName()));
            return InteractionResult.SUCCESS;
        }
        int rows = rowsFor(stack);
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ShulkerMenu(id, inventory, stack, rows),
                stack.getHoverName()));
        return InteractionResult.SUCCESS;
    }

    /**
     * Backing slot count for boxes that exceed the 6-row menus, or 0 when the
     * normal sized menus apply. Boxes listed in the config open with their
     * declared capacity even while empty — an empty item carries no capacity
     * information of its own.
     */
    private static int largeBoxCapacity(ItemStack stack, HandyShulkersConfig config) {
        Integer declared = config.largeBoxes.get(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (declared != null && declared > 54) {
            return Math.min(declared, 256);
        }
        // A block entity knows its own size (e.g. compressed-blocks' 243-slot
        // scrolling container). NBT-only boxes fall back to their content
        // footprint, rounded up to whole 9-slot rows.
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock entityBlock) {
            try {
                BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
                if (blockEntity instanceof net.minecraft.world.Container container && container.getContainerSize() > 54) {
                    return Math.min(container.getContainerSize(), 256);
                }
            } catch (Throwable ignored) {
            }
        }
        int slots = nbtSlots(stack);
        if (slots > 54) {
            // footprint rounded up to whole rows, so every visible cell stays usable
            return Math.min((slots + 8) / 9 * 9, 256);
        }
        return 0;
    }

    /**
     * Blocks that open a menu of their own (chests, crafting tables, looms,
     * furnaces, ...) always keep their vanilla behaviour.
     */
    public static boolean blockHasOwnMenu(Player player, BlockPos pos) {
        return player.level().getBlockState(pos).getMenuProvider(player.level(), pos) != null;
    }
}
