package net.earthcomputer.bingoextras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.earthcomputer.bingoextras.command.BingoSpreadPlayersCommand;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;
import org.joml.Vector3d;
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

    @ModifyExpressionValue(method = "findSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/PlayerSpawnFinder;fixupSpawnHeight(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 overwriteNetherSpawn(
            Vec3 original,
            @Local(name = "level", argsOnly = true) ServerLevel level,
            @Local(name = "spawnSuggestion", argsOnly = true) BlockPos spawnSuggestion
    ) {
        ServerLevel parentLevel = ((ServerLevelExt_Fantasy) level).bingoExtras$getParentLevel();
        if (parentLevel != null && parentLevel.dimension() == ServerLevel.NETHER) {
            Vector3d safeLocation = BingoSpreadPlayersCommand.adjustToSafeLocation(level, new Vector2d(spawnSuggestion.getX(), spawnSuggestion.getY()));
            return new Vec3(safeLocation.x, safeLocation.y, safeLocation.z);
        }
        return original;
    }
}
