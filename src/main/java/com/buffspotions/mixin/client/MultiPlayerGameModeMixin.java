package com.buffspotions.mixin.client;

import com.buffspotions.client.StandSubjectClicks;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.network.StandPlacePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The creative inventory is built on the client, so the stack is tied to that player before it is sent. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$placeSubject(int containerId, RecipeDisplayId recipe, boolean useMaxItems, CallbackInfo callback) {
        Item subject = StandSubjectClicks.take(recipe);
        if (subject == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new StandPlacePayload(containerId, recipe.index(), BuiltInRegistries.ITEM.getId(subject)));
        callback.cancel();
    }

    @Inject(method = "handleCreativeModeItemAdd", at = @At("HEAD"))
    private void buffsPotions$anchorCreativeAdd(ItemStack clicked, int slot, CallbackInfo callback) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        // The creative menu reports a copy. Bind that copy, and the stack actually in the slot.
        AnchoredEnderFlagonItem.bind(clicked, Minecraft.getInstance().player);
        var menu = Minecraft.getInstance().player.inventoryMenu;
        if (slot >= 0 && slot < menu.slots.size()) {
            AnchoredEnderFlagonItem.bind(menu.getSlot(slot).getItem(), Minecraft.getInstance().player);
        }
    }

    @Inject(method = "handleCreativeModeItemDrop", at = @At("HEAD"))
    private void buffsPotions$anchorCreativeDrop(ItemStack clicked, CallbackInfo callback) {
        if (Minecraft.getInstance().player != null) {
            AnchoredEnderFlagonItem.bind(clicked, Minecraft.getInstance().player);
        }
    }
}
