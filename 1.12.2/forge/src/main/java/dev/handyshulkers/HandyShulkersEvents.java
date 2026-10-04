package dev.handyshulkers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Forge event wiring. RightClickBlock is deliberately untouched: blocks aimed
 * at keep their vanilla behaviour (a block with a menu of its own always
 * wins). Hand-use dispatches from RightClickItem only, and only on the server
 * - the client passes so vanilla packets keep flowing.
 */
public class HandyShulkersEvents {

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isRemote) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItem(event.getHand());
        if (HandItemUse.useFromHand(player, event.getHand(), stack)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TempBedTracker.tick(FMLCommonHandler.instance().getMinecraftServerInstance());
        }
    }
}
