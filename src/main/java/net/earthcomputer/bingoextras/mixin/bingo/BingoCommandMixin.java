package net.earthcomputer.bingoextras.mixin.bingo;

import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.bingo.BingoCommand;
import net.earthcomputer.bingoextras.Configs;
import net.earthcomputer.bingoextras.FantasyLobby;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BingoCommand.class, remap = false)
public class BingoCommandMixin {
    @Inject(method = "startGame", at = @At("TAIL"))
    private static void onStartGame(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (Configs.createFantasyLobby) {
            FantasyLobby lobby = FantasyLobby.INSTANCE;
            lobby.onGameStart(context.getSource().getServer());
        }
    }
}
