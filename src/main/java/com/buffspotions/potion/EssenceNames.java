package com.buffspotions.potion;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.MobDrops;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public final class EssenceNames {
    private EssenceNames() {
    }

    public static Component forStack(ItemStack stack) {
        return named(stack, container(stack.getItem()));
    }

    public static Component named(ItemStack stack, String container) {
        EssenceSubject subject = stack.get(ModDataComponents.ESSENCE.get());
        if (subject == null) {
            return null;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        String suffix = contents.customName().or(() -> contents.potion().map(potion -> potion.value().name())).orElse("");
        if (suffix.equals("essence")) {
            return Component.translatable("item.buffs_potions.named." + container + ".essence", subject.label());
        }
        if (suffix.equals("block_perception") && subject.block().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".perception", subject.blockLabel());
        }
        if (suffix.equals("block_stacking") && subject.block().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".stacking", subject.blockLabel());
        }
        if (suffix.equals("block_cracking") && subject.block().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".cracking", subject.blockLabel());
        }
        if (suffix.equals("formation") && subject.block().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".formation", subject.blockLabel());
        }
        if (suffix.equals("item_eating") && subject.item().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".eating", subject.label());
        }
        if (suffix.equals("incarnation")) {
            if (subject.entity().isPresent()) {
                return Component.translatable(
                        "item.buffs_potions.named." + container + ".incarnation",
                        subject.entity().get().value().getDescription()
                );
            }
            if (subject.item().isPresent() && MobDrops.type(subject.item().get()).isPresent()) {
                return Component.translatable(
                        "item.buffs_potions.named." + container + ".incarnation",
                        MobDrops.type(subject.item().get()).orElseThrow().getDescription()
                );
            }
            return Component.translatable("item.buffs_potions.named." + container + ".unknown_incarnation");
        }
        if (suffix.equals("item_vomiting") && subject.item().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".vomiting", subject.label());
        }
        if (suffix.equals("seeking") && (subject.item().isPresent() || subject.playerName().isPresent())) {
            return Component.translatable("item.buffs_potions.named." + container + ".seeking", seekingSubject(subject));
        }
        if (suffix.equals("disguise") && subject.playerName().isPresent()) {
            return Component.translatable("item.buffs_potions.named." + container + ".disguise", subject.label());
        }
        return null;
    }

    public static Component seekingSubject(EssenceSubject subject) {
        if (subject.item().isPresent()) {
            var mob = MobDrops.type(subject.item().get());
            if (mob.isPresent()) {
                return mob.get().getDescription();
            }
        }
        if (subject.playerName().isPresent()) {
            return subject.label();
        }
        return subject.block().isPresent() ? subject.blockLabel() : subject.label();
    }

    private static String container(Item item) {
        if (item == Items.SPLASH_POTION) {
            return "splash";
        }
        if (item == Items.LINGERING_POTION) {
            return "lingering";
        }
        if (item == Items.TIPPED_ARROW) {
            return "arrow";
        }
        if (item instanceof FlagonItem) {
            return "flagon";
        }
        return "potion";
    }
}
