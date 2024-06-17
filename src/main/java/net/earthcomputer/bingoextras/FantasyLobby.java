package net.earthcomputer.bingoextras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public class FantasyLobby {
    public static FantasyLobby INSTANCE = null;

    private RuntimeWorldHandle world = null;

    private FantasyLobby(MinecraftServer server) {
        FlatLevelGeneratorPreset preset = server.registryAccess().registry(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                .get().get(ResourceLocation.withDefaultNamespace("classic_flat"));
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setFlat(true)
                .setGenerator(new FlatLevelSource(preset.settings()))
                .setDifficulty(Difficulty.PEACEFUL);
        world = Fantasy.get(server).openTemporaryWorld(config);
        world.asWorld().setDefaultSpawnPos(new BlockPos(0, 4, 0), 0);
        world.asWorld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        world.asWorld().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        server.overworld().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        server.overworld().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
    }

    public ServerLevel getWorld() {
        return world.asWorld();
    }

    public static void onStartup(MinecraftServer minecraftServer) {
        INSTANCE = new FantasyLobby(minecraftServer);
    }

    public void onGameStart(MinecraftServer server) {
        server.sendSystemMessage(Component.literal("enabling daylight cycle!!!"));
        server.overworld().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, server);
        server.overworld().getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(true, server);
        server.overworld().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true, server);
    }
}
