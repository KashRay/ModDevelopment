package djabouty47.djsfixedprogression.client.menu;

import djabouty47.djsfixedprogression.network.NametagRenamePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class DJsNametagScreen extends Screen {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/nametag/nametag_rename");

    //Vanilla text field sprite
    private static final Identifier TEXT_FIELD = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/nametag/text_field");

    private EditBox nameField;
    private final int imageWidth = 176;
    private final int imageHeight = 75;

    public DJsNametagScreen() {
        super(Component.literal("Rename Nametag"));
    }

    @Override
    protected void init() {
        super.init();
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Find nametag in player's hand to read current name
        String currentName = "";
        if (this.minecraft != null && this.minecraft.player != null) {
            ItemStack mainHand = this.minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand = this.minecraft.player.getItemInHand(InteractionHand.OFF_HAND);
            ItemStack nametag = mainHand.is(Items.NAME_TAG) ? mainHand : (offHand.is(Items.NAME_TAG) ? offHand : ItemStack.EMPTY);

            //Fetch the current item name
            if (!nametag.isEmpty()) currentName = nametag.getHoverName().getString();
        }

        //Initialize text input box
        this.nameField = new EditBox(this.font, startX + 36, startY + 24, 103, 12, Component.literal("Name"));
        this.nameField.setMaxLength(50);
        this.nameField.setBordered(false);
        this.nameField.setVisible(true);
        this.nameField.setEditable(true);

        //Pre-fill text box with existing name
        this.nameField.setValue(currentName);
        this.addRenderableWidget(this.nameField);

        //Automatically click into text box
        this.setInitialFocus(this.nameField);

        //Initialize rename button
        this.addRenderableWidget(Button.builder(Component.literal("Rename"), button -> this.onDone())
                .bounds(startX + 33, startY + 45, 110, 20)
                .build());
    }

    /**
     * Method triggered when user clicks rename button or presses enter.
     */
    private void onDone() {
        //Save entered name
        String newName = this.nameField.getValue();

        //Send string to server payload
        ClientPlayNetworking.send(new NametagRenamePayload(newName));

        //Close GUI
        this.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Draw custom base GUI
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, this.imageWidth, this.imageHeight);

        //Draw sunken text field sprite
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXT_FIELD, startX + 33, startY + 20, 110, 16);

        //Draw title text in vanilla color
        guiGraphics.drawString(this.font, this.title, startX + 8, startY + 6, 0xFF404040, false);

        //Draw interactive widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent keyEvent) {
        //Check if player pressed the enter key
        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            //Submit the rename automatically
            this.onDone();
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean isPauseScreen() {
        //Keep world running in the background while typing
        return false;
    }
}
