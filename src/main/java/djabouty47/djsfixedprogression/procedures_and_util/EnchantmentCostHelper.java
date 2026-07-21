package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentCostHelper {
    //The central database mapping enchantments to their specific level requirements
    private static final Map<ResourceKey<@NotNull Enchantment>, LevelRequirement[]> ENCHANTMENT_REQUIREMENTS = new HashMap<>();

    /**
     * A simple record to hold all calculated values together.
     */
    public record EnchantmentCost(Item ingredient, int ingredientAmount, int xpCost) {}

    /**
     * A private record specifically for lookup table.
     */
    public record LevelRequirement(Item item, int baseAmount) {}

    //Define arrays for each level (lowest tier = * 1, highest tier = * 8)
    static {
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.AQUA_AFFINITY, new LevelRequirement[]{
                new LevelRequirement(Items.SALMON, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.BANE_OF_ARTHROPODS, new LevelRequirement[]{
                new LevelRequirement(Items.SPIDER_EYE, 4),
                new LevelRequirement(Items.FERMENTED_SPIDER_EYE, 2),
                new LevelRequirement(Items.CRIMSON_ROOTS, 4),
                new LevelRequirement(Items.OPEN_EYEBLOSSOM, 1),
                new LevelRequirement(Items.ANVIL, 1),
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.BLAST_PROTECTION, new LevelRequirement[]{
                new LevelRequirement(Items.GUNPOWDER, 4),
                new LevelRequirement(Items.TNT, 2),
                new LevelRequirement(Items.OBSIDIAN, 1),
                new LevelRequirement(Items.END_CRYSTAL, 1),
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.BREACH, new LevelRequirement[]{
                new LevelRequirement(Items.CRACKED_STONE_BRICKS, 4),
                new LevelRequirement(Items.QUARTZ, 8),
                new LevelRequirement(Items.CRACKED_DEEPSLATE_BRICKS, 4),
                new LevelRequirement(Items.POINTED_DRIPSTONE, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.CHANNELING, new LevelRequirement[]{
                new LevelRequirement(Items.LIGHTNING_ROD, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.DENSITY, new LevelRequirement[]{
                new LevelRequirement(Items.GRAVEL, 8),
                new LevelRequirement(Items.DEEPSLATE, 4),
                new LevelRequirement(Items.GOLD_BLOCK, 1),
                new LevelRequirement(Items.PACKED_MUD, 4),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.DEPTH_STRIDER, new LevelRequirement[]{
                new LevelRequirement(Items.COPPER_GRATE, 4),
                new LevelRequirement(Items.SUGAR, 5),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.EFFICIENCY, new LevelRequirement[]{
                new LevelRequirement(Items.SUGAR, 4),
                new LevelRequirement(Items.REDSTONE, 8),
                new LevelRequirement(Items.GOLDEN_CARROT, 4),
                new LevelRequirement(Items.ECHO_SHARD, 1),
                new LevelRequirement(Items.ANVIL, 1),
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FEATHER_FALLING, new LevelRequirement[]{
                new LevelRequirement(Items.FEATHER, 6),
                new LevelRequirement(Items.COBWEB, 2),
                new LevelRequirement(Items.PHANTOM_MEMBRANE, 6),
                new LevelRequirement(Items.ANVIL, 1),
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FIRE_ASPECT, new LevelRequirement[]{
                new LevelRequirement(Items.FIRE_CHARGE, 4),
                new LevelRequirement(Items.TORCHFLOWER, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FIRE_PROTECTION, new LevelRequirement[]{
                new LevelRequirement(Items.GLISTERING_MELON_SLICE, 4),
                new LevelRequirement(Items.MAGMA_CREAM, 3),
                new LevelRequirement(Items.BLAZE_POWDER, 8),
                new LevelRequirement(Items.POPPED_CHORUS_FRUIT, 4),
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FLAME, new LevelRequirement[]{
                new LevelRequirement(Items.FIRE_CHARGE, 6)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FORTUNE, new LevelRequirement[]{
                new LevelRequirement(Items.DIAMOND, 2),
                new LevelRequirement(Items.RABBIT_FOOT, 1),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.FROST_WALKER, new LevelRequirement[]{
                new LevelRequirement(Items.SNOWBALL, 6),
                new LevelRequirement(Items.PACKED_ICE, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.IMPALING, new LevelRequirement[]{
                new LevelRequirement(Items.ARROW, 5),
                new LevelRequirement(Items.PUFFERFISH, 3),
                new LevelRequirement(Items.POINTED_DRIPSTONE, 4),
                new LevelRequirement(Items.PRISMARINE_SHARD, 4),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.INFINITY, new LevelRequirement[]{
                new LevelRequirement(Items.SPECTRAL_ARROW, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.KNOCKBACK, new LevelRequirement[]{
                new LevelRequirement(Items.SLIME_BLOCK, 4),
                new LevelRequirement(Items.WIND_CHARGE, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.LOOTING, new LevelRequirement[]{
                new LevelRequirement(Items.PITCHER_PLANT, 1),
                new LevelRequirement(Items.RABBIT_FOOT, 1),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.LOYALTY, new LevelRequirement[]{
                new LevelRequirement(Items.POPPY, 3),
                new LevelRequirement(Items.OPEN_EYEBLOSSOM, 1),
                new LevelRequirement(Items.SPECTRAL_ARROW, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.LUCK_OF_THE_SEA, new LevelRequirement[]{
                new LevelRequirement(Items.RABBIT_FOOT, 1),
                new LevelRequirement(Items.LILY_PAD, 2),
                new LevelRequirement(Items.NAUTILUS_SHELL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.LUNGE, new LevelRequirement[]{
                new LevelRequirement(Items.SUGAR, 4),
                new LevelRequirement(Items.LEAD, 4),
                new LevelRequirement(Items.WIND_CHARGE, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.LURE, new LevelRequirement[]{
                new LevelRequirement(Items.COD, 4),
                new LevelRequirement(Items.SALMON, 4),
                new LevelRequirement(Items.PUFFERFISH, 4)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.MENDING, new LevelRequirement[]{
                new LevelRequirement(Items.SCULK, 4)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.MULTISHOT, new LevelRequirement[]{
                new LevelRequirement(Items.DISPENSER, 4)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.PIERCING, new LevelRequirement[]{
                new LevelRequirement(Items.ARROW, 4),
                new LevelRequirement(Items.POINTED_DRIPSTONE, 2),
                new LevelRequirement(Items.PRISMARINE_SHARD, 1),
                new LevelRequirement(Items.PUFFERFISH, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.POWER, new LevelRequirement[]{
                new LevelRequirement(Items.TARGET, 1),
                new LevelRequirement(Items.GLOWSTONE_DUST, 8),
                new LevelRequirement(Items.CRIMSON_FUNGUS, 2),
                new LevelRequirement(Items.DRAGON_BREATH, 3),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.PROJECTILE_PROTECTION, new LevelRequirement[]{
                new LevelRequirement(Items.HAY_BLOCK, 4),
                new LevelRequirement(Items.WHITE_WOOL, 6),
                new LevelRequirement(Items.TARGET, 1),
                new LevelRequirement(Items.TURTLE_SCUTE, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.PROTECTION, new LevelRequirement[]{
                new LevelRequirement(Items.IRON_BLOCK, 2),
                new LevelRequirement(Items.OBSIDIAN, 2),
                new LevelRequirement(Items.ARMADILLO_SCUTE, 4),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.PUNCH, new LevelRequirement[]{
                new LevelRequirement(Items.SLIME_BLOCK, 4),
                new LevelRequirement(Items.WIND_CHARGE, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.QUICK_CHARGE, new LevelRequirement[]{
                new LevelRequirement(Items.SUGAR, 4),
                new LevelRequirement(Items.DISPENSER, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.RESPIRATION, new LevelRequirement[]{
                new LevelRequirement(Items.PUFFERFISH, 4),
                new LevelRequirement(Items.SEA_PICKLE, 1),
                new LevelRequirement(Items.TUBE_CORAL_FAN, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.RIPTIDE, new LevelRequirement[]{
                new LevelRequirement(Items.FIREWORK_ROCKET, 4),
                new LevelRequirement(Items.WIND_CHARGE, 1),
                new LevelRequirement(Items.SPECTRAL_ARROW, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SHARPNESS, new LevelRequirement[]{
                new LevelRequirement(Items.FLINT, 8),
                new LevelRequirement(Items.QUARTZ, 6),
                new LevelRequirement(Items.AMETHYST_SHARD, 3),
                new LevelRequirement(Items.ECHO_SHARD, 1),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SILK_TOUCH, new LevelRequirement[]{
                new LevelRequirement(Items.RESIN_CLUMP, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SMITE, new LevelRequirement[]{
                new LevelRequirement(Items.ROTTEN_FLESH, 8),
                new LevelRequirement(Items.BONE, 4),
                new LevelRequirement(Items.PHANTOM_MEMBRANE, 1),
                new LevelRequirement(Items.WARPED_FUNGUS, 4),
                new LevelRequirement(Items.WITHER_ROSE, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SOUL_SPEED, new LevelRequirement[]{
                new LevelRequirement(Items.SUGAR, 5),
                new LevelRequirement(Items.SOUL_SAND, 6),
                new LevelRequirement(Items.SOUL_SOIL, 4)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SWEEPING_EDGE, new LevelRequirement[]{
                new LevelRequirement(Items.ARMOR_STAND, 1),
                new LevelRequirement(Items.HONEYCOMB, 2),
                new LevelRequirement(Items.BREEZE_ROD, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.SWIFT_SNEAK, new LevelRequirement[]{
                new LevelRequirement(Items.SUGAR, 5),
                new LevelRequirement(Items.RABBIT_HIDE, 3),
                new LevelRequirement(Items.PRISMARINE_CRYSTALS, 2)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.THORNS, new LevelRequirement[]{
                new LevelRequirement(Items.CACTUS, 4),
                new LevelRequirement(Items.PUFFERFISH, 1),
                new LevelRequirement(Items.SWEET_BERRIES, 3)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.UNBREAKING, new LevelRequirement[]{
                new LevelRequirement(Items.OBSIDIAN, 2),
                new LevelRequirement(Items.ECHO_SHARD, 1),
                new LevelRequirement(Items.ANVIL, 1)
        });
        ENCHANTMENT_REQUIREMENTS.put(Enchantments.WIND_BURST, new LevelRequirement[]{
                new LevelRequirement(Items.WIND_CHARGE, 2),
                new LevelRequirement(Items.TNT, 4),
                new LevelRequirement(Items.ANVIL, 1)
        });
    }

    /**
     * Method that calculates the ingredient and XP cost of an enchantment based off the item tier.
     */
    public static EnchantmentCost calculateCost(ItemStack tool, Holder<@NotNull Enchantment> enchantment, int targetLevel) {
        //Get tool tier multiplier
        int multiplier = getTierMultiplier(tool);

        //Query the lookup table
        ResourceKey<@NotNull Enchantment> key = enchantment.unwrapKey().orElse(null);
        LevelRequirement[] requirements = ENCHANTMENT_REQUIREMENTS.get(key);

        //Failsafe if enchantment doesn't exist in table
        if (requirements == null) return new EnchantmentCost(Items.AIR, 0, targetLevel);

        //Safely map level 1 to index 0
        int index = Math.max(0, targetLevel - 1);

        //If the level exceeds the array, trigger manual anvil lock
        if (index >= requirements.length) return new EnchantmentCost(Items.ANVIL, 1, targetLevel);

        //Get the ingredient requirement for the specific level
        LevelRequirement requirement = requirements[index];

        //Multiply the custom base amount by the tool's tier
        int finalAmount = requirement.baseAmount * multiplier;

        return new EnchantmentCost(requirement.item, finalAmount, targetLevel);
    }

    /**
     * Method that extracts the tool/armor tier.
     */
    public static int getTierMultiplier(ItemStack item) {
        String name = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();

        //Lowest tool/armor tier
        if (name.contains("wood") || name.contains("stone") || name.contains("leather")) return 1;
        if (name.contains("copper") || name.contains("chain")) return 2;
        if (name.contains("iron") || name.contains("gold")) return 4;
        if (name.contains("diamond") || name.contains("netherite")) return 8;

        //Default fallback for bonus or unknown modded items
        return 4;
    }

    /**
     * Method that calculates how many unspent curse tokens a tool currently has.
     */
    public static int getAvailableCurseTokens(ItemStack tool) {
        int curseCount = 0;
        for (var existingEnchant : tool.getEnchantments().keySet()) {
            if (existingEnchant.is(EnchantmentTags.CURSE)) curseCount++;
        }

        CompoundTag tag = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int cursesSpent = tag.getInt("curses_spent").orElse(0);
        return curseCount - cursesSpent;
    }

    /**
     * Method that scans for the nearest enchanting table and calculates valid bookshelves.
     */
    public static int getBookShelves(Level level, BlockPos playerPos) {
        BlockPos tablePos = null;
        double closestDistance = Double.MAX_VALUE;

        //Scan a 5x5x5 area for the enchanting table
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) {
                        double dist = pos.distSqr(playerPos);
                        if (dist < closestDistance) {
                            closestDistance = dist;
                            tablePos = pos;
                        }
                    }
                }
            }
        }

        if (tablePos == null) return 0;

        //Use vanilla math to verify line-of-sight for bookshelves
        int count = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, tablePos, offset)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Method that checks if the tool has reached the maximum allowed unique enchantments based on the bookshelf count.
     */
    public static boolean hasHitEnchantmentCap(ItemStack tool, Holder<@NotNull Enchantment> enchantment, int bookshelves) {
        //Curses bypass this cap
        if (enchantment.is(net.minecraft.tags.EnchantmentTags.CURSE)) return false;

        int maxUnique = 1 + (bookshelves / 3);
        int currentUnique = 0;
        for (var existingEnchantment : tool.getEnchantments().keySet()) {
            if (!existingEnchantment.is(net.minecraft.tags.EnchantmentTags.CURSE)) currentUnique++;
        }

        boolean isNewEnchantment = tool.getEnchantments().getLevel(enchantment) == 0;
        return isNewEnchantment && currentUnique >= maxUnique;
    }
}
