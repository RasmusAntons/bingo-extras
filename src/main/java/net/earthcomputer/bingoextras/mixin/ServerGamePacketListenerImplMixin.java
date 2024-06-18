package net.earthcomputer.bingoextras.mixin;

import net.earthcomputer.bingoextras.FreezePeriod;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void teleport(double d, double e, double f, float g, float h);

    @Shadow
    private boolean clientIsFloating;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z"), cancellable = true)
    private void onMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!this.clientIsFloating && !(this.player.isChangingDimension()) && FreezePeriod.INSTANCE.isInFreezePeriod(this.player)) {
            teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
            this.player.serverLevel().getChunkSource().move(this.player);
            ci.cancel();
        }
    }
}
