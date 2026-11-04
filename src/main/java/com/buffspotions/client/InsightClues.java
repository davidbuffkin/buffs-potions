package com.buffspotions.client;

import com.buffspotions.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class InsightClues {
    private InsightClues() {
    }

    public static List<Component> extraLines(EnchantmentMenu menu, int slot, Player player) {
        MobEffectInstance insight = player.getEffect(ModEffects.INSIGHTFUL);
        if (insight == null || menu.costs[slot] <= 0 || player.level() == null) {
            return List.of();
        }
        ItemStack stack = menu.getSlot(0).getItem();
        if (!stack.isEnchantable()) {
            return List.of();
        }
        Optional<HolderSet.Named<Enchantment>> tag = player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return List.of();
        }
        RandomSource random = RandomSource.create();
        random.setSeed(menu.getEnchantmentSeed() + slot);
        List<EnchantmentInstance> offers = EnchantmentHelper.selectEnchantment(
                random, stack, menu.costs[slot], tag.get().stream()
        );
        if (stack.is(Items.BOOK) && offers.size() > 1) {
            offers.remove(random.nextInt(offers.size()));
        }
        if (offers.isEmpty()) {
            return List.of();
        }
        int shown = random.nextInt(offers.size());
        EnchantmentInstance vanilla = offers.get(shown);
        var holders = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
        if (holders.getId(vanilla.enchantment()) != menu.enchantClue[slot] || vanilla.level() != menu.levelClue[slot]) {
            return List.of();
        }
        int extra = insight.getAmplifier() + 1;
        List<Component> lines = new ArrayList<>();
        for (int index = 0; index < offers.size() && lines.size() < extra; index++) {
            if (index == shown) {
                continue;
            }
            EnchantmentInstance offer = offers.get(index);
            lines.add(Component.translatable(
                    "container.enchant.clue",
                    Enchantment.getFullname(offer.enchantment(), offer.level())
            ).withStyle(ChatFormatting.WHITE));
        }
        if (lines.size() < extra) {
            lines.add(Component.translatable("container.buffs_potions.no_further_enchantments")
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /** A clue that is followed by another one shows the enchantment name without the trailing guess. */
    public static Component settled(Component line) {
        if (!(line.getContents() instanceof TranslatableContents contents)
                || !"container.enchant.clue".equals(contents.getKey())
                || contents.getArgs().length == 0
                || !(contents.getArgs()[0] instanceof Component name)) {
            return line;
        }
        return name.copy();
    }
}
