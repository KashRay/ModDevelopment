package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WitherBoss.class)
public class WitherBossMixin {
    /**
     * Prevent wither from destroying blocks if within range of a beacon with an active ability preventing mob griefing.
     */
    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean preventWitherGriefing(ServerLevel instance, BlockPos blockPos, boolean dropBlock, Entity entity) {
        //Check if beacon changes are enabled in configs and if wither is inside an active beacon radius
        if (DJsConfig.getInstance().enableBeaconChanges && BeaconTracker.isAbilityActive(instance, blockPos, "mob_griefing")) return false;

        //Vanilla functionality
        return instance.destroyBlock(blockPos, dropBlock, entity);
    }
}
