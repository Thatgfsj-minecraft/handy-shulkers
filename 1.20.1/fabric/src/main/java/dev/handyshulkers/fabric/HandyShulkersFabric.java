package dev.handyshulkers.fabric;

import dev.handyshulkers.HandItemUse;
import dev.handyshulkers.HandyShulkers;
import dev.handyshulkers.HandyShulkersConfig;
import dev.handyshulkers.SelfTest;
import dev.handyshulkers.ScrollingMenu;
import dev.handyshulkers.TempBedTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class HandyShulkersFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        HandyShulkersConfig.init(FabricLoader.getInstance().getConfigDir().resolve("handyshulkers.json"));
        ScrollingMenu.TYPE = Registry.register(BuiltInRegistries.MENU,
                new ResourceLocation(HandyShulkers.MOD_ID, "scrolling"),
                new MenuType<>(ScrollingMenu::clientCreate, FeatureFlags.VANILLA_SET));
        SelfTest.run();
        UseBlockCallback.EVENT.register(HandyShulkersFabric::onUseBlock);
        UseItemCallback.EVENT.register(HandyShulkersFabric::onUseItem);
        ServerTickEvents.END_SERVER_TICK.register(TempBedTracker::tick);
    }

    /**
     * Plain right-click on a block: a block with its own menu (chest, crafting
     * table, ...) always wins; otherwise the held functional item is used
     * instead of placing it. Sneaking stays vanilla (place).
     */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (HandyShulkersConfig.get().requireSneak != player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!HandItemUse.isFunctional(stack)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (level.getBlockState(pos).getMenuProvider(level, pos) != null) {
            return InteractionResult.PASS;
        }
        return HandItemUse.useFromHand(player, stack);
    }

    /** 1.20.1 note: this callback returns a typed result over the held stack. */
    private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (HandyShulkersConfig.get().requireSneak != player.isShiftKeyDown() || !HandItemUse.isFunctional(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = HandItemUse.useFromHand(player, stack);
        if (result == InteractionResult.PASS) {
            return InteractionResultHolder.pass(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
