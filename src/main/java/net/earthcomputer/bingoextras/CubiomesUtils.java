package net.earthcomputer.bingoextras;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.xpple.cubiomes.BiomeFilter;
import dev.xpple.cubiomes.Cubiomes;
import dev.xpple.cubiomes.Generator;
import dev.xpple.cubiomes.Range;
import io.github.gaming32.bingo.data.BingoTag;
import io.github.gaming32.bingo.data.goal.GoalHolder;
import io.github.gaming32.bingo.data.goal.GoalManager;
import io.github.gaming32.bingo.game.ActiveGoal;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.command.BingoSpreadPlayersSeedfindCommand;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.fabricmc.loader.impl.util.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CubiomesUtils {
    public static int CUBIOMES_MC_VERSION = Cubiomes.MC_1_21_11();
    private static int CHUNK_SIZE = 16;

    //<editor-fold defaultstate="collapsed" desc="static final Map<String, Integer> BIOMES;">
    static final BiMap<String, Integer> BIOMES = ImmutableBiMap.<String, Integer>builder()
            .put("ocean", Cubiomes.ocean())
            .put("plains", Cubiomes.plains())
            .put("desert", Cubiomes.desert())
            .put("forest", Cubiomes.forest())
            .put("taiga", Cubiomes.taiga())
            .put("swamp", Cubiomes.swamp())
            .put("river", Cubiomes.river())
            .put("nether_wastes", Cubiomes.nether_wastes())
            .put("the_end", Cubiomes.the_end())
            .put("frozen_ocean", Cubiomes.frozen_ocean())
            .put("frozen_river", Cubiomes.frozen_river())
            .put("snowy_mountains", Cubiomes.snowy_mountains())
            .put("mushroom_fields", Cubiomes.mushroom_fields())
            .put("mushroom_field_shore", Cubiomes.mushroom_field_shore())
            .put("beach", Cubiomes.beach())
            .put("desert_hills", Cubiomes.desert_hills())
            .put("wooded_hills", Cubiomes.wooded_hills())
            .put("taiga_hills", Cubiomes.taiga_hills())
            .put("mountain_edge", Cubiomes.mountain_edge())
            .put("jungle", Cubiomes.jungle())
            .put("jungle_hills", Cubiomes.jungle_hills())
            .put("deep_ocean", Cubiomes.deep_ocean())
            .put("snowy_beach", Cubiomes.snowy_beach())
            .put("birch_forest", Cubiomes.birch_forest())
            .put("birch_forest_hills", Cubiomes.birch_forest_hills())
            .put("dark_forest", Cubiomes.dark_forest())
            .put("snowy_taiga", Cubiomes.snowy_taiga())
            .put("snowy_taiga_hills", Cubiomes.snowy_taiga_hills())
            .put("savanna", Cubiomes.savanna())
            .put("savanna_plateau", Cubiomes.savanna_plateau())
            .put("badlands", Cubiomes.badlands())
            .put("badlands_plateau", Cubiomes.badlands_plateau())
            .put("small_end_islands", Cubiomes.small_end_islands())
            .put("end_midlands", Cubiomes.end_midlands())
            .put("end_highlands", Cubiomes.end_highlands())
            .put("end_barrens", Cubiomes.end_barrens())
            .put("warm_ocean", Cubiomes.warm_ocean())
            .put("lukewarm_ocean", Cubiomes.lukewarm_ocean())
            .put("cold_ocean", Cubiomes.cold_ocean())
            .put("deep_warm_ocean", Cubiomes.deep_warm_ocean())
            .put("deep_lukewarm_ocean", Cubiomes.deep_lukewarm_ocean())
            .put("deep_cold_ocean", Cubiomes.deep_cold_ocean())
            .put("deep_frozen_ocean", Cubiomes.deep_frozen_ocean())
            .put("seasonal_forest", Cubiomes.seasonal_forest())
            .put("rainforest", Cubiomes.rainforest())
            .put("shrubland", Cubiomes.shrubland())
            .put("the_void", Cubiomes.the_void())
            .put("sunflower_plains", Cubiomes.sunflower_plains())
            .put("desert_lakes", Cubiomes.desert_lakes())
            .put("flower_forest", Cubiomes.flower_forest())
            .put("taiga_mountains", Cubiomes.taiga_mountains())
            .put("swamp_hills", Cubiomes.swamp_hills())
            .put("ice_spikes", Cubiomes.ice_spikes())
            .put("tall_birch_hills", Cubiomes.tall_birch_hills())
            .put("dark_forest_hills", Cubiomes.dark_forest_hills())
            .put("snowy_taiga_mountains", Cubiomes.snowy_taiga_mountains())
            .put("eroded_badlands", Cubiomes.eroded_badlands())
            .put("bamboo_jungle", Cubiomes.bamboo_jungle())
            .put("bamboo_jungle_hills", Cubiomes.bamboo_jungle_hills())
            .put("soul_sand_valley", Cubiomes.soul_sand_valley())
            .put("crimson_forest", Cubiomes.crimson_forest())
            .put("warped_forest", Cubiomes.warped_forest())
            .put("basalt_deltas", Cubiomes.basalt_deltas())
            .put("dripstone_caves", Cubiomes.dripstone_caves())
            .put("lush_caves", Cubiomes.lush_caves())
            .put("meadow", Cubiomes.meadow())
            .put("grove", Cubiomes.grove())
            .put("snowy_slopes", Cubiomes.snowy_slopes())
            .put("jagged_peaks", Cubiomes.jagged_peaks())
            .put("frozen_peaks", Cubiomes.frozen_peaks())
            .put("stony_peaks", Cubiomes.stony_peaks())
            .put("old_growth_birch_forest", Cubiomes.old_growth_birch_forest())
            .put("old_growth_pine_taiga", Cubiomes.old_growth_pine_taiga())
            .put("old_growth_spruce_taiga", Cubiomes.old_growth_spruce_taiga())
            .put("snowy_plains", Cubiomes.snowy_plains())
            .put("sparse_jungle", Cubiomes.sparse_jungle())
            .put("stony_shore", Cubiomes.stony_shore())
            .put("windswept_hills", Cubiomes.windswept_hills())
            .put("windswept_forest", Cubiomes.windswept_forest())
            .put("windswept_gravelly_hills", Cubiomes.windswept_gravelly_hills())
            .put("windswept_savanna", Cubiomes.windswept_savanna())
            .put("wooded_badlands", Cubiomes.wooded_badlands())
            .put("deep_dark", Cubiomes.deep_dark())
            .put("mangrove_swamp", Cubiomes.mangrove_swamp())
            .put("cherry_grove", Cubiomes.cherry_grove())
            .put("pale_garden", Cubiomes.pale_garden())
            .build();
    //</editor-fold>

    public static Holder<Biome> biomeIDToBiome(RegistryAccess access, int biomeID) {
        Registry<Biome> registry = access.lookup(Registries.BIOME).orElseThrow();
        Identifier identifier = Identifier.parse(BIOMES.inverse().get(biomeID));
        return registry.get(identifier).orElseThrow();
    }

    public static int biomeToBiomeID(Holder<Biome> biome) {
        String path = biome.unwrapKey().orElseThrow().identifier().getPath();
        return BIOMES.getOrDefault(path, Cubiomes.none());
    }

    public static int cubiomesDimension(ResourceKey<Level> dimension) {
        if (dimension == Level.NETHER) {
            return Cubiomes.DIM_NETHER();
        } else if (dimension == Level.END) {
            return Cubiomes.DIM_END();
        }
        return Cubiomes.DIM_OVERWORLD();
    }

    private record RequiredBiomesForDimension(Set<Holder<Biome>> requiredBiomes, Map<TagKey<Biome>, List<Holder<Biome>>> requiredBiomesTags) {
        public static RequiredBiomesForDimension empty() {
            return new RequiredBiomesForDimension(new HashSet<>(), new HashMap<>());
        }

        boolean isEmpty() {
            return requiredBiomes.isEmpty() && requiredBiomesTags().isEmpty();
        }
    }

    private static Map<ResourceKey<DimensionType>, RequiredBiomesForDimension> getRequiredBiomes(RegistryAccess access, BingoGame game) {
        Map<ResourceKey<DimensionType>, Identifier> dimensionTags = Map.of(
                BuiltinDimensionTypes.OVERWORLD, Identifier.withDefaultNamespace("is_overworld"),
                BuiltinDimensionTypes.NETHER, Identifier.withDefaultNamespace("is_nether"),
                BuiltinDimensionTypes.END, Identifier.withDefaultNamespace("is_end")
        );
        Map<ResourceKey<DimensionType>, RequiredBiomesForDimension> requiredBiomes = Map.of(
                BuiltinDimensionTypes.OVERWORLD, RequiredBiomesForDimension.empty(),
                BuiltinDimensionTypes.NETHER, RequiredBiomesForDimension.empty(),
                BuiltinDimensionTypes.END, RequiredBiomesForDimension.empty()
        );
        Registry<Biome> biomeRegistry = access.lookup(Registries.BIOME).orElseThrow();
        for (ActiveGoal activeGoal : game.getBoard().getGoals()) {
            GoalHolder goal = GoalManager.getGoal(activeGoal.id());
            for (Holder<BingoTag> goalTag : goal.goal().getTags()) {
                String tagName = goalTag.unwrapKey().orElseThrow().identifier().getPath();
                if (tagName.startsWith("seedfind_biome_")) {
                    Optional<Holder.Reference<Biome>> biome = biomeRegistry.get(Identifier.withDefaultNamespace(tagName.substring("seedfind_biome_".length())));
                    if (biome.isPresent()) {
                        for (Map.Entry<ResourceKey<DimensionType>, Identifier> dimensionTag : dimensionTags.entrySet()) {
                            if (biome.get().tags().anyMatch(t -> t.location().equals(dimensionTag.getValue()))) {
                                requiredBiomes.get(dimensionTag.getKey()).requiredBiomes().add(biome.get());
                            }
                        }
                    }
                } else if (tagName.startsWith("seedfind_biometag_")) {
                    var optionalTagKey = biomeRegistry.listTagIds().filter(t -> t.location().getPath().equals(tagName.substring("seedfind_biometag_".length()))).findAny();
                    if (optionalTagKey.isPresent()) {
                        List<Holder<Biome>> biomes = StreamSupport.stream(biomeRegistry.getTagOrEmpty(optionalTagKey.get()).spliterator(), false).toList();
                        if (!biomes.isEmpty()) {
                            for (Map.Entry<ResourceKey<DimensionType>, Identifier> dimensionTag : dimensionTags.entrySet()) {
                                if (biomes.getFirst().tags().anyMatch(t -> t.location().equals(dimensionTag.getValue()))) {
                                    requiredBiomes.get(dimensionTag.getKey()).requiredBiomesTags().put(optionalTagKey.get(), biomes);
                                }
                            }
                        }
                    }
                }
            }
        }
        return requiredBiomes;
    }

    public record SeedfinderResult(long seed, Vec2[] spawnLocations, List<Component> extraMessages) {
    }

    public static SeedfinderResult findSeed(
            BingoGame game,
            int mapSize,
            int nGroups,
            double distance,
            boolean sameBiome,
            ResourceKey<Level> startDimension,
            Predicate<Holder<Biome>> excludedBiomes,
            RandomSource rand,
            RegistryAccess access
    ) throws SeedfindException {
        long seed = 0;
        int attempts = 1000;
        Vec2[] spawnPositions = null;
        List<Component> extraMessages = List.of();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment generator = Generator.allocate(arena);
            Cubiomes.setupGenerator(generator, CUBIOMES_MC_VERSION, 0);
            MemorySegment range = Range.allocate(arena);
            MemorySegment filter = BiomeFilter.allocate(arena);
            final Map<ResourceKey<DimensionType>, RequiredBiomesForDimension> requiredBiomes = getRequiredBiomes(access, game);
            final Map<ResourceKey<DimensionType>, Integer> dimensions = Map.of(
                    BuiltinDimensionTypes.OVERWORLD, Cubiomes.DIM_OVERWORLD(),
                    BuiltinDimensionTypes.NETHER, Cubiomes.DIM_NETHER()
            );
            iterSeeds:
            while (seed == 0 && attempts > 0) {
                --attempts;
                seed = rand.nextLong();
                // System.out.printf("trying seed %dl\n", seed);
                Cubiomes.applySeed(generator, CubiomesUtils.cubiomesDimension(startDimension), seed);
                spawnPositions = null;
                GroupSpawnPositionGenerator spawnPositionGenerator = new GroupSpawnPositionGenerator(nGroups, distance);
                iterSpawns:
                while (spawnPositions == null && spawnPositionGenerator.hasNext()) {
                    spawnPositions = spawnPositionGenerator.getNext();
                    int chosenBiome = Cubiomes.none();
                    for (Vec2 spawnPosition : spawnPositions) {
                        int biomeID = Cubiomes.getBiomeAt(generator, 4, (int) spawnPosition.x / 4, 63, (int) spawnPosition.y / 4);
                        // check if all players spawn in the same biome
                        if (sameBiome) {
                            if (chosenBiome == Cubiomes.none()) {
                                chosenBiome = biomeID;
                            } else if (chosenBiome != biomeID) {
                                // System.out.printf("skipping spawn positions %s on seed %d because players would spawn in different biomes (%s vs %s)\n", Arrays.stream(spawnPositions).map(s -> String.format("(%.1f, %.1f)", s.x, s.y)).collect(Collectors.joining(", ")), seed, Cubiomes.biome2str(CUBIOMES_MC_VERSION, chosenBiome).getString(0), Cubiomes.biome2str(CUBIOMES_MC_VERSION, biomeID).getString(0));
                                spawnPositions = null;
                                continue iterSpawns;
                            }
                        }
                        // check if no players spawn in an excluded biome
                        Holder<Biome> biome = CubiomesUtils.biomeIDToBiome(access, biomeID);
                        if (excludedBiomes.test(biome)) {
                            // System.out.printf("skipping spawn positions %s on seed %d because it has %s at %f %f\n", Arrays.stream(spawnPositions).map(s -> String.format("(%.1f, %.1f)", s.x, s.y)).collect(Collectors.joining(", ")), seed, Cubiomes.biome2str(CUBIOMES_MC_VERSION, biomeID).getString(0), spawnPosition.x, spawnPosition.y);
                            spawnPositions = null;
                            continue iterSpawns;
                        }
                    }
                }
                if (spawnPositions == null) {
                    // System.out.printf("skipping seed %d because of invalid spawn biomes\n", seed);
                    seed = 0;
                    continue iterSeeds;
                }
                // check if required biomes for all goals exist
                if (mapSize > 0) {
                    for (Map.Entry<ResourceKey<DimensionType>, Integer> entry : dimensions.entrySet()) {
                        int rChunks = mapSize / CHUNK_SIZE;
                        if (entry.getKey() ==  BuiltinDimensionTypes.NETHER) {
                            rChunks /= 8;
                        }
                        Range.scale(range, CHUNK_SIZE);
                        Range.sx(range, 2 * rChunks);
                        Range.sz(range, 2 * rChunks);
                        Range.sy(range, 1);
                        Range.x(range, -rChunks);
                        Range.z(range, -rChunks);
                        Range.y(range, 63);
                        var biomeCache = Cubiomes.allocCache(generator, range);
                        Set<Holder<Biome>> biomesForDimension = requiredBiomes.get(entry.getKey()).requiredBiomes();
                        if (!biomesForDimension.isEmpty()) {
                            MemorySegment requiredBiomeIDs = arena.allocate(Cubiomes.C_INT, biomesForDimension.size());
                            long i = 0;
                            for (var b : biomesForDimension) {
                                requiredBiomeIDs.set(Cubiomes.C_INT, i++ * Cubiomes.C_INT.byteSize(), CubiomesUtils.biomeToBiomeID(b));
                            }
                            Cubiomes.setupBiomeFilter(filter, CUBIOMES_MC_VERSION, 0, requiredBiomeIDs, biomesForDimension.size(), MemorySegment.NULL, 0, MemorySegment.NULL, 0);
                            int hasBiome = Cubiomes.checkForBiomes(generator, biomeCache, range, entry.getValue(), seed, filter, MemorySegment.NULL);
                            if (hasBiome == 0) {
                                // System.out.printf("skipping seed %d because it is missing biomes in %s\n", seed, entry.getKey().identifier());
                                seed = 0;
                                continue iterSeeds;
                            }
                        }
                        Collection<List<Holder<Biome>>> biomeTagsForDimension = requiredBiomes.get(entry.getKey()).requiredBiomesTags().values();
                        MemorySegment matchAnyBiomeIDs = arena.allocate(Cubiomes.C_INT, biomeTagsForDimension.stream().map(List::size).reduce(0, Integer::max));
                        for (List<Holder<Biome>> matchAnyBiomes : biomeTagsForDimension) {
                            long i = 0;
                            for (var b : matchAnyBiomes) {
                                matchAnyBiomeIDs.set(Cubiomes.C_INT, i++ * Cubiomes.C_INT.byteSize(), CubiomesUtils.biomeToBiomeID(b));
                            }
                            Cubiomes.setupBiomeFilter(filter, CUBIOMES_MC_VERSION, 0, MemorySegment.NULL, 0, MemorySegment.NULL, 0, matchAnyBiomeIDs, matchAnyBiomes.size());
                            int hasBiome = Cubiomes.checkForBiomes(generator, biomeCache, range, entry.getValue(), seed, filter, MemorySegment.NULL);
                            if (hasBiome == 0) {
                                // System.out.printf("skipping seed %d because it is missing biomes in %s\n", seed, entry.getKey().identifier());
                                seed = 0;
                                continue iterSeeds;
                            }
                        }
                    }

                    extraMessages = formatExtraMessages(dimensions.keySet(), requiredBiomes, mapSize);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        if (seed == 0)
            throw new SeedfindException();
        return new SeedfinderResult(seed, spawnPositions, extraMessages);
    }

    private static List<Component> formatExtraMessages(
            Collection<ResourceKey<DimensionType>> dimensions,
            Map<ResourceKey<DimensionType>, RequiredBiomesForDimension> requiredBiomes,
            int mapSize) {
        List<Component> extraMessages = new ArrayList<>(dimensions.size());
        for (ResourceKey<DimensionType> dimension : dimensions) {
            RequiredBiomesForDimension biomesForDimension = requiredBiomes.get(dimension);
            if (biomesForDimension.isEmpty())
                continue;
            MutableComponent component = BingoExtras.translatable(
                    "bingo_extras.bingospreadplayers.seedfind_biomes",
                    dimension != BuiltinDimensionTypes.NETHER ? (mapSize / CHUNK_SIZE) * CHUNK_SIZE : (mapSize / CHUNK_SIZE / 8) * CHUNK_SIZE,
                    StringUtil.capitalize(dimension.identifier().toShortLanguageKey().replace("the_", ""))
            ).withStyle(s -> s.withColor(ChatFormatting.GOLD)
                    .withHoverEvent(new HoverEvent.ShowText(BingoExtras.translatable("bingo_extras.bingospreadplayers.seedfind_biomes_tooltip")))
            );
            for (Holder<Biome> someBiome : biomesForDimension.requiredBiomes()) {
                component.append(BingoExtras.translatable(
                        "bingo_extras.bingospreadplayers.seedfind_biome",
                        StringUtil.capitalize(someBiome.unwrapKey().orElseThrow().identifier().toShortLanguageKey().replace("_", " "))
                ).withStyle(ChatFormatting.GOLD));
            }
            for (TagKey<Biome> biomeTag : biomesForDimension.requiredBiomesTags().keySet()) {
                component.append(BingoExtras.translatable(
                        "bingo_extras.bingospreadplayers.seedfind_biometag",
                        biomeTag.location().toShortLanguageKey()
                ).withStyle(ChatFormatting.GOLD));
            }
            extraMessages.add(component);
        }
        return extraMessages;
    }

    private static class GroupSpawnPositionGenerator {
        private final static int STEP_DISTANCE = 64;
        final int nGroups;
        final double distance;
        int step = 0;

        GroupSpawnPositionGenerator(int nGroups, double distance) {
            this.nGroups = nGroups;
            this.distance = distance;
        }

        private double stepCount() {
            return Math.max(1, Math.floor((2 * Math.PI * distance) / STEP_DISTANCE / nGroups));
        }

        public boolean hasNext() {
            return step < stepCount();
        }

        public Vec2[] getNext() {
            Vec2[] res = new Vec2[nGroups];
            for (int i = 0; i < nGroups; i++) {
                double x = Math.floor(distance * Math.cos(2 * Math.PI / nGroups * (i + step / stepCount()))) + 0.5;
                double z = Math.floor(distance * Math.sin(2 * Math.PI / nGroups * (i + step / stepCount()))) + 0.5;
                res[i] = new Vec2((float) x, (float) z);
            }
            ++step;
            return res;
        }
    }

    public static class SeedfindException extends RuntimeException {
    }
}
