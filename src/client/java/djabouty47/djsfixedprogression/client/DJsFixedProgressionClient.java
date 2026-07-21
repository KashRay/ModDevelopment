package djabouty47.djsfixedprogression.client;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.client.Tooltip.FoodTooltipData;
import djabouty47.djsfixedprogression.client.menu.*;
import djabouty47.djsfixedprogression.client.Tooltip.FoodTooltipComponent;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.menu.DJsBeaconMenu;
import djabouty47.djsfixedprogression.network.*;
import djabouty47.djsfixedprogression.procedures_and_util.IFoodHistory;
import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecart;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Items;

import java.util.Objects;

import static djabouty47.djsfixedprogression.DJsFixedProgression.attemptSleepVote;

public class DJsFixedProgressionClient implements ClientModInitializer {
    //VARIABLE DECLARATIONS
    public static boolean hasVotedToSkip = false;

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

        //Set up in-bed sleep vote receiver
        ServerPlayNetworking.registerGlobalReceiver(SleepVotePayload.ID, (payload, context) -> context.server().execute(() -> attemptSleepVote(context.player(), true)));

        //Set up sleep vote success receiver
        ClientPlayNetworking.registerGlobalReceiver(SleepVoteSuccessPayload.ID, (payload, context) -> context.client().execute(() -> hasVotedToSkip = true));

        //Set up sleep vote cancel receiver
        ClientPlayNetworking.registerGlobalReceiver(SleepVoteCancelPayload.ID, (payload, context) -> context.client().execute(() -> hasVotedToSkip = false));

        //Check if anvil and enchanting changes are enabled in configs
        if (DJsConfig.getInstance().enableAnvilAndEnchantingChanges) {
            //Register custom enchanting table UI
            MenuScreens.register(DJsFixedProgression.DJS_ENCHANTMENT_MENU, DJsEnchantmentScreen::new);

            //Register custom anvil UI
            MenuScreens.register(DJsFixedProgression.DJS_ANVIL_MENU, DJsAnvilScreen::new);

            //Register custom grindstone UI
            MenuScreens.register(DJsFixedProgression.DJS_GRINDSTONE_MENU, DJsGrindstoneScreen::new);

            //Intercept right-clicking with a nametag on the client
            UseItemCallback.EVENT.register((player, level, hand) -> {
                if (level.isClientSide() && player.getItemInHand(hand).is(Items.NAME_TAG)) {
                    //Open custom nametag GUI
                    Minecraft.getInstance().setScreen(new DJsNametagScreen());
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            });
        }

        //Check if beacon changes are enabled in configs
        if (DJsConfig.getInstance().enableBeaconChanges) {
            ClientPlayNetworking.registerGlobalReceiver(
                    BeaconSyncPayload.ID,
                    (payload, context) -> context.client().execute(() -> {
                        if (Objects.requireNonNull(context.client().player).containerMenu instanceof DJsBeaconMenu menu) menu.setClientUpgrades(payload.upgrades());
                    })
            );
        }

        //Register custom fletching table UI
        MenuScreens.register(DJsFixedProgression.DJS_FLETCHING_TABLE_MENU, DJsFletchingScreen::new);

        //Check if beacon changes are enabled in configs
        if (DJsConfig.getInstance().enableBeaconChanges) {
            //Register custom beacon UI
            MenuScreens.register(DJsFixedProgression.DJS_BEACON_MENU, DJsBeaconScreen::new);
        }

        //Check if minecart changes are enabled in configs
        if (DJsConfig.getInstance().enableMinecartChanges) {
            //Listen for the server's bidirectional sync packet
            ClientPlayNetworking.registerGlobalReceiver(MinecartLinkPayload.ID, (payload, context) -> context.client().execute(() -> {
                if (context.player() != null && context.player().level() != null) {

                    //Fetch the entity using the updated payload variable
                    Entity targetCart = context.player().level().getEntity(payload.entityId());

                    if (targetCart instanceof ILinkableMinecart linkable) {
                        //Save the link IDs for the 3D renderer
                        linkable.djs$setClientLinkParent(payload.linkAId());
                        linkable.djs$setClientLinkChild(payload.linkBId());
                    }
                }
            }));
        }

        //Check if hunger and regeneration changes are enabled in configs
        if (DJsConfig.getInstance().enableHungerAndRegenerationChanges) {
            //Receive food history sync from the server
            ClientPlayNetworking.registerGlobalReceiver(FoodHistorySyncPayload.ID, (payload, context) -> context.client().execute(() -> {
                if (context.player() instanceof IFoodHistory historyPlayer) {
                    historyPlayer.djs$getFoodHistory().clear();
                    historyPlayer.djs$getFoodHistory().addAll(payload.history());
                }
            }));

            //Add custom saturation bar HUD
            HudElementRegistry.attachElementAfter(
                    VanillaHudElements.FOOD_BAR,
                    Identifier.fromNamespaceAndPath(DJsFixedProgression.MOD_ID, "saturation_bar"),
                    (drawContext, tickCounter) -> {
                        //Get client and player
                        Minecraft client = Minecraft.getInstance();
                        Player player = client.player;
                        if (player == null || player.isSpectator() || player.isCreative()) return;

                        //Get food and saturation data
                        FoodData foodData = player.getFoodData();
                        float saturation = foodData.getSaturationLevel();

                        //Hunger bar coordinates
                        int xBase = drawContext.guiWidth() / 2 + 91;
                        int y = drawContext.guiHeight() - 39;

                        //Saturation bar textures
                        Identifier SATURATION_HALF = Identifier.fromNamespaceAndPath("djsfixedprogression", "hud/saturation_half");
                        Identifier SATURATION_FULL = Identifier.fromNamespaceAndPath("djsfixedprogression", "hud/saturation_full");

                        //Round up to the nearest whole game unit
                        int displaySaturation = (int) Math.ceil(saturation);

                        //Draw saturation bars
                        for (int i = 0; i < 10; i++) {
                            int x = xBase - (i * 8) - 9;

                            //Only draw if saturation covers this icon
                            if (displaySaturation >= (i * 2) + 2) drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, SATURATION_FULL, x, y, 9, 9);
                            else if (displaySaturation == (i * 2) + 1) drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, SATURATION_HALF, x, y, 9, 9);
                        }
                    }
            );

            //Intercept tooltips and map custom data to visual component
            TooltipComponentCallback.EVENT.register(data -> {
                if (data instanceof FoodTooltipData foodData) return new FoodTooltipComponent(foodData);
                return null;
            });
        }
	}
}