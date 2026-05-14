package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.gaming32.bingo.ext.MinecraftServerExt;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.CubiomesUtils;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2d;

import java.util.*;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersCommand.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.DimensionArgument.dimension;
import static net.minecraft.commands.arguments.DimensionArgument.getDimension;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.getResourceOrTag;
import static net.minecraft.commands.arguments.ResourceOrTagArgument.resourceOrTag;

public class BingoSpreadPlayersSeedfindCommand {
    public static final SimpleCommandExceptionType NO_RUNNING_GAME_EXCEPTION = new SimpleCommandExceptionType(BingoExtras.translatable("bingo_extras.bingospreadplayers.noRunningGame"));
    public static final SimpleCommandExceptionType FAILED_TO_FIND_SEED_EXCEPTION = new SimpleCommandExceptionType(BingoExtras.translatable("bingo_extras.bingospreadplayers.failedToFindSeed"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("bingospreadplayersseedfind")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(argument("distance", doubleArg(0, 5000))
                        .then(argument("spread", doubleArg(0, 100))
                                .then(argument("mapSize", integer(0, 10000))
                                        .then(argument("respectTeams", bool())
                                                .then(argument("targets", entities())
                                                        .then(argument("sameBiome", bool())
                                                                .then(argument("startDimension", dimension())
                                                                        .then(argument("excludedBiomes", resourceOrTag(buildContext, Registries.BIOME))
                                                                                .executes(ctx -> bingoSpreadPlayers(
                                                                                        ctx.getSource(),
                                                                                        getDouble(ctx, "distance"),
                                                                                        getDouble(ctx, "spread"),
                                                                                        getInteger(ctx, "mapSize"),
                                                                                        getBool(ctx, "respectTeams"),
                                                                                        getEntities(ctx, "targets"),
                                                                                        getBool(ctx, "sameBiome"),
                                                                                        getDimension(ctx, "startDimension"),
                                                                                        getResourceOrTag(ctx, "excludedBiomes", Registries.BIOME)
                                                                                )))))))))));
    }

    private static int bingoSpreadPlayers(
            CommandSourceStack source,
            double distance,
            double spread,
            int mapSize,
            boolean respectTeams,
            Collection<? extends Entity> entities,
            boolean sameBiome,
            ServerLevel dimension,
            Predicate<Holder<Biome>> excludedBiomes
    ) throws CommandSyntaxException {
        List<List<Entity>> groups = groupEntities(entities, respectTeams);
        if (groups.isEmpty()) {
            throw new AssertionError("No groups");
        }

        BingoGame activeGame = ((MinecraftServerExt) source.getServer()).bingo$getGame();
        if (activeGame == null) {
            throw NO_RUNNING_GAME_EXCEPTION.create();
        }
        if (BingoExtras.seedfindGame != null) {
            FantasyUtil.destroyGameSpecificLevels(BingoExtras.seedfindGame);
        }
        BingoExtras.seedfindGame = activeGame;
        ((BingoGameExt) activeGame).bingo_extras$getExtraMessages().clear();

        RandomSource rand = RandomSource.create();
        Util.shuffle(groups, rand);
        CubiomesUtils.SeedfinderResult seedfinderResult;
        try {
            seedfinderResult = CubiomesUtils.findSeed(
                    activeGame,
                    mapSize,
                    groups.size(),
                    distance,
                    sameBiome,
                    dimension.dimension(),
                    excludedBiomes,
                    rand,
                    source.registryAccess()
            );
        } catch (CubiomesUtils.SeedfindException e) {
            throw FAILED_TO_FIND_SEED_EXCEPTION.create();
        }

        ((BingoGameExt) activeGame).bingo_extras$getExtraMessages().addAll(seedfinderResult.extraMessages());
        ((BingoGameExt) activeGame).bingo_extras$setGameSpecificWorldSeed(seedfinderResult.seed());
        teleportPlayers(source, activeGame, seedfinderResult.spawnLocations(), groups, dimension.dimension(), spread, rand);

        for (Component extraMessage : ((BingoGameExt) activeGame).bingo_extras$getExtraMessages()) {
            System.out.println(extraMessage.getString());
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(extraMessage);
            }
        }

        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingospreadplayers.success"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static void teleportPlayers(CommandSourceStack source, BingoGame activeGame, Vec2[] groupSpawns, List<List<Entity>> groups, ResourceKey<Level> dimension, double spread, RandomSource rand) {
        MinecraftServer server = source.getServer();

        ServerLevel gameLevel = BingoGameExt.getGameSpecificLevel(server, activeGame, dimension);

        for (int i = 0; i < groups.size(); ++i) {
            List<Entity> entities = groups.get(i);
            for (Entity entity : entities) {
                double effectiveX = groupSpawns[i].x - spread / 2 + rand.nextInt((int) spread);
                double effectiveY = groupSpawns[i].y - spread / 2 + rand.nextInt((int) spread);
                var adjustedSpawn = adjustToSafeLocation(gameLevel, new Vector2d(effectiveX, effectiveY));
                entity.teleportTo(gameLevel, adjustedSpawn.x, adjustedSpawn.y, adjustedSpawn.z, Relative.ROTATION, entity.getYRot(), entity.getXRot(), true);
            }
        }
    }
}
