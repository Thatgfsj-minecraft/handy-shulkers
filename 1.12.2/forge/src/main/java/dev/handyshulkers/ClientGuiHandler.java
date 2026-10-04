package dev.handyshulkers;

import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiRepair;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;

/**
 * Client-side gui construction for {@link HandyShulkersGuiHandler}. Kept in
 * its own class (and only referenced from inside a method body) so a dedicated
 * server never loads client classes.
 */
final class ClientGuiHandler {

    /**
     * Title source for the hand-opened enchanting gui (1.12.2's
     * GuiEnchantment wants an {@link IWorldNameable}); no real table position
     * is involved, so enchanting power stays 0 exactly like on the server.
     */
    private static final IWorldNameable ENCHANTING_TITLE = new IWorldNameable() {
        @Override
        public String getName() {
            return "container.enchant";
        }

        @Override
        public boolean hasCustomName() {
            return false;
        }

        @Override
        public ITextComponent getDisplayName() {
            return new TextComponentTranslation("container.enchant");
        }
    };

    private ClientGuiHandler() {
    }

    static Object getGui(int id, EntityPlayer player, World world, int x) {
        switch (id) {
            case HandyShulkersGuiHandler.ID_SHULKER: {
                ItemStack box = HandItemUse.findHeldStack(player, true);
                if (box.isEmpty()) {
                    return null;
                }
                int rows = MathHelper.clamp(x, 1, 6);
                return new GuiChest(player.inventory, new ItemStackContainer(box, rows * 9));
            }
            case HandyShulkersGuiHandler.ID_CRAFTING:
                return new GuiCrafting(player.inventory, world);
            case HandyShulkersGuiHandler.ID_ENCHANTING:
                return new GuiEnchantment(player.inventory, world, ENCHANTING_TITLE);
            case HandyShulkersGuiHandler.ID_ANVIL:
                return new GuiRepair(player.inventory, world);
            default:
                return null;
        }
    }
}
