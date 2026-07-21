package djabouty47.djsfixedprogression.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;

public class DJsConfig {
    //Set up the GSON parser
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("djs_fixed_progression.json").toFile();

    //Settings
    public boolean enableToolAndArmorRebalance = true;
    public boolean enablePhantomChanges = true;
    public int bedXPCost = 5; //Number of xp levels
    public boolean enableDelayedSleep = true;
    public int minimumSleepTime = 13500; //Vanilla sleep time is at 13000, mobs start spawning at 13188
    public boolean enableIdleHungerDrain = true;
    public float idleExhaustionPerSecond = 0.1f; //Hidden exhaustion to add per second (4.0 exhaustion = -1/2 a hunger point)
    public boolean enableCombatChanges = true;
    public boolean enableHungerAndRegenerationChanges = true;
    public boolean enableXPChanges = true;
    public int flatXPPerLevel = 30;
    public boolean enableAnvilAndEnchantingChanges = true;
    public boolean enableElytraChanges = true;
    public boolean enableMinecartChanges = true;
    public boolean enableCustomMinecartHoppingPhysics = true;
    public boolean enableHappyGhastChanges = true;
    public boolean enableCampfireRespawningInsteadOfBed = false;
    public boolean enableBeaconChanges = true;
    public boolean enableVillageChanges = true;
    public boolean enablePigAndSnifferChanges = true;
    public boolean undergroundMobsMoreArmored = true;
    public boolean removeTotemFromRaids = true;

    //Singleton logic
    private static DJsConfig instance;

    public static DJsConfig getInstance() {
        if  (instance == null) load();
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, DJsConfig.class);
            }
            catch (IOException e) {
                e.printStackTrace();
                instance = new DJsConfig();
            }
        }
        else {
            instance = new DJsConfig();
            save();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
