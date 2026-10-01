package dev.handyshulkers;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A generic chest menu sized to the opened box. The box's own slots refuse
 * container items (no shulker-in-shulker, even for modded boxes whose empty
 * item has no container component yet) and the opened box itself can never be
 * moved into its own inventory.
 */
public class ShulkerMenu extends ChestMenu {

    private final ItemStack box;
    private final int boxSlots;

    public ShulkerMenu(int id, Inventory playerInventory, ItemStack box, int rows) {
        super(menuType(rows), id, playerInventory, new ItemStackContainer(box, rows * 9), rows);
        this.box = box;
        this.boxSlots = rows * 9;

        ItemStackContainer container = (ItemStackContainer) getContainer();
        for (int index = 0; index < boxSlots; index++) {
            int x = 8 + (index % 9) * 18;
            int y = 18 + (index / 9) * 18;
            slots.set(index, new Slot(container, index, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !ItemStackContainer.isContainerItem(stack);
                }
            });
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (slotIndex >= 0 && slotIndex < boxSlots && slots.get(slotIndex).getItem() == box) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }

    public static MenuType<ChestMenu> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }
}
