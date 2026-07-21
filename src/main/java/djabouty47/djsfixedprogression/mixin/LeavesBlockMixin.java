package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin {
    /**
     * Inject code at the end of each tick, accelerating leaf decay.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void accelerateLeafDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        //Ensure block is a leaves block
        if (!(state.getBlock() instanceof LeavesBlock)) return;

        //Check if block has lost its log support and isn't player-placed
        if (!state.getValue(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.DISTANCE) == 7) {
            //20% chance to decay during this tick
            if (random.nextInt(5) == 0) state.randomTick(level, pos, random);
            //Schedule another tick
            else level.scheduleTick(pos, state.getBlock(), 2 + random.nextInt(6));
        }
    }
}
