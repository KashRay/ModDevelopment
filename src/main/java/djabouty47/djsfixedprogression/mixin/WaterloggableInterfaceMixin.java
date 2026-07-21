package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.level.block.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
        AnvilBlock.class,
        ButtonBlock.class,
        BeaconBlock.class,
        BrewingStandBlock.class,
        ComposterBlock.class,
        DaylightDetectorBlock.class,
        EnchantingTableBlock.class,
        GrindstoneBlock.class,
        LecternBlock.class,
        LeverBlock.class,
        StonecutterBlock.class,
        VineBlock.class,
        CarpetBlock.class,
        FenceGateBlock.class,
        DragonEggBlock.class,
        AbstractBannerBlock.class
})
public abstract class WaterloggableInterfaceMixin implements SimpleWaterloggedBlock { }
