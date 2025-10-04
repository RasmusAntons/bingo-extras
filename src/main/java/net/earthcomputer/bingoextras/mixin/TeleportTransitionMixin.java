package net.earthcomputer.bingoextras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.earthcomputer.bingoextras.ext.PlayerTeamExt;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeleportTransition.class)
public class TeleportTransitionMixin {
    @ModifyExpressionValue(method = {
        "createDefault",
        "missingRespawnBlock(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;findRespawnDimension()Lnet/minecraft/server/level/ServerLevel;"))
    private static ServerLevel modifyDimension(ServerLevel original, ServerPlayer player) {
        PlayerTeam team = player.getTeam();
        if (team != null) {
            GlobalPos teamSpawnPos = PlayerTeamExt.getTeamSpawnPos(team);
            if (teamSpawnPos != null) {
                ServerLevel newLevel = original.getServer().getLevel(teamSpawnPos.dimension());
                if (newLevel != null) {
                    return newLevel;
                }
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = {
        "createDefault",
        "missingRespawnBlock(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRespawnData()Lnet/minecraft/world/level/storage/LevelData$RespawnData;"))
    private static LevelData.RespawnData modifyRespawnData(LevelData.RespawnData original, ServerPlayer player) {
        PlayerTeam team = player.getTeam();
        if (team != null) {
            GlobalPos teamSpawnPos = PlayerTeamExt.getTeamSpawnPos(team);
            if (teamSpawnPos != null) {
                return new LevelData.RespawnData(teamSpawnPos, original.yaw(), original.pitch());
            }
        }

        return original;
    }
}
