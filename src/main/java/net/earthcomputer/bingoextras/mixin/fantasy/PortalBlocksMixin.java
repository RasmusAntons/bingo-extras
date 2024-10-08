package net.earthcomputer.bingoextras.mixin.fantasy;

import io.github.gaming32.bingo.Bingo;
import net.earthcomputer.bingoextras.FantasyUtil;
import net.earthcomputer.bingoextras.ext.BingoGameExt;
import net.earthcomputer.bingoextras.ext.fantasy.PlayerTeamExt_Fantasy;
import net.earthcomputer.bingoextras.ext.fantasy.ServerLevelExt_Fantasy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({NetherPortalBlock.class, EndPortalBlock.class})
public class PortalBlocksMixin {
    @ModifyVariable(method = "getPortalDestination", at = @At("STORE"), ordinal = 1)
    private ServerLevel modifyDestLevel(ServerLevel destLevel, ServerLevel sourceLevel) {
        PlayerTeam currentLevelTeam = ServerLevelExt_Fantasy.getTeam(sourceLevel);
        PlayerTeam destLevelTeam = ServerLevelExt_Fantasy.getTeam(destLevel);
        if (!FantasyUtil.isForcedDimensionChange() && currentLevelTeam != null && destLevelTeam == null) {
            destLevel = PlayerTeamExt_Fantasy.getTeamSpecificLevel(destLevel.getServer(), currentLevelTeam, destLevel.dimension());
        }
        if (Bingo.activeGame != null && ((BingoGameExt) Bingo.activeGame).bingo_extras$getGameSpecificWorldSeed() != 0) {
            ResourceKey<Level> dimension = destLevel.dimension();
            var parentLevel = ((ServerLevelExt_Fantasy) destLevel).bingoExtras$getParentLevel();
            if (parentLevel != null) {
                dimension = parentLevel.dimension();
            }
            destLevel = BingoGameExt.getGameSpecificLevel(destLevel.getServer(), Bingo.activeGame, dimension);
        }
        return destLevel;
    }
}
