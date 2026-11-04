package com.buffspotions.mixin.client;

import com.buffspotions.belt.BeltSource;
import com.buffspotions.belt.BeltUseState;
import com.buffspotions.client.BeltDrinkAnim;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private float mainHandHeight;
    @Shadow
    private float oMainHandHeight;

    @Inject(method = "tick", at = @At("RETURN"))
    private void buffsPotions$holdBeltSteady(CallbackInfo callback) {
        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null || !drinkingFromBelt(player)) {
            return;
        }
        this.mainHandItem = BeltSource.drinkingShown(player);
        this.mainHandHeight = 1.0F;
        this.oMainHandHeight = 1.0F;
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private ItemStack buffsPotions$dipToBelt(LocalPlayer player) {
        ItemStack hand = player.getMainHandItem();
        if (drinkingFromBelt(player)) {
            return BeltSource.drinkingShown(player);
        }
        return hand;
    }

    @Redirect(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z")
    )
    private boolean buffsPotions$beltCountsAsUsing(AbstractClientPlayer player) {
        return player.isUsingItem() || drinkingFromBelt(player);
    }

    @Redirect(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I")
    )
    private int buffsPotions$beltUseTicks(AbstractClientPlayer player) {
        return beltUseTicks(player);
    }

    @Redirect(
            method = "applyEatTransform",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getUseItemRemainingTicks()I")
    )
    private int buffsPotions$eatUseTicks(Player player) {
        return beltUseTicks(player);
    }

    @Redirect(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;"
            )
    )
    private InteractionHand buffsPotions$beltUseHand(AbstractClientPlayer player) {
        if (drinkingFromBelt(player)) {
            return InteractionHand.MAIN_HAND;
        }
        return player.getUsedItemHand();
    }

    private static int beltUseTicks(Player player) {
        if (!drinkingFromBelt(player)) {
            return player.getUseItemRemainingTicks();
        }
        return BeltDrinkAnim.remaining(player);
    }

    private static boolean drinkingFromBelt(Player player) {
        if (player.getMainHandItem().getItem() instanceof PotionBeltItem) {
            return false;
        }
        BeltUseState state = player.getData(ModAttachments.BELT_USE);
        return state.isDrinking() && !state.gulp() && !BeltSource.drinking(player).isEmpty();
    }
}
