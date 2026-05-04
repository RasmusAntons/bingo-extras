package net.earthcomputer.bingoextras;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

    public static void onTick(MinecraftServer server) {
        if (INSTANCE != null) {
            ArrayList<UUID> unfreezePlayers = new ArrayList<>();
            INSTANCE.frozenPlayers.replaceAll((id, time) -> {
                if (--time <= 0) {
                    unfreezePlayers.add(id);
                }
                if (time % SharedConstants.TICKS_PER_SECOND == 0) {
                    ServerPlayer player = server.getPlayerList().getPlayer(id);
                    if (player != null) {
                        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE)
                            player.setGameMode(GameType.ADVENTURE);
                        player.connection.send(new ClientboundSetActionBarTextPacket(BingoExtras.translatable("bingo_extras.freeze.time", time / 20)));
                        if (time <= 100) {
                            player.connection.send(new ClientboundSoundEntityPacket(
                                    Holder.direct(time == 0 ? SoundEvents.BELL_BLOCK : SoundEvents.LEVER_CLICK),
                                    SoundSource.MASTER, player,
                                    0.5f, 1f, player.getRandom().nextLong()
                            ));
                        }
                    }
                }
                return time;
            });
            for (UUID id : unfreezePlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null)
                    player.setGameMode(GameType.SURVIVAL);
                INSTANCE.frozenPlayers.remove(id);
            }
            if (INSTANCE.frozenPlayers.isEmpty() && server.tickRateManager().isFrozen()) {
                server.tickRateManager().setFrozen(false);
            }
        }
    }

    public void setFreezePeriod(ServerPlayer player, int ticks) {
        frozenPlayers.put(player.getUUID(), ticks);
    }

    public boolean isInFreezePeriod(ServerPlayer player) {
        return frozenPlayers.containsKey(player.getUUID());
    }
}
