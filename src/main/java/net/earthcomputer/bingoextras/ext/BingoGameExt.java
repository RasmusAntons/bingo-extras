package net.earthcomputer.bingoextras.ext;

import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.util.GameRuleStore;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface BingoGameExt {
    Map<ResourceKey<Level>, RuntimeWorldHandle> bingoExtras$getGameSpecificLevels();

    long bingo_extras$getGameSpecificWorldSeed();

    void bingo_extras$setGameSpecificWorldSeed(long seed);

    List<Component> bingo_extras$getExtraMessages();

    Map<GameRules.Key<?>, CommandContext<CommandSourceStack>> bingoExtras$getGameRules();

    @SuppressWarnings("deprecation")
    static ServerLevel getGameSpecificLevel(MinecraftServer server, BingoGame game, ResourceKey<Level> dimension) {
        return ((BingoGameExt) game).bingoExtras$getGameSpecificLevels().computeIfAbsent(dimension, k -> {
            ServerLevel parentLevel = Objects.requireNonNull(server.getLevel(dimension), () -> "No server level associated with " + dimension);
            RuntimeWorldHandle handle = Fantasy.get(server).openTemporaryWorld(
                    new RuntimeWorldConfig()
                            .setDimensionType(parentLevel.dimensionTypeRegistration())
                            .setDifficulty(Difficulty.NORMAL)
                            .setGenerator(parentLevel.getChunkSource().getGenerator())
                            .setSeed(((BingoGameExt) game).bingo_extras$getGameSpecificWorldSeed())
                            .setShouldTickTime(true)
                            .setMirrorOverworldGameRules(false)
                            .setTimeOfDay(0)
            );
            GameRules gameRules = handle.asWorld().getGameRules();
            for (Map.Entry<GameRules.Key<?>, CommandContext<CommandSourceStack>> entry : ((BingoGameExt) game).bingoExtras$getGameRules().entrySet()) {
                GameRules.Key<? extends GameRules.Value<?>> key = entry.getKey();
                CommandContext<CommandSourceStack> val = entry.getValue();
                gameRules.getRule(key).setFromArgument(val, "value");
            }
            ((ServerLevelExt_Fantasy) handle.asWorld()).bingoExtras$setParentLevel(parentLevel);
            if (parentLevel.dimension() == Level.END && parentLevel.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
                handle.asWorld().setDragonFight(new EndDragonFight(handle.asWorld(), parentLevel.getSeed(), EndDragonFight.Data.DEFAULT));
            }
            return handle;
        }).asWorld();
    }
}
