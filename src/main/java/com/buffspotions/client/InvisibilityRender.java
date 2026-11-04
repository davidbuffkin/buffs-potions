package com.buffspotions.client;

import com.buffspotions.effect.InvisibilityVeil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class InvisibilityRender {
    private InvisibilityRender() {
    }

    public static void hide(LivingEntity entity, HumanoidRenderState state) {
        if (InvisibilityVeil.hidesArmor(entity)) {
            state.headEquipment = ItemStack.EMPTY;
            state.chestEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
        }
        if (InvisibilityVeil.hidesHeldItem(entity)) {
            state.rightHandItemStack = ItemStack.EMPTY;
            state.leftHandItemStack = ItemStack.EMPTY;
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
            state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        }
    }

    public static void hideAvatar(LivingEntity entity, AvatarRenderState state) {
        hide(entity, state);
        if (InvisibilityVeil.hidesHeldItem(entity)) {
            state.heldOnHead.clear();
        }
    }
}
