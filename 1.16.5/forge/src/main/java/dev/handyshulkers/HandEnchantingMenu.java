package dev.handyshulkers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.util.IWorldPosCallable;

/**
 * A enchanting table menu not bound to a enchanting table block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position (the player's
 * feet) is not a enchanting table. The anchor stays a real position, so
 * bookshelves around the player still boost the offers.
 */
public class HandEnchantingMenu extends EnchantmentContainer {

    public HandEnchantingMenu(int id, PlayerInventory inventory, IWorldPosCallable access) {
        super(id, inventory, access);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return player.isAlive();
    }
}
