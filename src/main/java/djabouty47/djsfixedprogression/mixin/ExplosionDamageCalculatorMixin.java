package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {
    /**
     * Inject code to adjust the blast resistance of stone if within range of a beacon with an upgrade that weakens stone blast resistance.
     */
    @Inject(method = "getBlockExplosionResistance", at = @At("RETURN"), cancellable = true)
    private void weakenStoneResistance(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Target typical overworld stone blocks
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES) && reader instanceof Level level) {
            //Check if block is within range of a beacon with the reduced stone resistance upgrade
            if (BeaconTracker.hasUpgrade(level, pos, "stone_tnt")) {
                //If vanilla returned a resistance value, reduce it
                cir.setReturnValue(Optional.of(1.0F));
            }
        }
    }

    /**
     * Inject code to increase the blast resistance of prismarine blocks.
     */
    @Inject(method = "getBlockExplosionResistance", at = @At("RETURN"), cancellable = true)
    private void strengthenPrismarineResistance(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        //Target prismarine blocks, increasing resistance value if vanilla returns one
        if (state.is(Blocks.PRISMARINE) || state.is(Blocks.PRISMARINE_BRICKS) || state.is(Blocks.DARK_PRISMARINE) ||
                state.is(Blocks.PRISMARINE_SLAB) || state.is(Blocks.PRISMARINE_BRICK_SLAB) || state.is(Blocks.DARK_PRISMARINE_SLAB) ||
                state.is(Blocks.PRISMARINE_STAIRS) || state.is(Blocks.PRISMARINE_BRICK_STAIRS) || state.is(Blocks.DARK_PRISMARINE_STAIRS) ||
                state.is(Blocks.PRISMARINE_WALL))
            cir.setReturnValue(Optional.of(1200.0F));
    }
}
