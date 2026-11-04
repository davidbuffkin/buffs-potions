package com.buffspotions.crab;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BiomeTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A fiddler-style crab. It scuttles sideways much faster than it shuffles forward,
 * and it has a separate wall-climbing state that only moves straight up and down.
 * A blocked claw strike drops the big claw for twenty minutes.
 */
public class Crab extends Animal {
    public static final int REGROW_TICKS = 20 * 60 * 20;
    public static final float SIDEWAYS_SPEED = 0.35F;
    /** Front-back input compared with a full sideways stride. About a fifth as fast. */
    private static final float FORWARD_INPUT = 0.2F;
    /**
     * Sideways input is a third of a full strafe, including while fleeing.
     * Fleeing is still quicker than a stroll because its speed modifier is higher.
     */
    private static final float STRAFE_SCALE = 1.0F / 3.0F;
    /**
     * Idle stroll modifier. With the non-flee strafe scale, steady speed on ordinary
     * ground (drag 0.6 * 0.91) is about one block per second.
     */
    private static final double IDLE_SPEED = 0.1946;
    /** Idle trips between pauses. The pause itself stays the same length. */
    private static final double IDLE_TRAVEL_SPEED = IDLE_SPEED * 2.0;
    private static final double IDLE_CLIMB_PER_TICK = 0.05;
    private static final double CHASE_CLIMB_PER_TICK = 5.14 / 20.0;
    /** A third of the old flee climb, matching the slower flee gait. */
    private static final double FLEE_CLIMB_PER_TICK = 19.27 / 3.0 / 20.0;
    public static final int WAVE_TICKS = 34;
    private static final int AGGRAVATION_TICKS = 20 * 60;
    /** Same distance an ocelot uses when it keeps away from a player. */
    private static final double PLAYER_NOTICE_RANGE = 16.0;

    public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "crab_spawnable_on")
    );

    private static final EntityDataAccessor<Boolean> DATA_CLAW =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> DATA_WALL =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Integer> DATA_WAVE =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_AGGRAVATED =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_CLIMB_BLEND =
            SynchedEntityData.defineId(Crab.class, EntityDataSerializers.FLOAT);

    private long clawRegrowAt;
    private long aggravatedUntil;
    private ClimbMode climbMode = ClimbMode.FLOOR;
    private int climbCooldown;
    private boolean wallRest;
    private double restY;
    private int restTicks;
    private boolean continueAfterSummit;
    private double continueX;
    private double continueY;
    private double continueZ;
    private float climbBlend;
    private float climbBlendO;
    private int climbStuck;
    /** Ticks spent on a real wall, so a brief bump does not roll the body. */
    private int climbTicks;
    /** This crab was on a beach while it was raining, so it may leave once the rain ends. */
    private boolean soaked;

    public Crab(EntityType<? extends Crab> type, Level level) {
        super(type, level);
        this.moveControl = new SidewaysMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, SIDEWAYS_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.STEP_HEIGHT, 0.5);
    }

    public static boolean checkSpawn(
            EntityType<Crab> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random
    ) {
        BlockState ground = level.getBlockState(pos.below());
        boolean bright = EntitySpawnReason.ignoresLightRequirements(reason) || isBrightEnoughToSpawn(level, pos);
        if (!ground.is(SPAWNABLE_ON) || !bright) {
            return false;
        }
        // Beaches use the same list as turtles, at four times the weight. Rain suppresses that entry.
        return !(level instanceof Level world) || !world.isRaining() || !world.getBiome(pos).is(BiomeTags.IS_BEACH);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData groupData
    ) {
        if (groupData == null) {
            groupData = new AgeableMob.AgeableMobGroupData(false);
        }
        SpawnGroupData spawned = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (spawnsAsBaby(reason, level.getRandom().nextInt(10))) {
            this.setBaby(true);
        }
        return spawned;
    }

    /** Natural crabs, including ones placed during world generation, are babies on a roll of 0. */
    static boolean spawnsAsBaby(EntitySpawnReason reason, int roll) {
        return (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && roll == 0;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 5;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal(this, 1.25));
        this.goalSelector.addGoal(2, new ApproachGoal(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.05, stack -> stack.is(Items.KELP), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.05));
        this.goalSelector.addGoal(6, new IdleStrollGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLAW, true);
        builder.define(DATA_CLIMBING, false);
        builder.define(DATA_WALL, Direction.NORTH);
        builder.define(DATA_WAVE, 0);
        builder.define(DATA_AGGRAVATED, false);
        builder.define(DATA_CLIMB_BLEND, 0.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasClaw", this.hasClaw());
        output.putLong("ClawRegrowAt", this.clawRegrowAt);
        output.putLong("AggravatedUntil", this.aggravatedUntil);
        output.putInt("WaveTicks", this.waveTicks());
        output.putBoolean("Climbing", this.isClimbing());
        output.putString("WallFacing", this.getWallFacing().getSerializedName());
        output.putByte("ClimbMode", (byte) this.climbMode.ordinal());
        output.putBoolean("WallRest", this.wallRest);
        output.putDouble("RestY", this.restY);
        output.putInt("RestTicks", this.restTicks);
        output.putFloat("ClimbBlend", this.climbBlend);
        output.putBoolean("Soaked", this.soaked);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setHasClaw(input.getBooleanOr("HasClaw", true));
        this.clawRegrowAt = input.getLongOr("ClawRegrowAt", 0L);
        this.aggravatedUntil = input.getLongOr("AggravatedUntil", 0L);
        this.setWaveTicks(input.getIntOr("WaveTicks", 0));
        Direction facing = Direction.byName(input.getStringOr("WallFacing", "north"));
        this.setWallFacing(facing == null || facing.getAxis().isVertical() ? Direction.NORTH : facing);
        int mode = input.getByteOr("ClimbMode", (byte) 0);
        ClimbMode[] modes = ClimbMode.values();
        this.climbMode = mode >= 0 && mode < modes.length ? modes[mode] : ClimbMode.FLOOR;
        this.setClimbing(input.getBooleanOr("Climbing", false) && this.climbMode != ClimbMode.FLOOR);
        if (!this.isClimbing()) {
            this.climbMode = ClimbMode.FLOOR;
        }
        this.wallRest = input.getBooleanOr("WallRest", false) && this.isClimbing();
        this.restY = input.getDoubleOr("RestY", 0.0);
        this.restTicks = input.getIntOr("RestTicks", 0);
        this.setClimbBlend(input.getFloatOr("ClimbBlend", this.isClimbing() ? 1.0F : 0.0F));
        this.soaked = input.getBooleanOr("Soaked", false);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.soaked && !this.level().isRaining();
    }

    public boolean hasClaw() {
        return this.entityData.get(DATA_CLAW);
    }

    public void setHasClaw(boolean hasClaw) {
        this.entityData.set(DATA_CLAW, hasClaw);
    }

    public boolean isClimbing() {
        return this.entityData.get(DATA_CLIMBING);
    }

    /**
     * Suffocation tests a box four-fifths as wide as the crab, centered on its eyes.
     * The wall tuck leaves that box in the block even when the shell is only a few pixels in.
     */
    @Override
    public boolean isInWall() {
        return !this.isClimbing() && super.isInWall();
    }

    public void setClimbing(boolean climbing) {
        this.entityData.set(DATA_CLIMBING, climbing);
    }

    public Direction getWallFacing() {
        return this.entityData.get(DATA_WALL);
    }

    public void setWallFacing(Direction facing) {
        this.entityData.set(DATA_WALL, facing);
    }

    public int waveTicks() {
        return this.entityData.get(DATA_WAVE);
    }

    public void setWaveTicks(int ticks) {
        this.entityData.set(DATA_WAVE, ticks);
    }

    public boolean isAggravated() {
        if (this.level().isClientSide()) {
            return this.entityData.get(DATA_AGGRAVATED);
        }
        return this.level().getGameTime() < this.aggravatedUntil;
    }

    public void setAggravated(boolean aggravated) {
        this.entityData.set(DATA_AGGRAVATED, aggravated);
    }

    /** Aggravated, still has its claw, and is at half health or better: creep in and strike. A baby never does this. */
    public boolean shouldApproach() {
        return !this.isBaby() && this.isAggravated() && this.hasClaw() && this.getHealth() >= this.getMaxHealth() * 0.5F;
    }

    /**
     * An adult keeps away when aggravated and either hurt or declawed.
     * A baby keeps away for that same stretch after a player hits it, and never turns to fight.
     */
    public boolean shouldFlee() {
        if (this.isBaby()) {
            return this.isAggravated();
        }
        return this.isAggravated() && (this.getHealth() < this.getMaxHealth() * 0.5F || !this.hasClaw());
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    public void tick() {
        this.climbBlendO = this.climbBlend;
        boolean waving = this.waveTicks() > 0;
        float lockedYaw = this.yBodyRot;
        super.tick();
        if (waving) {
            this.setYRot(lockedYaw);
            this.yBodyRot = lockedYaw;
            this.yHeadRot = lockedYaw;
            this.setXRot(0.0F);
        }
        if (this.level().isClientSide()) {
            this.climbBlend = this.entityData.get(DATA_CLIMB_BLEND);
        }
    }

    public float climbBlend(float partialTick) {
        return Mth.lerp(partialTick, this.climbBlendO, this.climbBlend);
    }

    @Override
    public void calculateEntityAnimation(boolean useY) {
        super.calculateEntityAnimation(this.isClimbing() || useY);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isClimbing()) {
            this.travelAlongWall();
            return;
        }
        super.travel(input);
        if (this.climbBlend > 0.0F) {
            this.approachBlend(0.0F);
        }
        if (!this.level().isClientSide() && this.horizontalCollision && this.wantsToClimb(input)) {
            Direction face = this.wallAhead(input);
            if (face != null) {
                this.startClimbing(face);
            }
        }
    }

    private boolean wantsToClimb(Vec3 input) {
        if (this.climbCooldown > 0 || this.isInWater() || input.horizontalDistanceSqr() < 1.0E-4) {
            return false;
        }
        if (this.shouldFlee()) {
            return true;
        }
        LivingEntity target = this.getTarget();
        return target != null && target.getY() >= this.getY() - 0.5;
    }

    private @Nullable Direction wallAhead(Vec3 input) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double worldX = input.x * Mth.cos(yaw) - input.z * Mth.sin(yaw);
        double worldZ = input.z * Mth.cos(yaw) + input.x * Mth.sin(yaw);
        Direction preferred = Direction.getApproximateNearest((float) worldX, 0.0F, (float) worldZ);
        if (preferred.getAxis().isHorizontal() && this.sturdyBeside(preferred, this.getY() + 0.2)) {
            return preferred;
        }
        return this.anyClimbableFace();
    }

    private @Nullable Direction anyClimbableFace() {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (this.sturdyBeside(face, this.getY() + 0.2)) {
                return face;
            }
        }
        return null;
    }

    private boolean sturdyBeside(Direction face, double y) {
        double reach = this.getBbWidth() / 2.0 + 0.2;
        BlockPos pos = BlockPos.containing(
                this.getX() + face.getStepX() * reach,
                y,
                this.getZ() + face.getStepZ() * reach
        );
        return this.level().getBlockState(pos).isFaceSturdy(this.level(), pos, face.getOpposite());
    }

    private void startClimbing(Direction face) {
        float approachYaw = this.getYRot();
        MoveControl control = this.getMoveControl();
        this.continueX = control.getWantedX();
        this.continueY = control.getWantedY();
        this.continueZ = control.getWantedZ();
        double dx = this.continueX - this.getX();
        double dz = this.continueZ - this.getZ();
        this.continueAfterSummit = dx * dx + dz * dz > 0.75;
        this.climbMode = ClimbMode.UP;
        this.setWallFacing(face);
        this.setClimbing(true);
        this.getNavigation().stop();
        this.setXxa(0.0F);
        this.setZza(0.0F);
        this.setSpeed(0.0F);
        this.setYRot(approachYaw);
        this.alignAlongWall();
        this.stickToWall();
        this.climbStuck = 0;
        this.climbTicks = 0;
    }

    private void beginWallRest(Direction face, double restHeight) {
        this.wallRest = true;
        this.restY = restHeight;
        this.restTicks = 400 + this.random.nextInt(500);
        this.climbCooldown = 0;
        this.startClimbing(face);
    }

    private void stopClimbing() {
        this.exitClimb(false);
    }

    private void exitClimb(boolean summit) {
        Direction face = this.getWallFacing();
        boolean resume = summit && this.continueAfterSummit;
        double x = this.continueX;
        double y = this.continueY;
        double z = this.continueZ;
        double speed = this.shouldFlee() ? 1.25 : IDLE_SPEED;
        this.climbMode = ClimbMode.FLOOR;
        this.wallRest = false;
        this.continueAfterSummit = false;
        this.climbStuck = 0;
        this.climbTicks = 0;
        this.setClimbing(false);
        this.climbCooldown = 6;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        if (!summit) {
            this.setClimbBlend(0.0F);
            this.stickToWall();
            return;
        }
        if (!resume) {
            x = this.getX() + face.getStepX() * 1.25;
            y = this.getY();
            z = this.getZ() + face.getStepZ() * 1.25;
            speed = IDLE_SPEED;
        } else if (y < this.getY()) {
            y = this.getY();
        }
        this.getNavigation().moveTo(x, y, z, speed);
    }

    private void travelAlongWall() {
        if (this.isInWater() || this.getWallFacing().getAxis().isVertical()) {
            this.stopClimbing();
            return;
        }
        if (this.wallRest && (this.shouldFlee() || this.getTarget() != null || this.temptedByKelp())) {
            this.wallRest = false;
            LivingEntity target = this.getTarget();
            boolean above = this.shouldFlee()
                    || target != null && target.getY() > this.getY() + 0.5
                    || this.temptedByKelp() && this.kelpPlayer() != null && this.kelpPlayer().getY() > this.getY() + 0.5;
            this.climbMode = above ? ClimbMode.UP : ClimbMode.DOWN;
        }
        BlockPos wall = this.currentWall();
        if (wall == null) {
            if (this.getY() <= this.floorTop() + 0.35) {
                this.stopClimbing();
                return;
            }
            if (this.climbMode != ClimbMode.DOWN) {
                this.climbMode = ClimbMode.DOWN;
            }
        } else if (this.wallRest && this.getY() >= this.restY) {
            this.climbMode = ClimbMode.HOLD;
        } else if (this.climbMode == ClimbMode.UP && !this.wallRest && this.lipOpen(wall)) {
            this.slideOntoTop(wall);
            return;
        }
        if (this.climbMode == ClimbMode.HOLD && this.wallRest && --this.restTicks <= 0) {
            this.wallRest = false;
            this.climbMode = ClimbMode.DOWN;
        }
        if (this.climbMode == ClimbMode.DOWN && this.onGround() && this.floorBelow()) {
            this.approachBlend(0.0F);
            this.stickToWall();
            if (this.climbBlend <= 0.05F) {
                this.stopClimbing();
            }
            return;
        }
        this.alignAlongWall();
        double step = switch (this.climbMode) {
            case UP -> this.climbSpeedPerTick();
            case DOWN -> -this.climbSpeedPerTick();
            default -> 0.0;
        };
        if (this.climbMode != ClimbMode.UP || wall == null || this.getY() < wall.getY() + 1.0 - 0.05) {
            this.stickToWall();
        }
        double beforeY = this.getY();
        this.setDeltaMovement(0.0, step, 0.0);
        this.move(MoverType.SELF, new Vec3(0.0, step, 0.0));
        this.setDeltaMovement(Vec3.ZERO);
        this.resetFallDistance();
        if (wall != null && this.climbMode != ClimbMode.DOWN) {
            this.climbTicks++;
        }
        if (this.climbMode == ClimbMode.DOWN) {
            double remaining = this.getY() - this.floorTop();
            this.approachBlend(remaining < 0.6 ? (float) Mth.clamp(remaining / 0.6, 0.0, 1.0) : 1.0F);
        } else if (wall != null && (this.climbMode == ClimbMode.HOLD || this.climbTicks > 5)) {
            this.approachBlend(1.0F);
        } else {
            this.approachBlend(0.0F);
        }
        boolean blocked = this.climbMode == ClimbMode.UP && step > 0.0 && this.getY() - beforeY < step * 0.25;
        if (blocked && (wall == null || !this.lipOpen(wall))) {
            if (this.getY() <= this.floorTop() + 0.2) {
                if (++this.climbStuck > 8) {
                    this.stopClimbing();
                }
                return;
            }
            if (++this.climbStuck > 6) {
                this.climbMode = ClimbMode.DOWN;
                this.climbStuck = 0;
            }
        } else {
            this.climbStuck = 0;
        }
    }

    /**
     * Climbs the face until the feet reach the top, then scoots onto the surface
     * while the body unrolls. The position changes by at most one climb step per tick.
     */
    private void slideOntoTop(BlockPos wall) {
        Direction face = this.getWallFacing();
        double top = wall.getY() + 1.0;
        double remainingUp = top - this.getY();
        double speed = this.climbSpeedPerTick();
        this.alignAlongWall();
        double up = Mth.clamp(remainingUp, 0.0, speed);
        double hx = 0.0;
        double hz = 0.0;
        if (remainingUp > 0.05) {
            this.stickToWall();
        } else {
            Vec3 perch = this.perchOnTop(wall, face);
            double dx = perch.x - this.getX();
            double dz = perch.z - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 1.0E-4) {
                double step = Math.min(horizontal, Math.max(speed, 0.18));
                hx = dx / horizontal * step;
                hz = dz / horizontal * step;
            }
        }
        double beforeY = this.getY();
        double beforeX = this.getX();
        double beforeZ = this.getZ();
        this.setDeltaMovement(hx, up, hz);
        this.move(MoverType.SELF, new Vec3(hx, up, hz));
        if (up > 0.0 && up <= 0.08 && this.getY() < top - 0.02) {
            this.setPos(this.getX(), top, this.getZ());
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.resetFallDistance();
        Vec3 perch = this.perchOnTop(wall, face);
        double leftX = perch.x - this.getX();
        double leftZ = perch.z - this.getZ();
        double left = Math.sqrt(leftX * leftX + leftZ * leftZ);
        float blend = remainingUp > 0.05 ? 1.0F : (float) Mth.clamp(left / 0.55, 0.0, 1.0);
        if (remainingUp <= 0.05 && left <= 0.12) {
            blend = 0.0F;
        }
        this.approachBlend(blend);
        boolean movedUp = this.getY() - beforeY >= up * 0.25;
        boolean movedAcross = (this.getX() - beforeX) * (this.getX() - beforeX)
                        + (this.getZ() - beforeZ) * (this.getZ() - beforeZ)
                >= 0.0025;
        boolean blocked = (up > 0.01 && !movedUp) || (hx != 0.0 && !movedAcross && left > 0.08);
        if (blocked) {
            if (++this.climbStuck > 6) {
                this.climbMode = ClimbMode.DOWN;
                this.climbStuck = 0;
            }
            return;
        }
        this.climbStuck = 0;
        if (this.getY() >= top - 0.04 && left <= 0.08 && this.climbBlend <= 0.05F) {
            this.setClimbBlend(0.0F);
            this.exitClimb(true);
        }
    }

    private Vec3 perchOnTop(BlockPos wall, Direction face) {
        double inset = 0.5;
        double x = wall.getX() + 0.5;
        double z = wall.getZ() + 0.5;
        switch (face) {
            case NORTH -> z = wall.getZ() + 1.0 - inset;
            case SOUTH -> z = wall.getZ() + inset;
            case WEST -> x = wall.getX() + 1.0 - inset;
            case EAST -> x = wall.getX() + inset;
            default -> {
            }
        }
        return new Vec3(x, wall.getY() + 1.0, z);
    }

    private boolean lipOpen(BlockPos wall) {
        return this.lipOpen(wall, this.getWallFacing());
    }

    private boolean lipOpen(BlockPos wall, Direction face) {
        BlockPos above = wall.above();
        if (face.getAxis().isHorizontal()
                && this.level().getBlockState(above).isFaceSturdy(this.level(), above, face.getOpposite())) {
            return false;
        }
        return this.canStandAt(above);
    }

    private @Nullable BlockPos currentWall() {
        BlockPos wall = this.wallBlockAt(this.getY() + 0.15);
        if (wall == null) {
            wall = this.wallBlockAt(this.getY() - 0.35);
        }
        return wall;
    }

    private void approachBlend(float target) {
        this.setClimbBlend(this.climbBlend + Mth.clamp(target - this.climbBlend, -0.34F, 0.34F));
    }

    private void setClimbBlend(float blend) {
        this.climbBlend = Mth.clamp(blend, 0.0F, 1.0F);
        this.entityData.set(DATA_CLIMB_BLEND, this.climbBlend);
    }

    private double floorTop() {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.getX(), this.getY() - 0.01, this.getZ());
        for (int i = 0; i < 16; i++) {
            BlockState state = this.level().getBlockState(pos);
            if (state.isFaceSturdy(this.level(), pos, Direction.UP)
                    || !state.getCollisionShape(this.level(), pos).isEmpty()) {
                return pos.getY() + 1.0;
            }
            pos.move(Direction.DOWN);
        }
        return this.getY();
    }

    private @Nullable BlockPos wallBlockAt(double y) {
        Direction face = this.getWallFacing();
        if (face.getAxis().isVertical()) {
            return null;
        }
        double reach = this.getBbWidth() / 2.0 + 0.3;
        BlockPos pos = BlockPos.containing(
                this.getX() + face.getStepX() * reach,
                y,
                this.getZ() + face.getStepZ() * reach
        );
        if (this.level().getBlockState(pos).isFaceSturdy(this.level(), pos, face.getOpposite())) {
            return pos;
        }
        return null;
    }

    private boolean canStandAt(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState floor = this.level().getBlockState(below);
        boolean solid = floor.isFaceSturdy(this.level(), below, Direction.UP)
                || !floor.getCollisionShape(this.level(), below).isEmpty();
        return solid && this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
    }

    private double climbSpeedPerTick() {
        if (this.shouldFlee()) {
            return FLEE_CLIMB_PER_TICK;
        }
        if (this.getTarget() != null) {
            return CHASE_CLIMB_PER_TICK;
        }
        return IDLE_CLIMB_PER_TICK;
    }

    private boolean floorBelow() {
        BlockPos below = this.getBlockPosBelowThatAffectsMyMovement();
        return this.level().getBlockState(below).isFaceSturdy(this.level(), below, Direction.UP);
    }

    private void stickToWall() {
        Direction face = this.getWallFacing();
        if (face.getAxis().isVertical()) {
            return;
        }
        BlockPos wall = this.nearestWall(face);
        if (wall == null) {
            return;
        }
        // The collision box stays just outside the wall. The rolled shell is drawn closer.
        double gap = this.getBbWidth() / 2.0 + 0.01;
        double x = this.getX();
        double z = this.getZ();
        switch (face) {
            case NORTH -> z = wall.getZ() + 1.0 + gap;
            case SOUTH -> z = wall.getZ() - gap;
            case WEST -> x = wall.getX() + 1.0 + gap;
            case EAST -> x = wall.getX() - gap;
            default -> {
                return;
            }
        }
        this.setPos(x, this.getY(), z);
    }

    /** First solid face in front of the crab, so a probe never locks onto the block behind the wall. */
    private @Nullable BlockPos nearestWall(Direction face) {
        double[] heights = {this.getBbHeight() * 0.5, 0.2};
        for (double yOff : heights) {
            BlockPos previous = null;
            for (int step = 1; step <= 8; step++) {
                BlockPos pos = BlockPos.containing(
                        this.getX() + face.getStepX() * (step * 0.15),
                        this.getY() + yOff,
                        this.getZ() + face.getStepZ() * (step * 0.15)
                );
                if (pos.equals(previous)) {
                    continue;
                }
                previous = pos;
                if (this.level().getBlockState(pos).isFaceSturdy(this.level(), pos, face.getOpposite())) {
                    return pos;
                }
            }
        }
        return null;
    }

    private void alignAlongWall() {
        Direction face = this.getWallFacing();
        if (face.getAxis().isVertical()) {
            return;
        }
        float current = this.getYRot();
        float clockwise = face.getClockWise().toYRot();
        float counter = face.getCounterClockWise().toYRot();
        float yaw = Math.abs(Mth.wrapDegrees(clockwise - current)) <= Math.abs(Mth.wrapDegrees(counter - current))
                ? clockwise
                : counter;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /**
     * Vanilla pathing cannot step out of a one-block hole. Kelp above the crab
     * starts a climb on the nearest wall, then continues toward the player.
     */
    private void climbOutTowardKelp(ServerLevel level) {
        if (this.isClimbing() || this.climbCooldown > 0 || this.isInWater()) {
            return;
        }
        double range = this.getAttributeValue(Attributes.TEMPT_RANGE);
        Player player = level.getNearestPlayer(this, range);
        if (player == null || player.getY() < this.getY() + this.maxUpStep()) {
            return;
        }
        boolean kelp = player.getMainHandItem().is(Items.KELP) || player.getOffhandItem().is(Items.KELP);
        if (!kelp) {
            return;
        }
        if (this.getNavigation().isInProgress() && !this.horizontalCollision && !this.getNavigation().isStuck()) {
            return;
        }
        Direction face = Direction.getApproximateNearest(
                (float) (player.getX() - this.getX()), 0.0F, (float) (player.getZ() - this.getZ())
        );
        if (!face.getAxis().isHorizontal() || !this.sturdyBeside(face, this.getY() + 0.2)) {
            face = this.anyClimbableFace();
        }
        if (face == null) {
            return;
        }
        this.startClimbing(face);
        this.continueAfterSummit = true;
        this.continueX = player.getX();
        this.continueY = player.getY();
        this.continueZ = player.getZ();
    }

    /** Kelp pulls a crab off a wall, up toward the player or back down to the ground. */
    private void answerKelpWhileClimbing(ServerLevel level) {
        if (!this.isClimbing() || this.shouldFlee() || this.getTarget() != null) {
            return;
        }
        Player player = this.kelpPlayer();
        if (player == null) {
            return;
        }
        this.wallRest = false;
        this.continueAfterSummit = true;
        this.continueX = player.getX();
        this.continueY = player.getY();
        this.continueZ = player.getZ();
        this.climbMode = player.getY() > this.getY() + 0.5 ? ClimbMode.UP : ClimbMode.DOWN;
    }

    private boolean temptedByKelp() {
        return this.kelpPlayer() != null;
    }

    private @Nullable Player kelpPlayer() {
        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }
        double range = this.getAttributeValue(Attributes.TEMPT_RANGE);
        Player player = level.getNearestPlayer(this, range);
        if (player == null) {
            return null;
        }
        boolean kelp = player.getMainHandItem().is(Items.KELP) || player.getOffhandItem().is(Items.KELP);
        return kelp ? player : null;
    }

    /**
     * Ground pathing cannot step up a full block. Climb the lip toward a chosen
     * spot, then keep going once the crab is on that level.
     */
    private boolean climbToward(double x, double y, double z) {
        if (this.isClimbing() || this.climbCooldown > 0 || this.isInWater() || y < this.getY() + this.maxUpStep()) {
            return false;
        }
        Direction toward = Direction.getApproximateNearest((float) (x - this.getX()), 0.0F, (float) (z - this.getZ()));
        Direction face = null;
        if (toward.getAxis().isHorizontal() && this.faceLeadsUp(toward)) {
            face = toward;
        } else {
            for (Direction candidate : Direction.Plane.HORIZONTAL) {
                if (this.faceLeadsUp(candidate)) {
                    face = candidate;
                    break;
                }
            }
        }
        if (face == null) {
            return false;
        }
        this.startClimbing(face);
        this.continueAfterSummit = true;
        this.continueX = x;
        this.continueY = y;
        this.continueZ = z;
        return true;
    }

    private boolean faceLeadsUp(Direction face) {
        if (!this.sturdyBeside(face, this.getY() + 0.2)) {
            return false;
        }
        double reach = this.getBbWidth() / 2.0 + 0.2;
        BlockPos wall = BlockPos.containing(
                this.getX() + face.getStepX() * reach,
                this.getY() + 0.2,
                this.getZ() + face.getStepZ() * reach
        );
        return this.lipOpen(wall, face);
    }

    private @Nullable WallPerch pickWallPerch() {
        BlockPos origin = this.blockPosition();
        WallPerch best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    for (Direction face : Direction.Plane.HORIZONTAL) {
                        BlockPos wall = feet.relative(face);
                        if (!this.level().getBlockState(wall).isFaceSturdy(this.level(), wall, face.getOpposite())) {
                            continue;
                        }
                        if (!this.canStandAt(feet)) {
                            continue;
                        }
                        int height = 0;
                        for (int step = 0; step < 12; step++) {
                            BlockPos column = wall.above(step);
                            BlockPos space = feet.above(step);
                            if (!this.level().getBlockState(column).isFaceSturdy(this.level(), column, face.getOpposite())) {
                                break;
                            }
                            if (!this.level().getBlockState(space).getCollisionShape(this.level(), space).isEmpty()) {
                                break;
                            }
                            height++;
                        }
                        if (height < 2) {
                            continue;
                        }
                        double distance = feet.distSqr(origin);
                        if (distance > 100.0 || distance >= bestDistance) {
                            continue;
                        }
                        bestDistance = distance;
                        double low = 1.5;
                        double high = Math.max(low, height - 0.4);
                        double extra = low + this.random.nextDouble() * (high - low);
                        best = new WallPerch(
                                Vec3.atBottomCenterOf(feet), face, feet.getY() + Math.min(extra, height - 0.25)
                        );
                    }
                }
            }
        }
        return best;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (level.isRaining() && level.getBiome(this.blockPosition()).is(BiomeTags.IS_BEACH)) {
            this.soaked = true;
        }
        if (this.climbCooldown > 0) {
            this.climbCooldown--;
        }
        this.climbOutTowardKelp(level);
        this.answerKelpWhileClimbing(level);
        this.setAggravated(!this.isBaby() && level.getGameTime() < this.aggravatedUntil);
        if (!this.shouldApproach()) {
            this.setTarget(null);
        }
        if (this.waveTicks() > 0) {
            this.setWaveTicks(this.waveTicks() - 1);
        } else if (this.hasClaw()
                && !this.isAggravated()
                && !this.isClimbing()
                && this.random.nextInt(100) == 0) {
            Player player = level.getNearestPlayer(this, 8.0);
            if (player != null) {
                this.setWaveTicks(WAVE_TICKS);
            }
        }
        if (!this.hasClaw() && this.clawRegrowAt > 0L && level.getGameTime() >= this.clawRegrowAt) {
            this.setHasClaw(true);
            this.clawRegrowAt = 0L;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && source.getEntity() instanceof Player player) {
            this.aggravatedUntil = level.getGameTime() + AGGRAVATION_TICKS;
            if (this.isBaby()) {
                this.setAggravated(false);
                this.setTarget(null);
            } else {
                this.setAggravated(true);
                if (this.shouldApproach()) {
                    this.setTarget(player);
                } else {
                    this.setTarget(null);
                }
            }
        }
        return hurt;
    }

    @Override
    protected void blockedByItem(LivingEntity defender, DamageSource source, float damage) {
        super.blockedByItem(defender, source, damage);
        boolean droppedClaw = false;
        if (this.hasClaw() && this.level() instanceof ServerLevel server) {
            this.setHasClaw(false);
            this.clawRegrowAt = server.getGameTime() + REGROW_TICKS;
            this.spawnAtLocation(server, new ItemStack(ModItems.CRAB_CLAW.get()));
            this.playSound(SoundEvents.ITEM_BREAK.value(), 0.6F, 1.2F);
            droppedClaw = true;
        }
        if (droppedClaw && defender instanceof ServerPlayer player && defender.getUseItem().is(Items.SHIELD)) {
            ModCriteria.EXPELLIARMUS.get().trigger(player);
        }
        this.setTarget(null);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.KELP);
    }

    @Override
    public @Nullable Crab getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return com.buffspotions.registry.ModEntities.CRAB.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRAB_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRAB_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.CRAB_STEP.get(), 0.55F, 1.0F);
    }

    private enum ClimbMode {
        FLOOR,
        UP,
        DOWN,
        HOLD
    }

    /**
     * Faces perpendicular to travel so the stride is a strafe. An aggravated approach
     * always leads with the big claw. Other trips use whichever side is already closer.
     * Forward shuffle, used while turning into that stance, stays much slower.
     */
    private static final class SidewaysMoveControl extends MoveControl<Crab> {
        SidewaysMoveControl(Crab crab) {
            super(crab);
        }

        @Override
        public void tick() {
            if (this.mob.isClimbing()) {
                this.operation = Operation.WAIT;
                this.mob.setXxa(0.0F);
                this.mob.setZza(0.0F);
                return;
            }
            if (this.operation == Operation.MOVE_TO) {
                this.operation = Operation.WAIT;
                double xd = this.wantedX - this.mob.getX();
                double zd = this.wantedZ - this.mob.getZ();
                double yd = this.wantedY - this.mob.getY();
                if (xd * xd + yd * yd + zd * zd < MIN_SPEED_SQR) {
                    this.mob.setXxa(0.0F);
                    this.mob.setZza(0.0F);
                    return;
                }
                float travelYaw = (float) (Mth.atan2(zd, xd) * 180.0F / (float) Math.PI) - 90.0F;
                // Positive xxa is a left strafe. That walks toward the target when the body
                // faces travelYaw + 90, so the big claw (on the left) leads.
                float clawYaw = travelYaw + 90.0F;
                float otherYaw = travelYaw - 90.0F;
                float currentYaw = this.mob.getYRot();
                boolean leadWithClaw = this.mob.shouldApproach()
                        || Math.abs(Mth.wrapDegrees(clawYaw - currentYaw))
                                <= Math.abs(Mth.wrapDegrees(otherYaw - currentYaw));
                float bodyYaw = leadWithClaw ? clawYaw : otherYaw;
                this.mob.setYRot(this.rotlerp(currentYaw, bodyYaw, 40.0F));
                float misaligned = Math.abs(Mth.wrapDegrees(bodyYaw - this.mob.getYRot())) / 90.0F;
                float aligned = 1.0F - Mth.clamp(misaligned, 0.0F, 1.0F);
                float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.mob.setSpeed(speed);
                this.mob.setXxa(aligned * STRAFE_SCALE * (leadWithClaw ? 1.0F : -1.0F));
                this.mob.setZza((1.0F - aligned) * FORWARD_INPUT * STRAFE_SCALE);
                this.climbInsteadOfJump(xd, yd, zd);
            } else {
                this.mob.setXxa(0.0F);
                this.mob.setZza(0.0F);
            }
        }

        private void climbInsteadOfJump(double xd, double yd, double zd) {
            if (this.mob.isClimbing() || yd <= this.mob.maxUpStep()) {
                return;
            }
            if (xd * xd + zd * zd > Math.max(1.0F, this.mob.getBbWidth())) {
                return;
            }
            Direction face = Direction.getApproximateNearest((float) xd, 0.0F, (float) zd);
            if (!face.getAxis().isHorizontal() || !this.mob.sturdyBeside(face, this.mob.getY() + 0.2)) {
                face = this.mob.anyClimbableFace();
            }
            if (face != null) {
                this.mob.startClimbing(face);
            }
        }
    }

    private static final class FleeGoal extends Goal {
        private final Crab crab;
        private final double speed;
        private int pause;

        FleeGoal(Crab crab, double speed) {
            this.crab = crab;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.crab.shouldFlee()
                    && !this.crab.isClimbing()
                    && this.crab.level().getNearestPlayer(this.crab, PLAYER_NOTICE_RANGE) != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.crab.setTarget(null);
            this.pause = 0;
        }

        @Override
        public void tick() {
            this.crab.setTarget(null);
            if (this.pause-- > 0) {
                return;
            }
            this.pause = 8;
            Player player = this.crab.level().getNearestPlayer(this.crab, 24.0);
            if (player == null) {
                return;
            }
            double dx = this.crab.getX() - player.getX();
            double dz = this.crab.getZ() - player.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.01) {
                length = 1.0;
            }
            this.crab.getNavigation().moveTo(
                    this.crab.getX() + dx / length * 10.0,
                    this.crab.getY(),
                    this.crab.getZ() + dz / length * 10.0,
                    this.speed
            );
        }
    }

    /**
     * Walks straight at the player with the big claw leading. A path around the player
     * stops about two blocks short and then steps aside, so this does not use one.
     */
    private static final class ApproachGoal extends Goal {
        private final Crab crab;
        private int nextAttack;

        ApproachGoal(Crab crab) {
            this.crab = crab;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.crab.shouldApproach() || this.crab.isClimbing()) {
                return false;
            }
            Player player = this.crab.level().getNearestPlayer(this.crab, PLAYER_NOTICE_RANGE);
            if (player == null) {
                return false;
            }
            this.crab.setTarget(player);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.crab.getTarget();
            return this.crab.shouldApproach()
                    && !this.crab.isClimbing()
                    && target != null
                    && target.isAlive()
                    && this.crab.distanceToSqr(target) < PLAYER_NOTICE_RANGE * PLAYER_NOTICE_RANGE;
        }

        @Override
        public void tick() {
            if (!(this.crab.getTarget() instanceof Player player) || !player.isAlive()) {
                return;
            }
            this.crab.getLookControl().setLookAt(player, 30.0F, 30.0F);
            this.crab.getNavigation().stop();
            if (this.crab.isWithinMeleeAttackRange(player) && this.crab.getSensing().hasLineOfSight(player)) {
                if (this.crab.tickCount >= this.nextAttack && this.crab.level() instanceof ServerLevel server) {
                    this.nextAttack = this.crab.tickCount + 20;
                    this.crab.swing(InteractionHand.MAIN_HAND);
                    this.crab.doHurtTarget(server, player);
                }
                return;
            }
            this.crab.getMoveControl().setWantedPosition(player.getX(), player.getY(), player.getZ(), IDLE_SPEED);
        }
    }

    /** Stands still, then either scuttles a few blocks or climbs a nearby wall and rests there. */
    private static final class IdleStrollGoal extends Goal {
        private final Crab crab;
        private int wait = 154;
        private double wantedX;
        private double wantedY;
        private double wantedZ;
        private @Nullable WallPerch perch;

        IdleStrollGoal(Crab crab) {
            this.crab = crab;
            this.wait = 40 + crab.getRandom().nextInt(200);
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.crab.isClimbing() || this.crab.shouldFlee() || this.crab.getTarget() != null) {
                return false;
            }
            if (this.wait > 0) {
                this.wait--;
                return false;
            }
            this.perch = null;
            if (this.crab.getRandom().nextFloat() < 0.3F && !this.crab.temptedByKelp()) {
                WallPerch perch = this.crab.pickWallPerch();
                if (perch != null) {
                    this.perch = perch;
                    this.wantedX = perch.stand.x;
                    this.wantedY = perch.stand.y;
                    this.wantedZ = perch.stand.z;
                    return true;
                }
            }
            Vec3 pos = LandRandomPos.getPos(this.crab, 10, 4);
            if (pos == null) {
                this.wait = 31;
                return false;
            }
            double distance = pos.distanceTo(this.crab.position());
            double rise = Math.abs(pos.y - this.crab.getY());
            if (distance < 3.0 && rise < 0.75) {
                this.wait = 31;
                return false;
            }
            this.wantedX = pos.x;
            this.wantedY = pos.y;
            this.wantedZ = pos.z;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.crab.shouldFlee() || this.crab.getTarget() != null || this.crab.isClimbing()) {
                return false;
            }
            return !this.crab.getNavigation().isDone();
        }

        @Override
        public void start() {
            Path path = this.crab.getNavigation().createPath(this.wantedX, this.wantedY, this.wantedZ, 0);
            if (path != null && path.canReach()) {
                this.crab.getNavigation().moveTo(path, IDLE_TRAVEL_SPEED);
                return;
            }
            if (this.crab.climbToward(this.wantedX, this.wantedY, this.wantedZ)) {
                return;
            }
            this.crab.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, IDLE_TRAVEL_SPEED);
        }

        @Override
        public void tick() {
            if (this.perch != null
                    && !this.crab.isClimbing()
                    && !this.crab.temptedByKelp()
                    && this.crab.distanceToSqr(this.perch.stand) < 2.56
                    && this.crab.sturdyBeside(this.perch.face, this.crab.getY() + 0.2)) {
                this.crab.beginWallRest(this.perch.face, this.perch.restY);
            }
        }

        @Override
        public void stop() {
            this.crab.getNavigation().stop();
            this.wait = this.perch != null ? 62 : 154 + this.crab.getRandom().nextInt(231);
            this.perch = null;
        }
    }

    private record WallPerch(Vec3 stand, Direction face, double restY) {
    }
}
