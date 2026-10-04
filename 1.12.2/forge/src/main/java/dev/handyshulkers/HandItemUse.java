package dev.handyshulkers;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

/**
 * Turns plain right-clicks with functional items into their use action:
 * shulker boxes open, held beds sleep in place, held crafting tables,
 * enchanting tables and anvils show their screen, and ender chests open.
 * Sneak + right-click stays vanilla (place the item), and blocks aimed at
 * always behave vanilla on 1.12.2.
 *
 * <p>1.12.2 dispatch differences from 1.21.x (documented in PORTING.md):
 * <ul>
 * <li>no looms, stonecutters, cartography tables, grindstones or smithing
 * tables - none of those blocks exist before 1.14+</li>
 * <li>hand-use fires only from {@code PlayerInteractEvent.RightClickItem}
 * (air clicks); {@code RightClickBlock} is never touched, so a block clicked
 * at always keeps its vanilla behaviour - blocks with a menu of their own
 * simply win, and blocks without one receive the item instead</li>
 * </ul>
 *
 * <p>Everything is server authoritative. The client always passes so vanilla
 * packets keep flowing and no interaction is ever swallowed (all supported
 * items are block items whose vanilla air-use is a no-op anyway).
 */
public final class HandItemUse {

    private HandItemUse() {
    }

    /** True for items this mod gives a hand-use action. */
    public static boolean isFunctional(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (ShulkerOpenLogic.isShulkerBoxItem(stack)) {
            return true;
        }
        return isBlockItemOf(stack, BlockBed.class)
                || isBlockItemOf(stack, BlockEnderChest.class)
                || isBlockItemOf(stack, BlockWorkbench.class)
                || isBlockItemOf(stack, BlockEnchantmentTable.class)
                || isBlockItemOf(stack, BlockAnvil.class);
    }

    private static boolean isBlockItemOf(ItemStack stack, Class<? extends net.minecraft.block.Block> blockClass) {
        return stack.getItem() instanceof net.minecraft.item.ItemBlock
                && blockClass.isInstance(((net.minecraft.item.ItemBlock) stack.getItem()).getBlock());
    }

    /**
     * Finds the held functional item: main hand first, then off hand.
     * {@code boxOnly} restricts to shulker-box-like items (for the gui
     * handlers that must not grab an unrelated functional item after a hotbar
     * swap mid-menu).
     */
    public static ItemStack findHeldStack(EntityPlayer player, boolean boxOnly) {
        for (EnumHand hand : EnumHand.values()) {
            ItemStack stack = player.getHeldItem(hand);
            if (stack.isEmpty()) {
                continue;
            }
            if (boxOnly ? ShulkerOpenLogic.isShulkerBoxItem(stack) : isFunctional(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Runs the hand-use for the held item; true when this mod consumed the
     * click. Server side only. Gates mirror the 1.21.x implementation:
     * sneak parity, spectator, fake players and already-open menus.
     */
    public static boolean useFromHand(EntityPlayer player, EnumHand hand, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!isFunctional(stack)) {
            return false;
        }
        if (config.requireSneak != player.isSneaking()) {
            return false;
        }
        if (player.world.isRemote) {
            return false;
        }
        if (!(player instanceof EntityPlayerMP)) {
            return false;
        }
        if (player.isSpectator()) {
            return false;
        }
        if (!config.allowFakePlayers && player.getClass() != EntityPlayerMP.class) {
            return false;
        }
        if (player.openContainer != player.inventoryContainer) {
            return false;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;

        if (ShulkerOpenLogic.isShulkerBoxItem(stack)) {
            return ShulkerOpenLogic.openMenu(serverPlayer, stack);
        }
        if (isBlockItemOf(stack, BlockBed.class)) {
            return sleepInPlace(serverPlayer, stack);
        }
        if (isBlockItemOf(stack, BlockEnderChest.class)) {
            return openEnderChest(serverPlayer);
        }
        if (isBlockItemOf(stack, BlockWorkbench.class)) {
            serverPlayer.openGui(HandyShulkersMod.instance, HandyShulkersGuiHandler.ID_CRAFTING,
                    serverPlayer.world, 0, 0, 0);
            return true;
        }
        if (isBlockItemOf(stack, BlockEnchantmentTable.class)) {
            serverPlayer.openGui(HandyShulkersMod.instance, HandyShulkersGuiHandler.ID_ENCHANTING,
                    serverPlayer.world, 0, 0, 0);
            return true;
        }
        if (isBlockItemOf(stack, BlockAnvil.class)) {
            serverPlayer.openGui(HandyShulkersMod.instance, HandyShulkersGuiHandler.ID_ANVIL,
                    serverPlayer.world, 0, 0, 0);
            return true;
        }
        return false;
    }

    private static boolean openEnderChest(EntityPlayerMP player) {
        player.displayGUIChest(player.getInventoryEnderChest());
        return true;
    }

    /**
     * Sleeps right where the player stands by placing a temporary real bed
     * under the player. Vanilla trySleep (and every tick afterwards) requires
     * an actual bed block: the player wakes the moment there is none. The
     * temporary bed is removed without drops as soon as the player wakes up,
     * so the held bed item is not consumed and nothing is duplicated. All
     * vanilla sleep rules (night, monsters, dimension) run inside trySleep as
     * usual. 1.12.2's SleepResult carries no message of its own, so the
     * vanilla lang keys are mapped here (1.12.2 has no OBSTRUCTED value; the
     * blocked-space pre-check reuses tile.bed.notValid).
     */
    private static boolean sleepInPlace(EntityPlayerMP player, ItemStack stack) {
        WorldServer world = player.getServerWorld();
        BlockPos foot = player.getPosition();
        BlockPos head = foot.offset(player.getHorizontalFacing());
        if (!world.getBlockState(foot).getMaterial().isReplaceable()
                || !world.getBlockState(head).getMaterial().isReplaceable()) {
            sendProblem(player, "tile.bed.notValid");
            return true;
        }
        IBlockState bed = bedState(stack, player.getHorizontalFacing());
        world.setBlockState(foot, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT), 3);
        world.setBlockState(head, bed.withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 3);
        EntityPlayer.SleepResult result = player.trySleep(foot);
        if (result != EntityPlayer.SleepResult.OK) {
            TempBedTracker.removeBed(world, foot, head);
            sendProblem(player, problemKey(result));
        } else {
            TempBedTracker.track(player, foot, head);
        }
        return true;
    }

    private static String problemKey(EntityPlayer.SleepResult result) {
        switch (result) {
            case NOT_POSSIBLE_NOW:
                return "tile.bed.noSleep";
            case NOT_SAFE:
                return "tile.bed.notSafe";
            case TOO_FAR_AWAY:
                return "tile.bed.tooFarAway";
            case NOT_POSSIBLE_HERE:
                return "tile.bed.notValid";
            default:
                return null;
        }
    }

    private static IBlockState bedState(ItemStack stack, EnumFacing facing) {
        IBlockState state;
        if (stack.getItem() instanceof net.minecraft.item.ItemBlock
                && ((net.minecraft.item.ItemBlock) stack.getItem()).getBlock() instanceof BlockBed) {
            state = ((net.minecraft.item.ItemBlock) stack.getItem()).getBlock().getDefaultState();
        } else {
            state = net.minecraft.init.Blocks.BED.getDefaultState();
        }
        // modded pseudo-beds may lack vanilla bed properties
        if (state.getProperties().containsKey(BlockHorizontal.FACING)) {
            state = state.withProperty(BlockHorizontal.FACING, facing);
        }
        return state;
    }

    private static void sendProblem(EntityPlayerMP player, String key) {
        if (key != null) {
            player.sendStatusMessage(new TextComponentTranslation(key), true);
        }
    }
}
