package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.SmithingTableContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A smithing table menu not bound to a smithing table block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a smithing table.
 */
public class HandSmithingMenu extends SmithingTableContainer {

    public HandSmithingMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
