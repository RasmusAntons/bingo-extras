package net.earthcomputer.bingoextras;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import dev.xpple.cubiomes.Cubiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

public class CubiomesUtils {
    public static int CUBIOMES_MC_VERSION = Cubiomes.MC_1_21_11();

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
}
