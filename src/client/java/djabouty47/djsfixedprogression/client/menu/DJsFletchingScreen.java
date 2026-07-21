package djabouty47.djsfixedprogression.client.menu;

import djabouty47.djsfixedprogression.menu.DJsFletchingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DJsFletchingScreen extends AbstractContainerScreen<@NotNull DJsFletchingMenu> {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/fletching_table/fletching_table");

    //Empty slot sillhouettes
    private static final Identifier EMPTY_SLOT_ARROW = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/fletching_table/empty_slot_arrow");
    private static final Identifier EMPTY_SLOT_LINGERING_POTION = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/fletching_table/empty_slot_lingering_potion");

    //Tooltip text components
    private static final Component ARROW_TOOLTIP = Component.literal("Add 4 or more Arrows");
    private static final Component MODIFIER_TOOLTIP = Component.literal("Add Lingering Potion");

    public DJsFletchingScreen(DJsFletchingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
        this.titleLabelX = 60;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Draw custom fletching table background
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, this.imageWidth, this.imageHeight);

        //Draw arrow silhouette if slot 0 is empty
        if (!this.menu.getSlot(0).hasItem()) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_SLOT_ARROW, startX + 53, startY + 34, 16, 16);

        //Draw lingering potion silhouette if slot 1 is empty
        if (!this.menu.getSlot(1).hasItem()) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, EMPTY_SLOT_LINGERING_POTION, startX + 93, startY + 34, 16, 16);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        //Check if user is hovering over a slot and if the slot is currently empty
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            //Draw instructional tooltip based on which slot is hovered
            if (this.hoveredSlot.index == 0) guiGraphics.setTooltipForNextFrame(ARROW_TOOLTIP, mouseX, mouseY);
            else if (this.hoveredSlot.index == 1) guiGraphics.setTooltipForNextFrame(MODIFIER_TOOLTIP, mouseX, mouseY);
        }
    }
}
