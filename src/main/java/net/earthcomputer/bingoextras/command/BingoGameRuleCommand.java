package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.gaming32.bingo.Bingo;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersSeedfindCommand.NO_RUNNING_GAME_EXCEPTION;
import static net.minecraft.commands.Commands.literal;

public class BingoGameRuleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> argumentBuilder = literal("bingogamerule")
                .requires(source -> source.hasPermission(2));
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                argumentBuilder.then(
                        literal(key.getId())
                                .executes(ctx -> queryRule(ctx.getSource(), key))
                                .then(type.createArgument("value").executes(ctx -> setRule(ctx, key)))
                );
            }
        });
        dispatcher.register(argumentBuilder);
    }

    static <T extends GameRules.Value<T>> int setRule(CommandContext<CommandSourceStack> commandContext, GameRules.Key<T> key) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = commandContext.getSource();
        T value = null;
        if (Bingo.activeGame == null) {
            throw NO_RUNNING_GAME_EXCEPTION.create();
        }
        ((BingoGameExt) Bingo.activeGame).bingoExtras$getGameRules().put(key, commandContext);
        for (ServerLevel level : commandSourceStack.getServer().getAllLevels()) {
            ServerLevel parentLevel = ((ServerLevelExt_Fantasy) level).bingoExtras$getParentLevel();
            if (parentLevel == null)
                continue;
            value = level.getGameRules().getRule(key);
            value.setFromArgument(commandContext, "value");
        }
        final T lastValue = value;
        commandSourceStack.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingogamerule.set", key.getId(), lastValue.toString()), true);
        return value.getCommandResult();
    }

    static <T extends GameRules.Value<T>> int queryRule(CommandSourceStack commandSourceStack, GameRules.Key<T> key) {
        T value = commandSourceStack.getServer().getGameRules().getRule(key);
        commandSourceStack.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingogamerule.query", key.getId(), value.toString()), false);
        return value.getCommandResult();
    }
}
