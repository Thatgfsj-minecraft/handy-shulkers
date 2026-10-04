package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * A chest menu sized to the opened box. The box's own slots refuse container
 * items (no shulker-in-shulker, even for whitelisted modded boxes whose empty
 * item carries no NBT yet). Slot positions and indices stay identical to the
 * vanilla {@link ContainerChest}, so the client-side vanilla container keeps
 * syncing cleanly; only the slot behaviour differs on the server.
 */
public class ShulkerContainer extends ContainerChest {

    private final int boxSlots;

    public ShulkerContainer(InventoryPlayer playerInventory, IInventory boxInventory, EntityPlayer player) {
        super(playerInventory, boxInventory, player);
        this.boxSlots = boxInventory.getSizeInventory();
        for (int index = 0; index < this.boxSlots; index++) {
            final Slot old = this.inventorySlots.get(index);
            this.inventorySlots.set(index, new Slot(boxInventory, index, old.xPos, old.yPos) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return !ItemStackContainer.isContainerItem(stack);
                }
            });
        }
    }
}
