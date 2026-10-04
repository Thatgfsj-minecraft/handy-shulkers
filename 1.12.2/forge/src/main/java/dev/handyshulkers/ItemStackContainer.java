package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * An {@link IInventory} backed by the NBT ({@code Items} list) of a single
 * held shulker box item - the 1.12.2 equivalent of the 1.21.x component-backed
 * container. Every change is written straight back into the item NBT, so
 * nothing is lost when the menu is closed.
 */
public class ItemStackContainer implements IInventory {

    protected final NonNullList<ItemStack> items;
    protected final ItemStack box;

    public ItemStackContainer(ItemStack box, int size) {
        this.box = box;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        NBTTagCompound tag = box.getTagCompound();
        if (tag != null) {
            ItemStackHelper.loadAllItems(tag, this.items);
        }
    }

    @Override
    public int getSizeInventory() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.items.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = ItemStackHelper.getAndSplit(this.items, index, count);
        this.markDirty();
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(this.items, index);
        this.markDirty();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.items.set(index, stack);
        this.markDirty();
    }

    @Override
    public String getName() {
        return this.box.getDisplayName();
    }

    @Override
    public boolean hasCustomName() {
        return this.box.hasDisplayName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return this.hasCustomName() ? new TextComponentString(this.getName())
                : new TextComponentTranslation("container.shulkerBox");
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        NBTTagCompound tag = this.box.hasTagCompound() ? this.box.getTagCompound() : new NBTTagCompound();
        ItemStackHelper.saveAllItems(tag, this.items, false);
        this.box.setTagCompound(tag);
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        // Identity match on purpose: ItemStack.equals() also matches a second,
        // NBT-identical box, which would keep the menu "valid" and let edits
        // write into a different, possibly dropped, stack.
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == this.box) {
                return true;
            }
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            if (stack == this.box) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        this.markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return !isContainerItem(stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        this.markDirty();
    }

    /**
     * True for items that must never be stored inside an opened box
     * (no shulker-in-shulker, even for modded boxes whose empty item carries
     * no NBT yet).
     */
    static boolean isContainerItem(ItemStack stack) {
        return ShulkerOpenLogic.isShulkerBoxItem(stack);
    }
}
