package com.buffspotions.mixin;

import com.buffspotions.belt.BeltMending;
import com.buffspotions.equipment.BeltSlotAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getRandomItemWith", at = @At("RETURN"), cancellable = true)
    private static void buffsPotions$includeEquippedBelt(
            DataComponentType<?> componentType,
            LivingEntity entity,
            Predicate<ItemStack> predicate,
            CallbackInfoReturnable<Optional<EnchantedItemInUse>> callback
    ) {
        if (componentType != EnchantmentEffectComponents.REPAIR_WITH_XP || !(entity instanceof Player player)) {
            return;
        }
        ItemStack belt = BeltSlotAccess.getEquipped(player);
        if (!predicate.test(belt) || !BeltMending.hasMending(belt)) {
            return;
        }

        List<EnchantedItemInUse> candidates = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (predicate.test(stack) && BeltMending.acceptsRepair(stack, slot)) {
                candidates.add(new EnchantedItemInUse(stack, slot, entity));
            }
        }
        candidates.add(new EnchantedItemInUse(belt, EquipmentSlot.MAINHAND, entity));
        callback.setReturnValue(Util.getRandomSafe(candidates, entity.getRandom()));
    }
}
