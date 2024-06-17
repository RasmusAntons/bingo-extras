package net.earthcomputer.bingoextras.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.gaming32.bingo.Bingo;
import net.earthcomputer.bingoextras.Configs;
import net.earthcomputer.bingoextras.FantasyLobby;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @ModifyExpressionValue(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel respawnTeamInDimension(ServerLevel original, @Local(argsOnly = true) ServerPlayer player) {
        if (Configs.createFantasyLobby && Bingo.activeGame == null) {
            // I don't know how to set the default login position, so I move the player here
            player.setPosRaw(0, 4, 0);
            FantasyLobby lobby = FantasyLobby.INSTANCE;
            return lobby.getWorld();
        }

        return original;
    }
}
