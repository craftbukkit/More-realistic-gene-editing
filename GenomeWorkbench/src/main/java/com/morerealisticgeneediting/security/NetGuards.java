package com.morerealisticgeneediting.security;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A utility class for performing common security checks on C2S network packets.
 * This enforces server-authoritative logic and prevents exploits like out-of-range interactions.
 */
public final class NetGuards {

    private static final double MAX_INTERACTION_DISTANCE_SQ = Math.pow(6.0, 2); // Squared distance for performance

    /**
     * A comprehensive guard for typical BlockEntity interactions initiated by a player.
     *
     * @param player The player initiating the request.
     * @param world The world where the interaction happens.
     * @param pos The position of the BlockEntity.
     * @param expectedBlockEntityType The class of the expected BlockEntity.
     * @param customStateCheck An optional, additional predicate to check the BlockEntity's state.
     * @param <T> The type of the BlockEntity.
     * @return true if all checks pass, false otherwise.
     */
    public static <T> boolean inRangeAndCorrectBlock(ServerPlayerEntity player, World world, BlockPos pos, Class<T> expectedBlockEntityType, @Nullable Predicate<T> customStateCheck) {
        if (player == null || world == null || pos == null || !player.isAlive()) {
            return false;
        }

        // 1. Distance Check: Ensure the player is close enough to the block.
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_INTERACTION_DISTANCE_SQ) {
            return false;
        }

        // 2. World and Chunk Check: Ensure the block is in the same world and the chunk is loaded.
        if (player.getWorld() != world || !world.isChunkLoaded(pos)) {
            return false;
        }

        // 3. BlockEntity Type Check: Ensure the BE at the position is of the expected type.
        T blockEntity = world.getBlockEntity(pos, expectedBlockEntityType).orElse(null);
        if (blockEntity == null) {
            return false;
        }

        // 4. Custom State Check: Allow for additional, context-specific validation (e.g., machine is not busy).
        if (customStateCheck != null) {
            return customStateCheck.test(blockEntity);
        }

        return true;
    }
}
