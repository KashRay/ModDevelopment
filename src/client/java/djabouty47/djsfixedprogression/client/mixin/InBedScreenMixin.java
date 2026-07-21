package djabouty47.djsfixedprogression.client.mixin;

import djabouty47.djsfixedprogression.client.DJsFixedProgressionClient;
import djabouty47.djsfixedprogression.network.SleepVotePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(InBedChatScreen.class)
public abstract class InBedScreenMixin extends ChatScreen {
    @Shadow private Button leaveBedButton;
    @Unique private Button djs$voteSkipButton;

    public InBedScreenMixin(String string, boolean bl) {
        super(string, bl);
    }

    /**
     * Inject method after initializing bed chat screen, adjusting the leave bed button size, and adding a vote skip button.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void addVoteSkipButton(CallbackInfo ci) {
        //Shrink and align original "leave bed" button to the left
        this.leaveBedButton.setWidth(98);
        this.leaveBedButton.setX(this.width / 2 - 100);

        //Create a new "vote skip" button aligned to the right
        this.djs$voteSkipButton = Button.builder(Component.literal("Vote Skip Night"), button -> ClientPlayNetworking.send(new SleepVotePayload())).bounds(this.width / 2 + 2, this.height - 40, 98, 20).build();

        //Add it to the screen
        this.addRenderableWidget(djs$voteSkipButton);
    }

    @Override
    public void tick() {
        super.tick();
        //Check and update sleep vote status with each tick
        if (this.djs$voteSkipButton != null) {
            //Check if local player has already voted
            boolean voted = DJsFixedProgressionClient.hasVotedToSkip;

            //Enable/disable button based on whether the player has voted already
            this.djs$voteSkipButton.active = !voted;

            //Update button text based on vote status
            this.djs$voteSkipButton.setMessage(Component.literal(voted ? "Voted" : "Vote Skip Night"));
        }
    }

    /**
     * Inject code after rendering in bed screen, notifying players when time is being fast-forwarded.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void renderFastForwardText(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.level != null) {
            //Read the synced tick rate directly from the client level
            if (this.minecraft.level.tickRateManager().tickrate() > 20.0F) guiGraphics.drawString(this.font, Component.literal("Fast-Forwarding Night").withStyle(ChatFormatting.WHITE), 4, 4, 0xFFFFFFFF);
        }
    }
}
