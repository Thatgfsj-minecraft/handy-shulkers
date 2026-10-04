package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

/**
 * A generic chest menu sized to the opened box. The box's own slots refuse
 * container items (no shulker-in-shulker, even for modded boxes whose empty
 * item has no contents yet) and the opened box itself can never be moved into
 * its own inventory.
 */
public class ShulkerMenu extends ChestContainer {

    private final ItemStack box;
    private final int boxSlots;
    private final ItemStackContainer container;

    private ShulkerMenu(int id, PlayerInventory playerInventory, ItemStack box, int rows, ItemStackContainer container) {
        super(menuType(rows), id, playerInventory, container, rows);
        this.box = box;
        this.boxSlots = rows * 9;
        this.container = container;

        for (int index = 0; index < boxSlots; index++) {
            int x = 8 + (index % 9) * 18;
            int y = 18 + (index / 9) * 18;
            this.slots.set(index, new Slot(container, index, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !ItemStackContainer.isContainerItem(stack);
                }
            });
        }
    }

    /** Factory keeps the backing container accessible to the private ctor. */
    public static ShulkerMenu create(int id, PlayerInventory playerInventory, ItemStack box, int rows) {
        return new ShulkerMenu(id, playerInventory, box, rows, new ItemStackContainer(box, rows * 9));
    }

    public ItemStackContainer getBackingContainer() {
        return container;
    }

    @Override
    public ItemStack clicked(int slotIndex, int dragType, ClickType clickType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < boxSlots && getSlot(slotIndex).getItem() == box) {
            return ItemStack.EMPTY;
        }
        return super.clicked(slotIndex, dragType, clickType, player);
    }

    public static ContainerType<ChestContainer> menuType(int rows) {
        switch (rows) {
            case 1:
                return ContainerType.GENERIC_9x1;
            case 2:
                return ContainerType.GENERIC_9x2;
            case 3:
                return ContainerType.GENERIC_9x3;
            case 4:
                return ContainerType.GENERIC_9x4;
            case 5:
                return ContainerType.GENERIC_9x5;
            default:
                return ContainerType.GENERIC_9x6;
        }
    }
}
