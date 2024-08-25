package net.earthcomputer.bingoextras;

import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public final class FantasyUtil {
    public static final ThreadLocal<MinecraftServer> currentServer = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> isForcedDimensionChange = ThreadLocal.withInitial(() -> false);

    private FantasyUtil() {
    }

    public static ResourceKey<Level> getOriginalDimension(@Nullable MinecraftServer server, ResourceKey<Level> teamDimension) {
        if (server == null) {
            return teamDimension;
        }
        ServerLevel teamLevel = Objects.requireNonNull(server.getLevel(teamDimension), () -> "Could not find server level for dimension " + teamDimension);
        ServerLevel parentLevel = ServerLevelExt_Fantasy.getOriginalLevel(teamLevel);
        if (parentLevel != null) {
            return parentLevel.dimension();
        }
        ServerLevel originalLevel = ServerLevelExt_Fantasy.getOriginalLevel(teamLevel);
        if (originalLevel != null) {
            return originalLevel.dimension();
        }
        return teamDimension;
    }

    // Called from mass ASM
    public static Level originalLevelOrSelf(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level;
        }
        ServerLevel parentLevel = ServerLevelExt_Fantasy.getParentLevel(serverLevel);
        if (parentLevel != null) {
            return parentLevel;
        }
        ServerLevel originalLevel = ServerLevelExt_Fantasy.getOriginalLevel(serverLevel);
        return originalLevel != null ? originalLevel : level;
    }

    public static void destroyTeamSpecificLevels(PlayerTeam team) {
        var teamSpecificLevels = ((PlayerTeamExt_Fantasy) team).bingoExtras$getTeamSpecificLevels();
        for (RuntimeWorldHandle handle : teamSpecificLevels.values()) {
            for (ServerPlayer player : new ArrayList<>(handle.asWorld().players())) {
                forceDimensionChange(() -> player.teleportTo(ServerLevelExt_Fantasy.getOriginalLevel(handle.asWorld()), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
            }
            handle.delete();
        }
        teamSpecificLevels.clear();
    }

    public static void forceDimensionChange(Runnable action) {
        boolean wasForcedDimensionChange = isForcedDimensionChange.get();
        isForcedDimensionChange.set(true);
        try {
            action.run();
        } finally {
            isForcedDimensionChange.set(wasForcedDimensionChange);
        }
    }

    public static <T> T forceDimensionChange(Supplier<T> action) {
        boolean wasForcedDimensionChange = isForcedDimensionChange.get();
        isForcedDimensionChange.set(true);
        try {
            return action.get();
        } finally {
            isForcedDimensionChange.set(wasForcedDimensionChange);
        }
    }

    public static boolean isForcedDimensionChange() {
        return isForcedDimensionChange.get();
    }

    public static void destroyGameSpecificLevels(BingoGame game) {
        var gameSpecificLevels = ((BingoGameExt) game).bingoExtras$getGameSpecificLevels();
        for (RuntimeWorldHandle handle : gameSpecificLevels.values()) {
            for (ServerPlayer player : new ArrayList<>(handle.asWorld().players())) {
                ServerLevel overworld = player.getServer().overworld();
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), player.getYRot(), player.getXRot());
            }
            handle.delete();
        }
        gameSpecificLevels.clear();
    }
}
