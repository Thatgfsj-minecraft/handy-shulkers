package dev.handyshulkers;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * 手持右键“拿着就用”（对应 1.21.x HandItemUse）。
 *
 * <p>1.7.10 事件语义（源自 Forge 补丁）：RIGHT_CLICK_AIR 与 RIGHT_CLICK_BLOCK 都在
 * 服务端触发；useItem/useBlock 置 DENY 可分别拦截原版“用物品”/“用方块”。
 * 方块优先：瞄准自带界面的方块时不干预；潜行+右键=原版放置（requireSneak 门直接放行）。
 * 全部服务端权威，客户端侧事件一律忽略，原版包照常流动。
 */
public class HandItemUse {

    /** 与原版 ItemBed 放置一致的“脚→头”方向偏移（meta 0=+z 1=-x 2=-z 3=+x）。 */
    private static final int[][] FOOT_TO_HEAD = new int[][] {{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

    /** 原版点击面顺序：0=-y 1=+y 2=-z 3=+z 4=-x 5=+x。 */
    private static final int[][] FACE_OFFSETS = new int[][] {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

    /** 手持功能物品判定（对应 1.21 isFunctional）。 */
    public static boolean isFunctional(ItemStack stack) {
        return ShulkerOpenLogic.isBoxLike(stack)
                || isBedItem(stack)
                || isEnderChestItem(stack)
                || screenBlockOf(stack) != null;
    }

    private static Block screenBlockOf(ItemStack stack) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof BlockWorkbench || block instanceof BlockEnchantmentTable || block instanceof BlockAnvil) {
            return block;
        }
        return null;
    }

    private static boolean isBedItem(ItemStack stack) {
        return stack != null && Block.getBlockFromItem(stack.getItem()) instanceof BlockBed;
    }

    private static boolean isEnderChestItem(ItemStack stack) {
        return stack != null && Block.getBlockFromItem(stack.getItem()) instanceof BlockEnderChest;
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world.isRemote) {
            return; // 服务端权威：客户端预测不一致由服务端纠正
        }
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.useBlock == Event.Result.DENY || event.useItem == Event.Result.DENY) {
            return; // 其他模组已介入（保护类等），尊重其裁决
        }
        if (!(event.entityPlayer instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.entityPlayer;
        if (!HandyShulkersConfig.get().allowFakePlayers && player.getClass() != EntityPlayerMP.class) {
            return;
        }
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null || !isFunctional(held)) {
            return;
        }
        if (HandyShulkersConfig.get().requireSneak != player.isSneaking()) {
            return;
        }
        if (player.openContainer != player.inventoryContainer) {
            return; // 已开界面时不抢交互
        }
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                && ShulkerOpenLogic.blockHasOwnMenu(event.world, event.x, event.y, event.z)) {
            return; // 方块优先
        }

        if (consumeHandUse(player, event.world, held)) {
            event.useItem = Event.Result.DENY;
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                event.useBlock = Event.Result.DENY;
                sendPredictionFixup(player, event.world, event.x, event.y, event.z, event.face);
            }
        }
    }

    private boolean consumeHandUse(EntityPlayerMP player, World world, ItemStack held) {
        if (ShulkerOpenLogic.isBoxLike(held)) {
            return ShulkerOpenLogic.openBox(player, held);
        }
        if (isBedItem(held)) {
            return sleepInPlace(player, world);
        }
        if (isEnderChestItem(held)) {
            player.openGui(HandyShulkers.instance, HandyShulkers.GUI_ENDER_CHEST, world, 0, 0, 0);
            return true;
        }
        Block block = screenBlockOf(held);
        if (block == null) {
            return false;
        }
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (block instanceof BlockWorkbench) {
            player.openGui(HandyShulkers.instance, HandyShulkers.GUI_CRAFTING, world, x, y, z);
        } else if (block instanceof BlockEnchantmentTable) {
            player.openGui(HandyShulkers.instance, HandyShulkers.GUI_ENCHANTING, world, x, y, z);
        } else {
            player.openGui(HandyShulkers.instance, HandyShulkers.GUI_ANVIL, world, x, y, z);
        }
        return true;
    }

    /**
     * 手持床就地入睡（对应 1.21 sleepInPlace + TempBedTracker）：在玩家脚下按其朝向
     * 放一对临时床方块（1.7.10 床无 TileEntity，用 meta 判定部件），再走原版
     * sleepInBed 保留全部原版规则（白天/怪物/距离）。睡失败由
     * EntityPlayerMP.sleepInBed 的 gamestate 包提示客户端并立即回收临时床；
     * 睡成功由 TempBedTracker 在醒来/登出时回收。占位被挡时不消费事件，回落原版
     * （原版本就放不下，与 1.7.10 vanilla 行为一致）。
     */
    private boolean sleepInPlace(EntityPlayerMP player, World world) {
        int footX = MathHelper.floor_double(player.posX);
        int footY = MathHelper.floor_double(player.posY);
        int footZ = MathHelper.floor_double(player.posZ);
        int dir = MathHelper.floor_double((double) (player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
        int headX = footX + FOOT_TO_HEAD[dir][0];
        int headY = footY;
        int headZ = footZ + FOOT_TO_HEAD[dir][2];
        if (!isReplaceable(world, footX, footY, footZ) || !isReplaceable(world, headX, headY, headZ)) {
            return false;
        }
        world.setBlock(footX, footY, footZ, Blocks.bed, dir, 3);
        world.setBlock(headX, headY, headZ, Blocks.bed, dir | 8, 3);
        EntityPlayer.EnumStatus status = player.sleepInBedAt(footX, footY, footZ);
        if (status != null && status != EntityPlayer.EnumStatus.OK) {
            TempBedTracker.removeBed(world, footX, footY, footZ, headX, headY, headZ);
        } else {
            // 原版 BlockBed 成功后同样设置重生点
            player.setSpawnChunk(new ChunkCoordinates(footX, footY, footZ), false);
            TempBedTracker.track(player, world.provider.dimensionId, footX, footY, footZ, headX, headY, headZ);
        }
        return true;
    }

    private static boolean isReplaceable(World world, int x, int y, int z) {
        return world.isAirBlock(x, y, z) || world.getBlock(x, y, z).isReplaceable(world, x, y, z);
    }

    /**
     * 客户端预测修正：1.7.10 客户端会在点击面相邻位置本地预测放置方块；服务端拒绝后
     * 用事件携带的 face 把该位置真值回发，清掉残影（点击位本身由原版 DENY 分支回发）。
     */
    private static void sendPredictionFixup(EntityPlayerMP player, World world, int x, int y, int z, int face) {
        if (face < 0 || face >= FACE_OFFSETS.length) {
            return;
        }
        int[] off = FACE_OFFSETS[face];
        player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x + off[0], y + off[1], z + off[2], world));
    }
}
