package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.FreezePeriod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Collection;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

public final class FreezePlayersCommand {
    private FreezePlayersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("freezeplayers")
                .requires(source -> source.hasPermission(2))
                .then(argument("targets", players())
                        .then(argument("time", doubleArg(0))
                                .executes(ctx -> freezePlayers(ctx.getSource(), getPlayers(ctx, "targets"), getDouble(ctx, "time")))
                        )));
    }

    private static int freezePlayers(CommandSourceStack source, Collection<ServerPlayer> players, double time) {
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.ADVENTURE);
            FreezePeriod.INSTANCE.setFreezePeriod(player, time);
        }
        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.freeze.success", time), true);
        return Command.SINGLE_SUCCESS;
    }
}
