package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public class FarmBlockMixin {
    /**
     * Inject code before turning farmland to dirt, preventing mob crop trampling if within range of a beacon with an active ability preventing mob griefing.
     */
    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
    private static void preventTrampling(Entity entity, BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //If entity trampling crops is a mob or projectile
        if (entity instanceof Mob || entity instanceof Projectile) {
            //If crops are inside an active beacon radius, abort turning the farmland into dirt
            if (BeaconTracker.isAbilityActive(level, blockPos, "mob_griefing")) ci.cancel();
        }
    }
}
