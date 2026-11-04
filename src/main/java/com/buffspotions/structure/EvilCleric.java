package com.buffspotions.structure;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

/** The zombified cleric saved in the evil apothecary, and the villager he becomes. */
public final class EvilCleric {
    public static final String TAG = "buffs_potions.evil_cleric";
    private static final String HEAD_TRADE = "buffs_potions:player_head";

    private EvilCleric() {
    }

    /**
     * Replaces this cleric's bottle o' enchanting with one player head for 30 emeralds.
     * The head's profile is the player currently trading, so each buyer sees and receives their own.
     */
    public static void prepare(MerchantOffers offers, @Nullable GameProfile profile) {
        ResolvableProfile resolved = profile == null ? null : ResolvableProfile.createResolved(profile);
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.getResult().is(Items.EXPERIENCE_BOTTLE)) {
                offers.set(i, headOffer(resolved));
            } else if (resolved != null && isHeadTrade(offer)) {
                offer.getResult().set(DataComponents.PROFILE, resolved);
            }
        }
    }

    private static MerchantOffer headOffer(@Nullable ResolvableProfile profile) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(HEAD_TRADE, true);
        head.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        if (profile != null) {
            head.set(DataComponents.PROFILE, profile);
        }
        return new MerchantOffer(new ItemCost(Items.EMERALD, 30), head, 1, 30, 0.05F);
    }

    private static boolean isHeadTrade(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        if (!result.is(Items.PLAYER_HEAD)) {
            return false;
        }
        CustomData data = result.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(HEAD_TRADE);
    }

    public static boolean is(Entity entity) {
        return entity.entityTags().contains(TAG);
    }

    public static void lock(ZombieVillager zombie) {
        if (!is(zombie) || zombie.level().isClientSide()) {
            return;
        }
        VillagerData data = zombie.getVillagerData();
        if (!data.profession().is(VillagerProfession.CLERIC) || data.level() < 1) {
            zombie.setVillagerData(data.withProfession(zombie.registryAccess(), VillagerProfession.CLERIC).withLevel(Math.max(1, data.level())));
        }
        if (zombie.getVillagerXp() < 1) {
            zombie.setVillagerXp(1);
        }
    }

    public static void lock(Villager villager) {
        if (!is(villager) || villager.level().isClientSide()) {
            return;
        }
        VillagerData data = villager.getVillagerData();
        if (!data.profession().is(VillagerProfession.CLERIC) || data.level() < 1) {
            villager.setVillagerData(data.withProfession(villager.registryAccess(), VillagerProfession.CLERIC).withLevel(Math.max(1, data.level())));
        }
        if (villager.getVillagerXp() < 1) {
            villager.setVillagerXp(1);
        }
    }
}
