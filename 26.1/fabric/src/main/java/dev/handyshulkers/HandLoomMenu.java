package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.level.Level;

/**
 * A loom menu not bound to a loom block. Vanilla stillValid would close the
 * menu immediately because the block at the anchor position is not a loom.
 */
public class HandLoomMenu extends LoomMenu {

    public HandLoomMenu(int id, Inventory inventory, Level level, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(level, anchor));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
