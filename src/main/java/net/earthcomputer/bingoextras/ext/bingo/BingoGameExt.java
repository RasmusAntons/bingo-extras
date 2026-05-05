package net.earthcomputer.bingoextras.ext.bingo;

import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface BingoGameExt {
    long bingoExtras$getSeed();
    void bingoExtras$setSeed(long seed);

    Map<ResourceKey<Level>, RuntimeLevelHandle> bingoExtras$getGameSpecificLevels();

    long bingo_extras$getGameSpecificWorldSeed();

    void bingo_extras$setGameSpecificWorldSeed(long seed);

    List<Component> bingo_extras$getExtraMessages();

    Map<Identifier, CommandContext<CommandSourceStack>> bingoExtras$getGameRules();

    static long getSeed(Object bingoGame) {
        return ((BingoGameExt) bingoGame).bingoExtras$getSeed();
    }

    static void setSeed(Object bingoGame, long seed) {
        ((BingoGameExt) bingoGame).bingoExtras$setSeed(seed);
    }

    @SuppressWarnings("deprecation")
    static ServerLevel getGameSpecificLevel(MinecraftServer server, BingoGame game, ResourceKey<Level> dimension) {
        return ((BingoGameExt) game).bingoExtras$getGameSpecificLevels().computeIfAbsent(dimension, k -> {
            ServerLevel parentLevel = Objects.requireNonNull(server.getLevel(dimension), () -> "No server level associated with " + dimension);
            RuntimeLevelHandle handle = Fantasy.get(server).openTemporaryLevel(
                    new RuntimeLevelConfig()
                            .setDimensionType(parentLevel.dimensionTypeRegistration())
                            .setDifficulty(Difficulty.NORMAL)
                            .setGenerator(parentLevel.getChunkSource().getGenerator())
                            .setSeed(((BingoGameExt) game).bingo_extras$getGameSpecificWorldSeed())
                            .setShouldTickTime(true)
                            .setMirrorOverworldGameRules(false)
                            .setGameTime(0)
            );

            GameRules gameRules = handle.asLevel().getGameRules();
            for (Map.Entry<Identifier, CommandContext<CommandSourceStack>> entry : ((BingoGameExt) game).bingoExtras$getGameRules().entrySet()) {
                GameRule<?> gameRule = BuiltInRegistries.GAME_RULE.get(entry.getKey()).orElseThrow().value();
                CommandContext<CommandSourceStack> ctx = entry.getValue();
                if (gameRule.valueClass().equals(Integer.class)) {
                    GameRule<Integer>  gameRuleInteger = (GameRule<Integer>) gameRule;
                    gameRules.set(gameRuleInteger, ctx.getArgument("value", Integer.class), server);
                } else if (gameRule.valueClass().equals(Boolean.class)) {
                    GameRule<Boolean>  gameRuleBoolean = (GameRule<Boolean>) gameRule;
                    gameRules.set(gameRuleBoolean, ctx.getArgument("value", Boolean.class), server);
                }
            }

            ((ServerLevelExt_Fantasy) handle.asLevel()).bingoExtras$setParentLevel(parentLevel);
            if (parentLevel.dimension() == Level.END && parentLevel.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
                handle.asLevel().setDragonFight(EnderDragonFight.createDefault());
            }
            return handle;
        }).asLevel();
    }
}
