package net.earthcomputer.bingoextras.mixin.fantasy;

import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements ServerLevelExt_Fantasy {

    @Shadow
    public abstract WeatherData getWeatherData();

    @Unique
    @Nullable
    private PlayerTeam team = null;

    @Unique
    @Nullable
    private ServerLevel originalLevel = null;

    @Unique
    @Nullable
    private ServerLevel parentLevel = null;

    @Unique
    private boolean syncWeatherFromOriginal = true;

    @Override
    @Nullable
    public PlayerTeam bingoExtras$getTeam() {
        return team;
    }

    @Override
    public void bingoExtras$setTeam(@Nullable PlayerTeam team) {
        this.team = team;
    }

    @Override
    @Nullable
    public ServerLevel bingoExtras$getOriginalLevel() {
        return originalLevel;
    }

    @Override
    public void bingoExtras$setOriginalLevel(@Nullable ServerLevel level) {
        this.originalLevel = level;
    }

    @Override
    @Nullable
    public ServerLevel bingoExtras$getParentLevel() {
        return parentLevel;
    }

    @Override
    public void bingoExtras$setParentLevel(@Nullable ServerLevel level) {
        this.parentLevel = level;
    }

    @Override
    public void bingoExtras$setSyncWeatherFromOriginal(boolean syncWeatherFromOriginal) {
        this.syncWeatherFromOriginal = syncWeatherFromOriginal;
    }

    @Inject(method = "advanceWeatherCycle", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;oThunderLevel:F", ordinal = 0, opcode = Opcodes.PUTFIELD))
    private void copyWeatherFromOriginal(CallbackInfo ci) {
        if (originalLevel != null && syncWeatherFromOriginal) {
            WeatherData originalData = originalLevel.getWeatherData();
            WeatherData ourData = getWeatherData();
            ourData.setClearWeatherTime(originalData.getClearWeatherTime());
            ourData.setRainTime(originalData.getRainTime());
            ourData.setThunderTime(originalData.getThunderTime());
            ourData.setRaining(originalData.isRaining());
            ourData.setThundering(originalData.isThundering());
        }
    }

    @Inject(method = "resetWeatherCycle", at = @At("HEAD"))
    private void onResetWeatherCycle(CallbackInfo ci) {
        syncWeatherFromOriginal = false;
    }
}
