package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.StonecutterContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A stonecutter menu not bound to a stonecutter block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a stonecutter.
 */
public class HandStonecutterMenu extends StonecutterContainer {

    public HandStonecutterMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
