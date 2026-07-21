package djabouty47.djsfixedprogression.client.mixin;

import djabouty47.djsfixedprogression.client.Tooltip.FoodTooltipData;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.IFoodHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {
    /**
     * Inject method when returning tooltip image, injecting our custom tooltip data for food items.
     */
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void appendFoodTooltip(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        ItemStack stack = (ItemStack) (Object) this;
        //If the item is food, inject custom tooltip data
        if (cir.getReturnValue().isEmpty() && stack.has(DataComponents.FOOD)) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                float multiplier = ((IFoodHistory) client.player).djs$getCurrentMultiplier(itemId);
                cir.setReturnValue(Optional.of(new FoodTooltipData(stack.get(DataComponents.FOOD), multiplier)));
            }
        }
    }
}
