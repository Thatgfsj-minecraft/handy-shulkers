package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
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
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(HandyShulkers.LOOMS)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new HandLoomMenu(id, inventory,
                            serverPlayer.level(), serverPlayer.blockPosition()),
                    Component.translatable("container.loom")));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(HandyShulkers.BEDS)) {
            return sleepInPlace(serverPlayer);
        }
        return InteractionResult.PASS;
    }

    /**
     * Sleeps right where the player stands. Vanilla startSleepInBed cannot be
     * used here: it reads the bed's FACING property from the target position
     * and throws on anything that is not a bed block. The day/night, dimension
     * and thunder rules are taken from the position's BED_RULE environment
     * attribute so they stay identical to vanilla, then the plain sleep state
     * is entered, which still drives vanilla's night-skipping.
     */
    private static InteractionResult sleepInPlace(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();
        BedRule bedRule = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
        if (bedRule != null && !bedRule.canSleep(level)) {
            Player.BedSleepingProblem problem = bedRule.asProblem();
            if (problem != null && problem.message() != null) {
                player.displayClientMessage(problem.message(), true);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(8.0D, 5.0D, 8.0D),
                monster -> monster.isPreventingPlayerRest(level, player));
        if (!monsters.isEmpty()) {
            sendProblem(player, Player.BedSleepingProblem.NOT_SAFE);
            return InteractionResult.SUCCESS_SERVER;
        }
        player.startSleeping(pos);
        level.updateSleepingPlayerList();
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void sendProblem(ServerPlayer player, Player.BedSleepingProblem problem) {
        Component message = problem.message();
        if (message != null) {
            player.displayClientMessage(message, true);
        }
    }
}
