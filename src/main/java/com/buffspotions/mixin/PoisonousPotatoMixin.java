package com.buffspotions.mixin;

import com.buffspotions.effect.PoisonousPotatoDrop;
import com.buffspotions.effect.TamingLuck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LootItemRandomChanceCondition.class)
abstract class PoisonousPotatoMixin {
    @Redirect(
            method = "test",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/providers/number/NumberProvider;getFloat(Lnet/minecraft/world/level/storage/loot/LootContext;)F"
            )
    )
    private float buffsPotions$poisonousPotato(NumberProvider provider, LootContext context) {
        float probability = provider.getFloat(context);
        if (Math.abs(probability - PoisonousPotatoDrop.VANILLA) > 0.0001F) {
            return probability;
        }
        BlockState state = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(Blocks.POTATOES)) {
            return probability;
        }
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity living)) {
            return probability;
        }
        return PoisonousPotatoDrop.chance(TamingLuck.amplifier(living, true), TamingLuck.amplifier(living, false));
    }
}
