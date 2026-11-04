package com.buffspotions.still;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.RemedyRecipe;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * The brewing still's whole recipe list: mundane potions, remedies, suspicious brew,
 * and essence of whatever is added to latent eyedentification. A flower pot handles
 * the fifth brew, turning suspicious brew into a mundane potion.
 */
public final class StillBrewing {
    private static final RemedyRecipe REMEDIES = new RemedyRecipe();
    private static final SuspiciousBrew.Recipe SUSPICIOUS = new SuspiciousBrew.Recipe();

    private StillBrewing() {
    }

    public static boolean isIngredient(ItemStack stack) {
        return !stack.isEmpty();
    }

    public static boolean isBottle(ItemStack stack) {
        if (stack.is(Items.POTION) && (isWater(stack) || isPlainMundane(stack))) {
            return true;
        }
        if (FlagonContents.isMilkDose(stack)) {
            return true;
        }
        if (MundaneBrewing.isMundane(stack) || Remedies.isRemedy(stack)) {
            return true;
        }
        if (stack.is(Items.SPLASH_POTION) && isMilk(stack)) {
            return true;
        }
        if (stack.is(Items.POTION) && SuspiciousBrew.is(stack)) {
            return true;
        }
        return isLatent(stack);
    }

    public static boolean hasMix(PotionBrewing brewing, ItemStack bottle, ItemStack ingredient) {
        return !mix(brewing, ingredient, bottle).isEmpty();
    }

    public static ItemStack mix(PotionBrewing brewing, ItemStack ingredient, ItemStack bottle) {
        if (ingredient.isEmpty() || bottle.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = brew(brewing, ingredient, bottle);
        if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, bottle)) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    private static ItemStack brew(PotionBrewing brewing, ItemStack ingredient, ItemStack bottle) {
        if (MundaneBrewing.isMundane(bottle)) {
            return MundaneBrewing.mix(brewing, ingredient, bottle);
        }
        if (isMilk(bottle) && bottle.is(Items.POTION) && ingredient.is(Items.GUNPOWDER)) {
            return copyContents(bottle, Items.SPLASH_POTION);
        }
        if (isMilk(bottle) && bottle.is(Items.SPLASH_POTION) && ingredient.is(Items.DRAGON_BREATH)) {
            return copyContents(bottle, Items.LINGERING_POTION);
        }
        ItemStack remedy = REMEDIES.getOutput(bottle, ingredient);
        if (!remedy.isEmpty()) {
            return remedy;
        }
        ItemStack suspicious = SUSPICIOUS.getOutput(bottle, ingredient);
        if (!suspicious.isEmpty()) {
            return suspicious;
        }
        if (bottle.is(Items.POTION) && isWater(bottle) && ingredient.is(Items.REDSTONE)) {
            return PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
        }
        ItemStack mundane = MundaneBrewing.mix(brewing, ingredient, bottle);
        if (mundane != null) {
            return mundane;
        }
        return EyedBrewing.essenceOf(bottle, ingredient);
    }

    private static ItemStack copyContents(ItemStack bottle, net.minecraft.world.item.Item item) {
        ItemStack result = new ItemStack(item);
        PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            result.set(DataComponents.POTION_CONTENTS, contents);
        }
        return result;
    }

    private static boolean isMilk(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK);
    }

    private static boolean isWater(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    private static boolean isPlainMundane(ItemStack stack) {
        return MundaneBrewing.isPlain(stack);
    }

    private static boolean isLatent(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.LATENT_EYEDENTIFICATION);
    }
}
