package net.earthcomputer.bingoextras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerSpawnFinder.class)
public class PlayerSpawnFinderMixin {
    @ModifyExpressionValue(method = "findSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/WorldData;getGameType()Lnet/minecraft/world/level/GameType;"))
    private static GameType overwriteDefaultGameType(GameType defaultGameType, @Local(argsOnly = true, name = "level") ServerLevel level) {
        ServerLevelExt_Fantasy levelExt = (ServerLevelExt_Fantasy) level;
        if (levelExt.bingoExtras$getOriginalLevel() != null || levelExt.bingoExtras$getParentLevel() != null) {
            return GameType.SURVIVAL;
        }
        return defaultGameType;
    }
}
