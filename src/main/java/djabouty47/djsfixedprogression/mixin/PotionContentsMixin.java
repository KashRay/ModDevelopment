package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {
    @Shadow public abstract boolean is(Holder<@NotNull Potion> holder);

    /**
     * Inject code to force base splash and lingering potions to burst instead of fizzling out.
     */
    @Inject(method = "hasEffects", at = @At("HEAD"), cancellable = true)
    private void forceBasePotionsToHaveEffects(CallbackInfoReturnable<Boolean> cir) {
        if (this.is(Potions.WATER) || this.is(Potions.AWKWARD) || this.is(Potions.THICK) || this.is(Potions.MUNDANE)) cir.setReturnValue(true);
    }

    /**
     * Inject code so base splash potions, lingering potions, and tipped arrows apply effects on hit.
     */
    @Inject(method = "getAllEffects", at = @At("RETURN"), cancellable = true)
    private void injectSplashAndLingeringEffects(CallbackInfoReturnable<Iterable<MobEffectInstance>> cir) {
        //Awkward potions give nausea
        if (this.is(Potions.AWKWARD)) {
            List<MobEffectInstance> effects = new ArrayList<>();
            cir.getReturnValue().forEach(effects::add);
            effects.add(new MobEffectInstance(MobEffects.NAUSEA, 600, 0));
            cir.setReturnValue(effects);
        }
        //Thick potions give resistance
        else if (this.is(Potions.THICK)) {
            List<MobEffectInstance> effects = new ArrayList<>();
            cir.getReturnValue().forEach(effects::add);
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
            cir.setReturnValue(effects);
        }
        //Mundane potions cleanse an effect
        else if (this.is(Potions.MUNDANE)) {
            List<MobEffectInstance> effects = new ArrayList<>();
            cir.getReturnValue().forEach(effects::add);
            effects.add(new MobEffectInstance(DJsFixedProgression.CLEANSE_EFFECT, 1, 0));
            cir.setReturnValue(effects);
        }
    }

    /**
     * Inject code so drinking base potions directly applies effects natively.
     */
    @Inject(method = "forEachEffect", at = @At("TAIL"))
    private void injectDrinkingEffects(Consumer<MobEffectInstance> consumer, float f, CallbackInfo ci) {
        if (this.is(Potions.AWKWARD)) consumer.accept(new MobEffectInstance(MobEffects.NAUSEA, 600, 0).withScaledDuration(f));
        else if (this.is(Potions.THICK)) consumer.accept(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0).withScaledDuration(f));
        else if (this.is(Potions.MUNDANE)) consumer.accept(new MobEffectInstance(DJsFixedProgression.CLEANSE_EFFECT, 1, 0).withScaledDuration(f));
    }

    /**
     * Inject code when determining potion color, adjusting base potion color and glowing potions to be more thematic.
     */
    @Inject(method = "getColorOr", at = @At("HEAD"), cancellable = true)
    private void overridePotionColors(int defaultColor, CallbackInfoReturnable<Integer> cir) {
        //Awkward potions are a sickly yellow-green
        if (this.is(Potions.AWKWARD)) cir.setReturnValue(0x94A061);
        //Glowing potions are a bright gold
        else if (this.is(DJsFixedProgression.GLOWING_POTION) || this.is(DJsFixedProgression.LONG_GLOWING_POTION)) cir.setReturnValue(0xFFD700);
    }
}
