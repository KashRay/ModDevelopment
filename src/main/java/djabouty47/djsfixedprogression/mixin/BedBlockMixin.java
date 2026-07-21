package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    protected BedBlockMixin(Properties properties) { super(properties); }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void addWaterloggedProperty(StateDefinition.Builder<@NotNull Block, @NotNull BlockState> builder, CallbackInfo ci) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Redirect(method = "setPlacedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean redirectBedHeadPlacement(Level level, BlockPos pos, BlockState state, int flags) {
        boolean isWater = level.getFluidState(pos).getType() == Fluids.WATER;
        return level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, isWater), flags);
    }

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void preserveBedHeadWater(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (player.preventsBlockDrops() && state.getValue(BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
            BlockPos headPos = pos.relative(state.getValue(BedBlock.FACING));
            BlockState headState = level.getBlockState(headPos);

            if (headState.is(this) && headState.hasProperty(BlockStateProperties.WATERLOGGED) && headState.getValue(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(headPos, Blocks.WATER.defaultBlockState(), 35);
                level.levelEvent(player, 2001, headPos, Block.getId(headState));
            }
        }
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void scheduleWaterTick(BlockState state, LevelReader levelReader, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(levelReader));
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void preserveWaterOnUpdate(BlockState state, LevelReader levelReader, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos blockPos2, BlockState blockState2, RandomSource randomSource, CallbackInfoReturnable<BlockState> cir) {
        BlockState result = cir.getReturnValue();
        if (result != null) {
            if (result.isAir()) {
                if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) cir.setReturnValue(Blocks.WATER.defaultBlockState());
            }
            else if (result.hasProperty(BlockStateProperties.WATERLOGGED)) {
                //If it's just syncing with the other half, preserve our waterlogged state (ignore the neighbor's)
                boolean myWaterState = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
                cir.setReturnValue(result.setValue(BlockStateProperties.WATERLOGGED, myWaterState));
            }
        }
    }
}
