package dev.handyshulkers;

import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the temporary bed blocks placed for in-place sleeping and removes
 * them once the sleeper wakes up or disconnects. 1.12.2 removes blocks with
 * plain {@code setBlockState(air)} which never drops items, so the held bed
 * item can never be duplicated.
 */
public final class TempBedTracker {

    private static final Map<UUID, Entry> TRACKED = new ConcurrentHashMap<UUID, Entry>();

    private static final class Entry {
        final int dimension;
        final BlockPos foot;
        final BlockPos head;

        Entry(int dimension, BlockPos foot, BlockPos head) {
            this.dimension = dimension;
            this.foot = foot;
            this.head = head;
        }
    }

    private TempBedTracker() {
    }

    public static void track(EntityPlayerMP player, BlockPos foot, BlockPos head) {
        TRACKED.put(player.getUniqueID(), new Entry(player.dimension, foot, head));
    }

    /** Removes a just-placed bed after a failed sleep attempt (no tracking). */
    public static void removeBed(WorldServer level, BlockPos foot, BlockPos head) {
        removePart(level, head);
        removePart(level, foot);
    }

    /** Called once per server tick: beds whose sleeper woke up get removed. */
    public static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Entry> entry : TRACKED.entrySet()) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(entry.getKey());
            if (player == null || !player.isPlayerSleeping()) {
                WorldServer level = server.getWorld(entry.getValue().dimension);
                if (level != null) {
                    removeBed(level, entry.getValue().foot, entry.getValue().head);
                }
                TRACKED.remove(entry.getKey());
            }
        }
    }

    private static void removePart(WorldServer level, BlockPos pos) {
        IBlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BlockBed) {
            // setBlockState to air never drops items in 1.12.2: the held bed
            // item must not drop back
            level.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }
}
