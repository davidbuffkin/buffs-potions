package com.buffspotions.mixin;

import com.buffspotions.effect.CatGift;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.animal.feline.Cat$CatRelaxOnOwnerGoal")
abstract class CatGiftMixin {
    @Redirect(
            method = "giveMorningGift",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/feline/Cat;dropFromGiftLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/BiConsumer;)Z"
            )
    )
    private boolean buffsPotions$gift(Cat cat, ServerLevel level, ResourceKey<LootTable> key, BiConsumer<ServerLevel, ItemStack> drop) {
        LivingEntity owner = cat.getOwner();
        CatGift.Kind kind = CatGift.select(amplifier(owner, true), amplifier(owner, false));
        if (kind == CatGift.Kind.VANILLA || owner == null) {
            return cat.dropFromGiftLootTable(level, key, drop);
        }
        CatGift.deliver(level, owner, CatGift.roll(cat.getRandom(), kind), drop);
        return true;
    }

    private static int amplifier(LivingEntity living, boolean luck) {
        if (living == null) {
            return -1;
        }
        MobEffectInstance effect = living.getEffect(luck ? MobEffects.LUCK : MobEffects.UNLUCK);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
