package djabouty47.djsfixedprogression.client.menu;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import djabouty47.djsfixedprogression.menu.DJsEnchantmentMenu;
import djabouty47.djsfixedprogression.network.EnchantingAutoMovePayload;
import djabouty47.djsfixedprogression.network.EnchantmentSelectPayload;
import djabouty47.djsfixedprogression.procedures_and_util.EnchantmentCostHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DJsEnchantmentScreen extends AbstractContainerScreen<@NotNull DJsEnchantmentMenu> {
    //Main background
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/enchanting_screen");

    //Scrollbar assets
    private static final Identifier SCROLLER = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/scroller_disabled");

    //Button assets
    private static final Identifier SLOT = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/enchantment_slot");
    private static final Identifier SLOT_HIGHLIGHTED = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/enchantment_slot_highlighted");
    private static final Identifier SLOT_DISABLED = Identifier.fromNamespaceAndPath("djsfixedprogression", "container/enchanting_table/enchantment_slot_disabled");

    //Vanilla book assets
    private static final Identifier ENCHANTING_BOOK_LOCATION = Identifier.withDefaultNamespace("textures/entity/enchanting_table_book.png");
    private final RandomSource random = RandomSource.create();
    private BookModel bookModel;
    public float flip, oFlip, flipT, flipA, open, oOpen;
    private ItemStack last = ItemStack.EMPTY;

    //State variables
    private float scrollAmount = 0.0F;
    private boolean isDraggingScroller = false;
    private ItemStack cachedItem = ItemStack.EMPTY;
    private final List<Holder<@NotNull Enchantment>> availableEnchantments = new ArrayList<>();
    private final List<Component> displayEnchantments = new ArrayList<>();

    public DJsEnchantmentScreen(DJsEnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) this.bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.tickBook();
    }

    public void tickBook() {
        ItemStack itemStack = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(itemStack, this.last)) {
            this.last = itemStack;
            do {
                this.flipT = this.flipT + (this.random.nextInt(4) - this.random.nextInt(4));
            } while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
        }

        this.oFlip = this.flip;
        this.oOpen = this.open;

        if (!itemStack.isEmpty()) this.open += 0.2F;
        else this.open -= 0.2F;

        this.open = Mth.clamp(this.open, 0.0F, 1.0F);
        float f = (this.flipT - this.flip) * 0.4F;
        f = Mth.clamp(f, -0.2F, 0.2F);
        this.flipA = this.flipA + (f - this.flipA) * 0.9F;
        this.flip = this.flip + this.flipA;
    }

    private void updateAvailableEnchantments() {
        ItemStack currentItem = this.menu.getSlot(0).getItem();

        //If the item hasn't changed since last frame, stop
        if (ItemStack.matches(this.cachedItem, currentItem)) return;

        //If the item has changed, clear the list and rest the scroll wheel
        this.cachedItem = currentItem.copy();
        this.availableEnchantments.clear();
        this.displayEnchantments.clear();

        //If the item is unenchantable, stop
        if (currentItem.isEmpty() || (!currentItem.isEnchantable() && currentItem.getEnchantments().isEmpty()) || this.minecraft == null || this.minecraft.level == null) return;

        //Fetch the enchantment registry
        var registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        //Get enchantments on item
        var existingEnchants = currentItem.getEnchantments();

        //Scan every enchantment in the game
        for (Holder.Reference<@NotNull Enchantment> holder : registry.listElements().toList()) {
            Enchantment enchantment = holder.value();

            //Filter enchantments for the current tool
            if (!enchantment.isPrimaryItem(currentItem)) continue;

            //Check for valid enchanting table enchants and curses
            if (!holder.is(EnchantmentTags.IN_ENCHANTING_TABLE) && !holder.is(EnchantmentTags.CURSE)) continue;

            //Check for mutual exclusivity
            int displayLevel = 1;
            boolean isCompatible = true;
            for (var existingHolder : existingEnchants.keySet()) {
                //Check if enchantment is the same
                if (holder.equals(existingHolder)) {
                    //Display enchants if they are compatible with current enchants
                    int currentLevel = existingEnchants.getLevel(existingHolder);
                    if (currentLevel >= enchantment.getMaxLevel()) {
                        isCompatible = false;
                        break;
                    } else displayLevel = currentLevel + 1;
                }
                //Check if enchantments are compatible
                else if (!Enchantment.areCompatible(holder, existingHolder)) {
                    isCompatible = false;
                    break;
                }
            }
            if (!isCompatible) continue;

            //Keep the enchantment
            this.availableEnchantments.add(holder);
            this.displayEnchantments.add(Enchantment.getFullname(holder, displayLevel));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.drawCustomEnchantmentInterface(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, this.imageWidth, this.imageHeight);

        //Render animated book
        if (this.minecraft != null && this.bookModel != null) {
            float f = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float g = Mth.lerp(f, this.oOpen, this.open);
            float h = Mth.lerp(f, this.oFlip, this.flip);
            int bookX = x + 15;
            int bookY = y + 15;
            guiGraphics.submitBookModelRenderState(this.bookModel, ENCHANTING_BOOK_LOCATION, 40.0F, g, h, bookX, bookY, bookX + 38, bookY + 31);
        }
    }

    private void drawCustomEnchantmentInterface(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //Get all enchantments
        this.updateAvailableEnchantments();

        int startX = this.leftPos + 66;
        int startY = this.topPos + 14;
        int boxHeight = 57;
        int scrollerX = this.leftPos + 156;

        //Calculate scroll state
        boolean hasItem = this.menu.getSlot(0).hasItem();
        int maxScroll = hasItem ? Math.max(0, displayEnchantments.size() - 3) : 0;

        //Draw active or disabled scroller depending on item presence and list size
        if (hasItem && maxScroll > 0) {
            int travelDistance = boxHeight - 15;
            int thumbY = startY + (int) (this.scrollAmount * (float) travelDistance);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER, scrollerX, thumbY, 12, 15);

            //Change cursor if hovering over scroll thumb
            if (mouseX >= scrollerX && mouseX < scrollerX + 12 && mouseY >= thumbY && mouseY < thumbY + 15) guiGraphics.requestCursor(this.isDraggingScroller ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
        }
        else guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED, scrollerX, startY, 12, 15);

        //Don't list enchants if no item is inserted
        if (!hasItem) return;

        //Enable scissor (acts like a masking layer)
        guiGraphics.enableScissor(startX, startY, startX + 87, startY + boxHeight);

        //Tooltip variable initialization
        int hoverIndex = -1;
        EnchantmentCostHelper.EnchantmentCost hoveredCostData = null;
        boolean hoveredNeedsAnvil = false;
        boolean hoveredSpendingToken = false;
        boolean hoveredIsCurse = false;
        boolean hoveredHitEnchantmentCap = false;
        Player player = this.minecraft.player;
        assert player != null;
        ItemStack ingredientStack = this.menu.getSlot(1).getItem();
        ItemStack tool = this.menu.getSlot(0).getItem();

        //Calculate curse token balance
        int curseTokens = EnchantmentCostHelper.getAvailableCurseTokens(tool);

        //Calculate enchantment cap based on bookshelf amount
        int bookShelves = EnchantmentCostHelper.getBookShelves(player.level(), player.blockPosition());

        //Convert scroll for row selection
        int scrollOffset = maxScroll > 0 ? (int) ((double) (this.scrollAmount * (float) maxScroll) + 0.5) : 0;

        //Draw list, shifted up or down based on scroll offset
        for (int i = 0; i < displayEnchantments.size(); i++) {
            //Each button is 19 px tall, subtract the scroll offset to move them up
            int drawY = startY + (i * 19) - (scrollOffset * 19);

            if (drawY > startY - 19 && drawY < startY + boxHeight) {
                //Get tool enchantments and status
                Holder<@NotNull Enchantment> enchantment = this.availableEnchantments.get(i);
                int currentLevel = this.menu.getSlot(0).getItem().getEnchantments().getLevel(enchantment);
                int nextLevel = currentLevel + 1;
                boolean isCurse = enchantment.is(EnchantmentTags.CURSE);

                //Determine the enchantment XP and ingredient cost
                EnchantmentCostHelper.EnchantmentCost costData = EnchantmentCostHelper.calculateCost(this.menu.getSlot(0).getItem(), enchantment, nextLevel);

                //Check if anvil is required for max level enchant
                boolean needsAnvil = !player.isCreative() && costData.ingredient() == Items.ANVIL;

                //Determine enchantment cap
                boolean hitEnchantmentCap = !player.isCreative() && EnchantmentCostHelper.hasHitEnchantmentCap(tool, enchantment, bookShelves);

                //Check if the player is holding shift to spend a curse token on a normal upgrade (except creative players)
                boolean isShiftDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                boolean spendingToken = !player.isCreative() && !isCurse && curseTokens > 0 && isShiftDown;

                //Check if the player can afford the enchantment cost and are below the enchantment cap, or if they have a curse token (allow enchants with 0 cost to be bought with empty slots)
                boolean canAfford = hasTotalItems(enchantment);

                //If user is hovering over a button, highlight it and save stats for tooltips
                boolean isHovered = mouseX >= startX && mouseX <= startX + 87 && mouseY >= drawY && mouseY < drawY + 19 && mouseY >= startY && mouseY < startY + boxHeight;
                if (isHovered) {
                    hoverIndex = i;
                    hoveredCostData = costData;
                    hoveredNeedsAnvil = needsAnvil;
                    hoveredSpendingToken = spendingToken;
                    hoveredIsCurse = isCurse;
                    hoveredHitEnchantmentCap = hitEnchantmentCap;

                    //Show clicking hand if button is affordable
                    if (canAfford) guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                }

                //Draw specific button states
                Identifier buttonTexture = !canAfford ? SLOT_DISABLED : (isHovered ? SLOT_HIGHLIGHTED : SLOT);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, buttonTexture, startX, drawY, 87, 19);

                //Draw enchantment level indicator on the left side
                int visualLevel = Math.min(nextLevel, 5);
                Identifier LEVEL_SPRITE = Identifier.fromNamespaceAndPath("djsfixedprogression","container/enchanting_table/level_" + visualLevel);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LEVEL_SPRITE, startX + 2, drawY + 2, 16, 16);

                //Draw a random word in the standard galactic alphabet
                String[] words = {"djabouty", "plants", "versus", "zombies", "garden", "warfare", "best game", "bindy bo", "cucumber", "herobrine"};
                int hash = Math.abs(enchantment.unwrapKey().orElseThrow().identifier().hashCode());
                String galacticPhrase = words[hash % words.length];

                FontDescription SGA_FONT = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
                Component galacticText = Component.literal(galacticPhrase).withStyle(style -> style.withFont(SGA_FONT));
                int textColor = !canAfford ? 0xFF404040 : (isHovered ? 0xFFFFFF80 : 0xFF685E4A);
                guiGraphics.drawString(this.font, galacticText, startX + 19, drawY + 5, textColor, false);

                //Draw the ingredient, anvil, or bookshelf icon (unless the player is spending a curse token)
                if (!player.isCreative() && hitEnchantmentCap && !spendingToken) guiGraphics.renderItem(new ItemStack(Items.BOOKSHELF), startX + 66, drawY + 1);
                else if (needsAnvil && !spendingToken) guiGraphics.renderItem(new ItemStack(Items.ANVIL), startX + 66, drawY + 1);
                else if (!player.isCreative() && !spendingToken) guiGraphics.renderItem(new ItemStack(costData.ingredient()), startX + 66, drawY + 1);
            }
        }

        //Disable scissor so rest of minecraft renders normally
        guiGraphics.disableScissor();

        //Render custom enchantment tooltips
        this.renderTooltips(guiGraphics, mouseX, mouseY, hoverIndex, player, ingredientStack, hoveredCostData, hoveredNeedsAnvil, hoveredSpendingToken, hoveredIsCurse, hoveredHitEnchantmentCap, curseTokens);
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int hoverIndex, Player player, ItemStack ingredientStack, EnchantmentCostHelper.EnchantmentCost hoveredCostData, boolean hoveredNeedsAnvil, boolean hoveredSpendingToken, boolean hoveredIsCurse, boolean hoveredHitEnchantmentCap, int curseTokens) {
        if (hoverIndex == -1) return;

        //Draw hover tooltips
        Holder<@NotNull Enchantment> hoveredEnchantment = this.availableEnchantments.get(hoverIndex);
        List<Component> tooltip = new ArrayList<>();

        //Title
        tooltip.add(displayEnchantments.get(hoverIndex).copy().withStyle(ChatFormatting.GREEN));
        //Display status and explanations in tooltips
        if (hoveredIsCurse) {
            //Explain curse token system
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Dark Bargain:").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("Accepting this curse grants you").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("a Curse Token. Use this to bypass").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("an enchantment lock. This includes").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("max-level enchantments.").withStyle(ChatFormatting.GRAY));
        }
        else if (!player.isCreative() && hoveredHitEnchantmentCap && !hoveredSpendingToken) {
            //Explain bookshelf lock
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Table Power Too Low:").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Add more bookshelves to allow").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("for more unique enchantments.").withStyle(ChatFormatting.GRAY));
        }
        else if (hoveredNeedsAnvil && !hoveredSpendingToken) {
            //Explain anvil lock
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Anvil Required:").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Combine enchantments in an Anvil").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("to reach the maximum level.").withStyle(ChatFormatting.GRAY));
        }
        else if (hoveredSpendingToken) {
            //Change description if player is holding shift over a button and has a curse token
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Using Curse Token").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("- Bypasses Enchantment Lock").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("- Consumes 1 Curse Token (Free Upgrade)").withStyle(ChatFormatting.GRAY));
        }

        //Display upgrade costs (unless player is in creative, has hit an enchantment cap, is anvil-locked, or is spending a curse token)
        if (!player.isCreative() && !hoveredHitEnchantmentCap && !hoveredNeedsAnvil && !hoveredSpendingToken) {
            //Color tooltip depending on if player meets ingredient or XP cost
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Upgrade Cost:").withStyle(net.minecraft.ChatFormatting.GOLD));

            //Display XP cost
            ChatFormatting xpColor = (player.isCreative() || player.experienceLevel >= hoveredCostData.xpCost()) ? ChatFormatting.YELLOW : ChatFormatting.RED;
            tooltip.add(Component.literal("- " + hoveredCostData.xpCost() + "x Experience Level").withStyle(xpColor));

            //Display lapis cost
            ItemStack tool = this.menu.getSlot(0).getItem();
            int lapisCost = EnchantmentCostHelper.getTierMultiplier(tool);
            int lapisCount = 0;
            for (int i = 1; i < 39; i++) {
                if (this.menu.getSlot(i).getItem().is(Items.LAPIS_LAZULI)) lapisCount += this.menu.getSlot(i).getItem().getCount();
            }
            ChatFormatting lapisColor = (player.isCreative() || lapisCount >= lapisCost) ? ChatFormatting.AQUA : ChatFormatting.RED;
            tooltip.add(Component.literal("- " + lapisCost + "x Lapis Lazuli").withStyle(lapisColor));

            //Display ingredient cost
            if (hoveredCostData.ingredientAmount() > 0 && hoveredCostData.ingredient() != Items.AIR) {
                int itemCount = 0;
                for (int i = 1; i < 39; i++) {
                    ItemStack stack = this.menu.getSlot(i).getItem();
                    if (stack.is(hoveredCostData.ingredient())) itemCount += stack.getCount();
                }
                boolean hasHoveredItems = itemCount >= hoveredCostData.ingredientAmount();

                ChatFormatting itemColor = (player.isCreative() || hasHoveredItems) ? ChatFormatting.AQUA : ChatFormatting.RED;
                Component itemName = Component.translatable(hoveredCostData.ingredient().getDescriptionId());
                tooltip.add(Component.literal("- " + hoveredCostData.ingredientAmount() + "x ").append(itemName).withStyle(itemColor));
            }
        }

        //Display curse token hint if they have tokens, aren't currently using one, and if the hovered enchantment is not a curse
        if (!player.isCreative() && curseTokens > 0 && !hoveredSpendingToken && !hoveredIsCurse) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Hold SHIFT to unlock with a Curse Token.").withStyle(ChatFormatting.DARK_PURPLE));
        }

        List<ClientTooltipComponent> clientTooltipComponents = tooltip.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();

        //Render tooltip at the mouse cursor
        guiGraphics.renderTooltip(this.font, clientTooltipComponents, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        //Let vanilla handle scroll event first
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;

        //If vanilla didn't use scroll, apply it to custom list anywhere on the screen
        int maxScrollRows = this.displayEnchantments.size() - 3;
        if (maxScrollRows > 0) {
            //Scroll wheel changes percentage relative to amount of hidden items
            float rowFraction = 1.0F / (float) maxScrollRows;
            this.scrollAmount = Mth.clamp(this.scrollAmount - (float) scrollY * rowFraction, 0.0F, 1.0F);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        //Calculate dimensions
        int startX = this.leftPos + 66;
        int startY = this.topPos + 14;
        int scrollerX = this.leftPos + 156;

        //Check if the user clicks anywhere inside the enchant UI area
        if (mouseButtonEvent.x() >= startX && mouseButtonEvent.x() <= startX + 107 && mouseButtonEvent.y() >= startY && mouseButtonEvent.y() <= startY + 57) {
            //Only allow left clicks
            if (mouseButtonEvent.button() != 0) return true;

            //Check if scrollbar track is clicked
            if (mouseButtonEvent.x() >= scrollerX && mouseButtonEvent.x() <= scrollerX + 12) {
                this.isDraggingScroller = true;
                this.setDragging(true);
                return true;
            }

            //CHeck if text area is clicked
            if (mouseButtonEvent.x() >= startX && mouseButtonEvent.x() < startX + 87) {
                double relativeY = mouseButtonEvent.y() - startY;

                if (relativeY >= 0) {
                    //Determine scroll offset based of percentage
                    int maxScrollRows = this.displayEnchantments.size() - 3;
                    int itemScrollOffset = maxScrollRows > 0 ? (int) ((double) (this.scrollAmount * (float) maxScrollRows) + 0.5) : 0;

                    //Divide by 19 (the height of each button) and add scroll offset
                    int clickedIndex = (int) (relativeY / 19) + itemScrollOffset;

                    //Ensure they didn't click empty space below list
                    if (clickedIndex >= 0 && clickedIndex < this.availableEnchantments.size()) {
                        Player player = this.minecraft.player;
                        assert player != null;
                        Holder<@NotNull Enchantment> selectedEnchantment = this.availableEnchantments.get(clickedIndex);
                        Identifier enchantmentId = selectedEnchantment.unwrapKey().orElseThrow().identifier();

                        //Check if the player has a curse token
                        boolean isShiftDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                        boolean spendingToken = !this.minecraft.player.isCreative() && !selectedEnchantment.is(EnchantmentTags.CURSE) && EnchantmentCostHelper.getAvailableCurseTokens(this.menu.getSlot(0).getItem()) > 0 && isShiftDown;

                        //Check if the player has the correct ingredients in the slots
                        if (canAffordInSlots(selectedEnchantment)) {
                            //Send purchase packet to the server and play a UI button click sound
                            ClientPlayNetworking.send(new EnchantmentSelectPayload(enchantmentId, spendingToken));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        //If the player has the ingredients in their inventory
                        else if (hasTotalItems(selectedEnchantment)) {
                            //Send auto-move packet to the server and play a UI button click sound
                            ClientPlayNetworking.send(new EnchantingAutoMovePayload(enchantmentId, spendingToken));
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                    }
                }
            }
            return true;
        }
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        if (this.isDraggingScroller) {
            int startY = this.topPos + 14;

            //Smooth percentage mapping from pixel coordinates
            float scrollPercentage = ((float) mouseButtonEvent.y() - (float) startY - 7.5F) / 43.0F;
            this.scrollAmount = Mth.clamp(scrollPercentage, 0.0F, 1.0F);

            //Consume the drag event
            return true;
        }
        return super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.button() == 0) {
            this.isDraggingScroller = false;
            this.setDragging(false);
        }
        return super.mouseReleased(mouseButtonEvent);
    }

    /**
     * Method to calculate if the player can afford an enchantment.
     */
    private boolean checkAffordability(Holder<@NotNull Enchantment> enchantment, boolean checkTotalInventory) {
        if (this.minecraft == null || this.minecraft.player == null) return false;
        Player player = this.minecraft.player;

        //Creative players bypass all costs and locks
        if (player.isCreative()) return true;

        //Ensure tool is being enchanted
        ItemStack tool = this.menu.getSlot(0).getItem();
        if (tool.isEmpty()) return false;

        //Get enchantment details
        int nextLevel = tool.getEnchantments().getLevel(enchantment) + 1;
        boolean isCurse = enchantment.is(EnchantmentTags.CURSE);

        //Check anvil and bookshelf locks
        int bookShelves = EnchantmentCostHelper.getBookShelves(player.level(), player.blockPosition());
        boolean hitEnchantmentCap = EnchantmentCostHelper.hasHitEnchantmentCap(tool, enchantment, bookShelves);

        //Check if curse token is being used
        int curseTokens = EnchantmentCostHelper.getAvailableCurseTokens(tool);
        boolean isShiftDown = InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean spendingToken = !isCurse && curseTokens > 0 && isShiftDown;

        //Calculate ingredient cost
        EnchantmentCostHelper.EnchantmentCost costData = EnchantmentCostHelper.calculateCost(tool, enchantment, nextLevel);
        int lapisCost = EnchantmentCostHelper.getTierMultiplier(tool);
        boolean needsAnvil = costData.ingredient() == Items.ANVIL;

        //Using a token bypasses all ingredient/XP requirements and locks
        if (spendingToken) return true;

        //If the enchantment is capped from an anvil lock or max enchantment level, return
        if (needsAnvil || hitEnchantmentCap) return false;

        //Ensure player has enough XP
        if (player.experienceLevel < costData.xpCost()) return false;

        //Check if the lapis slot has lapis and ingredient slot has the required item
        if (!checkTotalInventory) {
            ItemStack lapisStack = this.menu.getSlot(1).getItem();
            ItemStack ingredientStack = this.menu.getSlot(2).getItem();

            boolean hasLapis = lapisStack.is(Items.LAPIS_LAZULI) && lapisStack.getCount() >= lapisCost;
            boolean hasIngredient = costData.ingredientAmount() <= 0 || (ingredientStack.is(costData.ingredient()) && ingredientStack.getCount() >= costData.ingredientAmount());

            return hasLapis && hasIngredient;
        }
        //Check inventory for ingredients and lapis
        else {
            int lapisCount = 0;
            int ingredientCount = 0;

            //Loop over all inventory slots
            for (int i = 1; i < 39; i++) {
                ItemStack stack = this.menu.getSlot(i).getItem();
                if (stack.is(Items.LAPIS_LAZULI)) lapisCount += stack.getCount();
                if (stack.is(costData.ingredient())) ingredientCount += stack.getCount();
            }

            boolean hasLapis = lapisCount >= lapisCost;
            boolean hasIngredient = costData.ingredientAmount() <= 0 || ingredientCount >= costData.ingredientAmount();

            return hasLapis && hasIngredient;
        }
    }

    /**
     * Helper method for checking if player has put required ingredients in the payment slot.
     */
    private boolean canAffordInSlots(Holder<@NotNull Enchantment> enchantment) {
        return checkAffordability(enchantment, false);
    }

    /**
     * Helper method for check if player has required ingredients in inventory.
     */
    private boolean hasTotalItems(Holder<@NotNull Enchantment> enchantment) {
        return checkAffordability(enchantment, true);
    }
}
