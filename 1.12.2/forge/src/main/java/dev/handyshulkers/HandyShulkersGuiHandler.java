package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Gui ids and container wiring. 1.12.2 has no ContainerType registry: FML's
 * {@link IGuiHandler} plus {@code player.openGui} is the way to open arbitrary
 * containers. Positions are ORIGIN on purpose - every vanilla constructor used
 * here works without a real block (mirrors the 1.21.x Hand*Menu pattern), and
 * for the anvil it also means the vanilla "damage the anvil on use" branch can
 * never find a real anvil to damage.
 */
public class HandyShulkersGuiHandler implements IGuiHandler {

    public static final int ID_SHULKER = 0;
    public static final int ID_CRAFTING = 1;
    public static final int ID_ENCHANTING = 2;
    public static final int ID_ANVIL = 3;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        switch (id) {
            case ID_SHULKER: {
                ItemStack box = HandItemUse.findHeldStack(player, true);
                if (box.isEmpty()) {
                    return null;
                }
                int rows = net.minecraft.util.math.MathHelper.clamp(x, 1, 6);
                return new ShulkerContainer(player.inventory, new ItemStackContainer(box, rows * 9), player);
            }
            case ID_CRAFTING:
                return new net.minecraft.inventory.ContainerWorkbench(player.inventory, world, BlockPos.ORIGIN);
            case ID_ENCHANTING:
                // 2-arg form: no real table position, so enchanting power is
                // always 0 - same as the client-side gui, which builds the
                // position-less container too
                return new net.minecraft.inventory.ContainerEnchantment(player.inventory, world);
            case ID_ANVIL:
                return new net.minecraft.inventory.ContainerRepair(player.inventory, world, BlockPos.ORIGIN, player);
            default:
                return null;
        }
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        // client-only gui classes stay behind a separate class so a dedicated
        // server never loads them
        return ClientGuiHandler.getGui(id, player, world, x);
    }
}
