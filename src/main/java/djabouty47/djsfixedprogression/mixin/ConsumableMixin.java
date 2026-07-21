package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.IFoodHistory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Consumable.class)
public class ConsumableMixin {
    /**
     * Inject code before consuming a food item, applying dynamic saturation multiplier before vanilla saturation is calculated.
     */
    @Inject(method = "onConsume", at = @At("HEAD"))
    private void preConsumeSaturation(Level level, LivingEntity livingEntity, ItemStack itemStack, CallbackInfoReturnable<ItemStack> ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        if (livingEntity instanceof Player player && player instanceof IFoodHistory historyPlayer) {
            String itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();

            //Set global multiplier right before stats are processed
            DJsFixedProgression.currentSaturationMultiplier = historyPlayer.djs$getCurrentMultiplier(itemId);

            //Add to history
            historyPlayer.djs$addFoodToHistory(itemId);
        }
    }

    /**
     * Inject code after consuming a food item, resetting the saturation multiplier to ensure no other mechanics are accidentally multiplied.
     */
    @Inject(method = "onConsume", at = @At("RETURN"))
    private void postConsume(Level level, LivingEntity livingEntity, ItemStack itemStack, CallbackInfoReturnable<ItemStack> cir) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Reset multiplier
        DJsFixedProgression.currentSaturationMultiplier = 1.0F;
    }
}
