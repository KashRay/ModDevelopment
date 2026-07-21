package djabouty47.djsfixedprogression.datagen;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.NotNull;

public class ModModelGenerator extends FabricModelProvider {
    public ModModelGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(@NotNull BlockModelGenerators blockModelGenerators) {}

    @Override
    public void generateItemModels(@NotNull ItemModelGenerators itemModelGenerator) {
        //Generate basic item models
        itemModelGenerator.generateFlatItem(DJsFixedProgression.END_TRIAL_KEY, ModelTemplates.FLAT_ITEM);
        itemModelGenerator.generateFlatItem(DJsFixedProgression.OMINOUS_END_TRIAL_KEY, ModelTemplates.FLAT_ITEM);

        //Generate standard trims for base vanilla copper armor (stage 1)
        itemModelGenerator.generateTrimmableItem(Items.COPPER_HELMET, EquipmentAssets.COPPER, ItemModelGenerators.TRIM_PREFIX_HELMET, false);
        itemModelGenerator.generateTrimmableItem(Items.COPPER_CHESTPLATE, EquipmentAssets.COPPER, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE, false);
        itemModelGenerator.generateTrimmableItem(Items.COPPER_LEGGINGS, EquipmentAssets.COPPER, ItemModelGenerators.TRIM_PREFIX_LEGGINGS, false);
        itemModelGenerator.generateTrimmableItem(Items.COPPER_BOOTS, EquipmentAssets.COPPER, ItemModelGenerators.TRIM_PREFIX_BOOTS, false);

        //Automate all custom stages and trims
        String[] stages = {"exposed", "weathered", "oxidized"};
        String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
        String[] trimMaterials = {"quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst", "resin"};
        for (String stage : stages) {
            for (String piece : armorPieces) {
                //Generate untrimmed custom stage model
                Identifier baseTexture = Identifier.parse("djsfixedprogression:item/" + stage + "_copper_" + piece);
                ModelTemplates.FLAT_ITEM.create(baseTexture, TextureMapping.layer0(baseTexture), itemModelGenerator.modelOutput);

                //Loop through all materials to generate layered trim models
                for (String material : trimMaterials) {
                    //Name of file to create
                    Identifier modelId = Identifier.parse("djsfixedprogression:item/" + stage + "_copper_" + piece + "_" + material + "_trim");

                    //Vanilla trim overlay texture
                    Identifier trimTexture = Identifier.parse("minecraft:trims/items/" + piece + "_trim_" + material);

                    //Create 2-layer json file
                    ModelTemplates.TWO_LAYERED_ITEM.create(modelId, TextureMapping.layered(baseTexture, trimTexture), itemModelGenerator.modelOutput);
                }
            }
        }
    }
}
