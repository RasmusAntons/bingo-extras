package net.earthcomputer.bingoextras;

import com.demonwav.mcdev.annotations.Translatable;
import dev.xpple.betterconfig.api.ModConfigBuilder;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.command.BingoExtrasCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

public class BingoExtras implements ModInitializer {
    public static BingoGame seedfindGame = null;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> BingoExtrasCommands.register(dispatcher, context));
        new ModConfigBuilder("bingoextras", Configs.class).build();

        ServerLifecycleEvents.SERVER_STARTED.register(FreezePeriod::onStartup);
        ServerTickEvents.START_SERVER_TICK.register(FreezePeriod::onTick);
    }

    public static MutableComponent translatable(@Translatable String translationKey) {
        return ensureHasFallback(Component.translatable(translationKey));
    }

    public static MutableComponent translatable(@Translatable String translationKey, Object... args) {
        return ensureHasFallback(Component.translatable(translationKey, args));
    }

    public static MutableComponent ensureHasFallback(MutableComponent component) {
        ComponentContents contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            if (translatable.getFallback() == null) {
                MutableComponent result = Component.translatableWithFallback(translatable.getKey(), component.getString(), translatable.getArgs());
                result.getSiblings().addAll(component.getSiblings());
                return result;
            }
        }
        return component;
    }
}
