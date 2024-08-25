package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.rasmusantons.cubiomes.BiomeID;
import de.rasmusantons.cubiomes.Cubiomes;
import de.rasmusantons.cubiomes.Dimension;
import de.rasmusantons.cubiomes.MCVersion;
import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DimensionTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.PlayerTeam;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.*;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersCommand.findSurface;
import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersCommand.groupEntities;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.getResourceOrTag;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.resourceOrTag;

public class BingoSpreadPlayersSeedfindCommand {
    public static final SimpleCommandExceptionType NO_RUNNING_GAME_EXCEPTION = new SimpleCommandExceptionType(BingoExtras.translatable("bingo_extras.bingospreadplayers.noRunningGame"));
    public static final SimpleCommandExceptionType FAILED_TO_FIND_SEED_EXCEPTION = new SimpleCommandExceptionType(BingoExtras.translatable("bingo_extras.bingospreadplayers.failedToFindSeed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("bingospreadplayersseedfind")
            .requires(source -> source.hasPermission(2))
                .then(argument("distance", doubleArg(0, 5000))
                    .then(argument("spread", doubleArg(0, 100))
                        .then(argument("respectTeams", bool())
                            .then(argument("targets", entities())
                                .then(argument("sameBiome", bool())
                                    .then(argument("excludedBiomes", resourceOrTag(buildContext, Registries.BIOME))
                                        .executes(ctx -> bingoSpreadPlayers(
                                            ctx.getSource(),
                                            getDouble(ctx, "distance"),
                                            getDouble(ctx, "spread"),
                                            getBool(ctx, "respectTeams"),
                                            getEntities(ctx, "targets"),
                                            getBool(ctx, "sameBiome"),
                                            getResourceOrTag(ctx, "excludedBiomes", Registries.BIOME)
                                        )))))))));
    }

    private static int bingoSpreadPlayers(
            CommandSourceStack source,
            double distance,
            double spread,
            boolean respectTeams,
            Collection<? extends Entity> entities,
            boolean sameBiome,
            Predicate<Holder<Biome>> excludedBiomes
    ) throws CommandSyntaxException {
        List<List<Entity>> groups = groupEntities(entities, respectTeams);
        if (groups.isEmpty()) {
            throw new AssertionError("No groups");
        }

        BingoGame activeGame = Bingo.activeGame;
        if (activeGame == null) {
            throw NO_RUNNING_GAME_EXCEPTION.create();
        }

        RandomSource rand = RandomSource.create();
        Util.shuffle(groups, rand);
        Vec2[] groupSpawns = getGroupSpawnPositions(groups.size(), distance);
        long seed = findSeed(groupSpawns, sameBiome, excludedBiomes, rand);

        ((BingoGameExt) activeGame).bingo_extras$setGameSpecificWorldSeed(seed);
        teleportPlayers(source, activeGame, groupSpawns, groups);

        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingospreadplayers.success"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static long findSeed(Vec2[] spawnPositions, boolean sameBiome, Predicate<Holder<Biome>> excludedBiomes, RandomSource rand) throws CommandSyntaxException {
        Cubiomes cubiomes = new Cubiomes(MCVersion.MC_1_21);
        long seed = 0;
        int attempts = 1000;
        iterSeeds: while (seed == 0 && attempts > 0) {
            --attempts;
            seed = rand.nextLong();
            cubiomes.applySeed(Dimension.DIM_OVERWORLD, seed);
            for (Vec2 spawnPosition : spawnPositions) {
                BiomeID biomeID = cubiomes.getBiomeAt(1, (int) spawnPosition.x, 63, (int) spawnPosition.y);
                if (biomeID != BiomeID.plains) {
                    System.out.printf("skipping seed %d\n", seed);
                    seed = 0;
                    continue iterSeeds;
                }
            }
        }
        if (seed == 0)
            throw FAILED_TO_FIND_SEED_EXCEPTION.create();
        return seed;
    }

    private static Vec2[] getGroupSpawnPositions(int nGroups, double distance) {
        Vec2[] res = new Vec2[nGroups];
        for (int i = 0; i < nGroups; i++) {
            double x = Math.floor(distance * Math.cos(2 * Math.PI / nGroups * i)) + 0.5;
            double z = Math.floor(distance * Math.sin(2 * Math.PI / nGroups * i)) + 0.5;
            res[i] = new Vec2((float) x, (float) z);
        }
        return res;
    }

    private static void teleportPlayers(CommandSourceStack source, BingoGame activeGame, Vec2[] groupSpawns, List<List<Entity>> groups) {
        MinecraftServer server = source.getServer();

        ServerLevel gameLevel = BingoGameExt.getGameSpecificLevel(server, activeGame, server.overworld().dimension());

        for (int i = 0; i < groups.size(); ++i) {
            List<Entity> entities = groups.get(i);
            Vec2 groupSpawn = groupSpawns[i];
            int targetY = findSurface(gameLevel, (int) Math.floor(groupSpawn.x), (int) Math.floor(groupSpawn.y));
            for (Entity entity : entities) {
                entity.teleportTo(gameLevel, groupSpawn.x, targetY, groupSpawn.y, Set.of(), entity.getYRot(), entity.getXRot());
            }
        }
    }
}
