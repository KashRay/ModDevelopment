package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ElderGuardianDashGoal extends Goal {
    private final ElderGuardian guardian;
    private final TagKey<@NotNull Block> smashableTag;
    private int dashTicks;
    private Vec3 dashDirection;
    private int currentDashCount;
    private int maxDashes;
    private long nextDashTime;

    public ElderGuardianDashGoal(ElderGuardian guardian, TagKey<@NotNull Block> smashableTag) {
        this.guardian = guardian;
        this.smashableTag = smashableTag;
        this.setFlags(EnumSet.of(Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.guardian.getTarget();
        if (target == null || !target.isAlive()) return false;

        //Get health
        float hp = this.guardian.getHealth();

        //Prevent dashing if phase 1
        if (hp > 100.0F) return false;

        //Perform dash attack once cooldown expires
        return this.guardian.level().getGameTime() >= this.nextDashTime;
    }

    @Override
    public void start() {
        this.dashTicks = 0;
        this.currentDashCount = 0;

        //Get max number of dashes depending on phase (phase 2 = 1, phase 3 = 3)
        this.maxDashes = this.guardian.getHealth() <= 50.0F ? 3 : 1;

        this.guardian.getNavigation().stop();

        //Play dash charge up sound
        this.guardian.playSound(SoundEvents.GUARDIAN_ATTACK, 2.0F, 0.5F);
    }

    @Override
    public boolean canContinueToUse() {
        return this.currentDashCount < this.maxDashes;
    }

    @Override
    public void tick() {
        this.dashTicks++;
        LivingEntity target = this.guardian.getTarget();

        //Tracking phase
        if (this.dashTicks < 30) {
            //Only update lock-on direction if dash tick count is under 20, allowing player to dodge dash attack
            if (target != null && this.dashTicks < 20) {
                this.guardian.getLookControl().setLookAt(target, 90.0F, 90.0F);
                this.dashDirection = target.position().subtract(this.guardian.position()).normalize();
            }
            if (this.guardian.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.guardian.getX(), this.guardian.getY() + 1.0, this.guardian.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        }
        //Dashing phase
        else if (this.dashTicks >= 30 && this.dashTicks < 40 && this.dashDirection != null) {
            //Propel forward
            this.guardian.setDeltaMovement(this.dashDirection.scale(0.8D));

            //Play dash attack sound
            this.guardian.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 2.0F, 1.0F);

            //Smash blocks in its path
            if (this.guardian.level() instanceof ServerLevel level) {
                AABB breakBox = this.guardian.getBoundingBox().inflate(0.8D);
                BlockPos.betweenClosedStream(breakBox).forEach(pos -> {
                    BlockState state = level.getBlockState(pos);
                    //Only destroy white-listed blocks
                    if (state.is(this.smashableTag)) level.destroyBlock(pos, true);
                });

                //Determine damage depending on difficulty
                float dashDamage = switch (level.getDifficulty()) {
                    case PEACEFUL -> 0.0F;
                    case EASY -> 3.0F;
                    case NORMAL -> 4.5F;
                    case HARD -> 6.0F;
                };

                //Damage any entities it rams into
                level.getEntitiesOfClass(LivingEntity.class, breakBox).forEach(entity -> {
                    if (entity != this.guardian) entity.hurtServer(level, level.damageSources().mobAttack(this.guardian), dashDamage);
                });
            }
        }
        else if (this.dashTicks >= 40) {
            //End of current dash
            this.currentDashCount++;

            //If there are more dashes left (phase 3), chain into the next one immediately
            if (this.currentDashCount < this.maxDashes) {
                this.dashTicks = 10;
                this.guardian.playSound(SoundEvents.GUARDIAN_ATTACK, 2.0F, 0.5F);
            }
        }
    }

    @Override
    public void stop() {
        //Set dash cooldown for the next attack sequence depending on phase
        int baseCooldown = this.guardian.getHealth() <= 50.0F ? 120 : 200;
        this.nextDashTime = this.guardian.level().getGameTime() + baseCooldown + this.guardian.getRandom().nextInt(40);
    }
}
