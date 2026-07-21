package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ravager.class)
public class RavagerMixin {
    /**
     * Prevent ravager from destroying blocks if within range of a beacon with an active ability preventing mob griefing.
     */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean preventRavagerGriefing(ServerLevel level, BlockPos pos, boolean dropBlock, Entity entity) {
        //Check if beacon changes are enabled in configs and if ravager is inside an active beacon radius
        if (DJsConfig.getInstance().enableBeaconChanges && BeaconTracker.isAbilityActive(level, pos, "mob_griefing")) return false;

        //Vanilla functionality
        return level.destroyBlock(pos, dropBlock, entity);
    }
}
