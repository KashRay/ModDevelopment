package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    /**
     * Inject code when placing a block, preserving the water source the block is placed into if the block has the waterlogged property.
     */
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void fixWaterloggedPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
            cir.setReturnValue(state.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER));
        }
    }
}
