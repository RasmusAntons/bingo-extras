package net.earthcomputer.bingoextras.mixin.bingo;

import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;
import xyz.nucleoid.fantasy.util.GameRuleStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = BingoGame.class, remap = false)
public class BingoGameMixin implements BingoGameExt {
    @Unique
    private final Map<ResourceKey<Level>, RuntimeWorldHandle> gameSpecificLevels = new HashMap<>();

    @Unique
    private long gameSpecificWorldSeed = 0;

    @Unique
    private final List<Component> extraMessages = new ArrayList<>();

    @Unique
    private final Map<GameRules.Key<?>, CommandContext<CommandSourceStack>> gameRules = new HashMap<>();

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
    public Map<ResourceKey<Level>, RuntimeWorldHandle> bingoExtras$getGameSpecificLevels() {
        return gameSpecificLevels;
    }

    @Override
    public Map<GameRules.Key<?>, CommandContext<CommandSourceStack>> bingoExtras$getGameRules() {
        return gameRules;
    }
}
