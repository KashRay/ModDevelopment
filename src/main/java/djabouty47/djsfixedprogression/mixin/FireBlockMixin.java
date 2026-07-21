package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    /**
     * Inject code at the start of each tick, cancelling fire spreading if within range of a beacon with an upgrade preventing fire spreading.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preventFireSpread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs, and if tracker finds the beacon upgrade
        if (DJsConfig.getInstance().enableBeaconChanges && BeaconTracker.hasUpgrade(level, pos, "fire_spread")) {
            //Cancel the tick so fire will no longer spread to adjacent blocks and will not consume the block it is sitting on
            ci.cancel();
        }
    }
}
