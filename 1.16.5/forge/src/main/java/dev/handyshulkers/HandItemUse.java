package dev.handyshulkers;

import com.mojang.datafixers.util.Either;
import net.minecraft.util.Unit;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.inventory.container.GrindstoneContainer;
import net.minecraft.inventory.container.LoomContainer;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.inventory.container.StonecutterContainer;
import net.minecraft.inventory.container.CartographyContainer;
import net.minecraft.inventory.container.SmithingTableContainer;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BedPart;
import net.minecraft.tags.ITag;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.world.server.ServerWorld;

/**
 * Turns plain right-clicks with functional items into their use action:
 * shulker boxes open, held beds sleep in place, held crafting tables,
 * enchanting tables, looms, stonecutters, cartography tables, smithing tables
 * and anvils show their screen, and ender chests open.
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
        Container create(int id, PlayerInventory inventory, ServerPlayerEntity player);
    }

    private static class HandScreen {
        final ITag.INamedTag<Item> tag;
        final String title;
        final ScreenFactory factory;

        HandScreen(ITag.INamedTag<Item> tag, String title, ScreenFactory factory) {
            this.tag = tag;
            this.title = title;
            this.factory = factory;
        }
    }

    // Java 8: no List.of, so a plain array it is.
    private static final HandScreen[] SCREENS = new HandScreen[]{
            new HandScreen(HandyShulkers.CRAFTING_TABLES, "container.crafting",
                    (id, inventory, player) -> new WorkbenchContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.LOOMS, "container.loom",
                    (id, inventory, player) -> new LoomContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.ENCHANTING_TABLES, "container.enchant",
                    (id, inventory, player) -> new EnchantmentContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.STONECUTTERS, "container.stonecutter",
                    (id, inventory, player) -> new StonecutterContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.CARTOGRAPHY_TABLES, "container.cartography_table",
                    (id, inventory, player) -> new CartographyContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.SMITHING_TABLES, "container.upgrade",
                    (id, inventory, player) -> new SmithingTableContainer(id, inventory, access(player))),
            new HandScreen(HandyShulkers.ANVILS, "container.repair",
                    (id, inventory, player) -> new RepairContainer(id, inventory, access(player))),
            // a grindstone's result is computed without world access, so the
            // vanilla menu with a null access works as-is
            new HandScreen(HandyShulkers.GRINDSTONES, "container.grindstone_title",
                    (id, inventory, player) -> new GrindstoneContainer(id, inventory)),
    };

    private static IWorldPosCallable access(ServerPlayerEntity player) {
        return IWorldPosCallable.create(player.level, player.blockPosition());
    }

    /** True for items this mod gives a hand-use action. */
    public static boolean isFunctional(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (HandyShulkers.isIn(stack, HandyShulkers.SHULKER_BOXES)
                || HandyShulkers.isIn(stack, HandyShulkers.BEDS)
                || HandyShulkers.isIn(stack, HandyShulkers.ENDER_CHESTS)
                || ShulkerOpenLogic.isVanillaShulkerBox(stack)) {
            return true;
        }
        for (HandScreen screen : SCREENS) {
            if (HandyShulkers.isIn(stack, screen.tag)) {
                return true;
            }
        }
        return ShulkerOpenLogic.declaredCapacity(stack) > 0;
    }

    public static ActionResultType useFromHand(PlayerEntity player, ItemStack stack) {
        HandyShulkersConfig config = HandyShulkersConfig.get();
        if (!isFunctional(stack)) {
            return ActionResultType.PASS;
        }
        if (config.requireSneak != player.isShiftKeyDown()) {
            return ActionResultType.PASS;
        }
        if (player.level.isClientSide) {
            return ActionResultType.PASS;
        }
        if (!(player instanceof ServerPlayerEntity)) {
            return ActionResultType.PASS;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (player.isSpectator()) {
            return ActionResultType.PASS;
        }
        if (!config.allowFakePlayers && player.getClass() != ServerPlayerEntity.class) {
            return ActionResultType.PASS;
        }
        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
            return ActionResultType.PASS;
        }

        if (HandyShulkers.isIn(stack, HandyShulkers.SHULKER_BOXES)
                || ShulkerOpenLogic.isVanillaShulkerBox(stack)
                || ShulkerOpenLogic.declaredCapacity(stack) > 0) {
            return ShulkerOpenLogic.openMenu(serverPlayer, stack);
        }
        if (HandyShulkers.isIn(stack, HandyShulkers.BEDS)) {
            return sleepInPlace(serverPlayer, stack);
        }
        if (HandyShulkers.isIn(stack, HandyShulkers.ENDER_CHESTS)) {
            return openEnderChest(serverPlayer);
        }
        for (HandScreen screen : SCREENS) {
            if (HandyShulkers.isIn(stack, screen.tag)) {
                final String title = screen.title;
                serverPlayer.openMenu(new SimpleNamedContainerProvider(new IContainerProvider() {
                    @Override
                    public Container createMenu(int id, PlayerInventory inventory, PlayerEntity p) {
                        return screen.factory.create(id, inventory, serverPlayer);
                    }
                }, new TranslationTextComponent(title)));
                return ActionResultType.SUCCESS;
            }
        }
        return ActionResultType.PASS;
    }

    private static ActionResultType openEnderChest(ServerPlayerEntity player) {
        EnderChestInventory enderChest = player.getEnderChestInventory();
        enderChest.startOpen(player);
        player.openMenu(new SimpleNamedContainerProvider(new IContainerProvider() {
            @Override
            public Container createMenu(int id, PlayerInventory inventory, PlayerEntity p) {
                return ChestContainer.threeRows(id, inventory, enderChest);
            }
        }, new TranslationTextComponent("container.enderchest")));
        return ActionResultType.SUCCESS;
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
    private static ActionResultType sleepInPlace(ServerPlayerEntity player, ItemStack stack) {
        ServerWorld level = player.getLevel();
        BlockPos foot = player.blockPosition();
        Direction facing = player.getDirection();
        BlockPos head = foot.relative(facing);
        if (!canReplace(level.getBlockState(foot)) || !canReplace(level.getBlockState(head))) {
            sendProblem(player, PlayerEntity.SleepResult.OBSTRUCTED);
            return ActionResultType.SUCCESS;
        }
        BlockState bed = bedState(stack, facing);
        level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), 3);
        Either<PlayerEntity.SleepResult, Unit> result = player.startSleepInBed(foot);
        if (result.left().isPresent()) {
            TempBedTracker.removeBed(level, foot, head);
            sendProblem(player, result.left().get());
        } else {
            TempBedTracker.track(player, foot, head);
        }
        return ActionResultType.SUCCESS;
    }

    private static boolean canReplace(BlockState state) {
        return state.getMaterial().isReplaceable();
    }

    private static BlockState bedState(ItemStack stack, Direction facing) {
        BlockState state;
        if (stack.getItem() instanceof BlockItem
                && ((BlockItem) stack.getItem()).getBlock() instanceof BedBlock) {
            state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        } else {
            state = Blocks.RED_BED.defaultBlockState();
        }
        // modded pseudo-beds may lack vanilla bed properties
        if (state.hasProperty(BedBlock.FACING)) {
            state = state.setValue(BedBlock.FACING, facing);
        }
        return state;
    }

    private static void sendProblem(ServerPlayerEntity player, PlayerEntity.SleepResult problem) {
        ITextComponent message = problem.getMessage();
        if (message != null) {
            player.displayClientMessage(message, true);
        }
    }
}
