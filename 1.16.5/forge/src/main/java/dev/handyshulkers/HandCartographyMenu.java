package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.CartographyContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A cartography table menu not bound to a cartography table block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a cartography table.
 */
public class HandCartographyMenu extends CartographyContainer {

    public HandCartographyMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
