package dev.handyshulkers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Shulker box specific logic: identification without tags, storage safety and
 * row sizing. The actual hand-use dispatch lives in {@link HandItemUse}.
 *
 * <p>1.12.2 identification is class based: the 17 vanilla colors are all
 * {@code BlockShulkerBox} items (metadata 0-16), and modded boxes join through
 * the {@code extraShulkerBoxes}/{@code largeBoxes} config whitelists instead
 * of tags.
 */
public final class ShulkerOpenLogic {

    private ShulkerOpenLogic() {
    }

    /** True for items this mod treats as a shulker box (vanilla colors + whitelists). */
    public static boolean isShulkerBoxItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isVanillaBoxItem(stack)) {
            return true;
        }
        String id = idOf(stack);
        HandyShulkersConfig config = HandyShulkersConfig.get();
        return config.isExtraBox(id) || config.isLargeBox(id);
    }

    private static boolean isVanillaBoxItem(ItemStack stack) {
        return stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock() instanceof BlockShulkerBox;
    }

    public static String idOf(ItemStack stack) {
        ResourceLocation name = Item.REGISTRY.getNameForObject(stack.getItem());
        return name == null ? "" : name.toString();
    }

    /**
     * True when the item certainly keeps its contents in a vanilla
     * {@code TileEntityShulkerBox} when placed, meaning opening it can never
     * desync from the owning mod. 1.12.2 equivalent of the 1.21.x "vanilla
     * container component" check.
     */
    public static boolean hasKnownStorage(ItemStack stack) {
        return isVanillaBoxItem(stack);
    }

    /**
     * Slot count of the tile entity the box would get when placed, or -1 when
     * it cannot be probed safely. Some modded block entities are not safe to
     * construct headless; every failure falls back to the content count.
     */
    private static int probeCapacity(ItemStack stack) {
        try {
            if (isVanillaBoxItem(stack)) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (block instanceof ITileEntityProvider) {
                    TileEntity tileEntity = ((ITileEntityProvider) block).createNewTileEntity(null, 0);
                    if (tileEntity instanceof IInventory && ((IInventory) tileEntity).getSizeInventory() > 0) {
                        return ((IInventory) tileEntity).getSizeInventory();
                    }
                }
            }
        } catch (Throwable ignored) {
            // headless construction failed; fall through to the content count
        }
        return -1;
    }

    /** Number of stacks stored in the item NBT ({@code Items} list). */
    public static int countStoredItems(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("Items", 9)) {
            return 0;
        }
        NBTTagList list = tag.getTagList("Items", 10);
        return list.tagCount();
    }

    private static int rowsFor(int capacity, int stored) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (config.forceRows >= 1 && config.forceRows <= 6) {
            return config.forceRows;
        }
        if (capacity > 0) {
            return MathHelper.clamp((capacity + 8) / 9, 1, 6);
        }
        if (stored > 0) {
            return MathHelper.clamp((stored + 8) / 9, 1, 6);
        }
        return 3;
    }

    /**
     * Opens the box inventory. Server side only; every gate (sneak, fake
     * players, spectator, open menus) has already been applied by
     * {@link HandItemUse}.
     *
     * <p>1.12.2 has no scrolling UI: boxes holding more than 54 slots of
     * content (or whose tile entity is bigger than six rows) are refused with
     * a log line instead of opening truncated. forceRows (1-6) always wins
     * within that limit.
     */
    public static boolean openMenu(EntityPlayerMP player, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!config.allowUnknownStorage && !hasKnownStorage(stack)) {
            return false;
        }

        int capacity = probeCapacity(stack);
        int stored = countStoredItems(stack);
        if (capacity > 54 || (capacity < 0 && stored > 54)) {
            HandyShulkers.LOGGER.info("[handyshulkers] refusing to open {} from hand: {} backing slots exceed the "
                    + "vanilla 6-row limit and 1.12.2 has no scrolling UI (nothing was truncated)",
                    idOf(stack), capacity > 0 ? capacity : stored);
            return false;
        }

        int rows = rowsFor(capacity, stored);
        // never let the fixed row count truncate what is actually inside
        if (stored > rows * 9) {
            rows = MathHelper.clamp((stored + 8) / 9, 1, 6);
        }

        player.openGui(HandyShulkersMod.instance, HandyShulkersGuiHandler.ID_SHULKER, player.world, rows, 0, 0);
        return true;
    }

    /**
     * Blocks that open a menu of their own (chests, crafting tables,
     * enchanting tables, anvils, ...) always keep their vanilla behaviour.
     * Since 1.12.2 hand-use only fires on air clicks
     * (PlayerInteractEvent.RightClickItem), this is not needed for dispatch -
     * it only feeds {@code isFunctional}'s documentation paths.
     */
    public static boolean blockHasOwnMenu(EntityPlayerMP player, BlockPos pos) {
        return player.world.getTileEntity(pos) != null
                || player.world.getBlockState(pos).getBlock().hasTileEntity();
    }
}
