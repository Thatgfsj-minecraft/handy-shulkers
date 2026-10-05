package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;

/**
 * An anvil menu not bound to an anvil block. Vanilla stillValid would close
 * the menu immediately because the block at the anchor position is not an
 * anvil. Vanilla's anvil damage chance reads the block at the anchor too;
 * since that position is not an anvil, a portable anvil never takes damage.
 */
public class HandAnvilMenu extends AnvilMenu {

    public HandAnvilMenu(int id, Inventory inventory, Level level, BlockPos anchor) {
        super(id, inventory, ContainerLevelAccess.create(level, anchor));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
