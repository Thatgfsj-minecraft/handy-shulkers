package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * 盒类物品的容器（对应 1.21.x ShulkerMenu）。排布与原版 ContainerChest 相同
 * （1-6 行自适应）。守卫只在服务端：盒的槽位拒绝一切盒类物品（无套娃），
 * 打开的盒本体不能被移进自己的槽位；客户端预测不一致由服务端纠正
 * （1.12.2 波同款结论）。
 */
public class ContainerBox extends Container {

    private final ItemStackContainer storage;
    private final ItemStack box;
    private final int boxSlots;

    public ContainerBox(InventoryPlayer playerInventory, ItemStackContainer storage) {
        this.storage = storage;
        this.box = storage.getBox();
        this.boxSlots = storage.getSizeInventory();
        int rows = boxSlots / 9;
        int shift = (rows - 4) * 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new SlotNoContainerItems(storage, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + shift));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 161 + shift));
        }
    }

    /** 打开的盒本体一旦离开背包（拿起/掉落），菜单立即失效关闭，杜绝把内容写进未知归属的盒子。 */
    public boolean canInteractWith(EntityPlayer player) {
        return storage.isUseableByPlayer(player);
    }

    /** 拿起自己槽位里的盒本体再点进自己的格子：直接吞掉这次点击。 */
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (slot >= 0 && slot < boxSlots && ((Slot) inventorySlots.get(slot)).getStack() == box) {
            return null;
        }
        return super.slotClick(slot, button, mode, player);
    }

    public static class SlotNoContainerItems extends Slot {

        public SlotNoContainerItems(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        public boolean isItemValid(ItemStack stack) {
            return !ShulkerOpenLogic.isContainerItem(stack);
        }
    }
}
