package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 由单个 ItemStack 的 NBT（原版 chest 约定的 Items 列表，Slot 字节 + 物品复合标签）
 * 支撑的 IInventory。每次变更立即整体写回物品，界面关不关都不丢数据。
 * 超出 6 行菜单的槽位（1.7.10 无滚动界面）保存在隐藏区，写回时原样带出，绝不截断。
 */
public class ItemStackContainer implements IInventory {

    public static final String TAG_ITEMS = "Items";

    protected final ItemStack[] items;
    protected final ItemStack[] hidden;
    protected final ItemStack box;

    public ItemStackContainer(ItemStack box, int menuSlots) {
        this.box = box;
        NBTTagList list = box.hasTagCompound() ? box.getTagCompound().getTagList(TAG_ITEMS, 10) : new NBTTagList();
        int hiddenSize = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            int slot = list.getCompoundTagAt(i).getByte("Slot") & 255;
            if (slot >= menuSlots && slot + 1 - menuSlots > hiddenSize) {
                hiddenSize = slot + 1 - menuSlots;
            }
        }
        this.items = new ItemStack[menuSlots];
        this.hidden = new ItemStack[hiddenSize];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound slotTag = list.getCompoundTagAt(i);
            int slot = slotTag.getByte("Slot") & 255;
            ItemStack stack = ItemStack.loadItemStackFromNBT(slotTag);
            if (stack == null) {
                continue;
            }
            if (slot < menuSlots) {
                this.items[slot] = stack;
            } else {
                this.hidden[slot - menuSlots] = stack;
            }
        }
    }

    public ItemStack getBox() {
        return box;
    }

    public ItemStack getHidden(int index) {
        return hidden[index];
    }

    /** NBT 中已存内容的最高槽位（无内容返回 0）。 */
    public static int detectStoredSlots(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0;
        }
        NBTTagList list = stack.getTagCompound().getTagList(TAG_ITEMS, 10);
        int max = 0;
        for (int i = 0; i < list.tagCount(); i++) {
            int slot = list.getCompoundTagAt(i).getByte("Slot") & 255;
            if (slot + 1 > max) {
                max = slot + 1;
            }
        }
        return max;
    }

    public int getSizeInventory() {
        return items.length;
    }

    public ItemStack getStackInSlot(int index) {
        return items[index];
    }

    public ItemStack decrStackSize(int index, int count) {
        if (items[index] == null) {
            return null;
        }
        ItemStack taken;
        if (items[index].stackSize <= count) {
            taken = items[index];
            items[index] = null;
        } else {
            taken = items[index].splitStack(count);
            if (items[index].stackSize == 0) {
                items[index] = null;
            }
        }
        markDirty();
        return taken;
    }

    public ItemStack getStackInSlotOnClosing(int index) {
        ItemStack stack = items[index];
        items[index] = null;
        if (stack != null) {
            markDirty();
        }
        return stack;
    }

    public void setInventorySlotContents(int index, ItemStack stack) {
        items[index] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        markDirty();
    }

    public String getInventoryName() {
        return box.getDisplayName();
    }

    public boolean hasCustomInventoryName() {
        return true;
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    /** 每次变更整体写回（含隐藏槽），等价 1.21 ItemStackContainer.setChanged。 */
    public void markDirty() {
        NBTTagCompound tag = box.hasTagCompound() ? box.getTagCompound() : new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < items.length; i++) {
            appendSlot(list, i, items[i]);
        }
        for (int i = 0; i < hidden.length; i++) {
            appendSlot(list, items.length + i, hidden[i]);
        }
        tag.setTag(TAG_ITEMS, list);
        box.setTagCompound(tag);
    }

    private static void appendSlot(NBTTagList list, int slot, ItemStack stack) {
        if (stack == null) {
            return;
        }
        NBTTagCompound slotTag = new NBTTagCompound();
        slotTag.setByte("Slot", (byte) slot);
        stack.writeToNBT(slotTag);
        list.appendTag(slotTag);
    }

    /** 身份匹配（==）：同种第二只盒不算持有，避免编辑写进另一只（可能已掉落）的盒子。 */
    public boolean isUseableByPlayer(EntityPlayer player) {
        InventoryPlayer inventory = player.inventory;
        for (int i = 0; i < inventory.mainInventory.length; i++) {
            if (inventory.mainInventory[i] == box) {
                return true;
            }
        }
        for (int i = 0; i < inventory.armorInventory.length; i++) {
            if (inventory.armorInventory[i] == box) {
                return true;
            }
        }
        return false;
    }

    public void openInventory() {
    }

    public void closeInventory() {
    }

    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return !ShulkerOpenLogic.isContainerItem(stack);
    }
}
