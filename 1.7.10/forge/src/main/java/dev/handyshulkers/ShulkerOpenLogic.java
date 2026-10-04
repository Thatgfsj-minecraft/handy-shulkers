package dev.handyshulkers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * 盒类物品识别与行数推断（对应 1.21.x 的 ShulkerOpenLogic）。
 * 1.7.10 原版没有潜影盒：候选 = 配置白名单（extraShulkerBoxes / largeBoxes），
 * 或 allowUnknownStorage 放行的“NBT 已带 Items 列表 + 方块形态是 TileEntity 容器”的物品。
 * 读取/写回走原版 chest 约定（NBT Items 列表 + Slot 字节），凡按该约定存内容的模组盒都能打开。
 */
public final class ShulkerOpenLogic {

    private ShulkerOpenLogic() {
    }

    /** 1.7.10 的“潜影盒识别层”：白名单盒，或未知的 Items-NBT 盒（受 allowUnknownStorage 约束）。 */
    public static boolean isBoxLike(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (isWhitelisted(stack)) {
            return true;
        }
        return hasUnknownItemsStorage(stack);
    }

    public static boolean isOpenable(ItemStack stack) {
        return isBoxLike(stack);
    }

    /** 白名单盒 = 存储约定已知（NBT Items），写回一定被打开方读回。 */
    public static boolean hasKnownStorage(ItemStack stack) {
        return stack != null && isWhitelisted(stack);
    }

    /** 套娃守卫谓词：识别出的盒类物品不能放进盒里。 */
    public static boolean isContainerItem(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return isWhitelisted(stack) || hasUnknownItemsStorage(stack);
    }

    private static boolean isWhitelisted(ItemStack stack) {
        String id = itemId(stack);
        if (id == null) {
            return false;
        }
        HandyShulkersConfig config = HandyShulkersConfig.get();
        return config.extraShulkerBoxes.contains(id) || config.largeBoxes.contains(id);
    }

    /** 未知存储：NBT 已带 Items 列表（内容证据）且方块形态带 TileEntity（instanceof 判定约束）。 */
    private static boolean hasUnknownItemsStorage(ItemStack stack) {
        if (!HandyShulkersConfig.get().allowUnknownStorage) {
            return false;
        }
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(ItemStackContainer.TAG_ITEMS, 9)) {
            return false;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block instanceof ITileEntityProvider;
    }

    public static String itemId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        String name = (String) Item.itemRegistry.getNameForObject(stack.getItem());
        return name == null ? null : name.toLowerCase();
    }

    /** 菜单行数：forceRows 优先，否则按已存最高槽位向上取整行，钳制 1-6，空盒 3 行。 */
    public static int rowsFor(ItemStack stack) {
        int force = HandyShulkersConfig.get().forceRows;
        if (force >= 1 && force <= 6) {
            return force;
        }
        int detected = ItemStackContainer.detectStoredSlots(stack);
        if (detected <= 0) {
            return 3;
        }
        int rows = (detected + 8) / 9;
        if (rows < 1) {
            rows = 1;
        }
        if (rows > 6) {
            rows = 6;
        }
        return rows;
    }

    /** 打开盒（服务端；行数经 openGui 坐标带给 IGuiHandler）。 */
    public static boolean openBox(EntityPlayerMP player, ItemStack stack) {
        if (!HandyShulkersConfig.get().allowUnknownStorage && !hasKnownStorage(stack)) {
            return false;
        }
        int rows = rowsFor(stack);
        player.openGui(HandyShulkers.instance, HandyShulkers.GUI_BOX, player.worldObj, rows, 0, 0);
        return true;
    }

    /**
     * 方块优先：自带界面的方块（箱子/熔炉等 IInventory TileEntity，以及无 TE 的工作台、
     * 附魔台、铁砧、末影箱、床）保持原版行为，手持功能不干预。
     */
    public static boolean blockHasOwnMenu(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockWorkbench || block instanceof BlockEnchantmentTable
                || block instanceof BlockAnvil || block instanceof BlockEnderChest
                || block instanceof BlockBed) {
            return true;
        }
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof net.minecraft.inventory.IInventory;
    }
}
