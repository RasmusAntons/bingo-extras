package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.xpple.cubiomes.BiomeFilter;
import dev.xpple.cubiomes.Cubiomes;
import dev.xpple.cubiomes.Generator;
import dev.xpple.cubiomes.Range;
import io.github.gaming32.bingo.data.BingoTag;
import io.github.gaming32.bingo.data.goal.GoalHolder;
import io.github.gaming32.bingo.data.goal.GoalManager;
import io.github.gaming32.bingo.ext.MinecraftServerExt;
import io.github.gaming32.bingo.game.ActiveGoal;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.CubiomesUtils;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.fabricmc.loader.impl.util.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2d;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.earthcomputer.bingoextras.CubiomesUtils.CUBIOMES_MC_VERSION;
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
        Vec2[] groupSpawns = getGroupSpawnPositions(groups.size(), distance);
        long seed = findSeed(activeGame, mapSize, groupSpawns, sameBiome, dimension.dimension(), excludedBiomes, rand, source.registryAccess());

        ((BingoGameExt) activeGame).bingo_extras$setGameSpecificWorldSeed(seed);
        teleportPlayers(source, activeGame, groupSpawns, groups, dimension.dimension(), spread, rand);

        for (Component extraMessage : ((BingoGameExt) activeGame).bingo_extras$getExtraMessages()) {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(extraMessage);
            }
        }

        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingospreadplayers.success"), true);

        return Command.SINGLE_SUCCESS;
    }

    private static Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> getRequiredBiomes(RegistryAccess access, BingoGame game) {
        Map<ResourceKey<DimensionType>, Identifier> dimensionTags = Map.of(
                BuiltinDimensionTypes.OVERWORLD, Identifier.withDefaultNamespace("is_overworld"),
                BuiltinDimensionTypes.NETHER, Identifier.withDefaultNamespace("is_nether"),
                BuiltinDimensionTypes.END, Identifier.withDefaultNamespace("is_end")
        );
        Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> requiredBiomes = Map.of(
                BuiltinDimensionTypes.OVERWORLD, new HashSet<>(),
                BuiltinDimensionTypes.NETHER, new HashSet<>(),
                BuiltinDimensionTypes.END, new HashSet<>()
        );
        Registry<Biome> biomeRegistry = access.lookup(Registries.BIOME).orElseThrow();
        for (ActiveGoal activeGoal : game.getBoard().getGoals()) {
            GoalHolder goal = GoalManager.getGoal(activeGoal.id());
            for (Holder<BingoTag> goalTag : goal.goal().getTags()) {
                Optional<Holder.Reference<Biome>> biome = biomeRegistry.get(Identifier.withDefaultNamespace(goalTag.unwrapKey().orElseThrow().identifier().getPath()));
                if (biome.isPresent()) {
                    for (Map.Entry<ResourceKey<DimensionType>, Identifier> dimensionTag : dimensionTags.entrySet()) {
                        if (biome.get().tags().anyMatch(t -> t.location().equals(dimensionTag.getValue()))) {
                            requiredBiomes.get(dimensionTag.getKey()).add(biome.get());
                        }
                    }
                }
            }
        }
        return requiredBiomes;
    }

    private static int cubiomesDimension(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return Cubiomes.DIM_NETHER();
        } else if (dimension == Level.END) {
            return Cubiomes.DIM_END();
        }
        return Cubiomes.DIM_OVERWORLD();
    }

    private static long findSeed(
            BingoGame game,
            int mapSize,
            Vec2[] spawnPositions,
            boolean sameBiome,
            ResourceKey<Level> startDimension,
            Predicate<Holder<Biome>> excludedBiomes,
            RandomSource rand,
            RegistryAccess access
    ) throws CommandSyntaxException {
        long seed = 0;
        int attempts = 1000;
        List<Component> extraMessages = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment generator = Generator.allocate(arena);
            Cubiomes.setupGenerator(generator, Cubiomes.MC_1_21_11(), 0);
            int rChunks = mapSize / 16;
            MemorySegment range = Range.allocate(arena);
            Range.scale(range, 16);
            Range.sx(range, 2 * rChunks);
            Range.sz(range, 2 * rChunks);
            Range.sy(range, 1);
            Range.x(range, -rChunks);
            Range.z(range, -rChunks);
            Range.y(range, 63);
            MemorySegment filter = BiomeFilter.allocate(arena);
            final Map<ResourceKey<DimensionType>, Set<Holder<Biome>>> requiredBiomes = getRequiredBiomes(access, game);
            final Map<ResourceKey<DimensionType>, Integer> dimensions = Map.of(
                    BuiltinDimensionTypes.OVERWORLD, Cubiomes.DIM_OVERWORLD(),
                    BuiltinDimensionTypes.NETHER, Cubiomes.DIM_NETHER()
            );
            iterSeeds:
            while (seed == 0 && attempts > 0) {
                --attempts;
                seed = rand.nextLong();
                System.out.printf("trying seed %dl\n", seed);
                Cubiomes.applySeed(generator, cubiomesDimension(startDimension), seed);
                int chosenBiome = Cubiomes.none();
                for (Vec2 spawnPosition : spawnPositions) {
                    extraMessages.clear();
                    int biomeID = Cubiomes.getBiomeAt(generator, 4, (int) spawnPosition.x, 63, (int) spawnPosition.y);
                    // check if all players spawn in the same biome
                    if (sameBiome) {
                        if (chosenBiome == Cubiomes.none()) {
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
                        // System.out.printf("skipping seed %d because it has %s at %f %f\n", seed, Cubiomes.biome2str(CUBIOMES_MC_VERSION, biomeID).getString(0), spawnPosition.x, spawnPosition.y);
                        seed = 0;
                        continue iterSeeds;
                    }
                    // check if required biomes for all goals exist
                    if (mapSize > 0) {
                        for (Map.Entry<ResourceKey<DimensionType>, Integer> entry : dimensions.entrySet()) {
                            Set<Holder<Biome>> biomesForDimension = requiredBiomes.get(entry.getKey());
                            if (!biomesForDimension.isEmpty()) {
                                MemorySegment requiredBiomeIDs = arena.allocate(Cubiomes.C_INT, biomesForDimension.size());
                                long i = 0;
                                for (var b: biomesForDimension) {
                                    requiredBiomeIDs.set(Cubiomes.C_INT, i++, CubiomesUtils.biomeToBiomeID(b));
                                }
                                Cubiomes.setupBiomeFilter(filter, CUBIOMES_MC_VERSION, 0, requiredBiomeIDs, biomesForDimension.size(), MemorySegment.NULL, 0, MemorySegment.NULL, 0);
                                int hasBiome = Cubiomes.checkForBiomes(generator, MemorySegment.NULL, range, entry.getValue(), seed, filter, MemorySegment.NULL);
                                if (hasBiome == 0) {
                                    // System.out.printf("skipping seed %d because it is missing biomes in %s\n", seed, entry.getKey().identifier());
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
                                    StringUtil.capitalize(dimension.identifier().toShortLanguageKey().replace("the_", ""))
                            ).withStyle(ChatFormatting.GOLD);
                            for (Holder<Biome> someBiome : biomesForDimension) {
                                component.append(BingoExtras.translatable(
                                        "bingo_extras.bingospreadplayers.seedfind_biome",
                                        StringUtil.capitalize(someBiome.unwrapKey().orElseThrow().identifier().toShortLanguageKey().replace("_", " "))
                                ).withStyle(ChatFormatting.GOLD));
                            }
                            extraMessages.add(component);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
