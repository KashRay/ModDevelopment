package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Shadow public abstract int getMaxAge();
    @Shadow public abstract int getAge(BlockState state);
    @Shadow public abstract BlockState getStateForAge(int age);

    /**
     * Inject code after random tick, accelerating growth if within range of a beacon with an upgrade increasing farmland growing speeds.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void applyFarmlandBoost(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Ask tracker if we have the boost
        if (BeaconTracker.hasUpgrade(level, pos, "farmland_boost")) {
            int currentAge = this.getAge(state);
            int maxAge = this.getMaxAge();

            if (currentAge < maxAge) {
                //Randomly adds an extra growth stage
                int extraGrowth = random.nextInt(2);
                int newAge = Math.min(currentAge + extraGrowth, maxAge);

                //If it successfully advanced, update the block
                if (newAge > currentAge) level.setBlock(pos, this.getStateForAge(newAge), 2);
            }
        }
    }
}
