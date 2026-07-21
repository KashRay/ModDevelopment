package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public abstract class MobMixin {
    /**
     * Modify base armor spawn chance, increasing it if the mob is below deepslate layer.
     */
    @ModifyConstant(method = "populateDefaultEquipmentSlots", constant = @Constant(floatValue = 0.15F))
    private float boostDeepCaveArmorChance(float originalChance) {
        Mob mob = (Mob) (Object) this;

        //Check if mob armor chance change is enabled in configs, or if the mob isn't deep underground
        if (!DJsConfig.getInstance().undergroundMobsMoreArmored || mob.getY() >= 0) return originalChance;

        //Increase the chance of spawning with armor
        return 0.45F;
    }

    /**
     * Modify armor tier upgrade chance, increasing it if the mob is below deepslate layer.
     */
    @ModifyConstant(method = "populateDefaultEquipmentSlots", constant = @Constant(floatValue = 0.1087F))
    private float boostDeepCaveArmorQuality(float originalUpgradeChance) {
        Mob mob = (Mob) (Object) this;

        //Check if mob armor chance change is enabled in configs, or if the mob isn't deep underground
        if (!DJsConfig.getInstance().undergroundMobsMoreArmored || mob.getY() >= 0) return originalUpgradeChance;

        //Increase the chance of upgrading the armor tier
        return 0.25F;
    }
}
