package dev.handyshulkers;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;

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
                    (id, inventory, p) -> new CraftingMenu(id, inventory,
                            ContainerLevelAccess.create(serverPlayer.level(), serverPlayer.blockPosition())),
                    Component.translatable("container.crafting")));
            return InteractionResult.SUCCESS;
        }
        if (stack.is(HandyShulkers.LOOMS)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new LoomMenu(id, inventory,
                            ContainerLevelAccess.create(serverPlayer.level(), serverPlayer.blockPosition())),
                    Component.translatable("container.loom")));
            return InteractionResult.SUCCESS;
        }
        if (stack.is(HandyShulkers.BEDS)) {
            Either<Player.BedSleepingProblem, net.minecraft.util.Unit> result =
                    serverPlayer.startSleepInBed(serverPlayer.blockPosition());
            result.ifLeft(problem -> serverPlayer.displayClientMessage(problem.getMessage(), true));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
