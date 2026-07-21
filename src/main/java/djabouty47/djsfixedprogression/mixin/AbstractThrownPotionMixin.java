package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractThrownPotion.class)
public abstract class AbstractThrownPotionMixin {
    @Shadow protected abstract void dowseFire(BlockPos pos);

    /**
     * Inject code upon lingering potion hit, forcing water lingering potions to drop a cloud.
     */
    @Inject(method = "onHit", at = @At("HEAD"))
    private void forceLingeringWaterCloud(HitResult hitResult, CallbackInfo ci) {
        AbstractThrownPotion potion = (AbstractThrownPotion) (Object) this;

        //Ensure potion is a lingering potion
        if (!potion.level().isClientSide() && potion instanceof ThrownLingeringPotion lingeringPotion) {
            ItemStack item = potion.getItem();
            PotionContents contents = item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

            //If the contents are water, force native cloud spawning method
            if (contents.is(Potions.WATER)) lingeringPotion.onHitAsPotion((ServerLevel) potion.level(), item, hitResult);
        }
    }
}
