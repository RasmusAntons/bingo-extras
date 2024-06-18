package net.earthcomputer.bingoextras;

import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;

import java.util.*;

public class FreezePeriod {
    public static FreezePeriod INSTANCE = null;

    private final HashMap<UUID, Integer> frozenPlayers = new HashMap<>();

    private FreezePeriod(MinecraftServer server) {
    }

    public static void onStartup(MinecraftServer minecraftServer) {
        INSTANCE = new FreezePeriod(minecraftServer);
    }

    public static void onTick(MinecraftServer minecraftServer) {
        if (INSTANCE != null) {
            ArrayList<UUID> unfreezePlayers = new ArrayList<>();
            INSTANCE.frozenPlayers.replaceAll((id, time) -> {
                if (--time == 0) {
                    unfreezePlayers.add(id);
                }
                if (time % 20 == 0) {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayer(id);
                    if (player != null) {
                        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE)
                            player.setGameMode(GameType.ADVENTURE);
                        player.connection.send(new ClientboundSetActionBarTextPacket(BingoExtras.translatable("bingo_extras.freeze.time", time / 20)));
                        if (time <= 100) {
                            player.playNotifySound((time == 0) ? SoundEvents.BELL_BLOCK : SoundEvents.LEVER_CLICK, SoundSource.MASTER, 0.5f, 1f);
                        }
                    }
                }
                return time;
            });
            for (UUID id : unfreezePlayers) {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayer(id);
                if (player != null)
                    player.setGameMode(GameType.SURVIVAL);
                INSTANCE.frozenPlayers.remove(id);
            }
        }
    }

    public void setFreezePeriod(ServerPlayer player, double seconds) {
        frozenPlayers.put(player.getUUID(), (int) Math.round(seconds * 20));
    }

    public boolean isInFreezePeriod(ServerPlayer player) {
        return frozenPlayers.containsKey(player.getUUID());
    }
}
