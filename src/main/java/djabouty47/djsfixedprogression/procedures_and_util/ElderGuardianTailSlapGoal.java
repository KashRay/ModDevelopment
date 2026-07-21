package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.ElderGuardian;

import java.util.EnumSet;
import java.util.List;

public class ElderGuardianTailSlapGoal extends Goal {
    private final ElderGuardian guardian;
    private int animationTicks;
    private long nextAttackTime;

    public ElderGuardianTailSlapGoal(ElderGuardian guardian) {
        this.guardian = guardian;
        this.setFlags(EnumSet.of(Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        //Enforce cooldown
        if (this.guardian.level().getGameTime() < this.nextAttackTime) return false;

        LivingEntity target = this.guardian.getTarget();
        //Trigger spin attack if target is within 6 blocks
        return target != null && target.isAlive() && this.guardian.distanceToSqr(target) < 36.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return this.animationTicks < 30;
    }

    @Override
    public void start() {
        this.animationTicks = 0;
        this.guardian.getNavigation().stop();

        //Play attack warning sound
        this.guardian.playSound(SoundEvents.ELDER_GUARDIAN_FLOP, 2.0F, 0.5F);
    }

    @Override
    public void tick() {
        this.animationTicks++;

        //Charge up phase
        if (this.animationTicks < 20) {
            //Look at player and spawn warning splash particles
            LivingEntity target = this.guardian.getTarget();
            if (target != null) this.guardian.getLookControl().setLookAt(target, 90.0F, 90.0F);
            if (this.guardian.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.SPLASH, this.guardian.getX(), this.guardian.getY() + 1.0, this.guardian.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
        }
        //Spin phase
        else {
            //Save previous rotations for client interpolation
            this.guardian.yRotO = this.guardian.getYRot();
            this.guardian.yBodyRotO = this.guardian.yBodyRot;
            this.guardian.yHeadRotO = this.guardian.yHeadRot;

            //Spin elder guardian while attacking
            float newRot = this.guardian.getYRot() + 72.0F;
            this.guardian.setYRot(newRot);
            this.guardian.setYBodyRot(newRot);
            this.guardian.setYHeadRot(newRot);

            //Play spin attack sound
            this.guardian.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2.0F, 1.0F);

            //Execute strike halfway through spin
            if (this.animationTicks == 25 && this.guardian.level() instanceof ServerLevel level) {
                //Get tail slap bounding box
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, this.guardian.getBoundingBox().inflate(4.0D));

                //Determine damage depending on difficulty
                float slapDamage = switch (level.getDifficulty()) {
                    case PEACEFUL -> 0.0F;
                    case EASY -> 4.0F;
                    case NORMAL -> 6.0F;
                    case HARD -> 8.0F;
                };

                //Damage all entities within the slap range
                for (LivingEntity entity : targets) {
                    if (entity != this.guardian) {
                        //Send massive sonic boom particle upon a hit
                        level.sendParticles(ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY(0.5), entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

                        //Damage and knockback entity
                        entity.hurtServer(level, level.damageSources().mobAttack(this.guardian), slapDamage);
                        entity.knockback(1.5D, this.guardian.getX() - entity.getX(), this.guardian.getZ() - entity.getZ());
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        //Initiate cooldown before it can tail slap again
        this.nextAttackTime = this.guardian.level().getGameTime() + 80L;
    }
}
