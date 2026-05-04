package net.earthcomputer.bingoextras.mixin.fantasy;

import io.github.gaming32.bingo.ext.MinecraftServerExt;
import io.github.gaming32.bingo.game.BingoGame;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.bingo.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private Level level;

    @ModifyVariable(method = "teleport", at = @At("HEAD"), argsOnly = true, name = "transition")
    private TeleportTransition modifyDestDimension(TeleportTransition dest) {
        ServerLevel overrideLevel = null;
        PlayerTeam currentLevelTeam = ServerLevelExt_Fantasy.getTeam((ServerLevel) level);
        PlayerTeam destLevelTeam = ServerLevelExt_Fantasy.getTeam(dest.newLevel());
        if (!FantasyUtil.isForcedDimensionChange() && currentLevelTeam != null && destLevelTeam == null) {
            overrideLevel = PlayerTeamExt_Fantasy.getTeamSpecificLevel(level.getServer(), currentLevelTeam, dest.newLevel().dimension());
        }
        final BingoGame activeGame = ((MinecraftServerExt) dest.newLevel().getServer()).bingo$getGame();
        if (activeGame != null && ((BingoGameExt) activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            ResourceKey<Level> dimension = dest.newLevel().dimension();
            var parentLevel = ((ServerLevelExt_Fantasy) dest.newLevel()).bingoExtras$getParentLevel();
            if (parentLevel != null) {
                dimension = parentLevel.dimension();
            }
            overrideLevel = BingoGameExt.getGameSpecificLevel(dest.newLevel().getServer(), activeGame, dimension);
        }
        if (overrideLevel != null) {
            dest = new TeleportTransition(
                overrideLevel,
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
