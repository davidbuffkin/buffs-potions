package com.buffspotions.mixin;

import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.effect.EyedDisplay;
import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.VermillionHarvest;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {
    @Inject(method = "onConsume", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$modifyDrunkPotion(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo callback) {
        if (!level.isClientSide() && user instanceof ServerPlayer server) {
            VermillionHarvest.drink(server, stack);
            if (SuspiciousBrew.is(stack) && stack.is(Items.POTION)) {
                ModCriteria.SUSPICIOUS_BREW.get().trigger(server);
            }
            if (EyedBrewing.isNetheriteBlockEssence(stack)) {
                ModCriteria.NETHERITE_ESSENCE.get().trigger(server);
            }
            if (!Remedies.isRemedy(stack)) {
                netherAgain(server, stack, (PotionContents) (Object) this);
            }
        }
        if (!level.isClientSide()) {
            Eyedentification.beginApply(stack);
        }
        if (Remedies.isRemedy(stack)) {
            if (!level.isClientSide()) {
                Remedies.apply(user, stack);
            }
            callback.cancel();
            return;
        }
        if (level.isClientSide() || !(user instanceof Player player)) {
            return;
        }
        ItemStack belt = BeltEnchantments.modifierSource(player, stack);
        float duration = BeltEnchantments.durationMultiplier(belt);
        int amplifier = BeltEnchantments.amplifierBonus(belt);
        if (duration == 1.0F && amplifier == 0) {
            return;
        }
        callback.cancel();
        BeltEnchantments.applyModified(player, (PotionContents) (Object) this, stack, duration, amplifier);
    }

    @Inject(method = "onConsume", at = @At("RETURN"))
    private void buffsPotions$endApply(Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo callback) {
        Eyedentification.endApply();
    }

    /** Twenty-four minutes, using the same duration scale a drink applies. */
    private static void netherAgain(ServerPlayer player, ItemStack stack, PotionContents contents) {
        ItemStack belt = BeltEnchantments.modifierSource(player, stack);
        float scale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F) * BeltEnchantments.durationMultiplier(belt);
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.is(MobEffects.FIRE_RESISTANCE) || effect.isInfiniteDuration() || effect.getDuration() <= 0) {
                continue;
            }
            int duration = Math.max(Mth.floor(effect.getDuration() * scale), 1);
            if (duration >= 24 * 60 * 20) {
                ModCriteria.NETHER_AGAIN.get().trigger(player);
                return;
            }
        }
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$tooltipSubject(Item.TooltipContext context, Consumer<Component> lines, TooltipFlag flag, DataComponentGetter components, CallbackInfo callback) {
        EyedDisplay.TOOLTIP.set(components.get(ModDataComponents.ESSENCE.get()));
        EyedDisplay.BOUNDLESS.set(false);
        for (MobEffectInstance effect : ((PotionContents) (Object) this).getAllEffects()) {
            if (effect.is(com.buffspotions.registry.ModEffects.DISGUISE) && effect.isInfiniteDuration()) {
                EyedDisplay.BOUNDLESS.set(true);
                break;
            }
        }
        if (Remedies.appendTooltip(components, lines)) {
            callback.cancel();
            return;
        }
        if (((PotionContents) (Object) this).is(ModPotions.SUSPICIOUS_BREW)) {
            lines.accept(SuspiciousBrew.unknownEffect());
            callback.cancel();
        }
    }

    @Inject(method = "addToTooltip", at = @At("RETURN"))
    private void buffsPotions$clearTooltipSubject(Item.TooltipContext context, Consumer<Component> lines, TooltipFlag flag, DataComponentGetter components, CallbackInfo callback) {
        EyedDisplay.TOOLTIP.remove();
        EyedDisplay.BOUNDLESS.remove();
    }

    @Inject(method = "getColorOr", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$suspiciousColor(int defaultColor, CallbackInfoReturnable<Integer> callback) {
        if (((PotionContents) (Object) this).is(ModPotions.SUSPICIOUS_BREW)) {
            callback.setReturnValue(SuspiciousBrew.COLOR);
        }
    }

    @Inject(method = "getPotionDescription", at = @At("HEAD"), cancellable = true)
    private static void buffsPotions$namedEffect(Holder<MobEffect> effect, int amplifier, CallbackInfoReturnable<MutableComponent> callback) {
        MutableComponent line = EyedDisplay.effectLine(effect, amplifier);
        if (line != null) {
            callback.setReturnValue(line);
        }
    }
}
