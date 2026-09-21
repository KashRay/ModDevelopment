package djabouty47.djsfixedprogression;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.effect.CleanseEffect;
import djabouty47.djsfixedprogression.menu.*;
import djabouty47.djsfixedprogression.network.*;
import djabouty47.djsfixedprogression.procedures_and_util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DJsFixedProgression implements ModInitializer {
	public static final String MOD_ID = "djs-fixed-progression";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    //VARIABLE DECLARATIONS
    //Vote skip night tracker
    public static final Set<UUID> SLEEP_VOTES = new HashSet<>();

    //Minecart link pending status
    public static final Map<UUID, UUID> PENDING_LINKS = new HashMap<>();
    public static int pendingLinkMinecartId = -1;

    //Food palette multiplier thread tracker
    public static float currentSaturationMultiplier = 1.0F;

    //Regen cooldown tracker
    public static final WeakHashMap<Player, Integer> REGEN_COOLDOWNS = new WeakHashMap<>();

    //Tag declaration
    public static final TagKey<@NotNull EntityType<?>> VAMPIRISM_TARGETS = TagKey.create(Registries.ENTITY_TYPE, Identifier.parse("djsfixedprogression:vampirism_targets"));

    //Trial key item declarations
    public static final ResourceKey<@NotNull Item> GUARDIAN_TRIAL_KEY_ID = ResourceKey.create(Registries.ITEM, Identifier.parse("djsfixedprogression:guardian_trial_key"));
    public static final Item GUARDIAN_TRIAL_KEY = Registry.register(BuiltInRegistries.ITEM, GUARDIAN_TRIAL_KEY_ID, new Item(new Item.Properties().setId(GUARDIAN_TRIAL_KEY_ID)));
    public static final ResourceKey<@NotNull Item> OMINOUS_GUARDIAN_TRIAL_KEY_ID = ResourceKey.create(Registries.ITEM, Identifier.parse("djsfixedprogression:ominous_guardian_trial_key"));
    public static final Item OMINOUS_GUARDIAN_TRIAL_KEY = Registry.register(BuiltInRegistries.ITEM, OMINOUS_GUARDIAN_TRIAL_KEY_ID, new Item(new Item.Properties().fireResistant().setId(OMINOUS_GUARDIAN_TRIAL_KEY_ID)));
    public static final ResourceKey<@NotNull Item> END_TRIAL_KEY_ID = ResourceKey.create(Registries.ITEM, Identifier.parse("djsfixedprogression:end_trial_key"));
    public static final Item END_TRIAL_KEY = Registry.register(BuiltInRegistries.ITEM, END_TRIAL_KEY_ID, new Item(new Item.Properties().setId(END_TRIAL_KEY_ID)));
    public static final ResourceKey<@NotNull Item> OMINOUS_END_TRIAL_KEY_ID = ResourceKey.create(Registries.ITEM, Identifier.parse("djsfixedprogression:ominous_end_trial_key"));
    public static final Item OMINOUS_END_TRIAL_KEY = Registry.register(BuiltInRegistries.ITEM, OMINOUS_END_TRIAL_KEY_ID, new Item(new Item.Properties().fireResistant().setId(OMINOUS_END_TRIAL_KEY_ID)));

    //Potion and effect declarations
    public static final Holder<@NotNull Potion> GLOWING_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, ResourceKey.create(Registries.POTION, Identifier.parse("djsfixedprogression:glowing")), new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 1200)));
    public static final Holder<@NotNull Potion> LONG_GLOWING_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, ResourceKey.create(Registries.POTION, Identifier.parse("djsfixedprogression:long_glowing")), new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 2400)));
    public static final Holder<@NotNull MobEffect> CLEANSE_EFFECT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.parse("djsfixedprogression:cleanse"), new CleanseEffect(MobEffectCategory.BENEFICIAL, 0x4B6BFF));

    //Custom menu declarations
    public static final MenuType<@NotNull DJsEnchantmentMenu> DJS_ENCHANTMENT_MENU = Registry.register(BuiltInRegistries.MENU, Identifier.parse("djsfixedprogression:enchantment_menu"), new MenuType<>(DJsEnchantmentMenu::new, FeatureFlagSet.of()));
    public static final MenuType<@NotNull DJsAnvilMenu> DJS_ANVIL_MENU = Registry.register(BuiltInRegistries.MENU, Identifier.parse("djsfixedprogression:anvil_menu"), new MenuType<>(DJsAnvilMenu::new, FeatureFlagSet.of()));
    public static final MenuType<@NotNull DJsGrindstoneMenu> DJS_GRINDSTONE_MENU = Registry.register(BuiltInRegistries.MENU, Identifier.parse("djsfixedprogression:grindstone_menu"), new MenuType<>(DJsGrindstoneMenu::new, FeatureFlagSet.of()));
    public static final MenuType<@NotNull DJsFletchingMenu> DJS_FLETCHING_TABLE_MENU = Registry.register(BuiltInRegistries.MENU, Identifier.parse("djsfixedprogression:fletching_menu"), new MenuType<>(DJsFletchingMenu::new, FeatureFlagSet.of()));
    public static final MenuType<@NotNull DJsBeaconMenu> DJS_BEACON_MENU = Registry.register(BuiltInRegistries.MENU, Identifier.parse("djsfixedprogression:beacon_menu"), new MenuType<>(DJsBeaconMenu::new, FeatureFlagSet.of()));

    @Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		LOGGER.info("Initializing DJ's Fixed Progression.");

        //Load config file
        djabouty47.djsfixedprogression.config.DJsConfig.load();

        //Network registry (unconditional)
        PayloadTypeRegistry.playS2C().register(FoodHistorySyncPayload.ID, FoodHistorySyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EnchantmentSelectPayload.ID, EnchantmentSelectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EnchantingAutoMovePayload.ID, EnchantingAutoMovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NametagRenamePayload.ID, NametagRenamePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BeaconUpgradePayload.ID, BeaconUpgradePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BeaconSyncPayload.ID, BeaconSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BeaconAutoMovePayload.ID, BeaconAutoMovePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MinecartLinkPayload.ID, MinecartLinkPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SleepVotePayload.ID, SleepVotePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SleepVoteSuccessPayload.ID, SleepVoteSuccessPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SleepVoteCancelPayload.ID, SleepVoteCancelPayload.CODEC);

        //Override the default vanilla components
        DefaultItemComponentEvents.MODIFY.register(context -> {
            //Check if tool and armor rebalancing is enabled in configs
            if (DJsConfig.getInstance().enableToolAndArmorRebalance) {
                //ARMOR TWEAKS (Item, Max Durability, Armor, Toughness, Knockback Resistance, Slot Name, Slot Group)
                //Copper
                applyArmorStats(context, Items.COPPER_HELMET, 155, 1.0, 0.0, 0.0, "helmet", EquipmentSlotGroup.HEAD);
                applyArmorStats(context, Items.COPPER_CHESTPLATE, 225, 3.0, 0.0, 0.0, "chestplate", EquipmentSlotGroup.CHEST);
                applyArmorStats(context, Items.COPPER_LEGGINGS, 208, 2.0, 0.0, 0.0, "leggings", EquipmentSlotGroup.LEGS);
                applyArmorStats(context, Items.COPPER_BOOTS, 175, 1.0, 0.0, 0.0, "boots", EquipmentSlotGroup.FEET);
                //Chainmail
                applyArmorStats(context, Items.CHAINMAIL_HELMET, 300, 2.0, 2.0, 0.2, "helmet", EquipmentSlotGroup.HEAD);
                applyArmorStats(context, Items.CHAINMAIL_CHESTPLATE, 385, 6.0, 2.0, 0.2, "chestplate", EquipmentSlotGroup.CHEST);
                applyArmorStats(context, Items.CHAINMAIL_LEGGINGS, 340, 5.0, 2.0, 0.2, "leggings", EquipmentSlotGroup.LEGS);
                applyArmorStats(context, Items.CHAINMAIL_BOOTS, 320, 2.0, 2.0, 0.2, "boots", EquipmentSlotGroup.FEET);
                //Gold
                applyArmorStats(context, Items.GOLDEN_HELMET, 155, 2.0, 0.0, 0.0, "helmet", EquipmentSlotGroup.HEAD);
                applyArmorStats(context, Items.GOLDEN_CHESTPLATE, 225, 7.0, 0.0, 0.0, "chestplate", EquipmentSlotGroup.CHEST);
                applyArmorStats(context, Items.GOLDEN_LEGGINGS, 208, 6.0, 0.0, 0.0, "leggings", EquipmentSlotGroup.LEGS);
                applyArmorStats(context, Items.GOLDEN_BOOTS, 175, 2.0, 0.0, 0.0, "boots", EquipmentSlotGroup.FEET);
                //Diamond
                applyArmorStats(context, Items.DIAMOND_HELMET, 363, 3.0, 2.0, 0.0, "helmet", EquipmentSlotGroup.HEAD);
                applyArmorStats(context, Items.DIAMOND_CHESTPLATE, 528, 8.0, 2.0, 0.0, "chestplate", EquipmentSlotGroup.CHEST);
                applyArmorStats(context, Items.DIAMOND_LEGGINGS, 495, 6.0, 2.0, 0.0, "leggings", EquipmentSlotGroup.LEGS);
                applyArmorStats(context, Items.DIAMOND_BOOTS, 429, 3.0, 2.0, 0.0, "boots", EquipmentSlotGroup.FEET);
                //Netherite
                applyArmorStats(context, Items.NETHERITE_HELMET, 407, 3.0, 3.0, 0.1, "helmet", EquipmentSlotGroup.HEAD);
                applyArmorStats(context, Items.NETHERITE_CHESTPLATE, 592, 8.0, 3.0, 0.1, "chestplate", EquipmentSlotGroup.CHEST);
                applyArmorStats(context, Items.NETHERITE_LEGGINGS, 555, 6.0, 3.0, 0.1, "leggings", EquipmentSlotGroup.LEGS);
                applyArmorStats(context, Items.NETHERITE_BOOTS, 481, 3.0, 3.0, 0.1, "boots", EquipmentSlotGroup.FEET);
                //Turtle Helmet
                applyArmorStats(context, Items.TURTLE_HELMET, 407, 3.0, 2.0, 0.0, "helmet", EquipmentSlotGroup.HEAD);

                //TOOL TWEAKS (Item, Max Durability, Total Damage, Attack Speed)
                //Wooden
                applyToolStats(context, Items.WOODEN_SWORD, 50, 4.0, 1.6, 3.0);
                applyToolStats(context, Items.WOODEN_AXE, 100, 7.0, 0.8, 3.0);
                applyToolStats(context, Items.WOODEN_PICKAXE, 100, 3.0, 1.2, 3.0);
                applyToolStats(context, Items.WOODEN_SHOVEL, 100, 2.0, 2.2, 2.0);
                applyToolStats(context, Items.WOODEN_HOE, 100, 4.0, 1.0, 4.0);
                applyToolStats(context, Items.WOODEN_SPEAR, 50, 1.0, 1.54, 4.0);
                //Stone
                applyToolStats(context, Items.STONE_SWORD, 100, 5.0, 1.6, 3.0);
                applyToolStats(context, Items.STONE_AXE, 200, 8.0, 0.8, 3.0);
                applyToolStats(context, Items.STONE_PICKAXE, 200, 4.0, 1.2, 3.0);
                applyToolStats(context, Items.STONE_SHOVEL, 200, 3.0, 2.2, 2.0);
                applyToolStats(context, Items.STONE_HOE, 200, 4.5, 1.0, 4.0);
                applyToolStats(context, Items.STONE_SPEAR, 100, 2.0, 1.33, 4.0);
                //Iron
                applyToolStats(context, Items.IRON_SWORD, 300, 6.0, 1.6, 3.0);
                applyToolStats(context, Items.IRON_AXE, 600, 9.0, 0.8, 3.0);
                applyToolStats(context, Items.IRON_PICKAXE, 600, 5.0, 1.2, 3.0);
                applyToolStats(context, Items.IRON_SHOVEL, 600, 4.0, 2.2, 2.0);
                applyToolStats(context, Items.IRON_HOE, 600, 6.0, 1.0, 4.0);
                applyToolStats(context, Items.IRON_SPEAR, 300, 3.0, 1.05, 4.0);
                //Copper (Special Method)
                applyCopperToolBase(context, Items.COPPER_SWORD, Items.STONE_SWORD, 200, 4.0, 1.6, 3.0);
                applyCopperToolBase(context, Items.COPPER_AXE, Items.STONE_AXE, 400, 7.0, 0.8, 3.0);
                applyCopperToolBase(context, Items.COPPER_PICKAXE, Items.STONE_PICKAXE, 400, 3.0, 1.2, 3.0);
                applyCopperToolBase(context, Items.COPPER_SHOVEL, Items.STONE_SHOVEL, 400, 2.0, 2.2, 2.0);
                applyCopperToolBase(context, Items.COPPER_HOE, Items.STONE_HOE, 400, 4.0, 1.0, 4.0);
                applyCopperToolBase(context, Items.COPPER_SPEAR, Items.STONE_SPEAR, 200, 1.0, 0.95, 4.0);
                //Gold
                applyToolStats(context, Items.GOLDEN_SWORD, 100, 7.0, 1.6, 3.0);
                applyToolStats(context, Items.GOLDEN_AXE, 200, 10.0, 0.8, 3.0);
                applyToolStats(context, Items.GOLDEN_PICKAXE, 200, 6.0, 1.2, 3.0);
                applyToolStats(context, Items.GOLDEN_SHOVEL, 200, 5.0, 2.2, 2.0);
                applyToolStats(context, Items.GOLDEN_HOE, 200, 7.0, 1.0, 4.0);
                applyToolStats(context, Items.GOLDEN_SPEAR, 100, 4.0, 1.05, 4.0);
                //Diamond
                applyToolStats(context, Items.DIAMOND_SWORD, 500, 7.0, 1.6, 3.0);
                applyToolStats(context, Items.DIAMOND_AXE, 1000, 10.0, 0.8, 3.0);
                applyToolStats(context, Items.DIAMOND_PICKAXE, 1000, 6.0, 1.2, 3.0);
                applyToolStats(context, Items.DIAMOND_SHOVEL, 1000, 5.0, 2.2, 2.0);
                applyToolStats(context, Items.DIAMOND_HOE, 1000, 7.0, 1.0, 4.0);
                applyToolStats(context, Items.DIAMOND_SPEAR, 500, 4.0, 0.95, 4.0);
                //Netherite
                applyToolStats(context, Items.NETHERITE_SWORD, 750, 8.0, 1.6, 3.0);
                applyToolStats(context, Items.NETHERITE_AXE, 1500, 11.0, 0.8, 3.0);
                applyToolStats(context, Items.NETHERITE_PICKAXE, 1500, 7.0, 1.2, 3.0);
                applyToolStats(context, Items.NETHERITE_SHOVEL, 1500, 6.0, 2.2, 2.0);
                applyToolStats(context, Items.NETHERITE_HOE, 1500, 8.0, 1.0, 4.0);
                applyToolStats(context, Items.NETHERITE_SPEAR, 750, 5.0, 0.87, 4.0);

                //Special Weapons
                applyToolStats(context, Items.TRIDENT, 250, 9.0, 1.0, 4.0);
                applyToolStats(context, Items.MACE, 500, 6, 0.5, 3.0);
            }

            //Make horse armor enchantable
            Item[] horseArmors = {Items.LEATHER_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR, Items.IRON_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR, Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR};
            for (Item armor : horseArmors) {
                context.modify(armor, builder -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(15)));
            }

            //Make nautilus armor enchantable
            Item[] nautilusArmors = {Items.COPPER_NAUTILUS_ARMOR, Items.IRON_NAUTILUS_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR, Items.DIAMOND_NAUTILUS_ARMOR, Items.NETHERITE_NAUTILUS_ARMOR};
            for (Item armor : nautilusArmors) {
                context.modify(armor, builder -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(15)));
            }

            //Make extra tools enchantable
            Item[] extraTools = {Items.SHIELD, Items.ELYTRA, Items.SHEARS, Items.FLINT_AND_STEEL, Items.BRUSH};
            for (Item extraTool : extraTools) {
                context.modify(extraTool, builder -> builder.set(DataComponents.ENCHANTABLE, new Enchantable(14)));
            }

            //Check if combat changes are enabled in configs
            if (DJsConfig.getInstance().enableCombatChanges) {
                context.modify(Items.POTION, builder -> builder.set(DataComponents.MAX_STACK_SIZE, 16));
                context.modify(Items.SPLASH_POTION, builder -> builder.set(DataComponents.MAX_STACK_SIZE, 16));
                context.modify(Items.LINGERING_POTION, builder -> builder.set(DataComponents.MAX_STACK_SIZE, 16));
            }
        });

        //Ensure server tick rate is always reset upon startup
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.tickRateManager().setTickRate(20.0F));

        //Set up in-bed sleep vote receiver
        ServerPlayNetworking.registerGlobalReceiver(SleepVotePayload.ID, (payload, context) -> context.server().execute(() -> attemptSleepVote(context.player(), true)));

        //Set up hidden command sleep vote receiver (used for chat voting)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("djs_vote_sleep").executes(context -> {
            if (context.getSource().getPlayer() != null) attemptSleepVote(context.getSource().getPlayer(), false);
            return 1;
        })));

        //Clear sleep votes every morning to prevent old votes from carrying over to the next night, and add thunderstorm invasion logic
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level.dimension() == Level.OVERWORLD) {
                long timeOfDay = level.getDayTime() % 24000;

                //If it's day time, not storming, and there are leftover votes, wipe them
                if (timeOfDay >= 0 && timeOfDay < 12000 && !SLEEP_VOTES.isEmpty()) {
                    SLEEP_VOTES.clear();
                    for (ServerPlayer p : level.players()) ServerPlayNetworking.send(p, new SleepVoteCancelPayload());
                }

                //Check if it is storming and mode isn't peaceful
                if (level.isThundering() && level.getDifficulty() != Difficulty.PEACEFUL) {
                    //Check every 100 ticks for a surface spawn
                    if (level.getGameRules().get(GameRules.SPAWN_MOBS) && level.getGameTime() % 100 == 0) {
                        for (ServerPlayer p : level.players()) {
                            //Check if player is loading spawn level
                            if (!p.isCreative() && !p.isSpectator() && level.canSeeSky(p.blockPosition())) {
                                //25% chance of spawning an extra mob
                                if (level.random.nextInt(4) == 0) {
                                    //Spawn mob 24-40 blocks away
                                    double angle = level.random.nextDouble() * Math.PI * 2;
                                    double distance = 24.0 + level.random.nextDouble() * 16.0;
                                    double spawnX = p.getX() + Math.cos(angle) * distance;
                                    double spawnZ = p.getZ() + Math.sin(angle) * distance;

                                    //Only spawn if the target block can see the sky
                                    BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(spawnX, 0, spawnZ));
                                    if (level.canSeeSky(spawnPos)) {
                                        EntityType<?>[] monsters = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER};
                                        EntityType<?> typeToSpawn = monsters[level.random.nextInt(monsters.length)];

                                        Monster monster = (Monster) typeToSpawn.create(level, EntitySpawnReason.EVENT);
                                        if (monster != null) {
                                            monster.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                                            level.addFreshEntity(monster);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //Check every 4800 ticks for a possible horde spawn near a village
                    if (level.getGameTime() % 4800 == 0) {
                        if (level.random.nextBoolean()) {
                            List<ServerPlayer> survivalPlayers = level.players().stream().filter(p -> !p.isCreative() && !p.isSpectator()).toList();
                            if (!survivalPlayers.isEmpty()) {
                                ServerPlayer randomPlayer = survivalPlayers.get(level.random.nextInt(survivalPlayers.size()));

                                //Approximate village detection by scanning for villagers within 64 blocks
                                List<Villager> villagers = level.getEntitiesOfClass(Villager.class, randomPlayer.getBoundingBox().inflate(64.0));
                                if (!villagers.isEmpty()) {
                                    //Target a specific villager
                                    Villager targetVillager = villagers.get(level.random.nextInt(villagers.size()));

                                    //Calculate spawn point 35-50 blocks away from target villager
                                    double angle = level.random.nextDouble() * 2 * Math.PI;
                                    double distance = 35.0 + level.random.nextDouble() * 15.0;
                                    double spawnX = targetVillager.getX() + Math.cos(angle) * distance;
                                    double spawnZ = targetVillager.getZ() + Math.sin(angle) * distance;

                                    //Only spawn if the target block can see the sky
                                    BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(spawnX, 0, spawnZ));

                                    //Ensure spawn isn't on top of player
                                    if (spawnPos.distToCenterSqr(randomPlayer.position()) > 400.0) {
                                        //Spawn a horde of zombies
                                        int hordeSize = 5 + level.random.nextInt(6);
                                        for (int i = 0; i < hordeSize; i++) {
                                            Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.EVENT);
                                            if (zombie != null) {
                                                //Scatter the zombies slighly
                                                zombie.setPos(spawnPos.getX() + level.random.nextDouble() * 4 - 2, spawnPos.getY(), spawnPos.getZ() + level.random.nextDouble() * 4 - 2);

                                                //Force zombie horde to aggro towards the village
                                                zombie.setTarget(villagers.get(level.random.nextInt(villagers.size())));

                                                //Spawn zombie
                                                level.addFreshEntity(zombie);
                                            }
                                        }
                                        //Notify player of the approaching horde
                                        randomPlayer.displayClientMessage(Component.literal("A horde of zombies is approaching...").withStyle(ChatFormatting.DARK_RED), true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        //Remove a player's night skip vote if they disconnect from the server
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (SLEEP_VOTES.remove(handler.getPlayer().getUUID())) {
                //If this was the last active vote, notify all clients that the global vote has ended
                if (SLEEP_VOTES.isEmpty()) {
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, new SleepVoteCancelPayload());
                }
            }

        });

        //Sync changes when joining a server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            //Players who first join a world should start with 0 saturation
            if (!player.getTags().contains("djs_has_joined_before")) {
                player.getFoodData().setSaturation(0.0F);
                player.addTag("djs_has_joined_before");
            }

            //Reset player night skip vote upon joining server
            ServerPlayNetworking.send(player, new SleepVoteCancelPayload());

            //Check if hunger and regeneration changes are enabled in configs
            if (DJsConfig.getInstance().enableHungerAndRegenerationChanges) {
                //Sync food history when joining a server
                if (player instanceof IFoodHistory historyPlayer) ServerPlayNetworking.send(player, new FoodHistorySyncPayload(new ArrayList<>(historyPlayer.djs$getFoodHistory())));
            }
        });


        //Check if hunger and regeneration changes are enabled in configs
        if (DJsConfig.getInstance().enableHungerAndRegenerationChanges) {
            //Retain and sync history after dying and respawning
            ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
                IFoodHistory oldHistory = (IFoodHistory) oldPlayer;
                IFoodHistory newHistory = (IFoodHistory) newPlayer;
                newHistory.djs$getFoodHistory().clear();
                newHistory.djs$getFoodHistory().addAll(oldHistory.djs$getFoodHistory());
                ServerPlayNetworking.send(newPlayer, new FoodHistorySyncPayload(new ArrayList<>(newHistory.djs$getFoodHistory())));
            });
        }

        //Check if phantom changes are enabled in configs
        if (DJsConfig.getInstance().enablePhantomChanges) {
            //Make phantoms spawn naturally in the outer end islands
            BiomeModifications.addSpawn(
                    BiomeSelectors.includeByKey(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS),
                    MobCategory.MONSTER,
                    EntityType.PHANTOM,
                    2,
                    1,
                    3
            );
        }

        //Check if anvil and enchanting changes are enabled in configs
        if (DJsConfig.getInstance().enableAnvilAndEnchantingChanges) {
            //Set up auto-move receiver
            ServerPlayNetworking.registerGlobalReceiver(
                    EnchantingAutoMovePayload.ID,
                    (payload, context) -> context.server().execute(() -> {
                        if (context.player().containerMenu instanceof DJsEnchantmentMenu menu) {
                            menu.tryAutoMoveItems(payload.enchantmentId(), payload.useToken(), context.player());
                        }
                    })
            );

            //Set up server enchantment receiver
            ServerPlayNetworking.registerGlobalReceiver(
                    EnchantmentSelectPayload.ID,
                    (payload, context) -> {
                        //Execute on the main server thread to prevent concurrency crashes
                        context.server().execute(() -> {
                            Player player = context.player();
                            Identifier requestedEnchantmentId = payload.enchantmentId();

                            //Ensure the player has the enchantment menu open
                            if (!(player.containerMenu instanceof DJsEnchantmentMenu menu)) return;

                            //Get inventory slots
                            ItemStack toolStack = menu.getSlot(0).getItem();
                            ItemStack lapisStack = menu.getSlot(1).getItem();
                            ItemStack ingredientStack = menu.getSlot(2).getItem();

                            //Ensure a tool is present
                            if (toolStack.isEmpty()) return;

                            //Fetch enchantment
                            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                            var enchantmentKey = ResourceKey.create(Registries.ENCHANTMENT, requestedEnchantmentId);
                            var optionalEnchantment = registry.get(enchantmentKey);

                            //Ensure enchantment exists
                            if (optionalEnchantment.isEmpty()) return;
                            Holder<@NotNull Enchantment> enchantmentHolder = optionalEnchantment.get();

                            //Determine the next enchantment level
                            int currentLevel = toolStack.getEnchantments().getLevel(enchantmentHolder);
                            int nextLevel = currentLevel + 1;

                            //Get the enchantment ingredient and lapis cost
                            EnchantmentCostHelper.EnchantmentCost costData = EnchantmentCostHelper.calculateCost(toolStack, enchantmentHolder, nextLevel);
                            int lapisCost = EnchantmentCostHelper.getTierMultiplier(toolStack);

                            //Get data from client and tools
                            boolean useToken = payload.useToken();
                            CustomData customData = toolStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                            boolean isCurse = enchantmentHolder.is(EnchantmentTags.CURSE);
                            int curseTokens = EnchantmentCostHelper.getAvailableCurseTokens(toolStack);
                            int bookshelves = EnchantmentCostHelper.getBookShelves(player.level(), player.blockPosition());
                            boolean hitEnchantmentCap = EnchantmentCostHelper.hasHitEnchantmentCap(toolStack, enchantmentHolder, bookshelves);
                            boolean usingToken = !player.isCreative() && !isCurse && useToken && curseTokens > 0;
                            boolean needsAnvil = costData.ingredient().equals(Items.ANVIL);

                            //Safety checks
                            if (!player.isCreative() && needsAnvil && !usingToken) return;
                            if (!player.isCreative() && hitEnchantmentCap && !usingToken) return;

                            //Verify player has enough lapis and ingredients
                            boolean hasCorrectItem = costData.ingredientAmount() <= 0 || ingredientStack.is(costData.ingredient());
                            if (!player.isCreative() && !usingToken) {
                                if (player.experienceLevel < costData.xpCost() ||
                                        lapisStack.getCount() < lapisCost ||
                                        (costData.ingredientAmount() > 0 && ingredientStack.getCount() < costData.ingredientAmount()) ||
                                        !hasCorrectItem) return;
                            }

                            //Apply the enchantment
                            toolStack.enchant(enchantmentHolder, nextLevel);

                            //Consume resources unless in creative
                            if (!player.isCreative()) {
                                //If using a token, consume token
                                if (usingToken) {
                                    CompoundTag currentTag = customData.copyTag();
                                    int cursesSpent = currentTag.getInt("curses_spent").orElse(0);
                                    currentTag.putInt("curses_spent", cursesSpent + 1);
                                    toolStack.set(DataComponents.CUSTOM_DATA, CustomData.of(currentTag));
                                }
                                //Otherwise deduct XP, lapis, and ingredients
                                else {
                                    player.giveExperienceLevels(-costData.xpCost());
                                    lapisStack.shrink(lapisCost);

                                    //Only shrink if cost > 0
                                    if (costData.ingredientAmount() > 0) ingredientStack.shrink(costData.ingredientAmount());
                                }
                            }

                            //Update the server and play sound
                            menu.broadcastChanges();
                            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, player.level().random.nextFloat() * 0.1F + 0.9F);
                        });
                    });

            //Set up nametag server receiver
            ServerPlayNetworking.registerGlobalReceiver(
                    NametagRenamePayload.ID,
                    (payload, context) -> context.server().execute(() -> {
                        Player player = context.player();

                        //Check both hands for nametag
                        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
                        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
                        ItemStack nametag = mainHand.is(Items.NAME_TAG) ? mainHand : (offHand.is(Items.NAME_TAG) ? offHand : ItemStack.EMPTY);

                        if (!nametag.isEmpty()) {
                            //Remove custom name if string is empty, otherwise apply it
                            if (payload.name().isBlank()) nametag.remove(DataComponents.CUSTOM_NAME);
                            else nametag.set(DataComponents.CUSTOM_NAME, Component.literal(payload.name()));


                            //Play sound to confirm rename
                            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.5F, 1.0F);
                        }
                    }));

            //Intercept right-clicking with a nametag
            UseItemCallback.EVENT.register((player, level, hand) -> {
                if (!level.isClientSide() && player.getItemInHand(hand).is(Items.NAME_TAG)) {
                    //Tell server action was successful
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            });
        }

        //Check if beacon changes are enabled in configs
        if (DJsConfig.getInstance().enableBeaconChanges) {
            ServerPlayNetworking.registerGlobalReceiver(BeaconUpgradePayload.ID, (payload, context) -> context.server().execute(() -> {
                if (context.player().containerMenu instanceof DJsBeaconMenu menu) {
                    //Attempt the purchase
                    menu.tryPurchasedUpgrade(payload.nodeId(), context.player());

                    //Clear weather if within range of a beacon with an active ability clearing the weather
                    if (payload.nodeId().equals("clear_weather")) {
                        ServerLevel level = context.player().level().getLevel();
                        if (level.isRaining() || level.isThundering()) level.setWeatherParameters(BeaconTreeRegistry.ABILITIES.get("clear_weather").durationTicks(), 0, false, false);
                    }

                    //Beam the newly updated NBT back to the client screen
                    ServerPlayNetworking.send(context.player(), new BeaconSyncPayload(menu.getUpgrades()));
                }
            }));

            ServerPlayNetworking.registerGlobalReceiver(BeaconAutoMovePayload.ID, (payload, context) -> context.server().execute(() -> {
                //Attempt auto-move
                if (context.player().containerMenu instanceof DJsBeaconMenu menu) menu.tryAutoMoveItems(payload.nodeId(), context.player());
            }));
        }

        //Check if minecart changes are enabled in configs
        if (DJsConfig.getInstance().enableMinecartChanges) {
            //Remind the client about links when chunks load
            EntityTrackingEvents.START_TRACKING.register((trackedEntity, trackingPlayer) -> {
                if (trackedEntity instanceof AbstractMinecart minecart) {
                    syncMinecartLinks(minecart, (ServerLevel) minecart.level());
                }
            });

            //Extinguish with dispenser + waterbucket
            DispenseItemBehavior originalWater = DispenserBlock.DISPENSER_REGISTRY.get(Items.WATER_BUCKET);
            DispenserBlock.registerBehavior(Items.WATER_BUCKET, (source, stack) -> {
                ServerLevel level = source.level();
                BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));

                List<MinecartFurnace> minecarts = level.getEntitiesOfClass(MinecartFurnace.class, new AABB(pos));
                for (MinecartFurnace minecart : minecarts) {
                    if (((ILinkableMinecart) minecart).djs$isFurnaceFueled()) {
                        ((ILinkableMinecart) minecart).djs$extinguish();
                        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        return new ItemStack(Items.BUCKET);
                    }
                }
                //Fallback to placing water if no minecart is found
                return originalWater != null ? originalWater.dispense(source, stack) : stack;
            });

            //Fuel with coal/charcoal
            DispenseItemBehavior coalBehavior = new DispenseItemBehavior() {
                @Override
                public @NotNull ItemStack dispense(BlockSource source, @NotNull ItemStack stack) {
                    ServerLevel level = source.level();
                    Direction facing = source.state().getValue(DispenserBlock.FACING);
                    BlockPos pos = source.pos().relative(facing);

                    List<MinecartFurnace> minecarts = level.getEntitiesOfClass(MinecartFurnace.class, new AABB(pos));
                    for (MinecartFurnace minecart : minecarts) {
                        //Push the minecart in the direction the dispenser is facing
                        Vec3 pushDir = Vec3.atLowerCornerOf(facing.getUnitVec3i());
                        ((ILinkableMinecart) minecart).djs$addFuel(pushDir);

                        stack.shrink(1);
                        return stack;
                    }
                    //Fallback to dropping the item if no minecart is found
                    return new DefaultDispenseItemBehavior().dispense(source, stack);
                }
            };

            DispenserBlock.registerBehavior(Items.COAL, coalBehavior);
            DispenserBlock.registerBehavior(Items.CHARCOAL, coalBehavior);
        }

        UseEntityCallback.EVENT.register(((player, level, interactionHand, entity, entityHitResult) -> {
            //Check if happy ghast changes are enabled in configs and the entity is a happy ghast
            if (DJsConfig.getInstance().enableHappyGhastChanges && entity instanceof HappyGhast happyGhast) {
                //Get item in player's hand
                ItemStack stack = player.getItemInHand(interactionHand);

                //Check if item is a block
                if (stack.getItem() instanceof BlockItem blockItem) {
                    //Prevent placing if player is actively riding ghast
                    if (player.getVehicle() == happyGhast) return InteractionResult.PASS;

                    //Prevent placing if player is not standing on the ghast's roof
                    if (player.getY() < happyGhast.getY() + happyGhast.getBbHeight() - 0.5) return  InteractionResult.PASS;

                    //Perform grid-perfect placement
                    if (!level.isClientSide()) {
                        //Determine if player clicked top face of the ghast
                        assert entityHitResult != null;
                        Vec3 hitLoc = entityHitResult.getLocation();

                        //Determine exact face clicked by comparing hit location to ghast's center
                        Direction hitFace = getDirection(happyGhast, hitLoc);

                        //Calculate exact placement block
                        Vec3 normal = Vec3.atLowerCornerOf(hitFace.getUnitVec3i());

                        //Nudge block inward to get the physical block space occupied by the ghast
                        BlockPos anchorPos = BlockPos.containing(hitLoc.subtract(normal.scale(0.01)));

                        //Shift outward by exactly 1 block in the direction of the clicked face
                        BlockPos placePos = anchorPos.relative(hitFace);

                        //Create fake placement context to get correct block rotations
                        BlockHitResult fakeHit = new BlockHitResult(hitLoc, hitFace, anchorPos, false);
                        BlockPlaceContext ctx = new BlockPlaceContext(player, interactionHand, stack, fakeHit);
                        BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(ctx);
                        if (stateToPlace == null) stateToPlace = blockItem.getBlock().defaultBlockState();

                        //Only allow placing full blocks to prevent bugged states
                        if (!Block.isShapeFullBlock(stateToPlace.getCollisionShape(level, placePos))) return InteractionResult.PASS;

                        //Ensure block can physically survive here and isn't replacing something unbreakable
                        if (stateToPlace.canSurvive(level, placePos) && level.isUnobstructed(stateToPlace, placePos, CollisionContext.of(player))) {
                            level.setBlock(placePos, stateToPlace, 3);
                            level.playSound(null, placePos, stateToPlace.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);

                            //Consume block if in survival
                            if (!player.isCreative()) stack.shrink(1);
                        }
                    }
                    //Return success on both client and server to play arm swing and stop further checks
                    return InteractionResult.SUCCESS;
                }
            }

            //Check if minecart changes are enabled in configs
            if (DJsConfig.getInstance().enableMinecartChanges) {
                if (entity instanceof AbstractMinecart minecart && player.isCrouching()) {
                    UUID minecartId = minecart.getUUID();
                    ItemStack stack = player.getItemInHand(interactionHand);
                    String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    ILinkableMinecart linkable = (ILinkableMinecart) minecart;

                    //Check if the player is trying to link minecarts together with a chain
                    if (itemName.contains("_chain")) {
                        if (!level.isClientSide()) {
                            UUID playerId = player.getUUID();

                            //First interacted minecart is the parent
                            if (!PENDING_LINKS.containsKey(playerId)) {
                                PENDING_LINKS.put(playerId, minecartId);
                                pendingLinkMinecartId = minecart.getId();
                                player.displayClientMessage(Component.literal("Parent selected; link a second cart to attach it as a follower"), true);
                            }
                            //Second interacted minecart is the child
                            else {
                                UUID parentId = PENDING_LINKS.remove(playerId);
                                Entity parentEntity = level.getEntity(parentId);

                                //Cancel link if clicking the same minecart twice
                                if (parentId.equals(minecartId)) {
                                    player.displayClientMessage(Component.literal("Link cancelled"), true);
                                    pendingLinkMinecartId = -1;
                                    return InteractionResult.SUCCESS;
                                }

                                if (parentEntity instanceof AbstractMinecart parentMinecart && !parentId.equals(minecartId)) {
                                    ILinkableMinecart parentLinkable = (ILinkableMinecart) parentMinecart;

                                    //Prevent pairing child with parent
                                    if (parentLinkable.djs$getTrainMinecarts(level).contains(minecart)) {
                                        player.displayClientMessage(Component.literal("Link failed; minecarts are already in the same train"), true);
                                        pendingLinkMinecartId = -1;
                                        return InteractionResult.SUCCESS;
                                    }

                                    //If minecarts are too far apart, notify player and fail connection
                                    if (minecart.distanceTo(parentMinecart) > 4.0) {
                                        player.displayClientMessage(Component.literal("Link failed; the minecarts are too far apart"), true);
                                        pendingLinkMinecartId = -1;
                                    }
                                    //Ensure parent doesn't already have a child, and child doesn't already have a parent
                                    else if (parentLinkable.djs$getChild() == null && linkable.djs$getParent() == null) {
                                        parentLinkable.djs$setChild(minecartId);
                                        parentLinkable.djs$setChainToChild(itemName);
                                        linkable.djs$setParent(parentId);

                                        player.displayClientMessage(Component.literal("Train linked"), true);
                                        if (!player.isCreative()) stack.shrink(1);
                                        pendingLinkMinecartId = -1;

                                        syncMinecartLinks(minecart, (ServerLevel) level);
                                        syncMinecartLinks(parentMinecart, (ServerLevel) level);
                                    }
                                    //If minecart is already linked, notify player
                                    else {
                                        player.displayClientMessage(Component.literal("Link failed; one of these minecarts is already connected in that direction"), true);
                                        pendingLinkMinecartId = -1;
                                    }
                                }
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                    //Check if the player is trying to shear a linked minecart
                    else if (stack.is(Items.SHEARS)) {
                        if (linkable.djs$getParent() != null || linkable.djs$getChild() != null) {
                            if (!level.isClientSide()) {
                                //Sever from parent
                                if (linkable.djs$getParent() != null) {
                                    Entity parentEntity = level.getEntity(linkable.djs$getParent());
                                    if (parentEntity != null) {
                                        ((ILinkableMinecart) parentEntity).djs$setChild(null);
                                        ((ILinkableMinecart) parentEntity).djs$setChainToChild(null);
                                        syncMinecartLinks(parentEntity, (ServerLevel) level);
                                    }
                                    linkable.djs$setParent(null);
                                }
                                //Sever from child
                                if (linkable.djs$getChild() != null) {
                                    Entity childEntity = level.getEntity(linkable.djs$getChild());
                                    if (childEntity != null) {
                                        ((ILinkableMinecart) childEntity).djs$setParent(null);
                                        syncMinecartLinks(childEntity, (ServerLevel) level);
                                    }
                                    minecart.spawnAtLocation((ServerLevel) level, BuiltInRegistries.ITEM.getValue(Identifier.parse(linkable.djs$getChainToChild() != null ? linkable.djs$getChainToChild() : "minecraft:chain")));
                                    linkable.djs$setChild(null);
                                    linkable.djs$setChainToChild(null);
                                }

                                player.displayClientMessage(Component.literal("Minecart severed from the train"), true);
                                if (!player.isCreative()) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

                                syncMinecartLinks(minecart, (ServerLevel) level);
                            }
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
            //Fallback for all other entities and conditions
            return InteractionResult.PASS;
        }));


        //Add custom potions
        FabricBrewingRecipeRegistryBuilder.BUILD.register(builder -> {
            //Awkward potion + golden apple = potion of luck
            builder.addMix(Potions.AWKWARD, Items.GOLDEN_APPLE, Potions.LUCK);

            //Awkward potion + glow berries = potion of glowing
            builder.addMix(Potions.AWKWARD, Items.GLOW_BERRIES, GLOWING_POTION);
            //Potion of glowing + redstone = long potion of glowing
            builder.addMix(GLOWING_POTION, Items.REDSTONE, LONG_GLOWING_POTION);
        });


        //Add new custom trial keys to creative menu
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> entries.addAfter(Items.OMINOUS_TRIAL_KEY, GUARDIAN_TRIAL_KEY, OMINOUS_GUARDIAN_TRIAL_KEY, END_TRIAL_KEY, OMINOUS_END_TRIAL_KEY));

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            //Fletching table interceptor
            if (level.getBlockState(hitResult.getBlockPos()).is(Blocks.FLETCHING_TABLE)) {
                if (!level.isClientSide()) {
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, inventory, p) -> new DJsFletchingMenu(containerId, inventory, ContainerLevelAccess.create(level, hitResult.getBlockPos())),
                            //Fallback title
                            Component.literal("Fletching")
                    ));
                }
                return InteractionResult.SUCCESS;
            }

            //Check if anvil and enchanting changes are enabled in configs
            if (DJsConfig.getInstance().enableAnvilAndEnchantingChanges) {
                //Grindstone interceptor
                if (level.getBlockState(hitResult.getBlockPos()).is(Blocks.GRINDSTONE)) {
                    if (!level.isClientSide()) {
                        player.openMenu(new SimpleMenuProvider(
                                (containerId, inventory, p) -> new DJsGrindstoneMenu(containerId, inventory, ContainerLevelAccess.create(level, hitResult.getBlockPos())),
                                Component.literal("Grindstone")
                        ));
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            //Check if campfire spawn changes are enabled in configs
            if (DJsConfig.getInstance().enableCampfireRespawningInsteadOfBed) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = level.getBlockState(pos);

                //Check if block is a lit campfire
                if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                    //Ensure player's hand is empty to prevent accidental spawn setting
                    if (player.getItemInHand(hand).isEmpty()) {
                        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                            //Set spawn point at campfire
                            ServerPlayer.RespawnConfig newSpawn = new ServerPlayer.RespawnConfig(
                                    LevelData.RespawnData.of(level.dimension(), pos, player.getYRot(), 0.0F),
                                    true
                            );

                            //Set respawn position
                            serverPlayer.setRespawnPosition(newSpawn, false);

                            //Add entity tag to player
                            serverPlayer.addTag("djs_campfire_spawn");

                            //Trigger physical hand swing and display message
                            serverPlayer.swing(hand, true);
                            serverPlayer.displayClientMessage(Component.translatable("block.minecraft.set_spawn"), true);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        //Check if village changes are enabled in configs
        if (DJsConfig.getInstance().enableVillageChanges) {
            //Villager crop stealing mechanic
            PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
                //If the block broken is a crop or hay block
                if (state.is(BlockTags.CROPS) || state.is(Blocks.HAY_BLOCK)) {
                    //Ignore if within range of a beacon with an upgrade preventing villager aggression
                    if (!level.isClientSide() && !BeaconTracker.hasUpgrade(level, pos, "angry_villager")) {
                        ServerLevel serverLevel = (ServerLevel) level;

                        //Scan radius for villagers
                        AABB scanArea = new AABB(pos).inflate(16.0);
                        List<net.minecraft.world.entity.npc.villager.Villager> villagers = serverLevel.getEntitiesOfClass(Villager.class, scanArea);

                        //Loop over all villagers within the radius
                        boolean angeredSomeone = false;
                        for (Villager villager : villagers) {
                            //Ensure villagers can actually see the player breaking it
                            if (villager.hasLineOfSight(player)) {
                                //Add negative gossip
                                villager.getGossips().add(player.getUUID(), GossipType.MINOR_NEGATIVE, 25);

                                //Spawn angry particles above the villager's head
                                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, villager.getX(), villager.getY() + 1.5, villager.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
                                angeredSomeone = true;
                            }
                        }

                        //Check if a villager was upset
                        if (angeredSomeone) {
                            //Alert iron golems in the area
                            List<IronGolem> ironGolems = serverLevel.getEntitiesOfClass(IronGolem.class, scanArea);
                            for (IronGolem ironGolem : ironGolems) {
                                if (ironGolem.hasLineOfSight(player)) ironGolem.setTarget(player);
                            }
                        }
                    }
                }
            });
        }

        //Create tag key for ancient city structure
        TagKey<@NotNull Structure> ancientCityTag = TagKey.create(Registries.STRUCTURE, Identifier.parse("djsfixedprogression:ancient_city_map"));

        //Add ancient city map to expert (level 5) cartographers
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CARTOGRAPHER, 5, factories -> factories.add(new VillagerTrades.TreasureMapForEmeralds(
                48, //Emerald cost
                ancientCityTag, //Tag key for ancient city
                "filled_map.ancient_city",
                MapDecorationTypes.TARGET_X, //The red 'X' on the map
                1, //Max uses before the villager locks the trade
                15 //Villager XP granted for the trade
        )));
    }

    /**
     * Helper method to determine the face of the happy ghast the player is placing a block off of.
     */
    private static @NotNull Direction getDirection(HappyGhast happyGhast, Vec3 hitLoc) {
        double dx = hitLoc.x - happyGhast.getX();
        double dy = hitLoc.y - (happyGhast.getY() + happyGhast.getBbHeight() / 2.0);
        double dz = hitLoc.z - happyGhast.getZ();
        Direction hitFace;
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) hitFace = dx > 0 ? Direction.EAST : Direction.WEST;
        else if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) hitFace = dy > 0 ? Direction.UP : Direction.DOWN;
        else hitFace = dz > 0 ? Direction.SOUTH : Direction.NORTH;
        return hitFace;
    }

    /**
     * Helper method to easily set the standard armor, toughness, and knockback resistance of any piece of armor.
     */
    private static void applyArmorStats(DefaultItemComponentEvents.ModifyContext context, Item item, int maxDurability, double armor, double toughness, double knockbackResistance, String slotName, EquipmentSlotGroup slotGroup) {
        context.modify(item, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, maxDurability);

            ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder().add(Attributes.ARMOR, new AttributeModifier(Identifier.parse("minecraft:armor." + slotName), armor, AttributeModifier.Operation.ADD_VALUE), slotGroup);

            //Only add toughness and knockback resistance if they are greater than 0
            if (toughness > 0) modifiers.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(Identifier.parse("minecraft:armor." + slotName), toughness, AttributeModifier.Operation.ADD_VALUE), slotGroup);
            if (knockbackResistance > 0) modifiers.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(Identifier.parse("minecraft:armor." + slotName), knockbackResistance, AttributeModifier.Operation.ADD_VALUE), slotGroup);

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.build());
        });
    }

    /**
     * Helper method to easily set the total attack damage, attack speed, and attack reach of any tool.
     */
    private static void applyToolStats(DefaultItemComponentEvents.ModifyContext context, Item item, int maxDurability, double totalAttackDamage, double attackSpeed, double attackReach) {
        context.modify(item, builder -> {
            builder.set(DataComponents.MAX_DAMAGE, maxDurability);

            //Adjusting input values according to default minecraft values
            double modifierDamage = totalAttackDamage - 1.0;
            double modifierSpeed = attackSpeed - 4.0;
            double modifierReach = attackReach - 3.0;

            ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Identifier.parse("minecraft:base_attack_damage"), modifierDamage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(Identifier.parse("minecraft:base_attack_speed"), modifierSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

            //Only inject reach attribute if it differs from vanilla reach (3)
            if (modifierReach != 0) modifiers.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(Identifier.parse("minecraft:base_entity_interaction_range"), modifierReach, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.build());
        });
    }

    /**
     * Helper method for copper tools. Applies tool stats, but also injects the stone tool mining component.
     */
    private static void applyCopperToolBase(DefaultItemComponentEvents.ModifyContext context, Item copperItem, Item stoneEquivalent, int maxDurability, double totalAttackDamage, double attackSpeed, double attackReach) {
        applyToolStats(context, copperItem, maxDurability, totalAttackDamage, attackSpeed, attackReach);
        context.modify(copperItem, builder -> builder.set(DataComponents.TOOL, new ItemStack(stoneEquivalent).get(DataComponents.TOOL)));
    }

    /**
     * Helper method to safely package and broadcast a minecart's link state to all nearby clients.
     */
    public static void syncMinecartLinks(Entity minecart, ServerLevel level) {
        ILinkableMinecart linkable = (ILinkableMinecart) minecart;

        int parentId = -1;
        if (linkable.djs$getParent() != null) {
            Entity parentMinecart = level.getEntity(linkable.djs$getParent());
            if (parentMinecart != null) parentId = parentMinecart.getId();
        }

        int childId = -1;
        if (linkable.djs$getChild() != null) {
            Entity childMinecart = level.getEntity(linkable.djs$getChild());
            if (childMinecart != null) childId = childMinecart.getId();
        }

        MinecartLinkPayload payload = new MinecartLinkPayload(minecart.getId(), parentId, childId);
        for (ServerPlayer sp : PlayerLookup.tracking(minecart)) {
            ServerPlayNetworking.send(sp, payload);
        }
    }

    /**
     * Method for tracking player votes to skip the night.
     */
    public static void attemptSleepVote(ServerPlayer player, boolean isFromButton) {
        //Get the time of day
        ServerLevel level = player.level().getLevel();
        long timeOfDay = level.getDayTime() % 24000;

        //Prevent voting from old chat messages during the day
        if (timeOfDay >= 0 && timeOfDay < 12542) return;

        //Enforce delayed sleep config for chat voting
        if (DJsConfig.getInstance().enableDelayedSleep) {
            if (timeOfDay < DJsConfig.getInstance().minimumSleepTime) return;
        }

        //Ensure someone is in a bed to accept any votes
        boolean anyoneSleeping = level.players().stream().anyMatch(Player::isSleeping);
        if (!anyoneSleeping) return;

        //Skip night vote must be initiated from a player in a bed
        if (SLEEP_VOTES.isEmpty() && !isFromButton) return;

        //Block vote skipping if voting player is in the dark
        if (!player.isCreative() && level.getDifficulty() != Difficulty.PEACEFUL && level.getBrightness(LightLayer.BLOCK, player.blockPosition().above()) <= 4) {
            player.displayClientMessage(Component.literal("You cannot vote to skip the night while in the dark").withStyle(ChatFormatting.WHITE), false);
            return;
        }

        //Ensure the player meets the XP requirement to vote
        int cost = DJsConfig.getInstance().bedXPCost;
        if (cost > 0 && !player.isCreative() && player.experienceLevel < cost) {
            player.displayClientMessage(Component.literal("You need at least " + cost + " XP levels to skip the night").withStyle(ChatFormatting.WHITE), false);
            return;
        }

        //Add vote, ensuring player hasn't already voted
        if (!SLEEP_VOTES.add(player.getUUID())) {
            player.displayClientMessage(Component.literal("You have already voted").withStyle(ChatFormatting.WHITE), false);
            return;
        }

        //Tell player's client their vote was successful
        ServerPlayNetworking.send(player, new SleepVoteSuccessPayload());

        //Warn player not in bed about remaining in a lit area
        if (!isFromButton) player.displayClientMessage(Component.literal("Vote cast; remain in a well-lit area").withStyle(ChatFormatting.WHITE), true);

        //Get total player count
        int totalPlayers = level.players().size();

        //Get count of players who voted
        int votes = SLEEP_VOTES.size();

        //Fetch the server's native sleeping percentage requirement
        int requiredPercent = level.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);

        //Calculate the actual number of players needed to sleep (minimum 1)
        int requiredPlayers = Math.max(1, (int) Math.ceil((totalPlayers * requiredPercent) / 100.0));

        //Check if the required number of players is either sleeping or voting to skip the night
        if (totalPlayers > 0 && votes >= requiredPlayers) {
            //Instantly skip to morning
            level.setDayTime(level.getDayTime() + 24000 - (level.getDayTime() % 24000));
            level.getServer().tickRateManager().setTickRate(20.0F);

            //Charge XP to everyone who voted
            for (ServerPlayer p : level.players()) {
                if (p.isSleeping() || SLEEP_VOTES.contains(p.getUUID())) {
                    if (!p.isCreative() && p.experienceLevel >= cost) p.giveExperienceLevels(-cost);
                }

                //Kick everyone out of their beds
                if (p.isSleeping()) p.stopSleepInBed(true, true);
            }

            //Clear votes for next night
            SLEEP_VOTES.clear();

            //Broadcast cancel payload for the next night
            for (ServerPlayer p : level.players()) ServerPlayNetworking.send(p, new SleepVoteCancelPayload());

            //Notify all players
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("The night was skipped").withStyle(ChatFormatting.WHITE), false);
        }
        //Not enough votes, broadcast clickable prompt
        else {
            //Only broadcast interactive prompt for the first vote
            if (SLEEP_VOTES.size() == 1) {
                //Message for person who initiated the vote
                Component baseMsg = Component.literal(player.getScoreboardName() + " wants to skip the night (" + votes + "/" + requiredPlayers + " votes) ").withStyle(ChatFormatting.WHITE);

                //Message for other players
                Component promptMsg = baseMsg.copy()
                        .append(Component.literal("[Vote Yes]")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
                                .withStyle(style -> style
                                        .withClickEvent(new ClickEvent.RunCommand("/djs_vote_sleep"))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to vote to skip the night")))
                                )
                        );

                //Send clickable prompt to other players
                for (ServerPlayer p : level.players()) {
                    if (SLEEP_VOTES.contains(p.getUUID())) p.sendSystemMessage(baseMsg, false);
                    else p.sendSystemMessage(promptMsg, false);
                }
            }
            //Subsequent votes just silently log to chat to prevent massive prompt spam
            else level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getScoreboardName() + " voted to skip the night (" + votes + "/" + requiredPlayers + " votes)" ).withStyle(ChatFormatting.WHITE), false);
        }
    }
}