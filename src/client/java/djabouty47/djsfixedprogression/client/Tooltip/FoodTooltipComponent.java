package djabouty47.djsfixedprogression.client.Tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record FoodTooltipComponent(FoodTooltipData data) implements ClientTooltipComponent {
    //Vanilla hunger sprites
    private static final Identifier EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full");

    //Custom saturation sprites
    private static final Identifier SATURATION_HALF = Identifier.fromNamespaceAndPath("djsfixedprogression", "hud/saturation_half");
    private static final Identifier SATURATION_FULL = Identifier.fromNamespaceAndPath("djsfixedprogression", "hud/saturation_full");

    @Override
    public int getHeight(@NotNull Font font) {
        //Return height of hunger icons
        return 12;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        //Calculate width depending on how many icons are needed
        int hunger = this.data.foodProperties().nutrition();
        int displaySaturation = (int) Math.ceil(this.data.foodProperties().saturation() * this.data.multiplier());
        int maxIcons = (int) Math.ceil(Math.max(hunger, displaySaturation) / 2.0);
        return maxIcons * 8;
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, int w, int z, @NotNull GuiGraphics guiGraphics) {
        int hunger = this.data.foodProperties().nutrition();
        float actualSaturation = this.data.foodProperties().saturation() * this.data.multiplier();

        //Round saturation up to the nearest whole game unit
        int displaySaturation = (int) Math.ceil(actualSaturation);

        //Calculate icons needed
        int maxIcons = (int) Math.ceil(Math.max(hunger, displaySaturation) / 2.0);

        for (int i = 0; i < maxIcons; i++) {
            int renderX = x + (i * 8);

            //Draw hunger background
            if (i * 2 < hunger) {
                //Draw empty hunger points
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_HUNGER, renderX, y, 9, 9);

                //Draw the filled hunger points
                if (i * 2 + 1 < hunger) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, FULL_HUNGER, renderX, y, 9, 9);
                else if (i * 2 + 1 == hunger) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, HALF_HUNGER, renderX, y, 9, 9);
            }

            //Overlay saturation bar above
            if (displaySaturation >= (i * 2) + 2) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SATURATION_FULL, renderX, y, 9, 9);
            else if (displaySaturation == (i * 2) + 1) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SATURATION_HALF, renderX, y, 9, 9);
        }
    }
}
