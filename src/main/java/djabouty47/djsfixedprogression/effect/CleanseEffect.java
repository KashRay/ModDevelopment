package djabouty47.djsfixedprogression.effect;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CleanseEffect extends InstantenousMobEffect {
    public CleanseEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyInstantenousEffect(@NotNull ServerLevel serverLevel, @Nullable Entity entity, @Nullable Entity entity2, @NotNull LivingEntity livingEntity, int i, double d) {
        //Apply cleanse effect for mundane splash potion and tipped arrows
        this.applyCleanse(livingEntity);
    }

    @Override
    public boolean applyEffectTick(@NotNull ServerLevel serverLevel, @NotNull LivingEntity livingEntity, int amplifier) {
        //Apply cleanse effect for mundane potions and lingering potion clouds
        this.applyCleanse(livingEntity);
        return true;
    }

    private void applyCleanse(LivingEntity livingEntity) {
        if (!livingEntity.level().isClientSide()) {
            List<Holder<@NotNull MobEffect>> negativeEffects = new ArrayList<>();

            //Isolate harmful effects
            for (MobEffectInstance instance : livingEntity.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    negativeEffects.add(instance.getEffect());
                }
            }

            //Remove one at random
            if (!negativeEffects.isEmpty()) {
                Holder<@NotNull MobEffect> toRemove = negativeEffects.get(livingEntity.getRandom().nextInt(negativeEffects.size()));
                livingEntity.removeEffect(toRemove);
            }
        }
    }
}
