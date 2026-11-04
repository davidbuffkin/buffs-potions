package com.buffspotions.mixin;

import com.buffspotions.structure.EvilCleric;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieVillager.class)
abstract class ZombieVillagerMixin {
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void buffsPotions$lockEvilCleric(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData groupData,
            CallbackInfoReturnable<SpawnGroupData> callback
    ) {
        EvilCleric.lock((ZombieVillager) (Object) this);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void buffsPotions$keepEvilCleric(CallbackInfo callback) {
        EvilCleric.lock((ZombieVillager) (Object) this);
    }
}
