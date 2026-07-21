package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {
    /**
     * Inject code after spawning a child from breeding, reducing cooldown if within range of a beacon with an upgrade reducing the breeding cooldown.
     */
    @Inject(method = "spawnChildFromBreeding", at = @At("TAIL"))
    private void applyBreedingCooldownBoost(ServerLevel level, Animal mate, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        Animal self = (Animal)(Object)this;

        //Check tracker for beacon upgrade
        if (BeaconTracker.hasUpgrade(level, self.blockPosition(), "breeding_cooldown")) {
            //Overwrite both parent's cooldowns, reducing it down to 1 minute instead of 5
            self.setAge(1200);
            mate.setAge(1200);
        }
    }
}
