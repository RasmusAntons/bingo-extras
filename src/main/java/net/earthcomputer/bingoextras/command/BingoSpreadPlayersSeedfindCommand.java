package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.rasmusantons.cubiomes.*;
import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.ActiveGoal;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.CubiomesUtils;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.fabricmc.loader.impl.util.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
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
                        .then(argument("mapSize", integer(0, 10000))
                            .then(argument("respectTeams", bool())
                                .then(argument("targets", entities())
                                    .then(argument("sameBiome", bool())
                                        .then(argument("excludedBiomes", resourceOrTag(buildContext, Registries.BIOME))
                                            .executes(ctx -> bingoSpreadPlayers(
                                                ctx.getSource(),
                                                getDouble(ctx, "distance"),
                                                getDouble(ctx, "spread"),
                                                getInteger(ctx, "mapSize"),
                                                getBool(ctx, "respectTeams"),
                                                getEntities(ctx, "targets"),
                                                getBool(ctx, "sameBiome"),
                                                getResourceOrTag(ctx, "excludedBiomes", Registries.BIOME)
                                            ))))))))));
    }

    private static int bingoSpreadPlayers(
            CommandSourceStack source,
            double distance,
            double spread,
            int mapSize,
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
        if (((BingoGameExt) activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            FantasyUtil.destroyGameSpecificLevels(activeGame);
        }
        ((BingoGameExt) activeGame).bingo_extras$getExtraMessages().clear();

        RandomSource rand = RandomSource.create();
        Util.shuffle(groups, rand);
        Vec2[] groupSpawns = getGroupSpawnPositions(groups.size(), distance);
        long seed = findSeed(activeGame, mapSize, groupSpawns, sameBiome, excludedBiomes, rand, source.registryAccess());

        ((BingoGameExt) activeGame).bingo_extras$setGameSpecificWorldSeed(seed);
        teleportPlayers(source, activeGame, groupSpawns, groups, spread, rand);

        for (Component extraMessage : ((BingoGameExt) activeGame).bingo_extras$getExtraMessages()) {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(extraMessage);
            }
        }

        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingospreadplayers.success"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> getRequiredBiomes(RegistryAccess access, BingoGame game) {
        Map<ResourceKey<DimensionType>, ResourceLocation> tags = Map.of(
                BuiltinDimensionTypes.OVERWORLD, ResourceLocation.withDefaultNamespace("is_overworld"),
                BuiltinDimensionTypes.NETHER, ResourceLocation.withDefaultNamespace("is_nether"),
                BuiltinDimensionTypes.END, ResourceLocation.withDefaultNamespace("is_end")
        );
        Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> requiredBiomes = Map.of(
                BuiltinDimensionTypes.OVERWORLD, new HashSet<>(),
                BuiltinDimensionTypes.NETHER, new HashSet<>(),
                BuiltinDimensionTypes.END, new HashSet<>()
        );
        for (ActiveGoal goal : game.getBoard().getGoals()) {
            for (ResourceLocation tagId : goal.goal().goal().getTagIds()) {
                Registry<Biome> registry = access.registry(Registries.BIOME).orElseThrow();
                Optional<Holder.Reference<Biome>> biome = registry.getHolder(ResourceLocation.withDefaultNamespace(tagId.getPath()));
                if (biome.isPresent()) {
                    for (Map.Entry<ResourceKey<DimensionType>, ResourceLocation> tag : tags.entrySet()) {
                        if (biome.get().tags().anyMatch(t -> t.location().equals(tag.getValue()))) {
                            requiredBiomes.get(tag.getKey()).add(biome.get());
                        }
                    }
                }
            }
        }
        return requiredBiomes;
    }

    private static long findSeed(
            BingoGame game,
            int mapSize,
            Vec2[] spawnPositions,
            boolean sameBiome,
            Predicate<Holder<Biome>> excludedBiomes,
            RandomSource rand,
            RegistryAccess access
    ) throws CommandSyntaxException {
        Cubiomes cubiomes = new Cubiomes(MCVersion.MC_1_21);
        long seed = 0;
        int attempts = 1000;
        List<Component> extraMessages = new ArrayList<>();
        iterSeeds: while (seed == 0 && attempts > 0) {
            --attempts;
            seed = rand.nextLong();
            cubiomes.applySeed(Dimension.DIM_OVERWORLD, seed);
            BiomeID chosenBiome = null;
            for (Vec2 spawnPosition : spawnPositions) {
                extraMessages.clear();
                BiomeID biomeID = cubiomes.getBiomeAt(1, (int) spawnPosition.x, 63, (int) spawnPosition.y);
                // check if all players spawn in the same biome
                if (sameBiome) {
                    if (chosenBiome == null) {
                        chosenBiome = biomeID;
                    } else if (chosenBiome != biomeID) {
                        // System.out.printf("skipping seed %d because players would spawn in different biomes\n", seed);
                        seed = 0;
                        continue iterSeeds;
                    }
                }
                // check if no players spawn in an excluded biome
                Holder<Biome> biome = CubiomesUtils.biomeIDToBiome(access, biomeID);
                if (excludedBiomes.test(biome)) {
                    // System.out.printf("skipping seed %d because it has %s at %f %f\n", seed, biomeID.name(), spawnPosition.x, spawnPosition.y);
                    seed = 0;
                    continue iterSeeds;
                }
                // check if required biomes for all goals exist
                if (mapSize > 0) {
                    Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> requiredBiomes = getRequiredBiomes(access, game);
                    Map<ResourceKey<DimensionType>, Dimension> dimensions = Map.of(
                            BuiltinDimensionTypes.OVERWORLD, Dimension.DIM_OVERWORLD,
                            BuiltinDimensionTypes.NETHER, Dimension.DIM_NETHER
                    );
                    int rChunks = mapSize / 16;
                    for (Map.Entry<ResourceKey<DimensionType>, Dimension> entry : dimensions.entrySet()) {
                        Set<Holder<Biome>> biomesForDimension = requiredBiomes.get(entry.getKey());
                        if (!biomesForDimension.isEmpty()) {
                            Range r = new Range(16, -rChunks, -rChunks, 2 * rChunks, rChunks, 60);
                            BiomeID[] requiredBiomeIDs = biomesForDimension.stream().map(CubiomesUtils::biomeToBiomeID).toArray(BiomeID[]::new);
                            BiomeFilter filter = BiomeFilter.Builder.with().allOf(requiredBiomeIDs).build();
                            boolean hasBiomes = cubiomes.checkForBiomes(r, entry.getValue(), seed, filter);
                            if (!hasBiomes) {
                                // System.out.printf("skipping seed %d because it is missing biomes in %s\n", seed, entry.getKey().location());
                                seed = 0;
                                continue iterSeeds;
                            }
                        }
                    }

                    for (ResourceKey<DimensionType> dimension : dimensions.keySet()) {
                        Set<Holder<Biome>> biomesForDimension = requiredBiomes.get(dimension);
                        if (biomesForDimension.isEmpty())
                            continue;
                        var component = BingoExtras.translatable(
                                "bingo_extras.bingospreadplayers.seedfind_biomes",
                                mapSize,
                                StringUtil.capitalize(dimension.location().toShortLanguageKey().replace("the_", ""))
                        ).withStyle(ChatFormatting.GOLD);
                        for (Holder<Biome> someBiome : biomesForDimension) {
                            component.append(BingoExtras.translatable(
                                    "bingo_extras.bingospreadplayers.seedfind_biome",
                                    StringUtil.capitalize(someBiome.unwrapKey().orElseThrow().location().toShortLanguageKey().replace("_", " "))
                            ).withStyle(ChatFormatting.GOLD));
                        }
                        extraMessages.add(component);
                    }
                }

            }
        }
        if (seed == 0)
            throw FAILED_TO_FIND_SEED_EXCEPTION.create();
        ((BingoGameExt) game).bingo_extras$getExtraMessages().addAll(extraMessages);
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

    private static void teleportPlayers(CommandSourceStack source, BingoGame activeGame, Vec2[] groupSpawns, List<List<Entity>> groups, double spread, RandomSource rand) {
        MinecraftServer server = source.getServer();

        ServerLevel gameLevel = BingoGameExt.getGameSpecificLevel(server, activeGame, server.overworld().dimension());

        for (int i = 0; i < groups.size(); ++i) {
            List<Entity> entities = groups.get(i);
            for (Entity entity : entities) {
                double effectiveX = groupSpawns[i].x - spread / 2 + rand.nextInt((int) spread);
                double effectiveY = groupSpawns[i].y - spread / 2 + rand.nextInt((int) spread);
                var adjustedSpawn = adjustToSafeLocation(gameLevel, new Vector2d(effectiveX, effectiveY));
                entity.teleportTo(gameLevel, adjustedSpawn.x, adjustedSpawn.y, adjustedSpawn.z, Set.of(), entity.getYRot(), entity.getXRot());
            }
        }
    }
}
