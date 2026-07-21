package djabouty47.djsfixedprogression.client.Tooltip;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FoodTooltipData(FoodProperties foodProperties, float multiplier) implements TooltipComponent {}
