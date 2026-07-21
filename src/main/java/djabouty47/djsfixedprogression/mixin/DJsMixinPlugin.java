package djabouty47.djsfixedprogression.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class DJsMixinPlugin implements IMixinConfigPlugin {
    //Default values in case config file hasn't been generated yet
    private boolean enableHungerChanges = true;
    private boolean enableHappyGhastChanges = true;
    private boolean enablePhantomChanges = true;

    @Override
    public void onLoad(String mixinPackage) {
        //Find config file in fabric config directory
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("djs-fixed-progression");

        //Safely read raw JSON before minecraft boots up
        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("enableHungerAndRegenerationChanges")) this.enableHungerChanges = json.get("enableHungerAndRegenerationChanges").getAsBoolean();
                if (json.has("enableHappyGhastChanges")) this.enableHappyGhastChanges = json.get("enableHappyGhastChanges").getAsBoolean();
                if (json.has("enablePhantomChanges")) this.enablePhantomChanges = json.get("enablePhantomChanges").getAsBoolean();
            }
            catch (Exception e) {
                System.err.println("[DJsFixedProgression] Failed to read config in MixinPlugin, defaulting to true.");
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        //Only block specific mixins that act as hard-overwrites, leave the rest alone so they can safely load their interfaces
        if (mixinClassName.endsWith("FoodDataMixin")) return this.enableHungerChanges;
        if (mixinClassName.endsWith("HappyGhastMixin")) return this.enableHappyGhastChanges;
        if (mixinClassName.endsWith("PhantomSpawnerMixin")) return this.enablePhantomChanges;

        //Allow all other mixins to load normally
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
