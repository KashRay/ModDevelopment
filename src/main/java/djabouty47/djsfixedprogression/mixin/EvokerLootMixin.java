package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Mob.class)
public class EvokerLootMixin {
    /**
     * Inject code at the start of each tick, branding any evoker's that are a part of a raid with a custom tag preventing totem drops.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tagRaidEvokers(CallbackInfo ci) {
        //Check if raid totem changes are enabled in configs
        if (!DJsConfig.getInstance().removeTotemFromRaids) return;

        //Check if mob is an evoker
        if ((Object) this instanceof Evoker evoker) {
            //If evoker is a part of a raid and hasn't been tagged yet, mark it
            if (evoker.getCurrentRaid() != null && !evoker.getTags().contains("djs_raid_evoker")) evoker.addTag("djs_raid_evoker");
        }
    }

    /**
     * Inject code when getting a mob's loot table, swapping marked evoker loot tables with one that doesn't contain a totem.
     */
    @Inject(method = "getLootTable", at = @At("HEAD"), cancellable = true)
    private void swapRaidEvokerLoot(CallbackInfoReturnable<Optional<ResourceKey<@NotNull LootTable>>> cir) {
        //Check if raid totem changes are enabled in configs
        if (!DJsConfig.getInstance().removeTotemFromRaids) return;

        //Check if mob is an evoker
        if ((Object) this instanceof Evoker evoker) {
            //Check if evoker has been branded
            if (evoker.getTags().contains("djs_raid_evoker")) {
                //Override vanilla loot table with custom totem-less table
                cir.setReturnValue(Optional.of(ResourceKey.create(
                        Registries.LOOT_TABLE,
                        Identifier.parse("djsfixedprogression:entities/raid_evoker")
                )));
            }
        }
    }
}
