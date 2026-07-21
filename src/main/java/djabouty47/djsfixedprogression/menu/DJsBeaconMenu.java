package djabouty47.djsfixedprogression.menu;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconNode;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTreeRegistry;
import djabouty47.djsfixedprogression.procedures_and_util.IUpgradableBeacon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class DJsBeaconMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final SimpleContainer paymentContainer = new SimpleContainer(3);
    private CompoundTag clientUpgrades = new CompoundTag();

    /**
     * Client-side constructor.
     */
    public DJsBeaconMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /**
     * Server-side constructor.
     */
    public DJsBeaconMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(DJsFixedProgression.DJS_BEACON_MENU, containerId);
        this.access = access;

        //3 ingredient slots
        this.addSlot(new Slot(this.paymentContainer, 0, 36, 103));
        this.addSlot(new Slot(this.paymentContainer, 1, 54, 103));
        this.addSlot(new Slot(this.paymentContainer, 2, 72, 103));

        //Inventory size
        int invX = 36;
        int invY = 137;

        //Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        //Player hotbar
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, Blocks.BEACON);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack oldStack = slot.getItem();
            newStack = oldStack.copy();

            //If clicking a payment slot, move to inventory
            if (index < 3) {
                if (!this.moveItemStackTo(oldStack, 3, 39, false)) return ItemStack.EMPTY;
            }
            //If clicking inventory, move to payment slots
            else {
                if (!this.moveItemStackTo(oldStack, 0, 3, false)) return ItemStack.EMPTY;
            }

            if (oldStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return newStack;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.clearContainer(player, this.paymentContainer);
    }

    /**
     * Helper method to safely set NBT upgrades for the current beacon.
     */
    public void setClientUpgrades(CompoundTag tag) {
        this.clientUpgrades = tag;
    }

    /**
     * Helper method to safely grab NBT upgrades from the current beacon.
     */
    public CompoundTag getUpgrades() {
        //If client asks for upgrades, give it synced cache
        if (this.access == ContainerLevelAccess.NULL) return this.clientUpgrades;

        //Let server safely read from the block
        return this.access.evaluate((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof IUpgradableBeacon upgradableBeacon) {
                //Copy NBT
                CompoundTag tag = upgradableBeacon.djs$getUpgrades().copy();

                //Inject calculate layers into sync payload
                tag.putInt("djs_beacon_level", IUpgradableBeacon.getBeaconLevel(level, pos));

                //Inject coordinates of beacon for map anchoring
                tag.putInt("beacon_x", pos.getX());
                tag.putInt("beacon_z", pos.getZ());

                return tag;
            }
            return new CompoundTag();
        }).orElse(new CompoundTag());
    }

    /**
     * Method for server to call when a network packet is received.
     */
    public void tryPurchasedUpgrade(String nodeId, Player player) {
        BeaconNode node = BeaconTreeRegistry.NODES.get(nodeId);
        if (node == null) return;

        //Verify item costs on server
        this.access.execute((level, pos) -> {
            if (level.getBlockEntity(pos) instanceof IUpgradableBeacon upgradableBeacon) {
                //Get beacon upgrades
                CompoundTag tag = upgradableBeacon.djs$getUpgrades();

                //Check if upgrade is owned
                boolean isOwned = tag.contains(nodeId);

                //Check if upgrade is an active ability
                boolean isAbility = BeaconTreeRegistry.ABILITIES.containsKey(node.id());

                //Get node cost
                List<BeaconNode.NodeCost> costToPay = node.costs();

                //Check if activating an owned ability
                if (isOwned && isAbility) {
                    //Fetch ability data from registry
                    BeaconTreeRegistry.AbilityData abilityData = BeaconTreeRegistry.ABILITIES.get(nodeId);
                    if (abilityData == null) return;

                    //Calculate the cooldown
                    long currentTime = level.getGameTime();
                    long cooldownEnd = tag.getLongOr(nodeId + "_cooldown_end", 0);

                    //Abort if ability is still on cooldown
                    if (currentTime < cooldownEnd) return;

                    //Get ability use cost
                    costToPay = abilityData.usageCosts();
                }
                //Prevent repurchasing of owned passives
                else if (isOwned) return;

                //Ensure player is not in creative
                if (!player.isCreative()) {
                    //Verify ingredient cost is met
                    for (BeaconNode.NodeCost cost : costToPay) {
                        int count = 0;
                        for (int i = 0; i < 3; i++) {
                            ItemStack stack = this.paymentContainer.getItem(i);
                            if (stack.is(cost.item())) count += stack.getCount();
                        }
                        //If player cannot afford upgrade, abort
                        if (count < cost.count()) return;
                    }

                    //Deduct items
                    for (BeaconNode.NodeCost cost : costToPay) {
                        int remainingToDeduct = cost.count();
                        for (int i = 0; i < 3 && remainingToDeduct > 0; i++) {
                            ItemStack stack = this.paymentContainer.getItem(i);
                            if (stack.is(cost.item())) {
                                int deducted = Math.min(stack.getCount(), remainingToDeduct);
                                stack.shrink(deducted);
                                remainingToDeduct -= deducted;
                            }
                        }
                    }
                }

                //Record newly purchased upgrades
                if (!isOwned) {
                    //Unlock upgrade
                    tag.putBoolean(nodeId, true);
                }
                //Record exact game tick that active ability will expire and cooldown
                else {
                    BeaconTreeRegistry.AbilityData abilityData = BeaconTreeRegistry.ABILITIES.get(nodeId);
                    long currentTime = level.getGameTime();
                    tag.putLong(nodeId + "_duration_end", currentTime + abilityData.durationTicks());
                    tag.putLong(nodeId + "_cooldown_end", currentTime + abilityData.cooldownTicks());

                    //Run ability
                    runAbilities(nodeId, level, pos, tag, abilityData);
                }

                //Update beacon tags
                upgradableBeacon.djs$setUpgrades(tag);

                //Mark block as dirty and force it to sync the new NBT to the client
                Objects.requireNonNull(level.getBlockEntity(pos)).setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);

                //Update tracker so ability takes effect locally on this exact tick
                BeaconTracker.updateBeacon(level, pos, IUpgradableBeacon.getBeaconLevel(level, pos), tag);
            }
        });
    }

    /**
     * Method called when player clicks a node but doesn't have the items in the payment slots yet.
     * Moves items from their inventory into the ingredient slots.
     */
    public void tryAutoMoveItems(String nodeId, Player player) {
        //Creative players don't need to move items
        if (player.isCreative()) return;

        //Get node being clicked
        BeaconNode node = BeaconTreeRegistry.NODES.get(nodeId);
        if (node == null) return;

        //Get upgrades
        CompoundTag tag = this.getUpgrades();

        //Get upgrade state
        boolean isOwned = tag.contains(nodeId);
        boolean isAbility = BeaconTreeRegistry.ABILITIES.containsKey(nodeId);

        //Get node cost
        List<BeaconNode.NodeCost> costToPay = node.costs();

        //Check if node is an ability
        if (isOwned && isAbility) {
            BeaconTreeRegistry.AbilityData abilityData = BeaconTreeRegistry.ABILITIES.get(nodeId);
            if (abilityData != null) costToPay = abilityData.usageCosts();
        }
        //Passive abilities don't have usage costs
        else if (isOwned) return;

        //Prevent free upgrades from clearing slots
        if (costToPay.isEmpty()) return;

        //Return any items currently in payment slots back to player's inventory
        boolean allReturned = true;
        for (int i = 0; i < 3; i++) {
            Slot slot = this.slots.get(i);
            if (slot.hasItem()) {
                ItemStack currentIngredient = slot.getItem();

                //Attempt to move item natively
                this.moveItemStackTo(currentIngredient, 3, 39, false);

                //Check if current ingredient is empty
                if (!currentIngredient.isEmpty()) allReturned = false;
            }
        }

        //Ensure all payment slots are empty
        if (allReturned) {
            //Extract required items from player's inventory and place them in the payment slots
            for (int i = 0; i < costToPay.size() && i < 3; i++) {
                Slot paymentSlot = scanInventoryForIngredientCosts(costToPay, i);
                paymentSlot.setChanged();
            }
        }

        //Sync newly moved items to the client screen
        this.broadcastChanges();
    }

    /**
     * Helper method that finds all required ingredients in the players inventory.
     */
    private @NotNull Slot scanInventoryForIngredientCosts(List<BeaconNode.NodeCost> costToPay, int i) {
        BeaconNode.NodeCost cost = costToPay.get(i);
        int needed = cost.count();

        Slot paymentSlot = this.slots.get(i);
        ItemStack newPayment = ItemStack.EMPTY;

        //Loop over all inventory slots
        for (int j = 3; j < 39 && needed > 0; j++) {
            Slot inventorySlot = this.slots.get(j);

            //Check if inventory slot has the required item
            if (inventorySlot.hasItem() && inventorySlot.getItem().is(cost.item())) {
                //Determine the amount of the ingredients to take
                ItemStack inventoryStack = inventorySlot.getItem();
                int take = Math.min(inventoryStack.getCount(), needed);

                //Transfer the amounts
                inventoryStack.shrink(take);
                if (newPayment.isEmpty()) newPayment = new ItemStack(cost.item(), take);
                else newPayment.grow(take);
                needed -= take;

                //Tell the menu this specific inventory slot was modified
                inventorySlot.setChanged();
            }
        }
        //Tell the menu the payment slot was modified
        paymentSlot.set(newPayment);
        return paymentSlot;
    }

    /**
     * Helper method to run certain beacon abilities on the server.
     */
    private static void runAbilities(String nodeId, Level level, BlockPos pos, CompoundTag upgrades, BeaconTreeRegistry.AbilityData abilityData) {
        if (level instanceof ServerLevel serverLevel) {
            //Calculate beacon radius
            int customLevels = IUpgradableBeacon.getBeaconLevel(level, pos);
            double radius = customLevels * 20 + 20;
            AABB aabb = new AABB(pos).inflate(radius).expandTowards(0.0D, level.getHeight(), 0.0D);

            switch (nodeId) {
                //Trigger a thunderstorm
                case "set_weather" -> serverLevel.setWeatherParameters(0, abilityData.durationTicks(), true, true);

                //Give fire resistance
                case "fire_resistance" -> {
                    //Apply to players
                    List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
                    for (Player player : players) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, abilityData.durationTicks(), 0, true, true));

                    //If pet effects are active, apply to pets too
                    if (upgrades.contains("pet_effects")) {
                        List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, aabb);
                        for (TamableAnimal pet : pets) {
                            if (pet.isTame()) pet.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, abilityData.durationTicks(), 0, true, true));
                        }

                        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, aabb);
                        for (AbstractHorse horse : horses) {
                            if (horse.isTamed()) horse.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, abilityData.durationTicks(), 0, true, true));
                        }

                        List<HappyGhast> happyGhasts = level.getEntitiesOfClass(HappyGhast.class, aabb);
                        for (HappyGhast happyGhast : happyGhasts) happyGhast.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, abilityData.durationTicks(), 0, true, true));
                    }
                }

                //Give slow falling
                case "give_slowfalling" -> {
                    //Apply to players
                    List<Player> players = level.getEntitiesOfClass(Player.class, aabb);
                    for (Player player : players) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, abilityData.durationTicks(), 0, true, true));

                    //If pet effects are active, apply to pets too
                    if (upgrades.contains("pet_effects")) {
                        List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, aabb);
                        for (TamableAnimal pet : pets) {
                            if (pet.isTame()) pet.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, abilityData.durationTicks(), 0, true, true));
                        }

                        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, aabb);
                        for (AbstractHorse horse : horses) {
                            if (horse.isTamed()) horse.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, abilityData.durationTicks(), 0, true, true));
                        }
                    }
                }
            }
        }
    }
}
