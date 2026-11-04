package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.menu.BrewingStackMenu;
import com.buffspotions.menu.StandBookMenu;
import com.buffspotions.stack.StackRecipeCatalog;
import com.buffspotions.stand.StandBrewRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Places a parameterized stand recipe for the subject that was on the button. */
public record StandPlacePayload(int containerId, int displayId, int subjectId) implements CustomPacketPayload {
    public static final Type<StandPlacePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "stand_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StandPlacePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StandPlacePayload::containerId,
            ByteBufCodecs.VAR_INT,
            StandPlacePayload::displayId,
            ByteBufCodecs.VAR_INT,
            StandPlacePayload::subjectId,
            StandPlacePayload::new
    );

    public static void handle(StandPlacePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.isSpectator()) {
                return;
            }
            boolean stand = player.containerMenu instanceof StandBookMenu;
            boolean stack = player.containerMenu instanceof BrewingStackMenu;
            if (!stand && !stack) {
                return;
            }
            if (player.containerMenu.containerId != payload.containerId || !player.containerMenu.stillValid(player)) {
                return;
            }
            RecipeManager.ServerDisplayInfo info = player.level().getServer().getRecipeManager()
                    .getRecipeFromDisplay(new RecipeDisplayId(payload.displayId));
            if (info == null) {
                return;
            }
            RecipeHolder<?> holder = info.parent();
            if (!player.getRecipeBook().contains(holder.id()) || !(holder.value() instanceof StandBrewRecipe brew)) {
                return;
            }
            Item subject = BuiltInRegistries.ITEM.byId(payload.subjectId);
            if (subject == Items.AIR) {
                return;
            }
            if (stand && !StackRecipeCatalog.owns(brew)) {
                ((StandBookMenu) player.containerMenu).placeSubject(player, brew, subject);
            } else if (stack && StackRecipeCatalog.owns(brew)) {
                ((BrewingStackMenu) player.containerMenu).placeSubject(player, brew, subject);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
