package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateCopperToolStatsProcedure {

    public static void execute(LivingEntity entity, ItemStack itemStack) {
        //Ensure item exists before proceeding
        if (itemStack == null || itemStack.isEmpty()) return;

        //Get max damage and current damage for calculations
        double max_durability = itemStack.getMaxDamage();
        double current_damage = itemStack.getDamageValue();

        //Prevent breaking if item is unbreakable
        if (max_durability == 0) return;

        //Calculate the damage percentage
        double damage_percentage = (current_damage / max_durability) * 100;

        //Define the stat bonuses
        float miningSpeed;
        double bonusDamage;
        TagKey<@NotNull Block> incorrectTierTag;

        //Check which stage the tool falls under and assign respective tool bonuses
        if (damage_percentage < 25) {
            //Stage 1
            miningSpeed = 4.0f;
            bonusDamage = 0;
            incorrectTierTag = BlockTags.INCORRECT_FOR_STONE_TOOL;
        }
        else if (damage_percentage < 50) {
            //Stage 2
            miningSpeed = 5.0f;
            bonusDamage = 1.0;
            incorrectTierTag = BlockTags.INCORRECT_FOR_STONE_TOOL;
        }
        else if (damage_percentage < 75) {
            //Stage 3
            miningSpeed = 6.0f;
            bonusDamage = 2.0;
            incorrectTierTag = BlockTags.INCORRECT_FOR_STONE_TOOL;
        }
        else {
            miningSpeed = 8.0f;
            bonusDamage = 3.0;
            incorrectTierTag = BlockTags.INCORRECT_FOR_IRON_TOOL;
        }

        //Find out what bonus damage the tool currently has
        double currentBonus = 0;
        ItemAttributeModifiers currentModifiers = itemStack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Identifier damageModifierId = Identifier.fromNamespaceAndPath("djsfixedprogression", "copper_tool_damage_boost");
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            if (entry.modifier().id().equals(damageModifierId)) currentBonus = entry.modifier().amount();
        }

        //If the current bonus exactly matches the stage it should be in, stop processing
        if (currentBonus == bonusDamage) return;

        //Determine the tool type to ensure correct mining animation
        TagKey<@NotNull Block> minableTag = BlockTags.MINEABLE_WITH_PICKAXE;
        if (itemStack.is(Items.COPPER_AXE)) minableTag = BlockTags.MINEABLE_WITH_AXE;
        else if (itemStack.is(Items.COPPER_SHOVEL)) minableTag = BlockTags.MINEABLE_WITH_SHOVEL;
        else if (itemStack.is(Items.COPPER_HOE)) minableTag = BlockTags.MINEABLE_WITH_HOE;
        else if (itemStack.is(Items.COPPER_SWORD) || itemStack.is(Items.COPPER_SPEAR)) minableTag = BlockTags.SWORD_INSTANTLY_MINES;

        //Get the block registry from the entity's level to convert TagKeys into HolderSets
        Registry<@NotNull Block> blockRegistry = entity.level().registryAccess().lookupOrThrow(Registries.BLOCK);
        HolderSet.Named<@NotNull Block> incorrectTierHolder = blockRegistry.getOrThrow(incorrectTierTag);
        HolderSet.Named<@NotNull Block> minableHolder = blockRegistry.getOrThrow(minableTag);

        //Rebuild tool data component (controls block breaking)
        List<Tool.Rule> rules = List.of(
                Tool.Rule.deniesDrops(incorrectTierHolder),
                Tool.Rule.minesAndDrops(minableHolder, miningSpeed)
        );

        //Grab the existing tool to inherit its default properties
        Tool currentTool = itemStack.get(DataComponents.TOOL);
        if (currentTool != null) {
            //Pass new rules, but copy rest of arguments from the vanilla item
            Tool newToolComponent = new Tool(rules, currentTool.defaultMiningSpeed(), currentTool.damagePerBlock(), currentTool.canDestroyBlocksInCreative());
            itemStack.set(DataComponents.TOOL, newToolComponent);
        }

        //Rebuild the attribute modifiers (controls combat damage)
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        //Keep existing vanilla stats
        for (ItemAttributeModifiers.Entry entry : currentModifiers.modifiers()) {
            if (!entry.modifier().id().equals(damageModifierId)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        //Apply bonus damage cleanly on top
        if (bonusDamage > 0) builder.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(damageModifierId, bonusDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

        //Save the new components back to the item
        itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());

        //Tell the server to broadcast the updated item to the player's screen
        if (entity instanceof Player player) {
            if (player.getMainHandItem() == itemStack) player.setItemSlot(EquipmentSlot.MAINHAND, itemStack.copy());
            else if (player.getOffhandItem() == itemStack) player.setItemSlot(EquipmentSlot.OFFHAND, itemStack.copy());
        }
    }
}
