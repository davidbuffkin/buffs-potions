package com.buffspotions.mixin;

import com.buffspotions.potion.EssenceNames;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.MundaneBrewing;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TippedArrowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PotionItem.class, TippedArrowItem.class})
public abstract class PotionNameMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$essenceName(ItemStack stack, CallbackInfoReturnable<Component> callback) {
        Component remedy = Remedies.name(stack);
        if (remedy != null) {
            callback.setReturnValue(remedy);
            return;
        }
        Component name = EssenceNames.forStack(stack);
        if (name != null) {
            callback.setReturnValue(MundaneBrewing.wrapName(stack, name));
        }
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$mundaneName(ItemStack stack, CallbackInfoReturnable<Component> callback) {
        if (!MundaneBrewing.isMundane(stack)) {
            return;
        }
        Component current = callback.getReturnValue();
        if (current.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("item.buffs_potions.mundane")) {
            return;
        }
        callback.setReturnValue(MundaneBrewing.wrapName(stack, current));
    }
}
