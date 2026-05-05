package net.earthcomputer.bingoextras.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.gaming32.bingo.ext.MinecraftServerExt;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.BingoExtras;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.NonNull;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.ArrayList;

import static net.earthcomputer.bingoextras.command.BingoSpreadPlayersSeedfindCommand.NO_RUNNING_GAME_EXCEPTION;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BingoGameRuleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> argumentBuilder = literal("bingogamerule")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));

        new GameRules(FeatureFlags.REGISTRY.allFlags()).visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(final @NonNull GameRule<T> gameRule) {
                argumentBuilder.then(
                        literal(gameRule.id())
                                .executes(ctx -> queryRule(ctx.getSource(), gameRule))
                                .then(argument("value", gameRule.argument()).executes(ctx -> setRule(ctx, gameRule)))
                );
            }
        });
        dispatcher.register(argumentBuilder);
    }

    static <T> int setRule(CommandContext<CommandSourceStack> ctx, GameRule<T> gameRule) throws CommandSyntaxException {
        CommandSourceStack commandSourceStack = ctx.getSource();
        T value = ctx.getArgument("value", gameRule.valueClass());
        final BingoGame activeGame = ((MinecraftServerExt) ctx.getSource().getServer()).bingo$getGame();
        if (activeGame == null) {
            throw NO_RUNNING_GAME_EXCEPTION.create();
        }
        ((BingoGameExt) activeGame).bingoExtras$getGameRules().put(gameRule.getIdentifier(), ctx);
        var gameSpecificLevels = ((BingoGameExt) activeGame).bingoExtras$getGameSpecificLevels();
        for (RuntimeLevelHandle handle : gameSpecificLevels.values()) {
            handle.asLevel().getGameRules().set(gameRule, value, ctx.getSource().getServer());
        }
        commandSourceStack.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingogamerule.set", gameRule.id(), value.toString()), true);
        return gameRule.getCommandResult(value);
    }

    static <T> int queryRule(CommandSourceStack commandSourceStack, GameRule<T> gameRule) throws CommandSyntaxException {
        final BingoGame activeGame = ((MinecraftServerExt) commandSourceStack.getServer()).bingo$getGame();
        if (activeGame == null) {
            throw NO_RUNNING_GAME_EXCEPTION.create();
        }
        CommandContext<CommandSourceStack> savedCtx = ((BingoGameExt) activeGame).bingoExtras$getGameRules().get(gameRule.getIdentifier());
        T value;
        if (savedCtx != null) {
            value = savedCtx.getArgument("value", gameRule.valueClass());
        } else {
            value = commandSourceStack.getServer().getGameRules().get(gameRule);
        }
        commandSourceStack.sendSuccess(() -> BingoExtras.translatable("bingo_extras.bingogamerule.query", gameRule.id(), value.toString()), false);
        return gameRule.getCommandResult(value);
    }
}
