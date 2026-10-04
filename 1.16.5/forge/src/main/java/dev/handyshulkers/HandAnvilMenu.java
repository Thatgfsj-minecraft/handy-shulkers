package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * An anvil menu not bound to an anvil block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not an anvil. Vanilla only damages an anvil block at the anchor position, so no
 * anvil durability is ever used.
 */
public class HandAnvilMenu extends RepairContainer {

    public HandAnvilMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
