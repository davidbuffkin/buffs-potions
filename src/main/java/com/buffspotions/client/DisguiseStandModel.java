package com.buffspotions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/** A disguised armor stand, villager, or adult zombie, drawn with the subject's skin and the entity's armor. */
public final class DisguiseStandModel {
    private static PlayerModel wide;
    private static PlayerModel slim;
    private static ArmorModelSet<PlayerModel> wideArmor;
    private static ArmorModelSet<PlayerModel> slimArmor;
    private static EquipmentLayerRenderer equipment;
    private static ElytraModel elytra;

    private DisguiseStandModel() {
    }

    public static void init(EntityRendererProvider.Context context) {
        equipment = context.getEquipmentRenderer();
        EntityModelSet models = context.getModelSet();
        wide = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER), false);
        slim = new PlayerModel(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        wideArmor = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, models, part -> new PlayerModel(part, false));
        slimArmor = ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR, models, part -> new PlayerModel(part, true));
        elytra = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
    }

    public static void submit(
            String name,
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        HumanoidRenderState humanoid = state instanceof HumanoidRenderState found ? found : null;
        ItemStackRenderState rightHand = humanoid != null ? humanoid.rightHandItemState : heldItem(state);
        ItemStackRenderState leftHand = humanoid != null ? humanoid.leftHandItemState : null;
        PlayerSkin skin = DisguiseSkins.skin(name);
        boolean slimSkin = skin.model() == PlayerModelType.SLIM;
        PlayerModel model = slimSkin ? slimModel() : wideModel();
        AvatarRenderState avatar = new AvatarRenderState();
        avatar.skin = skin;
        avatar.bodyRot = state.bodyRot;
        if (state instanceof ArmorStandRenderState stand) {
            avatar.yRot = 0.0F;
            avatar.xRot = 0.0F;
            ((DisguiseStandMark) (Object) avatar).buffsPotions$setStandPose(new Rotations[] {
                    stand.headPose,
                    stand.bodyPose,
                    stand.leftArmPose,
                    stand.rightArmPose,
                    stand.leftLegPose,
                    stand.rightLegPose
            });
        } else {
            avatar.yRot = state.yRot;
            avatar.xRot = state.xRot;
        }
        avatar.walkAnimationPos = state.walkAnimationPos;
        avatar.walkAnimationSpeed = state.walkAnimationSpeed;
        avatar.attackTime = humanoid != null ? humanoid.attackTime : 0.0F;
        avatar.attackArm = humanoid != null ? humanoid.attackArm : HumanoidArm.RIGHT;
        avatar.mainArm = humanoid != null ? humanoid.mainArm : HumanoidArm.RIGHT;
        if (!((DisguiseStandMark) state).buffsPotions$stillArms()) {
            avatar.ageInTicks = state.ageInTicks;
        }
        ((DisguiseStandMark) (Object) avatar).buffsPotions$setStillArms(((DisguiseStandMark) state).buffsPotions$stillArms());
        avatar.rightArmPose = held(rightHand) ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        avatar.leftArmPose = held(leftHand) ? HumanoidModel.ArmPose.ITEM : HumanoidModel.ArmPose.EMPTY;
        if (humanoid != null) {
            avatar.elytraRotX = humanoid.elytraRotX;
            avatar.elytraRotY = humanoid.elytraRotY;
            avatar.elytraRotZ = humanoid.elytraRotZ;
            avatar.headEquipment = humanoid.headEquipment;
            avatar.chestEquipment = humanoid.chestEquipment;
            avatar.legsEquipment = humanoid.legsEquipment;
            avatar.feetEquipment = humanoid.feetEquipment;
        }
        avatar.lightCoords = state.lightCoords;
        avatar.outlineColor = state.outlineColor;
        avatar.hasRedOverlay = state.hasRedOverlay;
        avatar.showCape = false;

        poseStack.pushPose();
        float scale = state.scale;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        // Same tip-over a dying player gets: up to 90 degrees over the death timer.
        if (state.deathTime > 0.0F) {
            float fall = (state.deathTime - 1.0F) / 20.0F * 1.6F;
            fall = Mth.sqrt(fall);
            if (fall > 1.0F) {
                fall = 1.0F;
            }
            poseStack.mulPose(Axis.ZP.rotationDegrees(fall * 90.0F));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(avatar);
        int overlay = LivingEntityRenderer.getOverlayCoords(avatar, 0.0F);
        collector.submitModel(
                model,
                avatar,
                poseStack,
                model.renderType(skin.body().texturePath()),
                state.lightCoords,
                overlay,
                -1,
                null,
                state.outlineColor,
                null
        );
        ArmorModelSet<PlayerModel> armor = slimSkin ? slimArmor : wideArmor;
        if (equipment != null && armor != null) {
            renderPiece(avatar, avatar.headEquipment, EquipmentSlot.HEAD, armor, poseStack, collector);
            renderPiece(avatar, avatar.chestEquipment, EquipmentSlot.CHEST, armor, poseStack, collector);
            renderPiece(avatar, avatar.legsEquipment, EquipmentSlot.LEGS, armor, poseStack, collector);
            renderPiece(avatar, avatar.feetEquipment, EquipmentSlot.FEET, armor, poseStack, collector);
        }
        renderWings(avatar, skin, poseStack, collector);
        submitHand(model, avatar, rightHand, HumanoidArm.RIGHT, poseStack, collector);
        submitHand(model, avatar, leftHand, HumanoidArm.LEFT, poseStack, collector);
        poseStack.popPose();
    }

    private static boolean held(ItemStackRenderState item) {
        return item != null && !item.isEmpty();
    }

    private static ItemStackRenderState heldItem(LivingEntityRenderState state) {
        if (state instanceof HoldingEntityRenderState holding && held(holding.heldItem)) {
            return holding.heldItem;
        }
        return null;
    }

    private static void submitHand(
            PlayerModel model,
            AvatarRenderState avatar,
            ItemStackRenderState item,
            HumanoidArm arm,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (!held(item)) {
            return;
        }
        poseStack.pushPose();
        model.translateToHand(avatar, arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        boolean left = arm == HumanoidArm.LEFT;
        poseStack.translate((left ? -1.0F : 1.0F) / 16.0F, 2.0F / 16.0F, -10.0F / 16.0F);
        item.submit(poseStack, collector, avatar.lightCoords, OverlayTexture.NO_OVERLAY, avatar.outlineColor);
        poseStack.popPose();
    }

    private static void renderWings(
            AvatarRenderState avatar,
            PlayerSkin skin,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        ItemStack stack = avatar.chestEquipment;
        if (stack == null || stack.isEmpty() || equipment == null || elytra == null) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty() || equippable.slot() != EquipmentSlot.CHEST) {
            return;
        }
        Identifier texture = null;
        if (skin.elytra() != null) {
            texture = skin.elytra().texturePath();
        } else if (skin.cape() != null) {
            texture = skin.cape().texturePath();
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.125F);
        equipment.renderLayers(
                EquipmentClientInfo.LayerType.WINGS,
                equippable.assetId().get(),
                elytra,
                avatar,
                stack,
                poseStack,
                collector,
                avatar.lightCoords,
                texture,
                avatar.outlineColor,
                0
        );
        poseStack.popPose();
    }

    private static void renderPiece(
            AvatarRenderState avatar,
            ItemStack stack,
            EquipmentSlot slot,
            ArmorModelSet<PlayerModel> armor,
            PoseStack poseStack,
            SubmitNodeCollector collector
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.assetId().isEmpty() || equippable.slot() != slot) {
            return;
        }
        EquipmentClientInfo.LayerType layer = slot == EquipmentSlot.LEGS
                ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS
                : EquipmentClientInfo.LayerType.HUMANOID;
        equipment.renderLayers(
                layer,
                equippable.assetId().get(),
                armor.get(slot),
                avatar,
                stack,
                poseStack,
                collector,
                avatar.lightCoords,
                avatar.outlineColor
        );
    }

    private static PlayerModel wideModel() {
        if (wide == null) {
            wide = new PlayerModel(net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return wide;
    }

    private static PlayerModel slimModel() {
        if (slim == null) {
            slim = new PlayerModel(net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        return slim;
    }
}
