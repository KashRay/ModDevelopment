package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Shadow protected abstract ItemStack getPickupItem();
    @Shadow protected abstract void setPickupItemStack(ItemStack itemStack);

    /**
     * Inject code after arrow hits a block, leaving a lingering potion cloud upon impact.
     */
    @Inject(method = "onHitBlock", at = @At("TAIL"))
    private void deployCloudOnBlockHit(BlockHitResult blockHitResult, CallbackInfo ci) {
        //Call helper method to leave lingering cloud effect at location
        this.djs$trySpawnLingeringCloud(blockHitResult.getLocation());
    }

    /**
     * Inject code after arrow hits an entity, leaving a lingering potion cloud upon impact.
     */
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void deployCloudOnEntityHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        //Call helper method to leave lingering cloud effect at location
        this.djs$trySpawnLingeringCloud(entityHitResult.getEntity().position());
    }

    /**
     * Helper method that reads arrow components and spawns a customized area-of-effect cloud.
     */
    @Unique
    private void djs$trySpawnLingeringCloud(Vec3 hitPos) {
        AbstractArrow arrow = (AbstractArrow)(Object)this;

        //Only run logic on server side
        if (arrow.level().isClientSide()) return;

        ItemStack arrowItem = this.getPickupItem();

        //Check if arrow item possesses any custom potion contents
        if (arrowItem.has(DataComponents.POTION_CONTENTS)) {
            PotionContents potionContents = arrowItem.get(DataComponents.POTION_CONTENTS);

            //Ensure arrow isn't empty or default-valued
            if (potionContents != null && !potionContents.equals(PotionContents.EMPTY)) {
                //Initialize the lingering cloud at the exact impact position
                AreaEffectCloud cloud = new AreaEffectCloud(arrow.level(), hitPos.x, hitPos.y, hitPos.z);

                //Assign the cloud's ownership to whoever shot the arrow
                Entity shooter = arrow.getOwner();
                if (shooter instanceof LivingEntity) cloud.setOwner((LivingEntity)shooter);

                //Adjust cloud stats
                cloud.setRadius(1.0F); //2-block diameter cloud range
                cloud.setDuration(100); //lasts 5 seconds
                cloud.setRadiusPerTick(-0.005F); // Slowly shrink over time
                cloud.setWaitTime(0); //Instantly apply effect on impact

                //Manually scale effect durations down by 1/8 to match arrow tooltip
                int potionColor = potionContents.getColor();
                List<MobEffectInstance> scaledEffects = new ArrayList<>();
                for (MobEffectInstance effect : potionContents.getAllEffects()) {
                    scaledEffects.add(new MobEffectInstance(
                            effect.getEffect(),
                            Math.max(effect.getDuration() / 8, 2),
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible()
                    ));
                }

                //Create brand-new potion contents using custom scaled effects
                PotionContents scaledContents = new PotionContents(Optional.empty(), Optional.of(potionColor), scaledEffects, potionContents.customName());

                //Pass potion data over to cloud
                cloud.setPotionContents(scaledContents);

                //Spawn in world safely
                arrow.level().addFreshEntity(cloud);

                //Glass vial shatters, revert projectile back into a standard arrow
                this.setPickupItemStack(new ItemStack(Items.ARROW));
            }
        }
    }
}
