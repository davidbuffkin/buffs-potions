package com.buffspotions.effect;

import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ItemEating {
    public static final int EAT_TICKS = 32;

    private ItemEating() {
    }

    public static boolean canEat(Entity entity, ItemStack stack) {
        if (!(entity instanceof Player player) || stack.isEmpty() || named(stack) || !player.hasEffect(ModEffects.ITEM_EATER)) {
            return false;
        }
        return ParameterDoses.amplifierForItem(player, ModEffects.ITEM_EATER, stack) >= 0;
    }

    public static boolean named(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_NAME);
    }

    /** True when the item already restores hunger, so Item Eater adds a point instead of replacing the meal. */
    public static boolean hasFoodValue(ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null && food.nutrition() > 0;
    }

    public static void tickEat(LivingEntity entity, ItemStack stack, int remaining) {
        if (entity.level().isClientSide() || !canEat(entity, stack) || stack.has(DataComponents.CONSUMABLE)) {
            return;
        }
        Consumable food = cookedPorkchop();
        if (food.shouldEmitParticlesAndSounds(remaining)) {
            playEat(entity, stack, food, 5);
        }
    }

    /**
     * {@code playSound} on a player excludes that player. The crunches have to be played at the
     * position so the eater hears them; the burp already is.
     */
    private static void playEat(LivingEntity entity, ItemStack stack, Consumable food, int particles) {
        RandomSource random = entity.getRandom();
        if (food.hasConsumeParticles()) {
            entity.spawnItemParticles(stack, particles);
        }
        entity.level().playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                food.sound().value(),
                SoundSource.NEUTRAL,
                random.nextBoolean() ? 0.5F : 1.0F,
                random.triangle(1.0F, 0.2F)
        );
    }

    public static void finishEat(Player player, ItemStack stack) {
        if (player.level().isClientSide() || !canEat(player, stack)) {
            return;
        }
        if (stack.is(Items.STICK) && player instanceof ServerPlayer server) {
            ModCriteria.HARD_TIMES.get().trigger(server);
        }
        Consumable food = cookedPorkchop();
        RandomSource random = player.getRandom();
        food.emitParticlesAndSounds(random, player, stack, 16);
        Level level = player.level();
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                food.sound().value(),
                SoundSource.NEUTRAL,
                1.0F,
                random.triangle(1.0F, 0.4F)
        );
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS,
                0.5F,
                Mth.randomBetween(random, 0.9F, 1.0F)
        );
    }

    /** One hunger point and no saturation. Food items receive this on top of their own meal. */
    public static void feed(Player player) {
        if (!player.level().isClientSide()) {
            player.getFoodData().eat(1, 0.0F);
        }
    }

    private static Consumable cookedPorkchop() {
        Consumable consumable = Items.COOKED_PORKCHOP.components().get(DataComponents.CONSUMABLE);
        return consumable == null ? Consumables.DEFAULT_FOOD : consumable;
    }

    public static void attract(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(ModEffects.ITEM_EATER) || !(player.level() instanceof ServerLevel level)) {
                continue;
            }
            int reach = reach(player);
            if (reach <= 0) {
                continue;
            }
            AABB area = player.getBoundingBox().inflate(reach);
            var doses = ParameterDoses.matching(player, ModEffects.ITEM_EATER);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, entity -> edible(entity))) {
                int distance = 0;
                for (ParameterDose dose : doses) {
                    if (dose.subject().item().isPresent() && item.getItem().is(dose.subject().item().get())) {
                        distance = Math.max(distance, EyedTuning.attractDistance(dose.amplifier()));
                    }
                }
                pull(player, item, distance);
            }
        }
    }

    /** The strongest Item Eater dose sets how far matching drops are pulled. */
    public static int reach(Player player) {
        int reach = 0;
        for (ParameterDose dose : ParameterDoses.matching(player, ModEffects.ITEM_EATER)) {
            if (dose.subject().item().isPresent()) {
                reach = Math.max(reach, EyedTuning.attractDistance(dose.amplifier()));
            }
        }
        return reach;
    }

    static void pull(ServerPlayer player, Entity target, int distance) {
        if (!withinPull(player, target, distance)) {
            return;
        }
        Vec3 pull = player.position().add(0.0, 0.4, 0.0).subtract(target.position());
        double gap = pull.length();
        double closeness = 1.0 - gap / distance;
        double speed = EyedTuning.attractSpeed(closeness);
        target.setDeltaMovement(target.getDeltaMovement().scale(0.55).add(pull.scale(speed / gap)));
        target.hurtMarked = true;
    }

    static boolean withinPull(ServerPlayer player, Entity target, int distance) {
        if (distance <= 0) {
            return false;
        }
        double gap = player.position().add(0.0, 0.4, 0.0).subtract(target.position()).length();
        return gap <= distance && gap >= 0.22;
    }

    private static boolean edible(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        return !stack.isEmpty() && !named(stack) && !entity.hasCustomName();
    }
}
