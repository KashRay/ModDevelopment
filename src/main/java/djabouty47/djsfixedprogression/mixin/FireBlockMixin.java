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
            //Reschedule tick to prevent fire from freezing in time
            level.scheduleTick(pos, state.getBlock(), 30 + random.nextInt(10));

            //Allow fire to age and burn out naturally, but never spread or destroy blocks
            int age = state.getValue(FireBlock.AGE);
            int nextAge = Math.min(15, age + random.nextInt(3) / 2);

            //Slowly increase the fire's age
            if (age != nextAge) level.setBlock(pos, state.setValue(FireBlock.AGE, nextAge), 4);
            //Once it reaches max age, let it naturally burn out and vanish
            if (nextAge == 15 && random.nextInt(4) == 0) level.removeBlock(pos, false);

            //Cancel the rest of the vanilla tick to prevent fire spreading or damage
            ci.cancel();
        }
    }
}
