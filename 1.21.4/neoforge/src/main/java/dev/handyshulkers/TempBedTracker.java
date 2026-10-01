package dev.handyshulkers;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the temporary bed blocks placed for in-place sleeping and removes
 * them once the sleeper wakes up or disconnects. Drops are suppressed while
 * removing so the bed item the player is holding can never be duplicated.
 */
public final class TempBedTracker {

    private record Entry(ResourceKey<Level> dimension, BlockPos foot, BlockPos head) {
    }

    private static final Map<UUID, Entry> TRACKED = new ConcurrentHashMap<>();

    private TempBedTracker() {
    }

    public static void track(ServerPlayer player, BlockPos foot, BlockPos head) {
        TRACKED.put(player.getUUID(), new Entry(player.level().dimension(), foot, head));
    }

    /** Removes a just-placed bed after a failed sleep attempt (no tracking). */
    public static void removeBed(ServerLevel level, BlockPos foot, BlockPos head) {
        removePart(level, head);
        removePart(level, foot);
    }

    /** Called once per server tick: beds whose sleeper woke up get removed. */
    public static void tick(MinecraftServer server) {
        if (TRACKED.isEmpty()) {
            return;
        }
        TRACKED.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isSleeping()) {
                ServerLevel level = server.getLevel(entry.getValue().dimension());
                if (level != null) {
                    removeBed(level, entry.getValue().foot(), entry.getValue().head());
                }
                return true;
            }
            return false;
        });
    }

    private static void removePart(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) {
            // Block.UPDATE_SUPPRESS_DROPS: the held bed item must not drop back
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3 | Block.UPDATE_SUPPRESS_DROPS);
        }
    }
}
