package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KineticWeapon.class)
public class SpearDamageMixin {
    /**
     * Inject code when getting the motion of the entity utilizing a spear, ensuring only horizontal motion is used to calculate damage.
     */
    @Inject(method = "getMotion", at = @At("HEAD"), cancellable = true)
    private static void getHorizontalMotionOnly(Entity entity, CallbackInfoReturnable<Vec3> cir) {
        //Check if the entity is riding a vehicle
        if (!(entity instanceof Player) && entity.isPassenger()) entity = entity.getRootVehicle();

        //Grab the vanilla scaled speed vector
        Vec3 originalMotion = entity.getKnownSpeed().scale(20.0D);

        //Strip the vertical velocity completely
        Vec3 horizontalMotion = new Vec3(originalMotion.x, 0.0D, originalMotion.z);

        //Return custom horizontal-only vector
        cir.setReturnValue(horizontalMotion);
    }
}
