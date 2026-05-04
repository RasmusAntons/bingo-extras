package net.earthcomputer.bingoextras.mixin.bingo;

import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(targets = "io.github.gaming32.bingo.game.BingoGame", remap = false)
@Pseudo
public class BingoGameMixin implements BingoGameExt {
    @Unique
    private long seed;

    @Unique
    private final Map<ResourceKey<Level>, RuntimeLevelHandle> gameSpecificLevels = new HashMap<>();

    @Unique
    private long gameSpecificWorldSeed = 0;

    @Unique
    private final List<Component> extraMessages = new ArrayList<>();

    @Override
    public long bingoExtras$getSeed() {
        return seed;
    }

    @Override
    public void bingoExtras$setSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public long bingo_extras$getGameSpecificWorldSeed() {
        return gameSpecificWorldSeed;
    }

    @Override
    public void bingo_extras$setGameSpecificWorldSeed(long seed) {
        gameSpecificWorldSeed = seed;
    }

    @Override
    public List<Component> bingo_extras$getExtraMessages() {
        return extraMessages;
    }

    @Override
    public Map<ResourceKey<Level>, RuntimeLevelHandle> bingoExtras$getGameSpecificLevels() {
        return gameSpecificLevels;
    }
}
