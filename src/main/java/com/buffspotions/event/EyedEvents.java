package com.buffspotions.event;

import com.buffspotions.BuffsPotions;
import com.buffspotions.advancement.BeltAdvancements;
import com.buffspotions.advancement.HalloweenSplash;
import com.buffspotions.advancement.StacksOnStacks;
import com.buffspotions.advancement.TipJar;
import com.buffspotions.effect.BlockCracking;
import com.buffspotions.effect.DisguiseArmorStands;
import com.buffspotions.effect.GreenThumb;
import com.buffspotions.effect.BlockStacking;
import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.Insight;
import com.buffspotions.effect.ItemEating;
import com.buffspotions.effect.ItemVomiting;
import com.buffspotions.effect.VermillionHarvest;
import com.buffspotions.effect.ParameterDoses;
import com.buffspotions.effect.Seeking;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class EyedEvents {
    private EyedEvents() {
    }

    private static void clearDisguise(Entity entity, Holder<MobEffect> effect) {
        if (effect.is(ModEffects.DISGUISE) && disguisedBody(entity)) {
            DisguiseArmorStands.clear((LivingEntity) entity);
        }
    }

    private static boolean disguisedBody(Entity entity) {
        return entity instanceof ArmorStand || entity instanceof Villager || entity instanceof Zombie;
    }

    @SubscribeEvent
    public static void onApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && effect.is(ModEffects.DISGUISE)) {
            boolean babyZombie = event.getEntity() instanceof Zombie zombie && zombie.isBaby();
            boolean babyVillager = event.getEntity() instanceof Villager villager && villager.isBaby();
            if (babyZombie || babyVillager) {
                event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            }
        }
    }

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof LivingEntity living) {
            HalloweenSplash.note(living, event.getEffectInstance());
        }
        if (event.getEntity() instanceof ServerPlayer server) {
            BeltAdvancements.onEffectAdded(server, event.getEffectInstance());
        }
        Eyedentification.onAdded(event);
    }

    @SubscribeEvent(receiveCanceled = false)
    public static void onRemove(MobEffectEvent.Remove event) {
        if (event.getEffect().is(ModEffects.GREEN_THUMB) && event.getEntity() instanceof ServerPlayer server) {
            VermillionHarvest.stop(server);
        }
        if (event.getEntity() instanceof Player player) {
            Eyedentification.onCleared(player, event.getEffect());
        } else {
            clearDisguise(event.getEntity(), event.getEffect());
        }
    }

    @SubscribeEvent
    public static void onExpire(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null) {
            return;
        }
        if (effect.is(ModEffects.GREEN_THUMB) && event.getEntity() instanceof ServerPlayer server) {
            VermillionHarvest.stop(server);
        }
        if (effect.is(ModEffects.DISGUISE) && disguisedBody(event.getEntity())) {
            DisguiseArmorStands.clear((LivingEntity) event.getEntity());
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Eyedentification.onExpired(player, effect.getEffect());
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        BlockStacking.rememberClick(event);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        BlockCracking.rememberClick(event);
    }

    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        GreenThumb.rememberFruit(event);
        BlockCracking.onBreak(event);
    }

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Post event) {
        GreenThumb.noteTorchflower(event);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        BlockStacking.onPlace(event);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        GreenThumb.onBlockDrops(event);
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        GreenThumb.preventTrample(event);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ParameterDoses.tick(event.getServer());
        BlockStacking.tick(event.getServer());
        BlockCracking.tick(event.getServer());
        ItemEating.attract(event.getServer());
        Insight.attract(event.getServer());
        ItemVomiting.tick(event.getServer());
        Seeking.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        BlockStacking.forgetAll();
        StacksOnStacks.forgetAll();
        TipJar.forgetAll();
        BlockCracking.forgetAll();
        ItemVomiting.forgetAll();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BlockStacking.forget(event.getEntity());
        StacksOnStacks.forget(event.getEntity().getUUID());
        TipJar.forget(event.getEntity().getUUID());
        BlockCracking.forget(event.getEntity());
        ItemVomiting.forget(event.getEntity());
    }
}
