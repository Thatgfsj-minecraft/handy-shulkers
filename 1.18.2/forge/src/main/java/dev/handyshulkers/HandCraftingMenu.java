package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;

/**
 * A crafting table menu not bound to a crafting table block. Vanilla
 * stillValid would close the menu immediately because the block at the anchor
 * position (the player's feet) is not a crafting table.
 */
public class HandCraftingMenu extends CraftingMenu {

    public HandCraftingMenu(int id, Inventory inventory, Level level, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(level, anchor));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
