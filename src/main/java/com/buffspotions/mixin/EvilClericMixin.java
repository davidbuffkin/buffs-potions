package com.buffspotions.mixin;

import com.buffspotions.structure.EvilCleric;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
abstract class EvilClericMixin {
    @Shadow
    private boolean assignProfessionWhenSpawned;

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void buffsPotions$lockCuredEvilCleric(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> callback
    ) {
        Villager villager = (Villager) (Object) this;
        if (!EvilCleric.is(villager)) {
            return;
        }
        this.assignProfessionWhenSpawned = false;
        EvilCleric.lock(villager);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void buffsPotions$keepCuredEvilCleric(ServerLevel level, CallbackInfo callback) {
        Villager villager = (Villager) (Object) this;
        if (!EvilCleric.is(villager)) {
            return;
        }
        this.assignProfessionWhenSpawned = false;
        EvilCleric.lock(villager);
    }

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void buffsPotions$replaceEnchantingBottle(ServerLevel level, CallbackInfo callback) {
        Villager villager = (Villager) (Object) this;
        if (EvilCleric.is(villager)) {
            EvilCleric.prepare(villager.getOffers(), null);
        }
    }

    @Inject(method = "startTrading", at = @At("HEAD"))
    private void buffsPotions$showTradingPlayersHead(Player player, CallbackInfo callback) {
        Villager villager = (Villager) (Object) this;
        if (EvilCleric.is(villager)) {
            EvilCleric.prepare(villager.getOffers(), player.getGameProfile());
        }
    }

    @Inject(method = "resendOffersToTradingPlayer", at = @At("HEAD"))
    private void buffsPotions$refreshTradingPlayersHead(CallbackInfo callback) {
        Villager villager = (Villager) (Object) this;
        Player player = villager.getTradingPlayer();
        if (player != null && EvilCleric.is(villager)) {
            EvilCleric.prepare(villager.getOffers(), player.getGameProfile());
        }
    }
}
