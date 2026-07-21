package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends Entity {
    @Shadow public abstract int getValue();
    @Shadow private void setValue(int value) {}

    public ExperienceOrbMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Inject code when XP orbs are spawned in the world, doubling its value if within range of a beacon with an upgrade increasing XP gain.
     */
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)V", at = @At("TAIL"))
    private void boostBeaconXP(Level level, Vec3 pos, Vec3 delta, int value, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //If the XP orb spawns within range of a beacon with an upgrade increasing XP gain, double its value
        if (!level.isClientSide() && BeaconTracker.hasUpgrade(level, this.blockPosition(), "more_xp")) this.setValue(this.getValue() * 2);
    }
}
