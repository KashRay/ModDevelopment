package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block implements SimpleWaterloggedBlock {
    public DoorBlockMixin(Properties properties) { super(properties); }

    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void addWaterloggedProperty(StateDefinition.Builder<@NotNull Block, @NotNull BlockState> builder, CallbackInfo ci) {
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void forceWaterloggedPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean isWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
            cir.setReturnValue(state.setValue(BlockStateProperties.WATERLOGGED, isWater));
        }
    }

    @Redirect(method = "setPlacedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean redirectDoorTopPlacement(Level level, BlockPos pos, BlockState state, int flags) {
        boolean isWater = level.getFluidState(pos).getType() == Fluids.WATER;
        return level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, isWater), flags);
    }

    @Inject(method = "updateShape", at = @At("HEAD"))
    private void scheduleWaterTick(BlockState state, LevelReader levelReader, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(levelReader));
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void preserveWaterOnUpdate(BlockState state, LevelReader levelReader, ScheduledTickAccess tickAccess, BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
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

    @Inject(method = "setOpen", at = @At("TAIL"))
    private void triggerWaterFlow(Entity entity, Level level, BlockState state, BlockPos pos, boolean open, CallbackInfo ci) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            BlockPos topPos = pos.above();
            BlockState topState = level.getBlockState(topPos);
            if (topState.is((DoorBlock)(Object)this) && topState.hasProperty(BlockStateProperties.WATERLOGGED) && topState.getValue(BlockStateProperties.WATERLOGGED)) {
                level.scheduleTick(topPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
        }
    }
}
