package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {
    @Shadow public abstract Entity getDirectSourceEntity();
    @Shadow public abstract LivingEntity getIndirectSourceEntity();
    @Shadow public abstract ServerLevel level();

    /**
     * Inject code to filter out mob-damaged blocks within range of a beacon with an active ability preventing mob griefing.
     */
    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void preventMobExplosions(CallbackInfoReturnable<List<BlockPos>> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Get source of explosion
        Entity directSource = this.getDirectSourceEntity();
        LivingEntity indirectSource = this.getIndirectSourceEntity();

        //Ignore if explosion was caused by the player
        if (!(indirectSource instanceof Player)) {
            //Grab the vanilla list of blocks about to be blown up
            List<BlockPos> originalBlocks = cir.getReturnValue();
            List<BlockPos> unprotectedBlocks = new ArrayList<>();
            ServerLevel level = this.level();

            //Filter the list, keeping blocks that are outside the beacon's protection
            for (BlockPos pos : originalBlocks) {
                if (!BeaconTracker.isAbilityActive(level, pos, "mob_griefing")) unprotectedBlocks.add(pos);
            }

            //Hand the filtered list back to the game
            cir.setReturnValue(unprotectedBlocks);
        }
    }
}
