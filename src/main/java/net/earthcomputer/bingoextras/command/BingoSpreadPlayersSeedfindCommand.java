package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.rasmusantons.cubiomes.BiomeID;
import de.rasmusantons.cubiomes.Cubiomes;
import de.rasmusantons.cubiomes.Dimension;
import de.rasmusantons.cubiomes.MCVersion;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersCommand.groupEntities;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.getResourceOrTag;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.resourceOrTag;

public class BingoSpreadPlayersSeedfindCommand {
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

        RandomSource rand = RandomSource.create();
        Util.shuffle(groups, rand);
        Vec2[] groupSpawns = getGroupSpawnPositions(groups.size(), distance);
        for (Vec2 vec2 : groupSpawns) {
            System.out.printf("Group Spawn: %f, %f\n", vec2.x, vec2.y);
        }

        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingospreadplayers.success"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static long findSeed(Vec2[] spawnPositions, boolean sameBiome, Predicate<Holder<Biome>> excludedBiomes, RandomSource rand) {
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
                    seed = 0;
                    continue iterSeeds;
                }
            }
        }
        return seed;
    }

    private static Vec2[] getGroupSpawnPositions(int groups, double distance) {
        Vec2[] res = new Vec2[groups];
        for (int i = 0; i < groups; i++) {
            double x = distance * Math.cos(2 * Math.PI / groups * i);
            double z = distance * Math.sin(2 * Math.PI / groups * i);
            res[i] = new Vec2((float) x, (float) z);
        }
        return res;
    }

    private static void teleportPlayers(CommandSourceStack source, BlockPos destPos, Collection<? extends Entity> entities) {
        ServerLevel level = source.getLevel();
        ServerLevel originalLevel = Objects.requireNonNullElse(ServerLevelExt_Fantasy.getOriginalLevel(source.getLevel()), level);

        for (Entity entity : entities) {
            PlayerTeam team = entity.getTeam();
            ServerLevel destLevel;
            if (team != null) {
                destLevel = PlayerTeamExt_Fantasy.getTeamSpecificLevel(source.getServer(), team, originalLevel.dimension());
            } else {
                destLevel = originalLevel;
            }
            entity.teleportTo(destLevel, destPos.getX() + 0.5, destPos.getY(), destPos.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot());
        }
    }
}
