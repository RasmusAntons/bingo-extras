package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.FreezePeriod;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;

import java.util.Collection;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

public final class FreezePlayersCommand {
    private FreezePlayersCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("freezeplayers")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(argument("targets", players())
                        .then(argument("time", TimeArgument.time())
                                .then(argument("tickFreeze", bool())
                                        .executes(ctx -> freezePlayers(
                                                ctx.getSource(),
                                                getPlayers(ctx, "targets"),
                                                getInteger(ctx, "time"),
                                                getBool(ctx, "tickFreeze")
                                        ))
                                ))
                        .then(literal("cancel")
                                .executes(ctx -> cancelFreezePlayers(ctx.getSource(), getPlayers(ctx, "targets")))
                        )
                ));
    }

    private static int freezePlayers(CommandSourceStack source, Collection<ServerPlayer> players, int time, boolean tickFreeze) {
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.ADVENTURE);
            FreezePeriod.INSTANCE.setFreezePeriod(player, time);
        }
        if (tickFreeze) {
            source.getServer().tickRateManager().setFrozen(true);
        }
        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.freeze.success", time / SharedConstants.TICKS_PER_SECOND), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int cancelFreezePlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.setGameMode(GameType.ADVENTURE);
            FreezePeriod.INSTANCE.unsetFreezePeriod(player);
        }
        source.getServer().tickRateManager().setFrozen(false);
        source.sendSuccess(() -> BingoExtras.translatable("bingo_extras.cancel_freeze.success"), true);
        return Command.SINGLE_SUCCESS;
    }
}
