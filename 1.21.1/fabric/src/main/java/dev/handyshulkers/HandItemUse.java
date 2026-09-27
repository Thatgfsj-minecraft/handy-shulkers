package dev.handyshulkers;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.List;

/**
 * Turns plain right-clicks with functional items into their use action:
 * shulker boxes open, held beds sleep in place, held crafting tables,
 * enchanting tables, looms, stonecutters, cartography tables, grindstones,
 * smithing tables and anvils show their screen, and ender chests open.
 * Sneak + right-click stays vanilla (place the item), and blocks with a menu
 * of their own always win when aimed at.
 *
 * <p>Everything is server authoritative. The client always returns PASS so
 * vanilla packets keep flowing and no interaction is ever swallowed.
 */
public final class HandItemUse {

    private HandItemUse() {
    }

    private interface ScreenFactory {
        AbstractContainerMenu create(int id, Inventory inventory, ServerPlayer player);
    }

    private record HandScreen(TagKey<Item> tag, String title, ScreenFactory factory) {
    }

    private static final List<HandScreen> SCREENS = List.of(
            new HandScreen(HandyShulkers.CRAFTING_TABLES, "container.crafting",
                    (id, inventory, player) -> new HandCraftingMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.LOOMS, "container.loom",
                    (id, inventory, player) -> new HandLoomMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.ENCHANTING_TABLES, "container.enchant",
                    (id, inventory, player) -> new HandEnchantingMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.STONECUTTERS, "container.stonecutter",
                    (id, inventory, player) -> new HandStonecutterMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.CARTOGRAPHY_TABLES, "container.cartography_table",
                    (id, inventory, player) -> new HandCartographyMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.SMITHING_TABLES, "container.upgrade",
                    (id, inventory, player) -> new HandSmithingMenu(id, inventory, player.level(), player.blockPosition())),
            new HandScreen(HandyShulkers.ANVILS, "container.repair",
                    (id, inventory, player) -> new HandAnvilMenu(id, inventory, player.level(), player.blockPosition())),
            // a grindstone's result is computed without world access, so the
            // vanilla menu with a null access works as-is
            new HandScreen(HandyShulkers.GRINDSTONES, "container.grindstone_title",
                    (id, inventory, player) -> new GrindstoneMenu(id, inventory)));

    /** True for items this mod gives a hand-use action. */
    public static boolean isFunctional(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(HandyShulkers.SHULKER_BOXES) || stack.is(HandyShulkers.BEDS)
                || stack.is(HandyShulkers.ENDER_CHESTS)) {
            return true;
        }
        for (HandScreen screen : SCREENS) {
            if (stack.is(screen.tag())) {
                return true;
            }
        }
        return false;
    }

    public static InteractionResult useFromHand(Player player, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!isFunctional(stack)) {
            return InteractionResult.PASS;
        }
        if (config.requireSneak != player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!config.allowFakePlayers && player.getClass() != ServerPlayer.class) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
            return InteractionResult.PASS;
        }

        if (stack.is(HandyShulkers.SHULKER_BOXES)) {
            return ShulkerOpenLogic.openMenu(serverPlayer, stack);
        }
        if (stack.is(HandyShulkers.BEDS)) {
            return sleepInPlace(serverPlayer, stack);
        }
        if (stack.is(HandyShulkers.ENDER_CHESTS)) {
            return openEnderChest(serverPlayer);
        }
        for (HandScreen screen : SCREENS) {
            if (stack.is(screen.tag())) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> screen.factory().create(id, inventory, serverPlayer),
                        Component.translatable(screen.title())));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult openEnderChest(ServerPlayer player) {
        var enderChest = player.getEnderChestInventory();
        enderChest.startOpen(player);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.threeRows(id, inventory, enderChest),
                Component.translatable("container.enderchest")));
        return InteractionResult.SUCCESS;
    }

    /**
     * Sleeps right where the player stands by placing a temporary real bed
     * under the player. Vanilla startSleepInBed (and every tick afterwards)
     * requires an actual bed block: LivingEntity.tick calls checkBedExists()
     * and wakes the player the moment there is none. The temporary bed is
     * removed without drops as soon as the player wakes up, so the held bed
     * item is not consumed and nothing is duplicated. All vanilla sleep rules
     * (night, monsters, dimension) run inside startSleepInBed as usual.
     */
    private static InteractionResult sleepInPlace(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        BlockPos foot = player.blockPosition();
        BlockPos head = foot.relative(player.getDirection());
        if (!level.getBlockState(foot).canBeReplaced() || !level.getBlockState(head).canBeReplaced()) {
            sendProblem(player, Player.BedSleepingProblem.OBSTRUCTED);
            return InteractionResult.SUCCESS;
        }
        BlockState bed = bedState(stack);
        level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 3);
        Either<Player.BedSleepingProblem, net.minecraft.util.Unit> result = player.startSleepInBed(foot);
        if (result.left().isPresent()) {
            TempBedTracker.removeBed(level, foot, head);
            sendProblem(player, result.left().get());
        } else {
            TempBedTracker.track(player, foot, head);
        }
        return InteractionResult.SUCCESS;
    }

    private static BlockState bedState(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BedBlock) {
            return blockItem.getBlock().defaultBlockState();
        }
        return Blocks.RED_BED.defaultBlockState();
    }

    private static void sendProblem(ServerPlayer player, Player.BedSleepingProblem problem) {
        Component message = problem.getMessage();
        if (message != null) {
            player.displayClientMessage(message, true);
        }
    }
}
