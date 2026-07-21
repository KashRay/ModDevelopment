package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CampfireBlock.class)
public abstract class CampfireBlockMixin {

    /**
     * Inject code after initializing the block, setting the default campfire state to be unlit.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void unlitByDefault(boolean spawnParticles, int fireDamage, BlockBehaviour.Properties properties, CallbackInfo ci) {
        CampfireBlock campfire = (CampfireBlock) (Object) this;
        ((BlockAccessor) campfire).djs$registerDefaultState(campfire.defaultBlockState().setValue(CampfireBlock.LIT, false));
    }

    /**
     * Inject code when interacting with a lit campfire, allowing the player to set their spawn.
     */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    private void unlitOnPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state != null && state.hasProperty(CampfireBlock.LIT)) cir.setReturnValue(state.setValue(CampfireBlock.LIT, false));
    }
}
