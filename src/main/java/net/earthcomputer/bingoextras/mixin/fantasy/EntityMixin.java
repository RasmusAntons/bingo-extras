package net.earthcomputer.bingoextras.mixin.fantasy;

import io.github.gaming32.bingo.Bingo;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Level level;

    @Shadow
    @Nullable
    public abstract MinecraftServer getServer();

    @ModifyVariable(method = "teleport", at = @At("HEAD"), argsOnly = true)
    private TeleportTransition modifyDestDimension(TeleportTransition dest) {
        ServerLevel overrideLevel = null;
        PlayerTeam currentLevelTeam = ServerLevelExt_Fantasy.getTeam((ServerLevel) level);
        PlayerTeam destLevelTeam = ServerLevelExt_Fantasy.getTeam(dest.newLevel());
        if (!FantasyUtil.isForcedDimensionChange() && currentLevelTeam != null && destLevelTeam == null) {
            overrideLevel = PlayerTeamExt_Fantasy.getTeamSpecificLevel(getServer(), currentLevelTeam, dest.newLevel().dimension());
        }
        if (Bingo.activeGame != null && ((BingoGameExt) Bingo.activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            ResourceKey<Level> dimension = dest.newLevel().dimension();
            var parentLevel = ((ServerLevelExt_Fantasy) dest.newLevel()).bingoExtras$getParentLevel();
            if (parentLevel != null) {
                dimension = parentLevel.dimension();
            }
            overrideLevel = BingoGameExt.getGameSpecificLevel(dest.newLevel().getServer(), Bingo.activeGame, dimension);
        }
        if (overrideLevel != null) {
            dest = new TeleportTransition(
                    PlayerTeamExt_Fantasy.getTeamSpecificLevel(getServer(), currentLevelTeam, dest.newLevel().dimension()),
                    dest.position(),
                    dest.deltaMovement(),
                    dest.yRot(),
                    dest.xRot(),
                    dest.missingRespawnBlock(),
                    dest.asPassenger(),
                    dest.relatives(),
                    dest.postTeleportTransition()
            );
        }
        return dest;
    }
}
