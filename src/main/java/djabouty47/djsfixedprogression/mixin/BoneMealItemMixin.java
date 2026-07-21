package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

    /**
     * Inject code before growing crop with bonemeal, applying a boost if within range of a beacon with an upgrade buffing bonemeal.
     */
    @Inject(method = "growCrop", at = @At("HEAD"))
    private static void applyBonemealBoost(ItemStack itemStack, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges || level.isClientSide()) return;

        BlockState initialState = level.getBlockState(pos);

        //Check if the initial block can be bone mealed and the beacon upgrade is active
        if (initialState.getBlock() instanceof BonemealableBlock && BeaconTracker.hasUpgrade(level, pos, "bonemeal_boost")) {
            //Loop the growth logic to guarantee plant reaches maturity
            int safetyLimit = 0;
            while (safetyLimit < 10) {
                //Re-fetch the state to see what the block currently is
                BlockState currentState = level.getBlockState(pos);

                //Exit loop if new block is still bone mealable
                if (!(currentState.getBlock() instanceof BonemealableBlock currentBonemealable)) break;

                //Exit loop if the crop is fully grown
                if (!currentBonemealable.isValidBonemealTarget(level, pos, currentState)) break;

                //Apply bonemeal
                if (currentBonemealable.isBonemealSuccess(level, level.random, pos, currentState)) currentBonemealable.performBonemeal((ServerLevel) level, level.random, pos, currentState);

                safetyLimit++;
            }
        }
    }
}
