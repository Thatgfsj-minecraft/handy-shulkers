package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/**
 * A cartography menu not bound to a cartography table block. Vanilla
 * stillValid would close the menu immediately because the block at the anchor
 * position is not a cartography table.
 */
public class HandCartographyMenu extends CartographyTableMenu {

    public HandCartographyMenu(int id, Inventory inventory, Level level, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(level, anchor));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
