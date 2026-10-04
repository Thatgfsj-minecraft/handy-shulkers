package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A crafting table menu not bound to a crafting table block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a crafting table.
 */
public class HandCraftingMenu extends WorkbenchContainer {

    public HandCraftingMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
