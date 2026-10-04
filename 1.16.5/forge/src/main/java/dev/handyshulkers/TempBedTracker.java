package dev.handyshulkers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the temporary bed blocks placed for in-place sleeping and removes
 * them once the sleeper wakes up or disconnects. Drops are suppressed while
 * removing so the bed item the player is holding can never be duplicated.
 */
public final class TempBedTracker {

    /** Mirrors the Forge block-update flag; 1.16.5 Block lacks the constant. */
    private static final int UPDATE_SUPPRESS_DROPS = 16;

    private static final class Entry {
        final RegistryKey<World> dimension;
        final BlockPos foot;
        final BlockPos head;

        Entry(RegistryKey<World> dimension, BlockPos foot, BlockPos head) {
            this.dimension = dimension;
            this.foot = foot;
            this.head = head;
        }
    }

    private static final Map<UUID, Entry> TRACKED = new ConcurrentHashMap<>();

    private TempBedTracker() {
    }

    public static void track(ServerPlayerEntity player, BlockPos foot, BlockPos head) {
        TRACKED.put(player.getUUID(), new Entry(player.level.dimension(), foot, head));
    }

    /** Removes a just-placed bed after a failed sleep attempt (no tracking). */
    public static void removeBed(ServerWorld level, BlockPos foot, BlockPos head) {
        removePart(level, head);
        removePart(level, foot);
    }

    /** Called once per server tick: beds whose sleeper woke up get removed. */
    public static void tick() {
        if (TRACKED.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        TRACKED.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isSleeping()) {
                ServerWorld level = server.getLevel(entry.getValue().dimension);
                if (level != null) {
                    removeBed(level, entry.getValue().foot, entry.getValue().head);
                }
                return true;
            }
            return false;
        });
    }

    private static void removePart(ServerWorld level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) {
            // UPDATE_SUPPRESS_DROPS: the held bed item must not drop back
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3 | UPDATE_SUPPRESS_DROPS);
        }
    }
}
