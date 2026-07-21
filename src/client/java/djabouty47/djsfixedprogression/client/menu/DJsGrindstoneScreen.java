package djabouty47.djsfixedprogression.client.menu;

import djabouty47.djsfixedprogression.menu.DJsGrindstoneMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class DJsGrindstoneScreen extends AbstractContainerScreen<DJsGrindstoneMenu> {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/grindstone/grindstone");

    public DJsGrindstoneScreen(DJsGrindstoneMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Draw custom grindstone background
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, this.imageWidth, this.imageHeight);

    }
}
