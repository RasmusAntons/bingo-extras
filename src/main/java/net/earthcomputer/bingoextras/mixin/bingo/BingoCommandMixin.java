package net.earthcomputer.bingoextras.mixin.bingo;

import com.mojang.brigadier.context.CommandContext;
import io.github.gaming32.bingo.BingoCommand;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BingoCommand.class, remap = false)
public class BingoCommandMixin {
    @Inject(method = "resetGame", at = @At("TAIL"))
    private static void resetGame(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (BingoExtras.seedfindGame != null) {
            FantasyUtil.destroyGameSpecificLevels(BingoExtras.seedfindGame);
            BingoExtras.seedfindGame = null;
        }
    }
}
