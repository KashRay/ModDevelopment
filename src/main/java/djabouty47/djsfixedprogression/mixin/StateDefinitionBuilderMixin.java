package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(StateDefinition.Builder.class)
public abstract class StateDefinitionBuilderMixin<O, S extends StateHolder<@NotNull O, @NotNull S>> {
    @Shadow public abstract StateDefinition.Builder<@NotNull O, @NotNull S> add(Property<?>... properties);
    @Final @Shadow private O owner;

    @Inject(method = "create", at = @At("HEAD"))
    private void injectWaterloggedParity(Function<O, S> function, StateDefinition.Factory<@NotNull O, @NotNull S> factory, CallbackInfoReturnable<StateDefinition<@NotNull O, @NotNull S>> cir) {
        if (this.owner instanceof Block block) {
            if (block instanceof AnvilBlock || block instanceof ButtonBlock ||
                    block instanceof BeaconBlock || block instanceof BrewingStandBlock ||
                    block instanceof ComposterBlock || block instanceof DaylightDetectorBlock ||
                    block instanceof EnchantingTableBlock || block instanceof GrindstoneBlock ||
                    block instanceof LecternBlock || block instanceof LeverBlock ||
                    block instanceof StonecutterBlock || block instanceof VineBlock ||
                    block instanceof CarpetBlock || block instanceof FenceGateBlock ||
                    block instanceof DragonEggBlock || block instanceof AbstractBannerBlock) {
                this.add(BlockStateProperties.WATERLOGGED);
            }
        }
    }
}
