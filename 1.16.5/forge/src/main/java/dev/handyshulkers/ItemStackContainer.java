package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

/**
 * A container backed by the vanilla {@code BlockEntityTag} NBT of a single
 * held ItemStack — the storage location 1.16.5 uses when a shulker box is
 * placed (and where breaking writes the contents back), so every change is
 * read by vanilla placement and by modded boxes that follow the vanilla
 * convention. Nothing is lost when the menu is closed.
 */
public class ItemStackContainer implements IInventory {

    protected final NonNullList<ItemStack> items;
    protected final ItemStack box;
    protected boolean dirty;

    public ItemStackContainer(ItemStack box, int size) {
        this.box = box;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        CompoundNBT tag = box.getTag();
        if (tag != null && tag.contains("BlockEntityTag", 10)) {
            CompoundNBT blockEntityTag = tag.getCompound("BlockEntityTag");
            if (blockEntityTag.contains("Items", 9)) {
                ItemStackHelper.loadAllItems(blockEntityTag, items);
            }
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = ItemStackHelper.removeItem(items, index, count);
        setChanged();
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = ItemStackHelper.takeItem(items, index);
        setChanged();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {
        dirty = true;
        writeBack();
    }

    /** Writes the full slot list back into the item's BlockEntityTag.Items. */
    protected void writeBack() {
        CompoundNBT tag = box.getOrCreateTag();
        CompoundNBT blockEntityTag = tag.contains("BlockEntityTag", 10)
                ? tag.getCompound("BlockEntityTag")
                : new CompoundNBT();
        ItemStackHelper.saveAllItems(blockEntityTag, items, true);
        tag.put("BlockEntityTag", blockEntityTag);
        box.setTag(tag);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return !isContainerItem(stack);
    }

    @Override
    public void startOpen(PlayerEntity player) {
        playSound(player.level, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_BOX_OPEN);
    }

    @Override
    public void stopOpen(PlayerEntity player) {
        if (dirty) {
            setChanged();
        }
        playSound(player.level, player.getX(), player.getY(), player.getZ(), SoundEvents.SHULKER_BOX_CLOSE);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        // Identity match on purpose: ItemStack/Item equality would keep the
        // menu "valid" for an identical second box, and edits would then write
        // into a different, possibly dropped, stack.
        PlayerInventory inventory = player.inventory;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == box) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    /**
     * Anything that holds other items must not go back into a box: no
     * shulker-in-shulker, including modded boxes whose empty item carries no
     * contents yet.
     */
    static boolean isContainerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (HandyShulkers.isIn(stack, HandyShulkers.SHULKER_BOXES)) {
            return true;
        }
        return ShulkerOpenLogic.isVanillaShulkerBox(stack);
    }

    private static void playSound(World level, double x, double y, double z, SoundEvent sound) {
        level.playSound(null, x, y, z, sound, SoundCategory.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }
}
