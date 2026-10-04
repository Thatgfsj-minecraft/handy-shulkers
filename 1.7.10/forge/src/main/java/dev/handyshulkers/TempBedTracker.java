package dev.handyshulkers;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 临时床追踪（对应 1.21 TempBedTracker）：就地入睡放置的床方块在醒来/登出/
 * 服务端 tick 兜底时回收。用 setBlock 直接替换为空气，不触发 breakBlock，不掉落，
 * 手持的床物品永远不会被复制。
 */
public final class TempBedTracker {

    private static final class Entry {
        final int dimension;
        final int footX;
        final int footY;
        final int footZ;
        final int headX;
        final int headY;
        final int headZ;

        Entry(int dimension, int footX, int footY, int footZ, int headX, int headY, int headZ) {
            this.dimension = dimension;
            this.footX = footX;
            this.footY = footY;
            this.footZ = footZ;
            this.headX = headX;
            this.headY = headY;
            this.headZ = headZ;
        }
    }

    private static final Map<UUID, Entry> TRACKED = new LinkedHashMap<UUID, Entry>();

    public TempBedTracker() {
    }

    public static void track(EntityPlayerMP player, int dimension, int footX, int footY, int footZ, int headX, int headY, int headZ) {
        TRACKED.put(player.getUniqueID(), new Entry(dimension, footX, footY, footZ, headX, headY, headZ));
    }

    /** 睡眠失败后的立即回收（不追踪）。 */
    public static void removeBed(World world, int footX, int footY, int footZ, int headX, int headY, int headZ) {
        clearPart(world, headX, headY, headZ);
        clearPart(world, footX, footY, footZ);
    }

    private static void clearPart(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) instanceof BlockBed) {
            // setBlock 不走 breakBlock：无掉落、无音效；meta 0 清部件标记
            world.setBlock(x, y, z, Blocks.air, 0, 2);
        }
    }

    /** 醒来即拆床（PlayerWakeUpEvent 在 wakeUpPlayer 入口触发）。 */
    @SubscribeEvent
    public void onWake(PlayerWakeUpEvent event) {
        Entry entry = TRACKED.remove(event.entityPlayer.getUniqueID());
        if (entry != null) {
            World world = dimensionWorld(entry.dimension);
            if (world != null) {
                removeBed(world, entry.footX, entry.footY, entry.footZ, entry.headX, entry.headY, entry.headZ);
            }
        }
    }

    /** 登出/断线：床必须回收，否则留下隐形床脚手架。 */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Entry entry = TRACKED.remove(event.player.getUniqueID());
        if (entry != null) {
            World world = dimensionWorld(entry.dimension);
            if (world != null) {
                removeBed(world, entry.footX, entry.footY, entry.footZ, entry.headX, entry.headY, entry.headZ);
            }
        }
    }

    /** 服务端 tick 兜底：玩家不在睡（或已消失）即回收。 */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACKED.isEmpty()) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        Iterator<Map.Entry<UUID, Entry>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Entry> entry = iterator.next();
            EntityPlayerMP player = findPlayer(server, entry.getKey());
            if (player == null || !player.isPlayerSleeping()) {
                World world = dimensionWorld(entry.getValue().dimension);
                if (world != null) {
                    removeBed(world, entry.getValue().footX, entry.getValue().footY, entry.getValue().footZ,
                            entry.getValue().headX, entry.getValue().headY, entry.getValue().headZ);
                }
                iterator.remove();
            }
        }
    }

    private static World dimensionWorld(int dimension) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return null;
        }
        WorldServer[] worlds = server.worldServers;
        if (dimension >= 0 && worlds != null && dimension < worlds.length) {
            return worlds[dimension];
        }
        return null;
    }

    private static EntityPlayerMP findPlayer(MinecraftServer server, UUID id) {
        Iterator<?> iterator = server.getConfigurationManager().playerEntityList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object instanceof EntityPlayerMP && ((EntityPlayerMP) object).getUniqueID().equals(id)) {
                return (EntityPlayerMP) object;
            }
        }
        return null;
    }
}
