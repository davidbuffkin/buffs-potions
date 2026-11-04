package com.buffspotions.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public final class Incarnation {
    public static final double UNKNOWN_SCALE = 2.0;
    private static final ThreadLocal<Vec3> AT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<boolean[]> SPAWNED = new ThreadLocal<>();

    private Incarnation() {
    }

    public static void at(Vec3 position) {
        AT.set(position);
    }

    public static void clearAt() {
        AT.remove();
    }

    public static void suppress() {
        SUPPRESS.set(true);
    }

    public static void clearSuppress() {
        SUPPRESS.remove();
    }

    /** Notes whether {@link #spawn} adds a mob, for the tipping splash that asked. */
    public static void watchSpawn() {
        SPAWNED.set(new boolean[1]);
    }

    public static boolean takeSpawn() {
        boolean[] spawned = SPAWNED.get();
        SPAWNED.remove();
        return spawned != null && spawned[0];
    }

    public static boolean isIncarnation(ItemStack stack) {
        PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().unwrapKey().equals(com.buffspotions.registry.ModEffects.INSTANT_INCARNATION.unwrapKey())) {
                return true;
            }
        }
        return false;
    }

    public static void spawnFor(ServerLevel level, @Nullable LivingEntity atEntity) {
        if (SUPPRESS.get()) {
            return;
        }
        Vec3 position = AT.get();
        float yaw = 0.0F;
        if (atEntity != null) {
            if (position == null) {
                position = atEntity.position();
            }
            yaw = atEntity.getYRot();
        }
        if (position == null) {
            return;
        }
        spawn(level, Eyedentification.applying(), position, yaw);
    }

    public static void spawn(ServerLevel level, @Nullable EssenceSubject subject, Vec3 position, float yaw) {
        EntityType<?> type = EntityTypes.PIG;
        boolean unknown = true;
        if (subject != null && subject.entity().isPresent()) {
            type = subject.entity().get().value();
            unknown = false;
        } else if (subject != null && subject.item().isPresent()) {
            var recognized = MobDrops.type(subject.item().get());
            if (recognized.isPresent()) {
                type = recognized.get();
                unknown = false;
            }
        }
        Entity entity = type.create(level, new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true));
        if (entity == null) {
            return;
        }
        entity.snapTo(position.x, position.y, position.z, yaw, 0.0F);
        if (entity instanceof Mob mob) {
            BlockPos pos = BlockPos.containing(position);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        }
        if (unknown && entity instanceof LivingEntity living) {
            if (living instanceof AgeableMob ageable) {
                ageable.setBaby(true);
            }
            var scale = living.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(UNKNOWN_SCALE);
            }
            living.refreshDimensions();
        }
        BlockPos feet = BlockPos.containing(position);
        BlockState ground = level.getBlockState(feet);
        if (!ground.getCollisionShape(level, feet, CollisionContext.empty()).isEmpty()) {
            entity.snapTo(position.x, feet.getY() + 1.0, position.z, yaw, 0.0F);
        }
        level.addFreshEntity(entity);
        boolean[] spawned = SPAWNED.get();
        if (spawned != null) {
            spawned[0] = true;
        }
    }
}
