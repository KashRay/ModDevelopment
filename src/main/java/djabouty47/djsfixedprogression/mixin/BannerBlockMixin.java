package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BannerBlock.class, WallBannerBlock.class})
public class BannerBlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void forceWaterloggedBannerPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean isWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
            cir.setReturnValue(state.setValue(BlockStateProperties.WATERLOGGED, isWater));
        }
    }
}
