package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Turns plain right-clicks with functional items into their use action:
 * shulker boxes open, held beds sleep in place, held crafting tables and
 * looms show their screen. Sneak + right-click stays vanilla (place the
 * item), and blocks with a menu of their own always win when aimed at.
 *
 * <p>Everything is server authoritative. The client always returns PASS so
 * vanilla packets keep flowing and no interaction is ever swallowed.
 */
public final class HandItemUse {

    private HandItemUse() {
    }

    /** True for items this mod gives a hand-use action. */
    public static boolean isFunctional(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(HandyShulkers.SHULKER_BOXES)
                || stack.is(HandyShulkers.CRAFTING_TABLES)
                || stack.is(HandyShulkers.LOOMS)
                || stack.is(HandyShulkers.BEDS);
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
        if (stack.is(HandyShulkers.CRAFTING_TABLES)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new HandCraftingMenu(id, inventory,
                            serverPlayer.level(), serverPlayer.blockPosition()),
                    Component.translatable("container.crafting")));
            return InteractionResult.SUCCESS;
        }
        if (stack.is(HandyShulkers.LOOMS)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new HandLoomMenu(id, inventory,
                            serverPlayer.level(), serverPlayer.blockPosition()),
                    Component.translatable("container.loom")));
            return InteractionResult.SUCCESS;
        }
        if (stack.is(HandyShulkers.BEDS)) {
            return sleepInPlace(serverPlayer);
        }
        return InteractionResult.PASS;
    }

    /**
     * Sleeps right where the player stands. Vanilla startSleepInBed cannot be
     * used here: it reads the bed's FACING property from the target position
     * and throws on anything that is not a bed block. So the checks (working
     * bed dimension, night time, no monsters nearby) are replicated and then
     * the plain sleep state is entered, which still drives vanilla's
     * night-skipping.
     */
    private static InteractionResult sleepInPlace(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.dimensionType().bedWorks()) {
            sendProblem(player, Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            return InteractionResult.SUCCESS;
        }
        if (level.isDay()) {
            sendProblem(player, Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            return InteractionResult.SUCCESS;
        }
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(8.0D, 5.0D, 8.0D),
                monster -> monster.isPreventingPlayerRest(player));
        if (!monsters.isEmpty()) {
            sendProblem(player, Player.BedSleepingProblem.NOT_SAFE);
            return InteractionResult.SUCCESS;
        }
        player.startSleeping(player.blockPosition());
        level.updateSleepingPlayerList();
        return InteractionResult.SUCCESS;
    }

    private static void sendProblem(ServerPlayer player, Player.BedSleepingProblem problem) {
        Component message = problem.getMessage();
        if (message != null) {
            player.displayClientMessage(message, true);
        }
    }
}
