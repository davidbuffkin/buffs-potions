package com.buffspotions.effect;

import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;

/**
 * Full-moon midnight, high above the world, with a disguise bottle and a clock.
 * Ten seconds later the clock is spent and the bottle becomes boundless.
 */
public final class BoundlessNight {
    private static final int RITUAL_TICKS = 10 * 20;
    /** Look vector Y at five degrees from straight up. */
    private static final double UPWARD = Math.cos(Math.toRadians(5.0));

    private BoundlessNight() {
    }

    public static void tick(ServerPlayer player) {
        Watch watch = player.getData(ModAttachments.BOUNDLESS_WATCH);
        if (watch.remaining() > 0) {
            advance(player, watch);
            return;
        }
        if (!moment(player)) {
            return;
        }
        long night = Math.floorDiv(player.level().getOverworldClockTime(), 24000L);
        if (watch.night() == night) {
            return;
        }
        begin(player);
        advance(player, new Watch(RITUAL_TICKS, night));
    }

    public static boolean midnight(long clock) {
        return Math.floorMod(clock, 24000L) == 18000L;
    }

    public static boolean highEnough(double y) {
        return y > 319.75;
    }

    public static boolean lookingUp(Vec3 look) {
        return look.y >= UPWARD;
    }

    public static boolean luckTwo(MobEffectInstance luck) {
        return luck != null && luck.is(MobEffects.LUCK) && luck.getAmplifier() >= 1;
    }

    /** A disguise bottle in one hand and a clock in the other. */
    public static boolean offered(ItemStack main, ItemStack off) {
        return (disguiseBottle(main) && off.is(Items.CLOCK)) || (disguiseBottle(off) && main.is(Items.CLOCK));
    }

    public static boolean disguiseBottle(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        EssenceSubject subject = stack.get(ModDataComponents.ESSENCE.get());
        if (subject == null || subject.playerName().isEmpty()) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(ModEffects.DISGUISE)) {
                return true;
            }
        }
        return false;
    }

    /** Spend one clock and turn the disguise bottle boundless, when both are still held. */
    public static boolean completeHands(ItemStack main, ItemStack off) {
        if (disguiseBottle(main) && off.is(Items.CLOCK)) {
            bind(main);
            off.shrink(1);
            return true;
        }
        if (disguiseBottle(off) && main.is(Items.CLOCK)) {
            bind(off);
            main.shrink(1);
            return true;
        }
        return false;
    }

    public static void bind(ItemStack bottle) {
        EssenceSubject subject = bottle.get(ModDataComponents.ESSENCE.get());
        if (subject == null || subject.playerName().isEmpty()) {
            return;
        }
        PotionContents contents = bottle.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        bottle.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.of(contents.getColor()),
                List.of(new MobEffectInstance(ModEffects.DISGUISE, MobEffectInstance.INFINITE_DURATION)),
                Optional.of("disguise")
        ));
        bottle.set(DataComponents.CUSTOM_NAME, Component.translatable(
                "item.buffs_potions.boundless.disguise",
                subject.label()
        ));
        bottle.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    }

    private static boolean moment(ServerPlayer player) {
        Level level = player.level();
        if (!player.isAlive() || !level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (!midnight(level.getOverworldClockTime()) || level.isRaining()) {
            return false;
        }
        if (level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, player.position()) != MoonPhase.FULL_MOON) {
            return false;
        }
        return highEnough(player.getY())
                && lookingUp(player.getLookAngle())
                && luckTwo(player.getEffect(MobEffects.LUCK))
                && offered(player.getMainHandItem(), player.getOffhandItem());
    }

    private static void begin(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, RITUAL_TICKS));
        player.level().playSound(
                null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.0F
        );
        player.level().playSound(
                null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.55F, 1.6F
        );
    }

    private static void advance(ServerPlayer player, Watch watch) {
        sparkle(player);
        int left = watch.remaining() - 1;
        if (left == 0) {
            completeHands(player.getMainHandItem(), player.getOffhandItem());
        }
        player.setData(ModAttachments.BOUNDLESS_WATCH.get(), new Watch(left, watch.night()));
    }

    private static void sparkle(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var random = player.getRandom();
        for (int i = 0; i < 3; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * 2.4;
            double y = player.getEyeY() + 0.5 + random.nextDouble() * 1.8;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 2.4;
            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = -0.02 - random.nextDouble() * 0.03;
            double vz = (random.nextDouble() - 0.5) * 0.02;
            level.sendParticles(ModParticles.FALLING_ROD.get(), x, y, z, 0, vx, vy, vz, 1.0);
        }
    }

    public record Watch(int remaining, long night) {
        public static final Watch IDLE = new Watch(0, -1L);
        public static final Codec<Watch> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("remaining").forGetter(Watch::remaining),
                Codec.LONG.fieldOf("night").forGetter(Watch::night)
        ).apply(instance, Watch::new));
    }
}
