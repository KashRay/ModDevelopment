package djabouty47.djsfixedprogression.client.menu;

import com.mojang.blaze3d.platform.NativeImage;
import djabouty47.djsfixedprogression.menu.DJsBeaconMenu;
import djabouty47.djsfixedprogression.network.BeaconUpgradePayload;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconNode;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTreeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DJsBeaconScreen extends AbstractContainerScreen<@NotNull DJsBeaconMenu> {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/beacon/beacon_screen");

    //Beacon sprite layer
    private static final Identifier BEACON_1_LAYER = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/beacon/beacon_1_layer");
    private static final Identifier BEACON_2_LAYERS = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/beacon/beacon_2_layers");
    private static final Identifier BEACON_3_LAYERS = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/beacon/beacon_3_layers");
    private static final Identifier BEACON_4_LAYERS = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/beacon/beacon_4_layers");

    //Scrolling state variables
    private double treeScrollX = 0, treeScrollY = 0;
    private double mapScrollX = 0, mapScrollY = 0;
    private boolean isDraggingTree = false, isDraggingMap = false;

    //GLFW mouse cursors
    private static long handCursor = 0;
    private static long dragCursor = 0;

    //Beacon map tracking
    private DynamicTexture mapTexture;
    private Identifier mapIdentifier;
    private int lastKnownLayers = -1;
    private int currentMapSize = 0;

    //Upgrade tree box boundaries
    private static final int TREE_BOX_X_OFFSET = 10;
    private static final int TREE_BOX_Y_OFFSET = 7;
    private static final int TREE_BOX_WIDTH = 105;
    private static final int TREE_BOX_HEIGHT = 91;

    //Beacon range box boundaries
    private static final int MAP_BOX_X_OFFSET = 117;
    private static final int MAP_BOX_Y_OFFSET = 7;
    private static final int MAP_BOX_WIDTH = 103;
    private static final int MAP_BOX_HEIGHT = 91;

    public DJsBeaconScreen(DJsBeaconMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 219;
        this.inventoryLabelX = 36;
        this.inventoryLabelY = 126;
    }

    @Override
    protected void init() {
        super.init();
        if (handCursor == 0) handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        if (dragCursor == 0) dragCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        //Get beacon custom NBT data
        CompoundTag upgrades = this.menu.getUpgrades();

        //Get beacon layer count
        int layers = upgrades.getIntOr("djs_beacon_level", 0);

        boolean isHoveringAffordableNode = false;

        //Only render tooltips if beacon is active
        if (layers > 0) {
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            int treeScissorX = x + TREE_BOX_X_OFFSET;
            int treeScissorY = y + TREE_BOX_Y_OFFSET;

            //Only process node tooltips if cursor is physically inside box window
            if (mouseX >= treeScissorX && mouseX <= treeScissorX + TREE_BOX_WIDTH && mouseY >= treeScissorY && mouseY <= treeScissorY + TREE_BOX_HEIGHT) {
                double relativeX = mouseX - treeScissorX - this.treeScrollX;
                double relativeY = mouseY - treeScissorY - this.treeScrollY;

                for (BeaconNode node : BeaconTreeRegistry.NODES.values()) {
                    if (relativeX >= node.x() && relativeX <= node.x() + 16 && relativeY >= node.y() && relativeY <= node.y() + 16) {
                        //Get upgrade state
                        boolean isOwned = upgrades.contains(node.id());
                        boolean isBlocked = isNodeBlocked(node, upgrades);
                        boolean isParentLocked = isLockedByParent(node, upgrades);
                        boolean affordable = hasTotalItems(node, upgrades);

                        //If player hovered an available and affordable node, prepare the hand cursor
                        if (!isOwned && !isBlocked && !isParentLocked && affordable) isHoveringAffordableNode = true;
                        if (isOwned && BeaconTreeRegistry.ABILITIES.containsKey(node.id()) && affordable) isHoveringAffordableNode = true;

                        //Build tooltip
                        List<Component> tooltip = new ArrayList<>();
                        tooltip.add(Component.translatable(node.name()).withStyle(ChatFormatting.GOLD));
                        tooltip.add(Component.literal(node.description()).withStyle(ChatFormatting.WHITE));

                        //Display upgrade status in tooltips
                        if (isOwned && BeaconTreeRegistry.ABILITIES.containsKey(node.id())) {
                            //Get ability data from registry
                            BeaconTreeRegistry.AbilityData data = BeaconTreeRegistry.ABILITIES.get(node.id());
                            if (data != null) {
                                //Calculate the cooldown
                                assert this.minecraft.level != null;
                                long currentTime = this.minecraft.level.getGameTime();
                                long cooldownEnd = upgrades.getLongOr(node.id() + "_cooldown_end", 0);
                                long durationEnd = upgrades.getLongOr(node.id() + "_duration_end", 0);

                                //If ability is still active
                                if (currentTime < durationEnd) {
                                    //Display ability duration remaining
                                    int secondsLeft = (int) ((durationEnd - currentTime) / 20);
                                    tooltip.add(Component.literal("Status: ACTIVE (" + secondsLeft + "s)").withStyle(ChatFormatting.AQUA));
                                }
                                //If ability is still on cooldown
                                else if (currentTime < cooldownEnd) {
                                    //Display cooldown time remaining
                                    int secondsLeft = (int) ((cooldownEnd - currentTime) / 20);
                                    tooltip.add(Component.literal("Status: On Cooldown (" + secondsLeft + "s)").withStyle(ChatFormatting.RED));
                                }
                                //If ability is ready
                                else {
                                    //Display ability activation costs
                                    tooltip.add(Component.literal("Status: Ready to Activate!").withStyle(ChatFormatting.GREEN));
                                    //Ensure player is not in creative
                                    if (this.minecraft != null && this.minecraft.player != null && !this.minecraft.player.isCreative()) {
                                        tooltip.add(Component.literal("Activation Cost:").withStyle(ChatFormatting.YELLOW));
                                        for (BeaconNode.NodeCost cost : data.usageCosts()) {
                                            ChatFormatting costColor = affordable ? ChatFormatting.WHITE : ChatFormatting.RED;
                                            tooltip.add(Component.literal("- " + cost.count() + "x ").append(cost.item().getName()).withStyle(costColor));
                                        }
                                    }
                                }
                            }
                        }
                        else if (isOwned) {
                            //Tell player upgrade is already owned
                            tooltip.add(Component.literal("Purchased").withStyle(ChatFormatting.GREEN));
                        } else if (isBlocked) {
                            //Tell player upgrade is on a locked path
                            tooltip.add(Component.literal("Path Locked").withStyle(ChatFormatting.RED));
                        } else if (isParentLocked) {
                            //Tell player upgrade requires prerequisite upgrade to be unlocked
                            tooltip.add(Component.literal("Requires Previous Upgrade").withStyle(ChatFormatting.GRAY));
                        } else {
                            //Ensure player is not in creative
                            if (this.minecraft != null && this.minecraft.player != null && !this.minecraft.player.isCreative()) {
                                //Display cost of upgrade
                                tooltip.add(Component.literal("Cost:").withStyle(ChatFormatting.YELLOW));
                                for (BeaconNode.NodeCost cost : node.costs()) {
                                    //Display red or white text if the player meets cost requirement
                                    ChatFormatting costColor = affordable ? ChatFormatting.WHITE : ChatFormatting.RED;
                                    tooltip.add(Component.literal("- " + cost.count() + "x ").append(cost.item().getName()).withStyle(costColor));
                                }
                            }
                        }

                        List<ClientTooltipComponent> comps = tooltip.stream().map(Component::getVisualOrderText).map(ClientTooltipComponent::create).toList();
                        graphics.renderTooltip(this.font, comps, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
                    }
                }
            }
        }

        //Render tooltips
        this.renderTooltip(graphics, mouseX, mouseY);

        //Cursor update logic
        if (this.minecraft != null) {
            long window = this.minecraft.getWindow().handle();

            //4-way arrow for panning
            if (this.isDraggingTree || this.isDraggingMap) GLFW.glfwSetCursor(window, dragCursor);
            //Hand for clicking
            else if (isHoveringAffordableNode) GLFW.glfwSetCursor(window, handCursor);
            //Default pointer
            else GLFW.glfwSetCursor(window, 0);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        //Draw custom beacon background
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, this.imageWidth, this.imageHeight);

        //Get beacon custom NBT data
        CompoundTag upgrades = this.menu.getUpgrades();

        //Get beacon layer count
        int layers = upgrades.getIntOr("djs_beacon_level", 0);

        //Only render background if beacon is active
        if (layers > 0) {
            //Determine beacon layer sprite to be used
            Identifier layerSprite = switch (layers) {
                case 1 -> BEACON_1_LAYER;
                case 2 -> BEACON_2_LAYERS;
                case 3 -> BEACON_3_LAYERS;
                case 4 -> BEACON_4_LAYERS;
                default -> null;
            };

            //Draw beacon layer sprite
            if (layerSprite != null) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, layerSprite, x + 159, y + 107, 19, 19);

            //Enable scissor mask area for upgrade tree box
            int treeScissorX = x + TREE_BOX_X_OFFSET;
            int treeScissorY = y + TREE_BOX_Y_OFFSET;
            graphics.enableScissor(treeScissorX, treeScissorY + 14, treeScissorX + TREE_BOX_WIDTH, treeScissorY + TREE_BOX_HEIGHT);

            //Push matrix and translate for scrolling
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) this.treeScrollX, (float) this.treeScrollY);

            //Draw upgrade tree path (pass 0: black borders, pass 1: dark grey/blocked, pass 2: light grey/available, pass 4: white/owned)
            for (int pass = 0; pass < 4; pass++) {
                for (BeaconNode node : BeaconTreeRegistry.NODES.values()) {
                    //Get node coordinates
                    int nodeX = treeScissorX + node.x() + 8;
                    int nodeY = treeScissorY + node.y() + 8;

                    //Get upgrade state
                    boolean childOwned = upgrades.contains(node.id());
                    boolean childBlocked = isNodeBlocked(node, upgrades);

                    for (String parentId : node.parents()) {
                        //Get parent node(s) for upgrade
                        BeaconNode parentNode = BeaconTreeRegistry.NODES.get(parentId);
                        if (parentNode != null) {
                            //Get parent coordinates
                            int parentX = treeScissorX + parentNode.x() + 8;
                            int parentY = treeScissorY + parentNode.y() + 8;

                            //Check whether the parent is unlocked
                            boolean parentOwned = upgrades.contains(parentId);

                            //Determine state of this specific line connection
                            boolean isWhite = childOwned && parentOwned;
                            boolean isDarkGrey = !parentOwned || childBlocked;
                            boolean isLightGrey = !isWhite && !isDarkGrey;

                            //Draw path according to current pass hierarchy
                            if (pass == 0) drawBranch(graphics, parentX, parentY, nodeX, nodeY, 0xFF000000, true);
                            else if (pass == 1 && isDarkGrey) drawBranch(graphics, parentX, parentY, nodeX, nodeY, 0xFF333333, false);
                            else if (pass == 2 && isLightGrey) drawBranch(graphics, parentX, parentY, nodeX, nodeY, 0xFF888888, false);
                            else if (pass == 3 && isWhite) drawBranch(graphics, parentX, parentY, nodeX, nodeY, 0xFFFFFFFF, false);
                        }
                    }
                }
            }

            //Draw node icons
            for (BeaconNode node : BeaconTreeRegistry.NODES.values()) {
                int drawX = treeScissorX + node.x();
                int drawY = treeScissorY + node.y();

                //Draw temporary box
                graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0x88000000);

                //Draw item icon assigned to node
                graphics.renderItem(node.icon(), drawX, drawY);

                //Get upgrade state
                boolean isOwned = upgrades.contains(node.id());
                boolean isBlocked = isNodeBlocked(node, upgrades);
                boolean isParentLocked = isLockedByParent(node, upgrades);

                //If node is an active ability
                if (isOwned && BeaconTreeRegistry.ABILITIES.containsKey(node.id()) && this.minecraft != null && this.minecraft.level != null) {
                    //Get ability data from registry
                    BeaconTreeRegistry.AbilityData data = BeaconTreeRegistry.ABILITIES.get(node.id());

                    if (data != null) {
                        //Calculate the cooldown
                        long currentTime = this.minecraft.level.getGameTime();
                        long durationEnd = upgrades.getLongOr(node.id() + "_duration_end", 0);
                        long cooldownEnd = upgrades.getLongOr(node.id() + "_cooldown_end", 0);

                        //Draw glowing cyan border if ability is actively running
                        if (currentTime < durationEnd) {
                            graphics.fill(drawX - 1, drawY - 1, drawX + 17, drawY, 0xFF00FFFF);
                            graphics.fill(drawX - 1, drawY + 16, drawX + 17, drawY + 17, 0xFF00FFFF);
                            graphics.fill(drawX - 1, drawY, drawX, drawY + 16, 0xFF00FFFF);
                            graphics.fill(drawX + 16, drawY, drawX + 17, drawY + 16, 0xFF00FFFF);
                        }

                        //Draw cooldown sweep
                        if (currentTime < cooldownEnd) {
                            float progress = (float)(cooldownEnd - currentTime) / data.cooldownTicks();
                            int height = (int) (16.0F * progress);
                            graphics.fill(drawX, drawY + 16 - height, drawX + 16, drawY + 16, 0x80FFFFFF);
                        }
                    }
                }

                //If node is blocked or missing its parent, overlay dark tint
                if (!isOwned && (isBlocked || isParentLocked)) graphics.fill(drawX, drawY, drawX + 16, drawY + 16, 0xAA000000);
            }

            //Pop matrix and disable scissors
            graphics.pose().popMatrix();
            graphics.disableScissor();

            //Check if beacon coordinates were written
            if (upgrades.contains("beacon_x")) {
                //Get beacon coordinates
                int bX = upgrades.getIntOr("beacon_x", 0);
                int bZ = upgrades.getIntOr("beacon_z", 0);

                //Calculate radius
                int radius = (layers * 20 + 20);

                //Generate map only if it hasn't been made yet, or if the pyramid changed size
                if (this.lastKnownLayers != layers) {
                    generateMapTexture(bX, bZ, radius);
                    this.lastKnownLayers = layers;
                }

                //Draw generated map onto the screen
                if (this.mapIdentifier != null) {
                    //Enable scissor mask area for beacon range box
                    int mapScissorX = x + MAP_BOX_X_OFFSET;
                    int mapScissorY = y + MAP_BOX_Y_OFFSET;
                    int visibleMapHeight = MAP_BOX_HEIGHT - 14;
                    int visibleMapY = mapScissorY + 14;
                    graphics.enableScissor(mapScissorX, visibleMapY, mapScissorX + MAP_BOX_WIDTH, mapScissorY + MAP_BOX_HEIGHT);

                    //Push matrix and translate for scrolling
                    graphics.pose().pushMatrix();
                    graphics.pose().translate((float) this.mapScrollX, (float) this.mapScrollY);

                    //Perfectly center image inside box natively, offset by scroll values
                    int renderX = mapScissorX + (MAP_BOX_WIDTH - this.currentMapSize) / 2;
                    int renderY = visibleMapY + (visibleMapHeight - this.currentMapSize) / 2;

                    //Render map image
                    graphics.blit(RenderPipelines.GUI_TEXTURED, this.mapIdentifier, renderX, renderY, 0, 0, this.currentMapSize, this.currentMapSize, this.currentMapSize, this.currentMapSize);

                    //Pop matrix and disable scissors
                    graphics.pose().popMatrix();
                    graphics.disableScissor();
                }
            }
        }
        //Draw warning text when no base is detected
        else {
            graphics.drawCenteredString(this.font, "Requires Pyramid", x + TREE_BOX_X_OFFSET + (TREE_BOX_WIDTH / 2), y + TREE_BOX_Y_OFFSET + (TREE_BOX_HEIGHT / 2) + 7, 0xFFFF5555);
            graphics.drawCenteredString(this.font, "Requires Pyramid", x + MAP_BOX_X_OFFSET + (MAP_BOX_WIDTH / 2), y + MAP_BOX_Y_OFFSET + (MAP_BOX_HEIGHT / 2) + 7, 0xFFFF5555);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        //Inventory title
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);

        //Upgrade tree title
        graphics.drawString(this.font, Component.literal("Upgrade Tree"), 14, 12, 0xFFFFFFFF, false);

        //Effect range title
        graphics.drawString(this.font, Component.literal("Effect Range"), 121, 12, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent mouseButtonEvent, boolean button) {
        //Get beacon custom NBT data
        CompoundTag upgrades = this.menu.getUpgrades();

        //Disable clicking if beacon has no base
        if (upgrades.getIntOr("djs_beacon_level", 0) == 0) return super.mouseClicked(mouseButtonEvent, button);
        
        int startX = (this.width - this.imageWidth) / 2;
        int startY = (this.height - this.imageHeight) / 2;

        //Scissor mask area for upgrade tree and beacon range boxes
        int treeScissorX = startX + TREE_BOX_X_OFFSET;
        int treeScissorY = startY + TREE_BOX_Y_OFFSET;
        int mapScissorX = startX + MAP_BOX_X_OFFSET;
        int mapScissorY = startY + MAP_BOX_Y_OFFSET;

        //Check if click occurred inside upgrade tree box
        if (mouseButtonEvent.x() >= treeScissorX && mouseButtonEvent.x() <= treeScissorX + TREE_BOX_WIDTH && mouseButtonEvent.y() >= treeScissorY && mouseButtonEvent.y() <= treeScissorY + TREE_BOX_HEIGHT) {
            //Calculate mouse's relative position to scrolling canvas
            double relativeX = mouseButtonEvent.x() - treeScissorX - this.treeScrollX;
            double relativeY = mouseButtonEvent.y() - treeScissorY - this.treeScrollY;

            //Check if click overlaps with any node
            for (BeaconNode node : BeaconTreeRegistry.NODES.values()) {
                if (relativeX >= node.x() && relativeX <= node.x() + 16 && relativeY >= node.y() && relativeY <= node.y() + 16) {
                    //Get upgrade state
                    boolean isOwned = upgrades.contains(node.id());
                    boolean isBlocked = isNodeBlocked(node, upgrades);
                    boolean isParentLocked = isLockedByParent(node, upgrades);
                    boolean isAbility = BeaconTreeRegistry.ABILITIES.containsKey(node.id());

                    //If node is available or is an ability, attempt to buy it
                    if ((!isOwned && !isBlocked && !isParentLocked) || (isOwned && isAbility)) {
                        //If player can afford, and the items are already in the ingredient slots
                        if (canAffordInSlots(node, upgrades)) {
                            //Play vanilla click sound
                            if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                            //Send purchase packet to server
                            ClientPlayNetworking.send(new BeaconUpgradePayload(node.id()));
                        }
                        //If player can afford, but the items are in their inventory
                        else if (hasTotalItems(node, upgrades)) {
                            //Play vanilla click sound
                            if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                            //Send auto move packet to server
                            ClientPlayNetworking.send(new djabouty47.djsfixedprogression.network.BeaconAutoMovePayload(node.id()));
                        }

                        //Stop processing click
                        return true;
                    }
                }
            }

            //If valid node wasn't clicked, scroll drag is initiated
            this.isDraggingTree = true;
            return true;
        }

        //Check if click occurred inside beacon range box
        if (mouseButtonEvent.x() >= mapScissorX && mouseButtonEvent.x() <= mapScissorX + MAP_BOX_WIDTH && mouseButtonEvent.y() >= mapScissorY && mouseButtonEvent.y() <= mapScissorY + MAP_BOX_HEIGHT) {
            //Initiate scroll drag
            this.isDraggingMap = true;
            return true;
        }

        return super.mouseClicked(mouseButtonEvent, button);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent mouseButtonEvent) {
        this.isDraggingTree = false;
        this.isDraggingMap = false;
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        //Check if player is dragging within upgrade tree box
        if (this.isDraggingTree) {
            //Apply drag delta to scroll coordinates
            this.treeScrollX += dragX;
            this.treeScrollY += dragY;

            //Calculate absolute bounds of the entire tree
            int maxTreeX = 0;
            int maxTreeY = 0;
            for (BeaconNode node : BeaconTreeRegistry.NODES.values()) {
                maxTreeX = Math.max(maxTreeX, node.x() + 16);
                maxTreeY = Math.max(maxTreeY, node.y() + 16);
            }

            //Define boundaries to prevent infinite scrolling
            double minScrollX = Math.min(0, TREE_BOX_WIDTH - maxTreeX - 20);
            double minScrollY = Math.min(0, TREE_BOX_HEIGHT - maxTreeY - 20);

            //Clamp coordinates
            this.treeScrollX = Mth.clamp(this.treeScrollX, minScrollX, 0);
            this.treeScrollY = Mth.clamp(this.treeScrollY, minScrollY, 0);

            return true;
        }

        //Check if player is dragging within beacon range box
        if (this.isDraggingMap) {
            //Apply drag delta to scroll coordinates
            this.mapScrollX += dragX;
            this.mapScrollY += dragY;
            int visibleMapHeight = MAP_BOX_HEIGHT - 14;

            //Define boundaries to prevent infinite scrolling
            double maxScrollX = Math.max(0, (this.currentMapSize - MAP_BOX_WIDTH) / 2.0);
            double maxScrollY = Math.max(0, (this.currentMapSize - visibleMapHeight) / 2.0);

            //Clamp coordinates
            this.mapScrollX = Mth.clamp(this.mapScrollX, -maxScrollX, maxScrollX);
            this.mapScrollY = Mth.clamp(this.mapScrollY, -maxScrollY, maxScrollY);

            return true;
        }

        return super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    /**
     * Helper method that checks if items in payment slots meet the node's cost.
     */
    private boolean canAffordInSlots(BeaconNode node, CompoundTag upgrades) {
        //Creative players can always afford upgrades and abilities
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative()) return true;

        //Check if upgrade is owned
        boolean isOwned = upgrades.contains(node.id());

        //Get node cost
        List<BeaconNode.NodeCost> costsToPay = node.costs();

        //Check if activating an owned ability
        if (isOwned && BeaconTreeRegistry.ABILITIES.containsKey(node.id())) {
            //Get ability data from registry
            BeaconTreeRegistry.AbilityData data = BeaconTreeRegistry.ABILITIES.get(node.id());
            if (data == null) return false;

            //Calculate the cooldown
            long currentTime = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0;
            long cooldownEnd = upgrades.getLongOr(node.id() + "_cooldown_end", 0);

            //Cannot afford if ability is still on cooldown
            if (currentTime < cooldownEnd) return false;

            //Get ability usage cost
            costsToPay = data.usageCosts();
        }
        //Prevent repurchasing of owned passives
        else if (isOwned) return false;

        //Loop over all costs
        for (BeaconNode.NodeCost cost : costsToPay) {
            int count = 0;
            //Determine if player has enough ingredients in the slots
            for (int i = 0; i < 3; i++) {
                ItemStack stack = this.menu.getSlot(i).getItem();
                if (stack.is(cost.item())) count += stack.getCount();
            }
            if (count < cost.count()) return false;
        }
        return true;
    }

    /**
     * Helper method that checks if the player has the items anywhere in their inventory.
     */
    private boolean hasTotalItems(BeaconNode node, CompoundTag upgrades) {
        //Creative players can always afford upgrades and abilities
        if (this.minecraft != null && this.minecraft.player != null && this.minecraft.player.isCreative()) return true;

        //Check if upgrade is owned
        boolean isOwned = upgrades.contains(node.id());

        //Get node cost
        List<BeaconNode.NodeCost> costsToPay = node.costs();

        //Check if activating an owned ability
        if (isOwned && BeaconTreeRegistry.ABILITIES.containsKey(node.id())) {
            //Get ability data from registry
            BeaconTreeRegistry.AbilityData data = BeaconTreeRegistry.ABILITIES.get(node.id());
            if (data == null) return false;

            //Calculate the cooldown
            long currentTime = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0;
            long cooldownEnd = upgrades.getLongOr(node.id() + "_cooldown_end", 0);

            //Cannot afford if ability is still on cooldown
            if (currentTime < cooldownEnd) return false;

            //Get ability usage cost
            costsToPay = data.usageCosts();
        }
        //Prevent repurchasing of owned passives
        else if (isOwned) return false;

        //Loop over all costs
        for (BeaconNode.NodeCost cost : costsToPay) {
            int count = 0;
            //Check all player inventory slots for items
            for (int i = 0; i < 39; i++) {
                ItemStack stack = this.menu.getSlot(i).getItem();
                if (stack.is(cost.item())) count += stack.getCount();
            }
            if (count < cost.count()) return false;
        }
        return true;
    }

    /**
     * Helper method to evaluate pathing restrictions, and mutually exclusive locks.
     * A player can complete 1 full path, and select 2 upgrades from another path, blocking the remaining path entirely.
     * Returns true if the path should be blocked.
     */
    private boolean isNodeBlocked(BeaconNode node, CompoundTag upgrades) {
        //If upgrade is already bought, it isn't blocked
        if (upgrades.contains(node.id())) return false;

        //Check local mutually exclusive splits
        for (String ex : node.mutuallyExclusive()) if (upgrades.contains(ex)) return true;

        //Evaluate Max-2-0 pathing rule
        int p1 = 0, p2 = 0, p3 = 0;
        for (String key : upgrades.keySet()) {
            BeaconNode n = BeaconTreeRegistry.NODES.get(key);
            if (n != null) {
                if (n.pathId() == 1) p1++;
                else if (n.pathId() == 2) p2++;
                else if (n.pathId() == 3) p3++;
            }
        }

        //Determine active path split
        int myPath = node.pathId();
        int myCount = (myPath == 1) ? p1 : (myPath == 2) ? p2 : p3;
        int activePaths = (p1 > 0 ? 1 : 0) + (p2 > 0 ? 1 : 0) + (p3 > 0 ? 1 : 0);

        //If two other paths are established, block the empty 3rd path entirely
        if (activePaths >= 2 && myCount == 0) return true;

        //Find the highest upgrade count among the other paths
        int highestOtherPath = 0;
        if (myPath == 1) highestOtherPath = Math.max(p2, p3);
        if (myPath == 2) highestOtherPath = Math.max(p1, p3);
        if (myPath == 3) highestOtherPath = Math.max(p1, p2);

        //When a path has at least 3 selected, cap the second selected path to 2 upgrades
        return myCount >= 2 && highestOtherPath >= 3;
    }

    /**
     * Helper method that checks if a node is locked because the player hasn't purchased its prerequisite yet.
     */
    private boolean isLockedByParent(BeaconNode node, CompoundTag upgrades) {
        //Starting nodes are never locked
        if (node.parents().isEmpty()) return false;

        //If any of the parent nodes are owned, this node becomes available
        for (String parentId : node.parents()) if (upgrades.contains(parentId)) return false;

        //None of the parents are owned, so upgrade remains locked
        return true;
    }

    /**
     * Helper method to calculate and draw an L-shaped branch between two nodes.
     */
    private void drawBranch(GuiGraphics graphics, int startX, int startY, int endX, int endY, int color, boolean isBorder) {
        int midX = startX + (endX - startX) / 2;
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);

        //Draw black line borders
        if (isBorder) {
            //Horizontal segment 1
            graphics.fill(startX, startY - 1, midX + 2, startY + 2, color);

            //Vertical segment
            graphics.fill(midX - 1, minY - 1, midX + 2, maxY + 2, color);

            //Horizontal segment 2
            graphics.fill(midX - 1, endY - 1, endX, endY + 2, color);
        }
        //Draw colored line centers
        else {
            //Horizontal segment 1
            graphics.fill(startX, startY, midX + 1, startY + 1, color);

            //Vertical segment
            graphics.fill(midX, minY, midX + 1, maxY + 1, color);

            //Horizontal segment 2
            graphics.fill(midX, endY, endX, endY + 1, color);
        }
    }

    /**
     * Method to scan the world and generate a pixel-perfect minimap centered on the beacon.
     */
    private void generateMapTexture(int beaconX, int beaconZ, int radius) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        //Image size = diameter + 20px of padding so border doesn't clip edge
        int mapSize = radius * 2 + 20;
        this.currentMapSize = mapSize;

        NativeImage image = new NativeImage(mapSize, mapSize, false);

        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                //Pixel offsets from the center of the map
                int dx = i - (mapSize / 2);
                int dz = j - (mapSize / 2);

                int worldX = beaconX + dx;
                int worldZ = beaconZ + dz;

                //Get current block height and color
                BlockPos currentPos = getVisibleMapBlock(worldX, worldZ);
                BlockPos northPos = getVisibleMapBlock(worldX, worldZ - 1);
                BlockState state = this.minecraft.level.getBlockState(currentPos);

                //Calculate vanilla topographic shading
                int diff = currentPos.getY() - northPos.getY();
                double shadeMultiplier;
                if (diff > 0) shadeMultiplier = 1.0; //Sloping up
                else if (diff == 0) shadeMultiplier = 0.86; //Flat terrain
                else shadeMultiplier = 0.71; //Sloping down

                //Extract base map colors
                int rgb = state.getMapColor(this.minecraft.level, currentPos).col;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                //Apply shading
                r = Math.min(255, (int)(r * shadeMultiplier));
                g = Math.min(255, (int)(g * shadeMultiplier));
                b = Math.min(255, (int)(b * shadeMultiplier));

                //Calculate distance from the center to draw range border
                int absDx = Math.abs(dx);
                int absDz = Math.abs(dz);
                int maxDist = Math.max(absDx, absDz);

                //Darken out-of-bounds terrain
                if (maxDist > radius) {
                    r = (int)(r * 0.4);
                    g = (int)(g * 0.4);
                    b = (int)(b * 0.4);
                }
                //Draw a glowing cyan border ring exactly on the boundary
                else if (maxDist == radius) {
                    r = 0;
                    g = 255;
                    b = 255;
                }

                //Convert from standard RGB to ABGR
                int abgr = (0xFF << 24) | (b << 16) | (g << 8) | r;
                image.setPixelABGR(i, j, abgr);
            }
        }

        //Clean up old texture to prevent memory leaks
        if (this.mapTexture != null) this.mapTexture.close();

        //Provide a string supplier for GPU debugger, and store identifier safely
        this.mapTexture = new DynamicTexture(() -> "beacon_map_texture", image);
        this.mapIdentifier = Identifier.fromNamespaceAndPath("djsfixedprogression", "beacon_map");
        this.minecraft.getTextureManager().register(this.mapIdentifier, this.mapTexture);
    }

    /**
     * Helper method for map generation. Gets the visible map block, ignoring transparent blocks like rails, glass, tallgrass.
     */
    private BlockPos getVisibleMapBlock(int x, int z) {
        if (this.minecraft == null || this.minecraft.level == null) return new BlockPos(x, 0, z);

        BlockPos pos = this.minecraft.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));

        //Step down until we find a block that actually has a map color
        while (pos.getY() > this.minecraft.level.getMinY()) {
            pos = pos.below();
            BlockState state = this.minecraft.level.getBlockState(pos);

            //Return block position if it doesn't have an invisible map color
            if (state.getMapColor(this.minecraft.level, pos) != MapColor.NONE) return pos;
        }
        return pos;
    }

    @Override
    public void removed() {
        super.removed();
        if (this.mapTexture != null) this.mapTexture.close();
        if (this.minecraft != null) GLFW.glfwSetCursor(this.minecraft.getWindow().handle(), 0);
    }
}
