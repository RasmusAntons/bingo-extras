package net.earthcomputer.bingoextras.mixin.bingo;

import io.github.gaming32.bingo.Bingo;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

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

    @Inject(method = "endGame", at = @At(value = "RETURN"))
    private void endGame(CallbackInfo ci) {
        if (gameSpecificWorldSeed != 0) {
            FantasyUtil.destroyGameSpecificLevels((BingoGame) (Object) this);
        }
    }
}
