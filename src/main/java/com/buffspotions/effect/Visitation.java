package com.buffspotions.effect;

import com.buffspotions.mixin.MannequinInvoker;
import com.buffspotions.mixin.ServerPlayerTravelAccess;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class Visitation {
    public static final int CHUNK_RADIUS = 2;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<LivingEntity> SUPPRESS_KNOCKBACK = new ThreadLocal<>();
    private static final ThreadLocal<LivingEntity> SHADOW_STRIKE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PULLED = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IGNORE_TRAVEL = ThreadLocal.withInitial(() -> false);

    private Visitation() {
    }

    public static boolean chorusInsteadOfCrossing(int amplifier, boolean crossesDimensions) {
        return amplifier < 1 && crossesDimensions;
    }

    public static boolean ignoresTravel() {
        return IGNORE_TRAVEL.get();
    }

    public static void chorus(ServerPlayer player) {
        withoutTravel(player, () -> new TeleportRandomlyConsumeEffect().apply(player.level(), ItemStack.EMPTY, player));
    }

    public static void depart(ServerPlayer player, int amplifier) {
        if (!(player.level() instanceof ServerLevel origin) || player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        TeleportTransition destination = destination(player);
        boolean cross = destination != null && destination.newLevel().dimension() != origin.dimension();
        if (!leaveShadow(player, origin, amplifier)) {
            player.removeEffect(ModEffects.DISLOCATED);
            return;
        }
        if (chorusInsteadOfCrossing(amplifier, cross)) {
            chorus(player);
            return;
        }
        if (destination == null) {
            discard(player);
            player.removeEffect(ModEffects.DISLOCATED);
            return;
        }
        if (teleport(player, destination) == null) {
            discard(player);
            player.removeEffect(ModEffects.DISLOCATED);
            return;
        }
        if (player.level() instanceof ServerLevel arrived) {
            play(arrived, player.position());
        }
    }

    public static void recall(ServerPlayer player) {
        if (!player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        if (!player.isAlive()) {
            discard(player);
            return;
        }
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        player.removeData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return;
        }
        keepLoaded(level, anchor.chunk());
        Entity shadow = level.getEntity(anchor.shadowId());
        if (chorusInsteadOfCrossing(anchor.amplifier(), level.dimension() != player.level().dimension())) {
            if (shadow != null) {
                play(level, shadow.position());
                shadow.discard();
            }
            chorus(player);
            return;
        }
        Vec3 pos = shadow != null ? shadow.position() : anchor.position();
        float yRot = shadow != null ? shadow.getYRot() : anchor.yRot();
        float xRot = shadow != null ? shadow.getXRot() : anchor.xRot();
        teleport(player, new TeleportTransition(level, pos, Vec3.ZERO, yRot, xRot, TeleportTransition.DO_NOTHING));
        if (shadow != null) {
            shadow.discard();
        }
        if (player.level() instanceof ServerLevel arrived) {
            play(arrived, player.position());
        }
    }

    public static void discard(ServerPlayer player) {
        if (player == null || !player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        if (REDIRECTING.get()) {
            player.removeData(ModAttachments.DISLOCATION);
            return;
        }
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        player.removeData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return;
        }
        keepLoaded(level, anchor.chunk());
        Entity shadow = level.getEntity(anchor.shadowId());
        if (shadow != null) {
            shadow.discard();
        }
    }

    public static void tick(ServerPlayer player) {
        if (!player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        if (!player.isAlive()) {
            discard(player);
            return;
        }
        if (!player.hasEffect(ModEffects.DISLOCATED)) {
            recall(player);
            return;
        }
        mirrorArmor(player);
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            keepLoaded(level, anchor.chunk());
        }
        Entity shadow = level.getEntity(anchor.shadowId());
        if (shadow == null) {
            restoreShadow(player);
            return;
        }
        if (shadow.distanceToSqr(anchor.position()) > 0.04) {
            shadow.snapTo(anchor.x(), anchor.y(), anchor.z(), anchor.yRot(), anchor.xRot());
            shadow.setDeltaMovement(Vec3.ZERO);
            shadow.setYHeadRot(anchor.yRot());
        }
        if (player.tickCount % 10 == 0) {
            level.sendParticles(
                    ParticleTypes.PORTAL,
                    shadow.getX(),
                    shadow.getY() + shadow.getBbHeight() * 0.5,
                    shadow.getZ(),
                    2,
                    0.2,
                    0.4,
                    0.2,
                    0.01
            );
        }
    }

    public static void guard(Entity entity) {
        if (!entity.hasData(ModAttachments.ENDER_SHADOW) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        UUID ownerId = entity.getData(ModAttachments.ENDER_SHADOW);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || !owner.hasEffect(ModEffects.DISLOCATED) || !owner.hasData(ModAttachments.DISLOCATION)) {
            entity.discard();
            return;
        }
        if (!entity.getUUID().equals(owner.getData(ModAttachments.DISLOCATION).shadowId())) {
            entity.discard();
        }
    }

    /** Removes the shadow while the player is offline. The anchor stays, so it can return in the same place. */
    public static void hideShadow(ServerPlayer player) {
        if (!player.hasData(ModAttachments.DISLOCATION) || REDIRECTING.get()) {
            return;
        }
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return;
        }
        keepLoaded(level, anchor.chunk());
        Entity shadow = level.getEntity(anchor.shadowId());
        if (shadow != null) {
            shadow.discard();
        }
    }

    /** Puts the shadow back where it was left when the player logs in. */
    public static void restoreShadow(ServerPlayer player) {
        if (!player.hasData(ModAttachments.DISLOCATION) || REDIRECTING.get()) {
            return;
        }
        if (!player.isAlive() || !player.hasEffect(ModEffects.DISLOCATED)) {
            if (!player.hasEffect(ModEffects.DISLOCATED)) {
                discard(player);
            }
            return;
        }
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null) {
            return;
        }
        keepLoaded(level, anchor.chunk());
        Entity existing = level.getEntity(anchor.shadowId());
        if (existing != null && existing.isAlive()) {
            mirrorArmor(player);
            return;
        }
        Mannequin shadow = createShadow(player, level);
        if (shadow == null) {
            return;
        }
        shadow.snapTo(anchor.x(), anchor.y(), anchor.z(), anchor.yRot(), anchor.xRot());
        shadow.setYHeadRot(anchor.yRot());
        shadow.setYBodyRot(anchor.yRot());
        shadow.setDeltaMovement(Vec3.ZERO);
        if (!level.addFreshEntity(shadow)) {
            return;
        }
        player.setData(ModAttachments.DISLOCATION, new DislocationAnchor(
                anchor.dimension(),
                shadow.getUUID(),
                anchor.x(),
                anchor.y(),
                anchor.z(),
                anchor.yRot(),
                anchor.xRot(),
                anchor.amplifier()
        ));
        mirrorArmor(player);
    }

    public static void redirectDamage(LivingEntity shadow, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (REDIRECTING.get()) {
            return;
        }
        UUID ownerId = shadow.getData(ModAttachments.ENDER_SHADOW);
        MinecraftServer server = shadow.level().getServer();
        if (server == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || !(owner.level() instanceof ServerLevel)) {
            return;
        }
        float before = owner.getHealth();
        boolean lethal = absorbed(owner, source, amount) >= before + owner.getAbsorptionAmount();
        REDIRECTING.set(true);
        SUPPRESS_KNOCKBACK.set(owner);
        SHADOW_STRIKE.set(shadow);
        PULLED.set(false);
        try {
            if (lethal && shadow.level() != owner.level()) {
                pullToShadow(owner, shadow);
            }
            ServerLevel hurtLevel = (ServerLevel) owner.level();
            boolean hurt = owner.hurtServer(hurtLevel, source, amount);
            if (hurt && owner.isAlive() && owner.getHealth() < before && !PULLED.get()) {
                playQuiet(hurtLevel, owner.position());
            }
        } finally {
            REDIRECTING.set(false);
            SUPPRESS_KNOCKBACK.remove();
            SHADOW_STRIKE.remove();
            PULLED.remove();
        }
    }

    /** Moves the owner onto the shadow when a shadow strike is about to kill them. */
    public static void beforeOwnerDeath(ServerPlayer player) {
        LivingEntity shadow = SHADOW_STRIKE.get();
        if (shadow == null || PULLED.get()) {
            return;
        }
        pullToShadow(player, shadow);
    }

    private static void pullToShadow(ServerPlayer player, LivingEntity shadow) {
        if (!(shadow.level() instanceof ServerLevel level)) {
            return;
        }
        teleport(player, new TeleportTransition(
                level,
                shadow.position(),
                Vec3.ZERO,
                shadow.getYRot(),
                shadow.getXRot(),
                TeleportTransition.DO_NOTHING
        ));
        PULLED.set(true);
        if (player.level() instanceof ServerLevel arrived) {
            playQuiet(arrived, player.position());
        }
    }

    public static boolean suppressesKnockback(LivingEntity entity) {
        return SUPPRESS_KNOCKBACK.get() == entity;
    }

    /** The damage the owner actually takes after armor, protection, and resistance. */
    private static float absorbed(ServerPlayer owner, DamageSource source, float amount) {
        float dealt = amount;
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            dealt = CombatRules.getDamageAfterAbsorb(
                    owner,
                    dealt,
                    source,
                    owner.getArmorValue(),
                    (float) owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
            );
        }
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return dealt;
        }
        if (owner.hasEffect(MobEffects.RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int absorb = 25 - (owner.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
            dealt = Math.max(dealt * absorb / 25.0F, 0.0F);
        }
        if (dealt <= 0.0F || source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS) || !(owner.level() instanceof ServerLevel level)) {
            return dealt;
        }
        float protection = EnchantmentHelper.getDamageProtection(level, owner, source);
        return protection > 0.0F ? CombatRules.getDamageAfterMagicAbsorb(dealt, protection) : dealt;
    }

    private static void mirrorArmor(ServerPlayer player) {
        if (!player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(anchor.dimension());
        if (level == null || !(level.getEntity(anchor.shadowId()) instanceof Mannequin shadow)) {
            return;
        }
        for (EquipmentSlot slot : ARMOR) {
            ItemStack desired = player.getItemBySlot(slot).copy();
            if (!ItemStack.matches(shadow.getItemBySlot(slot), desired)) {
                shadow.setItemSlot(slot, desired);
            }
        }
    }

    private static boolean leaveShadow(ServerPlayer player, ServerLevel origin, int amplifier) {
        Mannequin shadow = createShadow(player, origin);
        if (shadow == null) {
            return false;
        }
        // The effect is not on the player yet: this runs from the added event, before the map insert.
        player.setData(ModAttachments.DISLOCATION, new DislocationAnchor(
                origin.dimension(),
                shadow.getUUID(),
                shadow.getX(),
                shadow.getY(),
                shadow.getZ(),
                shadow.getYRot(),
                shadow.getXRot(),
                amplifier
        ));
        if (!origin.addFreshEntity(shadow)) {
            player.removeData(ModAttachments.DISLOCATION);
            return false;
        }
        mirrorArmor(player);
        keepLoaded(origin, ChunkPos.containing(shadow.blockPosition()));
        play(origin, shadow.position());
        return true;
    }

    private static TeleportTransition destination(ServerPlayer player) {
        TeleportTransition personal = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        if (personal != null && personal.newLevel().dimension() == player.level().dimension()) {
            return personal;
        }
        if (player.level().dimension() == Level.OVERWORLD) {
            return personal;
        }
        if (personal != null && personal.newLevel().dimension() == Level.OVERWORLD) {
            return personal;
        }
        ServerLevel overworld = player.level().getServer().overworld();
        if (overworld == null) {
            return null;
        }
        LevelData.RespawnData data = overworld.getRespawnData();
        return new TeleportTransition(
                overworld,
                Vec3.atBottomCenterOf(data.pos()),
                Vec3.ZERO,
                data.yaw(),
                data.pitch(),
                TeleportTransition.DO_NOTHING
        );
    }

    private static Mannequin createShadow(ServerPlayer player, ServerLevel level) {
        Mannequin shadow = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
        if (shadow == null) {
            return null;
        }
        shadow.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        shadow.setYHeadRot(player.getYHeadRot());
        shadow.setYBodyRot(player.getYRot());
        MannequinInvoker invoked = (MannequinInvoker) shadow;
        invoked.buffsPotions$setProfile(ResolvableProfile.createResolved(player.getGameProfile()));
        invoked.buffsPotions$setImmovable(true);
        invoked.buffsPotions$setHideDescription(true);
        shadow.setMainArm(player.getMainArm());
        shadow.setNoGravity(true);
        shadow.setSilent(true);
        shadow.setDeltaMovement(Vec3.ZERO);
        Pose pose = player.getPose();
        if (pose == Pose.CROUCHING || pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SLEEPING) {
            shadow.setPose(pose);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            shadow.setItemSlot(slot, ItemStack.EMPTY);
        }
        shadow.setData(ModAttachments.ENDER_SHADOW, player.getUUID());
        return shadow;
    }

    /**
     * Moves the player without counting the jump as walked, ridden, or Nether travel.
     * A real portal trip already in progress keeps its entry point.
     */
    private static @Nullable ServerPlayer teleport(ServerPlayer player, TeleportTransition transition) {
        ServerPlayer[] arrived = new ServerPlayer[1];
        withoutTravel(player, () -> arrived[0] = player.teleport(transition));
        return arrived[0];
    }

    private static void withoutTravel(ServerPlayer player, Runnable move) {
        ServerPlayerTravelAccess travel = (ServerPlayerTravelAccess) player;
        Vec3 enteredNether = travel.buffsPotions$enteredNetherPosition();
        travel.buffsPotions$enteredNetherPosition(null);
        IGNORE_TRAVEL.set(true);
        try {
            move.run();
        } finally {
            travel.buffsPotions$enteredNetherPosition(enteredNether);
            IGNORE_TRAVEL.set(false);
            player.resetFallDistance();
        }
    }

    private static void keepLoaded(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addTicketWithRadius(TicketType.ENDER_PEARL, chunk, CHUNK_RADIUS);
        level.getChunk(chunk.x(), chunk.z());
    }

    private static void play(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void playQuiet(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.35F, 1.2F);
    }
}
