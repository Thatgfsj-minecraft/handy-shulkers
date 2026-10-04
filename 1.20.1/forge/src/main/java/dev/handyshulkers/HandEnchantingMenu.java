package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.Level;

/**
 * An enchanting menu not bound to an enchanting table block. Vanilla
 * stillValid would close the menu immediately because the block at the anchor
 * position is not an enchanting table. The anchor stays a real position, so
 * bookshelves around the player still boost the offers.
 */
public class HandEnchantingMenu extends EnchantmentMenu {

    public HandEnchantingMenu(int id, Inventory inventory, Level level, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(level, anchor));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
