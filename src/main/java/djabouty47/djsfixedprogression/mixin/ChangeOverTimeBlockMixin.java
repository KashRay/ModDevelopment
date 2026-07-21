package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChangeOverTimeBlock.class)
public interface ChangeOverTimeBlockMixin {
    /**
     * Inject code to prevent copper blocks from aging when within range of a beacon with an upgrade preventing oxidization.
     */
    @Inject(method = "changeOverTime", at = @At("HEAD"), cancellable = true)
    private void preventOxidization(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Check if block is a weathering copper block
        if (blockState.getBlock() instanceof WeatheringCopper) {
            //If the block is within range of a beacon with an upgrade prevent oxidization, cancel the block update
            if (BeaconTracker.hasUpgrade(serverLevel, blockPos, "prevent_oxidization")) ci.cancel();
        }
    }
}
