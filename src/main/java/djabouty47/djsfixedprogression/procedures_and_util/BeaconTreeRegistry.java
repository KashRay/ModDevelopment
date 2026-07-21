package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeaconTreeRegistry {
    public static final Map<String, BeaconNode> NODES = new HashMap<>();
    public record AbilityData(List<BeaconNode.NodeCost> usageCosts, int durationTicks, int cooldownTicks) {}
    public static final Map<String, AbilityData> ABILITIES = new HashMap<>();

    //Initialize abilities and entire upgrade tree when class is loaded
    static {
        //Register active abilities
        ABILITIES.put("mob_griefing", new AbilityData(List.of(new BeaconNode.NodeCost(Items.TNT, 1)), 1200, 6000));
        ABILITIES.put("mob_spawning", new AbilityData(List.of(new BeaconNode.NodeCost(Items.BARRIER, 1)), 1200, 6000));
        ABILITIES.put("set_weather", new AbilityData(List.of(new BeaconNode.NodeCost(Items.LIGHTNING_ROD, 1)), 1200, 6000));
        ABILITIES.put("inflict_wither", new AbilityData(List.of(new BeaconNode.NodeCost(Items.WITHER_ROSE, 1)), 1200, 6000));
        ABILITIES.put("disable_sculk", new AbilityData(List.of(new BeaconNode.NodeCost(Items.SCULK, 1)), 1200, 6000));
        ABILITIES.put("fire_resistance", new AbilityData(List.of(new BeaconNode.NodeCost(Items.LAVA_BUCKET, 1)), 2400, 6000));
        ABILITIES.put("mob_glowing", new AbilityData(List.of(new BeaconNode.NodeCost(Items.GLOWSTONE_DUST, 1)), 2400, 6000));
        ABILITIES.put("give_slowfalling", new AbilityData(List.of(new BeaconNode.NodeCost(Items.PHANTOM_MEMBRANE, 1)), 1200, 6000));
        ABILITIES.put("clear_weather", new AbilityData(List.of(new BeaconNode.NodeCost(Items.ICE, 1)),  15000, 15000));

        //Register upgrade path 1
        register(new BeaconNode( //Parent
                "Reaping What You Sow",
                "farmland_boost", 1,
                15, 55,
                "Farmland crops grow faster",
                List.of(new BeaconNode.NodeCost(Items.DIRT, 16)),
                new ItemStack(Items.FARMLAND),
                List.of(),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 1 upper
                "Lawnmageddon",
                "grass_boost", 1,
                45, 35,
                "Grass, mycelium, and vines spread faster",
                List.of(),
                new ItemStack(Items.GRASS_BLOCK),
                List.of("farmland_boost"),
                List.of("bonemeal_boost"),
                false, false
        ));
        register(new BeaconNode( //Branch 1 lower
                "Nutrient Overload",
                "bonemeal_boost", 1,
                45, 75,
                "1 bonemeal fully grows crops and trees",
                List.of(),
                new ItemStack(Items.BONE_MEAL),
                List.of("farmland_boost"),
                List.of("grass_boost"),
                false, false
        ));
        register(new BeaconNode( //Merge
                "Fire Suppression",
                "fire_spread", 1,
                75, 55,
                "Prevent fire spreading",
                List.of(new BeaconNode.NodeCost(Items.FIRE_CHARGE, 16), new BeaconNode.NodeCost(Items.BLAZE_ROD, 48)),
                new ItemStack(Items.FLINT_AND_STEEL),
                List.of("grass_boost", "bonemeal_boost"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "Rapid Repopulation",
                "breeding_cooldown", 1,
                105, 35,
                "Reduce the cooldown for breeding mobs",
                List.of(),
                new ItemStack(Items.WHEAT),
                List.of("fire_spread"),
                List.of("effect_time_1", "mob_griefing"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 middle
                "Extended Duration",
                "effect_time_1", 1,
                105, 55,
                "Increase beacon effect time",
                List.of(),
                new ItemStack(Items.CLOCK),
                List.of("fire_spread"),
                List.of("breeding_cooldown", "mob_griefing"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 lower
                "Not On My Watch",
                "mob_griefing", 1,
                105, 75,
                "Ability: Prevent mob griefing for 1 minute",
                List.of(),
                new ItemStack(Items.CREEPER_HEAD),
                List.of("fire_spread"),
                List.of("effect_time_1", "breeding_cooldown"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "Evicted",
                "mob_spawning", 1,
                135, 35,
                "Ability: Prevent mob spawning for 1 minute",
                List.of(),
                new ItemStack(Items.BARRIER),
                List.of("breeding_cooldown"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 middle
                "Brewing A Storm",
                "set_weather", 1,
                135, 55,
                "Ability: Bring about a thunderstorm",
                List.of(),
                new ItemStack(Items.EXPOSED_LIGHTNING_ROD),
                List.of("effect_time_1"),
                List.of(),
                false, false
        ));

        //Register upgrade path 2
        register(new BeaconNode( //Parent
                "Extended Duration",
                "effect_time_2", 2,
                15, 115,
                "Increase beacon effect time",
                List.of(),
                new ItemStack(Items.CLOCK),
                List.of(),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 1 upper
                "Come Again Another Day",
                "clear_weather", 2,
                45, 95,
                "Clear the weather",
                List.of(),
                new ItemStack(Items.WET_SPONGE),
                List.of("effect_time_2"),
                List.of("pet_effects"),
                false, false
        ));
        register(new BeaconNode( //Branch 1 lower
                "Friend Of A Friend",
                "pet_effects", 2,
                45, 135,
                "Beacon effects pass onto pets",
                List.of(),
                new ItemStack(Items.AXOLOTL_BUCKET),
                List.of("effect_time_2"),
                List.of("clear_weather"),
                false, false
        ));
        register(new BeaconNode( //Middle
                "Anti-corrosion Coating",
                "prevent_oxidization", 2,
                75, 115,
                "Prevent copper block oxidization",
                List.of(),
                new ItemStack(Items.COPPER_BLOCK),
                List.of("clear_weather", "pet_effects"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "Sturdy Wings",
                "elytra_rain", 2,
                105, 95,
                "Can fly with elytra in the rain",
                List.of(),
                new ItemStack(Items.ELYTRA),
                List.of("prevent_oxidization"),
                List.of("angry_villager"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 lower
                "Borrowing Permanently",
                "angry_villager", 2,
                105, 135,
                "Villagers and iron golems don't get upset when crops/hay blocks are stolen",
                List.of(),
                new ItemStack(Items.HAY_BLOCK),
                List.of("prevent_oxidization"),
                List.of("elytra_rain"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "The Black Plague",
                "inflict_wither", 2,
                135, 95,
                "Inflict the wither-effect on all nearby hostile mobs",
                List.of(),
                new ItemStack(Items.WITHER_ROSE),
                List.of("elytra_rain"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 lower
                "Bargaining",
                "villager_deals", 2,
                135, 135,
                "Villagers have their trade costs reduced",
                List.of(),
                new ItemStack(Items.EMERALD),
                List.of("angry_villager"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 lower
                "Extended Duration",
                "effect_time_3", 2,
                165, 135,
                "Increase beacon effect time",
                List.of(),
                new ItemStack(Items.CLOCK),
                List.of("villager_deals"),
                List.of(),
                false, false
        ));

        //Register upgrade path 3
        register(new BeaconNode( //Parent
                "Excavation",
                "mine_infested", 3,
                15, 195,
                "Safely mine infested stone",
                List.of(),
                new ItemStack(Items.INFESTED_STONE),
                List.of(),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Middle
                "Fasting",
                "afk_drain", 3,
                45, 195,
                "Prevent AFK hunger drain",
                List.of(),
                new ItemStack(Items.COOKED_BEEF),
                List.of("mine_infested"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 1 upper
                "Controlled Demolition",
                "stone_tnt", 3,
                75, 175,
                "Stone is more vulnerable to explosives",
                List.of(),
                new ItemStack(Items.TNT),
                List.of("afk_drain"),
                List.of("more_xp", "effect_time_4"),
                false, false
        ));
        register(new BeaconNode( //Branch 1 middle
                "Prestige",
                "more_xp", 3,
                75, 195,
                "Gain more XP",
                List.of(),
                new ItemStack(Items.EXPERIENCE_BOTTLE),
                List.of("afk_drain"),
                List.of("stone_tnt", "effect_time_4"),
                false, false
        ));
        register(new BeaconNode( //Branch 1 lower
                "Extended Duration",
                "effect_time_4", 3,
                75, 215,
                "Increase beacon effect time",
                List.of(),
                new ItemStack(Items.CLOCK),
                List.of("afk_drain"),
                List.of("stone_tnt", "more_xp"),
                false, false
        ));
        register(new BeaconNode( //Branch 1 lower
                "Unlimited Power",
                "effect_increase", 3,
                105, 215,
                "Beacon effects increased from level 1 to 2",
                List.of(),
                new ItemStack(Items.NETHER_STAR),
                List.of("effect_time_4"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "Sneak 100",
                "disable_sculk", 3,
                105, 155,
                "Ability: Prevent sculk from hearing you for 1 minute",
                List.of(),
                new ItemStack(Items.SCULK),
                List.of("stone_tnt"),
                List.of("fire_resistance"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 lower
                "Melting Point",
                "fire_resistance", 3,
                105, 175,
                "Ability: Give fire resistance for 2 minutes",
                List.of(),
                new ItemStack(Items.LAVA_BUCKET),
                List.of("stone_tnt"),
                List.of("disable_sculk"),
                false, false
        ));
        register(new BeaconNode( //Branch 2 upper
                "Now You See Me...",
                "mob_glowing", 3,
                135, 155,
                "Ability: Make hostile mobs glow for 2 minutes",
                List.of(),
                new ItemStack(Items.GLOWSTONE_DUST),
                List.of("disable_sculk"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 1 middle
                "Float Like A Butterfly",
                "give_slowfalling", 3,
                105, 195,
                "Ability: Give slowfalling for 5 minutes",
                List.of(),
                new ItemStack(Items.PHANTOM_MEMBRANE),
                List.of("more_xp"),
                List.of(),
                false, false
        ));
        register(new BeaconNode( //Branch 1 merge
                "Breaking It Down",
                "mine_deepslate", 3,
                135, 195,
                "Deepslate mines at normal stone speed",
                List.of(),
                new ItemStack(Items.DEEPSLATE),
                List.of("give_slowfalling", "effect_increase"),
                List.of(),
                false, false
        ));
    }

    private static void register(BeaconNode node) {
        NODES.put(node.id(), node);
    }
}
