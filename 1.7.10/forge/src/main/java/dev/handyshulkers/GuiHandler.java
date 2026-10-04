package dev.handyshulkers;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * IGuiHandler + NetworkRegistry（1.7.10 无 ContainerType/GuiHandler 注册差异）。
 * 服务端给 Container、客户端给 Gui；两侧都从“当前手持物品实例”重建盒容器。
 */
public class GuiHandler implements IGuiHandler {

    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (!(player instanceof EntityPlayerMP)) {
            return null;
        }
        ItemStack held = player.inventory.getCurrentItem();
        switch (id) {
            case HandyShulkers.GUI_BOX:
                if (held == null || !ShulkerOpenLogic.isOpenable(held)) {
                    return null;
                }
                return new ContainerBox(player.inventory,
                        new ItemStackContainer(held, ShulkerOpenLogic.rowsFor(held) * 9));
            case HandyShulkers.GUI_CRAFTING:
                return new ContainerWorkbench(player.inventory, world, x, y, z);
            case HandyShulkers.GUI_ENCHANTING:
                // 书架加成由原版逻辑按锚点周围真实方块计算；锚点=玩家脚下（见差异清单）
                return new ContainerEnchantment(player.inventory, world, x, y, z);
            case HandyShulkers.GUI_ANVIL:
                // 无实体砧：不耗砧耐久；锚点=玩家脚下，blockExists 恒真
                return new ContainerRepair(player.inventory, world, x, y, z, player);
            case HandyShulkers.GUI_ENDER_CHEST:
                return new ContainerChest(player.inventory, player.getInventoryEnderChest());
            default:
                return null;
        }
    }

    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        ItemStack held = player.inventory.getCurrentItem();
        switch (id) {
            case HandyShulkers.GUI_BOX:
                if (held == null || !ShulkerOpenLogic.isOpenable(held)) {
                    return null;
                }
                return new GuiChest(player.inventory,
                        new ItemStackContainer(held, ShulkerOpenLogic.rowsFor(held) * 9));
            case HandyShulkers.GUI_CRAFTING:
                return new GuiCrafting(player.inventory, world, x, y, z);
            case HandyShulkers.GUI_ENCHANTING:
                return new GuiEnchantment(player.inventory, world, x, y, z, "container.enchant");
            case HandyShulkers.GUI_ANVIL:
                return new GuiRepair(player.inventory, world, x, y, z);
            case HandyShulkers.GUI_ENDER_CHEST:
                return new GuiChest(player.inventory, player.getInventoryEnderChest());
            default:
                return null;
        }
    }
}
