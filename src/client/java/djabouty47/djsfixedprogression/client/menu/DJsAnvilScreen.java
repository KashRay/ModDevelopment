package djabouty47.djsfixedprogression.client.menu;

import djabouty47.djsfixedprogression.menu.DJsAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DJsAnvilScreen extends AbstractContainerScreen<@NotNull DJsAnvilMenu> {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/anvil/anvil_screen");

    //Error arrow
    private static final Identifier ERROR_SPRITE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/anvil/error");

    public DJsAnvilScreen(DJsAnvilMenu menu, Inventory playerInventory, Component title) {
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Draw custom anvil background
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, this.imageWidth, this.imageHeight);

        //If slot 1 and 2 have items but output is empty, combination is invalid
        boolean hasInput1 = this.menu.getSlot(0).hasItem();
        boolean hasInput2 = this.menu.getSlot(1).hasItem();
        boolean hasOutput = this.menu.getSlot(2).hasItem();

        //Draw error cross over arrow
        if (hasInput1 && hasInput2 && !hasOutput) guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, startX + 112, startY + 34, 28, 21);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //Draw title and inventory text
        super.renderLabels(guiGraphics, mouseX, mouseY);

        //Fetch synced XP cost from the server
        int cost = this.menu.cost.get();

        //Check if the cost is greater than 0 and if there is a result item
        if (cost > 0 && this.menu.getSlot(2).hasItem()) {
            //Create repair cost text
            int textColor = 0xFF80FF20;
            Component costText = Component.translatable("container.repair.cost", cost);

            //Change to red if the player cannot afford it
            assert this.minecraft.player != null;
            if (!this.minecraft.player.isCreative() && this.minecraft.player.experienceLevel < cost) textColor = 0xFFFF6060;

            //Calculate exact position to anchor the text to the right side, just below the slots
            int textWidth = this.font.width(costText);
            int textX = this.imageWidth - 16 - textWidth;
            int textY = 60;

            //Draw a dark translucent shadow box behind the text
            guiGraphics.fill(textX - 2, textY - 2, textX + textWidth + 2, textY + 9, 0x4F000000);

            //Draw the text
            guiGraphics.drawString(this.font, costText, textX, textY, textColor, true);
        }
    }
}
