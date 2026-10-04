package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.LoomContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A loom menu not bound to a loom block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a loom.
 */
public class HandLoomMenu extends LoomContainer {

    public HandLoomMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
