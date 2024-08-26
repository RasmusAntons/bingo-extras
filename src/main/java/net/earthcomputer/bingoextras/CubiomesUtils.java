package net.earthcomputer.bingoextras;

import de.rasmusantons.cubiomes.BiomeID;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CubiomesUtils {
    public static Holder<Biome> biomeIDToBiome(RegistryAccess access, BiomeID biomeID) throws NoSuchElementException {
        Registry<Biome> registry = access.registry(Registries.BIOME).orElseThrow();
        Optional<Holder.Reference<Biome>> biome = registry.getHolder(ResourceLocation.withDefaultNamespace(biomeID.name()));
        if (biome.isPresent()) {
            return biome.get();
        } else {
            for (String altName : biomeID.getAltNames()) {
                if (!ResourceLocation.isValidPath(altName))
                    continue;
                biome = registry.getHolder(ResourceLocation.withDefaultNamespace(altName));
                if (biome.isPresent())
                    return biome.get();
            }
        }
        throw new NoSuchElementException("No value present");
    }

    public static BiomeID biomeToBiomeID(Holder<Biome> biome) {
        String path = biome.unwrapKey().orElseThrow().location().getPath();
        try {
            return BiomeID.valueOf(path);
        } catch (IllegalArgumentException e) {
            for (BiomeID biomeID : BiomeID.values()) {
                if (Arrays.asList(biomeID.getAltNames()).contains(path))
                    return biomeID;
            }
        }
        throw new NoSuchElementException("No value present");
    }
}
