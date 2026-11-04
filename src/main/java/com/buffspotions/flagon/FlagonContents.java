package com.buffspotions.flagon;

import com.buffspotions.potion.EssenceNames;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public record FlagonContents(ItemStack dose, int doses) {
    public static final int MAX_DOSES = 9;
    public static final int BUCKET_DOSES = 3;
    public static final FlagonContents EMPTY = new FlagonContents(ItemStack.EMPTY, 0);
    public static final Codec<FlagonContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("dose", ItemStack.EMPTY).forGetter(FlagonContents::dose),
            Codec.intRange(0, MAX_DOSES).fieldOf("doses").forGetter(FlagonContents::doses)
    ).apply(instance, FlagonContents::new));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FlagonContents> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    FlagonContents::dose,
                    ByteBufCodecs.VAR_INT,
                    FlagonContents::doses,
                    FlagonContents::new
            );

    public FlagonContents {
        if (dose.isEmpty() || doses <= 0) {
            dose = ItemStack.EMPTY;
            doses = 0;
        } else {
            dose = dose.copyWithCount(1);
            doses = Math.min(MAX_DOSES, doses);
        }
    }

    public static FlagonContents of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLAGON_CONTENTS.get(), EMPTY);
    }

    public boolean sameFill(FlagonContents other) {
        if (doses != other.doses) {
            return false;
        }
        if (doses == 0) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(dose, other.dose);
    }

    public boolean isEmpty() {
        return doses <= 0 || dose.isEmpty();
    }

    public int space() {
        return MAX_DOSES - doses;
    }

    public static boolean isDoseItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isMilkDose(stack) || stack.is(Items.HONEY_BOTTLE)) {
            return true;
        }
        return stack.getItem() instanceof PotionItem && !(stack.getItem() instanceof ThrowablePotionItem);
    }

    public static boolean isMilkDose(ItemStack stack) {
        if (!(stack.getItem() instanceof PotionItem) || stack.getItem() instanceof ThrowablePotionItem) {
            return false;
        }
        PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
        return potion != null && potion.is(ModPotions.MILK);
    }

    public boolean matches(ItemStack stack) {
        if (isEmpty()) {
            return false;
        }
        if (isMilk() && isMilkDose(stack)) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(dose, stack);
    }

    public boolean canInsertDose(ItemStack stack) {
        if (!isDoseItem(stack) || doses >= MAX_DOSES) {
            return false;
        }
        return isEmpty() || matches(stack);
    }

    public FlagonContents insertDose(ItemStack stack) {
        if (!canInsertDose(stack)) {
            return this;
        }
        ItemStack template = isEmpty() ? storedTemplate(stack) : dose;
        return new FlagonContents(template, doses + 1);
    }

    public boolean canAccept(ItemStack template, int amount) {
        if (template.isEmpty() || amount <= 0 || amount > space()) {
            return false;
        }
        return isEmpty() || matches(template);
    }

    public FlagonContents insert(ItemStack template, int amount) {
        if (!canAccept(template, amount)) {
            return this;
        }
        ItemStack stored = isEmpty() ? template : dose;
        return new FlagonContents(stored, doses + amount);
    }

    public FlagonContents removeDose() {
        if (doses <= 1) {
            return EMPTY;
        }
        return new FlagonContents(dose, doses - 1);
    }

    public FlagonContents remove(int amount) {
        if (amount <= 0 || amount > doses) {
            return this;
        }
        if (amount == doses) {
            return EMPTY;
        }
        return new FlagonContents(dose, doses - amount);
    }

    public ItemStack bottledDose() {
        if (isMilk()) {
            return milkTemplate();
        }
        return dose.copy();
    }

    public boolean isMilk() {
        return isMilkDose(dose);
    }

    public boolean isHoney() {
        return dose.is(Items.HONEY_BOTTLE);
    }

    public boolean isWater() {
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        return potion != null && potion.is(Potions.WATER);
    }

    public boolean canMilk() {
        return isEmpty() || (isMilk() && doses < MAX_DOSES);
    }

    public FlagonContents fillWithMilk() {
        return new FlagonContents(milkTemplate(), MAX_DOSES);
    }

    public boolean canFillFromWater() {
        return isEmpty() || (isWater() && doses < MAX_DOSES);
    }

    public FlagonContents fillWithWater() {
        return new FlagonContents(waterTemplate(), MAX_DOSES);
    }

    public static ItemStack waterTemplate() {
        return PotionContents.createItemStack(Items.POTION, Potions.WATER);
    }

    public static ItemStack milkTemplate() {
        return PotionContents.createItemStack(Items.POTION, ModPotions.MILK);
    }

    private static ItemStack storedTemplate(ItemStack stack) {
        return isMilkDose(stack) ? milkTemplate() : stack;
    }

    public ItemStack automationResult(ItemStack input) {
        if (canInsertDose(input)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (input.is(Items.GLASS_BOTTLE) && doses > 0) {
            return bottledDose();
        }
        return ItemStack.EMPTY;
    }

    public FlagonContents applyAutomation(ItemStack input) {
        if (canInsertDose(input)) {
            return insertDose(input);
        }
        if (input.is(Items.GLASS_BOTTLE) && doses > 0) {
            return removeDose();
        }
        return this;
    }

    public int fluidColor() {
        if (isEmpty()) {
            return 0;
        }
        if (isMilk()) {
            return 0xF4F4F4;
        }
        if (isHoney()) {
            return 0xF3A11A;
        }
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            return potion.getColor();
        }
        return 0x3F76E4;
    }

    public Component itemName() {
        if (isEmpty()) {
            return Component.translatable("item.buffs_potions.flagon");
        }
        if (isMilk()) {
            return Component.translatable("item.buffs_potions.flagon.of_milk");
        }
        if (isHoney()) {
            return Component.translatable("item.buffs_potions.flagon.of_honey");
        }
        if (isWater()) {
            return mundane(Component.translatable("item.buffs_potions.flagon.of_water"));
        }
        Component remedy = Remedies.flagonName(dose);
        if (remedy != null) {
            return remedy;
        }
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion == null) {
            return Component.translatable("item.buffs_potions.flagon");
        }
        Component named = EssenceNames.named(dose, "flagon");
        if (named != null) {
            return mundane(named);
        }
        return mundane(potion.getName("item.buffs_potions.flagon.effect."));
    }

    private Component mundane(Component name) {
        if (!MundaneBrewing.isMundane(dose)) {
            return name;
        }
        return Component.translatable("item.buffs_potions.mundane", name);
    }
}
