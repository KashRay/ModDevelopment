package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class UpdateCopperArmorStatsProcedure {

    public static void execute(Entity entity, ItemStack itemStack, EquipmentSlot armorSlot) {
        //Ensure item is armor before proceeding
        if (itemStack == null || itemStack.isEmpty()) return;
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return;

        //Get max damage and current damage for calculations
        double max_durability = itemStack.getMaxDamage();
        double current_damage = itemStack.getDamageValue();

        //Prevent breaking if item is unbreakable
        if (max_durability == 0) return;

        //Calculate damage percentage
        double damage_percent = (current_damage / max_durability) * 100.0;

        //Define the stat bonuses
        double bonusArmor = 0;
        double bonusKnockback = 0;

        //Define 3D equipment asset name
        Identifier assetId;

        //Check which stage the armor falls under and assign respective armor bonuses and textures
        if (damage_percent < 25) {
            //Stage 1
            bonusArmor = 0;
            assetId = Identifier.fromNamespaceAndPath("minecraft", "copper");
        }
        else if (damage_percent < 50) {
            //Stage 2
            if (armorSlot == EquipmentSlot.HEAD || armorSlot == EquipmentSlot.FEET) bonusArmor = 1.0;
            else if (armorSlot == EquipmentSlot.CHEST || armorSlot == EquipmentSlot.LEGS) bonusArmor = 2.0;
            assetId = Identifier.fromNamespaceAndPath("djsfixedprogression", "exposed_copper");
        }
        else if (damage_percent < 75) {
            //Stage 3
            if (armorSlot == EquipmentSlot.HEAD || armorSlot == EquipmentSlot.FEET) bonusArmor = 1.0;
            else if (armorSlot == EquipmentSlot.CHEST || armorSlot == EquipmentSlot.LEGS) bonusArmor = 3.0;
            assetId = Identifier.fromNamespaceAndPath("djsfixedprogression", "weathered_copper");
        }
        else {
            //Stage 4
            if (armorSlot == EquipmentSlot.HEAD || armorSlot == EquipmentSlot.FEET) bonusArmor = 1.0;
            else if (armorSlot == EquipmentSlot.CHEST || armorSlot == EquipmentSlot.LEGS) bonusArmor = 4.0;
            bonusKnockback = 0.2;
            assetId = Identifier.fromNamespaceAndPath("djsfixedprogression", "oxidized_copper");
        }

        //If the armor is already wearing the correct texture for its stage, stop processing
        if (equippable.assetId().isPresent() && equippable.assetId().get().identifier().equals(assetId)) return;

        //Create the registry key and resource key pointing to the new equipment asset
        ResourceKey<@NotNull Registry<@NotNull EquipmentAsset>> equipmentAssetRegistry = ResourceKey.createRegistryKey(Identifier.parse("equipment_asset"));
        ResourceKey<@NotNull EquipmentAsset> newAssetKey = ResourceKey.create(equipmentAssetRegistry, assetId);

        //Build a new equipable component, copying old properties but replacing the asset ID
        Equippable newEquippable = new Equippable(
                equippable.slot(),
                equippable.equipSound(),
                java.util.Optional.of(newAssetKey),
                equippable.cameraOverlay(),
                equippable.allowedEntities(),
                true,
                true,
                true,
                true,
                false,
                equippable.equipSound()
        );

        //Save the new worn texture component back to the item
        itemStack.set(DataComponents.EQUIPPABLE, newEquippable);

        //Define unique IDs for custom modifiers (avoid overwriting vanilla ones)
        Identifier armorModifierId = Identifier.parse("djsfixedprogression:copper_armor_boost");
        Identifier knockbackModifierId = Identifier.parse("djsfixedprogression:copper_knockback_boost");

        //Get existing modifiers and prepare a new builder
        ItemAttributeModifiers currentModifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        //Keep all existing modifiers except custom copper ones (to prevent infinite stacking on repairs)
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().equals(armorModifierId) && !entry.modifier().id().equals(knockbackModifierId)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        //Apply new modifiers if the armor is past stage 1
        if (bonusArmor > 0) builder.add(Attributes.ARMOR, new AttributeModifier(armorModifierId, bonusArmor, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);
        if (bonusKnockback > 0) builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(knockbackModifierId, bonusKnockback, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);

        //Save new components back to item
        itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());

        //Tell server to broadcast the updated item to the player's screen
        if (entity instanceof Player player) {
            player.setItemSlot(armorSlot, itemStack.copy());
        }
    }
}
