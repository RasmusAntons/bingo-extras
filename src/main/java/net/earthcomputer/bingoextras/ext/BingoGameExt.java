package net.earthcomputer.bingoextras.ext;

import com.google.common.base.Preconditions;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Map;
import java.util.Objects;

public interface BingoGameExt {
    Map<ResourceKey<Level>, RuntimeWorldHandle> bingoExtras$getGameSpecificLevels();

    public long bingo_extras$getGameSpecificWorldSeed();

    public void bingo_extras$setGameSpecificWorldSeed(long seed);

    @SuppressWarnings("deprecation")
    static ServerLevel getGameSpecificLevel(MinecraftServer server, BingoGame game, ResourceKey<Level> dimension) {
        System.out.printf("Attempting to get Game Level for %s, Game Levels:\n", dimension);
        for (Map.Entry<ResourceKey<Level>, RuntimeWorldHandle> entry : ((BingoGameExt) game).bingoExtras$getGameSpecificLevels().entrySet()) {
            System.out.printf("%s: %s\n", entry.getKey(), entry.getValue());
        }
        return ((BingoGameExt) game).bingoExtras$getGameSpecificLevels().computeIfAbsent(dimension, k -> {
            ServerLevel parentLevel = Objects.requireNonNull(server.getLevel(dimension), () -> "No server level associated with " + dimension);
            RuntimeWorldHandle handle = Fantasy.get(server).openTemporaryWorld(
                    new RuntimeWorldConfig()
                            .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                            .setDifficulty(Difficulty.NORMAL)
                            .setGenerator(parentLevel.getChunkSource().getGenerator())
                            .setSeed(((BingoGameExt) game).bingo_extras$getGameSpecificWorldSeed())
                            .setShouldTickTime(true)
                            .setMirrorOverworldGameRules(false)
                            .setTimeOfDay(0)
            );
            ((ServerLevelExt_Fantasy) handle.asWorld()).bingoExtras$setParentLevel(parentLevel);
            if (parentLevel.dimension() == Level.END && parentLevel.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
                handle.asWorld().setDragonFight(new EndDragonFight(handle.asWorld(), parentLevel.getSeed(), EndDragonFight.Data.DEFAULT));
            }
            return handle;
        }).asWorld();
    }
}
