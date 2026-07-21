package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    /**
     * Inject code when trying to destroy a vault or trial spawner, making them indestructible in Survival.
     */
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void makeVaultsAndTrialSpawnersIndestructible(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;
        if (state.is(Blocks.VAULT) || state.is(Blocks.TRIAL_SPAWNER)) cir.setReturnValue(-1.0F);
    }

    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void getGlobalWaterloggedFluidState(CallbackInfoReturnable<FluidState> cir) {
        BlockBehaviour.BlockStateBase state = (BlockBehaviour.BlockStateBase) (Object) this;
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) cir.setReturnValue(Fluids.WATER.getSource(false));
    }
}
