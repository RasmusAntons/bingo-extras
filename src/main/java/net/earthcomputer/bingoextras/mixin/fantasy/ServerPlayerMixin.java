package net.earthcomputer.bingoextras.mixin.fantasy;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import io.github.gaming32.bingo.ext.MinecraftServerExt;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    @Shadow
    public abstract Level level();

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @ModifyVariable(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), argsOnly = true, name = "transition")
    private TeleportTransition modifyDestDimension(TeleportTransition dest) {
        ServerLevel overrideLevel = null;
        PlayerTeam currentLevelTeam = ServerLevelExt_Fantasy.getTeam((ServerLevel) level());
        PlayerTeam destLevelTeam = ServerLevelExt_Fantasy.getTeam(dest.newLevel());
        if (!FantasyUtil.isForcedDimensionChange() && currentLevelTeam != null && destLevelTeam == null) {
            overrideLevel = PlayerTeamExt_Fantasy.getTeamSpecificLevel(level().getServer(), currentLevelTeam, dest.newLevel().dimension());
        }
        final BingoGame activeGame = ((MinecraftServerExt) level().getServer()).bingo$getGame();
        if (activeGame != null && ((BingoGameExt) activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            ResourceKey<Level> dimension = dest.newLevel().dimension();
            var parentLevel = ((ServerLevelExt_Fantasy) dest.newLevel()).bingoExtras$getParentLevel();
            if (parentLevel != null) {
                dimension = parentLevel.dimension();
            }
            overrideLevel = BingoGameExt.getGameSpecificLevel(dest.newLevel().getServer(), activeGame, dimension);
        }
        if (overrideLevel != null) {
            dest = new TeleportTransition(
                overrideLevel,
                dest.position(),
                dest.deltaMovement(),
                dest.yRot(),
                dest.xRot(),
                dest.missingRespawnBlock(),
                dest.asPassenger(),
                dest.relatives(),
                dest.postTeleportTransition()
            );
        }
        return dest;
    }

    @ModifyVariable(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At("HEAD"), argsOnly = true, name = "level")
    private ServerLevel modifyDestDimension(ServerLevel dest) {
        PlayerTeam currentLevelTeam = ServerLevelExt_Fantasy.getTeam((ServerLevel) level());
        PlayerTeam destLevelTeam = ServerLevelExt_Fantasy.getTeam(dest);
        if (!FantasyUtil.isForcedDimensionChange() && currentLevelTeam != null && destLevelTeam == null) {
            dest = PlayerTeamExt_Fantasy.getTeamSpecificLevel(level().getServer(), currentLevelTeam, dest.dimension());
        }
        final BingoGame activeGame = ((MinecraftServerExt) level().getServer()).bingo$getGame();
        if (activeGame != null && ((BingoGameExt) activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            ResourceKey<Level> dimension = dest.dimension();
            var parentLevel = ((ServerLevelExt_Fantasy) dest).bingoExtras$getParentLevel();
            if (parentLevel != null) {
                dimension = parentLevel.dimension();
            }
            dest = BingoGameExt.getGameSpecificLevel(dest.getServer(), activeGame, dimension);
        }
        return dest;
    }

    @WrapOperation(method = "setCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"))
    private boolean forceDimensionChange(ServerPlayer instance, ServerLevel level, double x, double y, double z, Set<Relative> relatives, float newYRot, float newXRot, boolean resetCamera, Operation<Boolean> original) {
        return FantasyUtil.forceDimensionChange(() -> original.call(instance, level, x, y, z, relatives, newYRot, newXRot, resetCamera));
    }
}
