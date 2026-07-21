package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SpreadingSnowyDirtBlock.class, VineBlock.class})
public class SpreadingBlockMixin {
    //A thread-safe flag to prevent our extra ticks from triggering an infinite loop
    @Unique private static final ThreadLocal<Boolean> DJS_IS_EXTRA_TICK = ThreadLocal.withInitial(() -> false);

    /**
     * Inject code at the end of the random tick, accelerating grass and vine growth if within range of a beacon with an upgrade increasing grass and vine growth.
     */
    @Inject(method = "randomTick", at = @At("TAIL"))
    private void applyGrassBoost(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //If this is already an extra tick we forced, stop
        if (DJS_IS_EXTRA_TICK.get()) return;

        //Check tracker for beacon upgrade
        if (BeaconTracker.hasUpgrade(level, pos, "grass_boost")) {
            //Lock the flag
            DJS_IS_EXTRA_TICK.set(true);

            //Force block to randomly tick an extra time to accelerate spreading
            int extraTicks = random.nextInt(3) + 2;
            for (int i = 0; i < extraTicks; i++) {
                BlockState currentState = level.getBlockState(pos);

                //Ensure block hasn't been broken or changed before ticking it
                if (currentState.getBlock() == state.getBlock()) currentState.randomTick(level, pos, random);
                else break;
            }

            //Unlock flag
            DJS_IS_EXTRA_TICK.set(false);
        }
    }
}
