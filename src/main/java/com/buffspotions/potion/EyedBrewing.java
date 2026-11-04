package com.buffspotions.potion;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.effect.MobDrops;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

public final class EyedBrewing {
    public static final int LATENT_COLOR = 0x3DDC9A;

    private EyedBrewing() {
    }

    public static void addRecipes(PotionBrewing.Builder brewing) {
        brewing.addRecipe(new LatentRecipe());
        brewing.addRecipe(new BlockEssenceRecipe(Items.RESIN_CLUMP, ModPotions.BLOCK_PERCEPTION));
        brewing.addRecipe(new BlockEssenceRecipe(Items.SCAFFOLDING, ModPotions.FORMATION));
        brewing.addRecipe(new ExtendRecipe(ModPotions.BLOCK_PERCEPTION, Items.REDSTONE, ModPotions.LONG_BLOCK_PERCEPTION));
        brewing.addRecipe(new ExtendRecipe(ModPotions.BLOCK_PERCEPTION, Items.GLOWSTONE_DUST, ModPotions.STRONG_BLOCK_PERCEPTION));
        brewing.addRecipe(new ItemEssenceRecipe(Items.COOKIE, ModPotions.ITEM_EATING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.ITEM_EATING, Items.REDSTONE, ModPotions.LONG_ITEM_EATING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.ITEM_EATING, Items.GLOWSTONE_DUST, ModPotions.STRONG_ITEM_EATING));
        brewing.addRecipe(new ItemEssenceRecipe(Items.ECHO_SHARD, ModPotions.INCARNATION));
        brewing.addRecipe(new ExtendRecipe(ModPotions.ITEM_EATING, Items.FERMENTED_SPIDER_EYE, ModPotions.ITEM_VOMITING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.LONG_ITEM_EATING, Items.FERMENTED_SPIDER_EYE, ModPotions.ITEM_VOMITING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.STRONG_ITEM_EATING, Items.FERMENTED_SPIDER_EYE, ModPotions.ITEM_VOMITING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.ITEM_VOMITING, Items.REDSTONE, ModPotions.LONG_ITEM_VOMITING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.ITEM_VOMITING, Items.GLOWSTONE_DUST, ModPotions.STRONG_ITEM_VOMITING));
        brewing.addRecipe(new AnyEssenceRecipe(Items.LODESTONE, ModPotions.SEEKING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.SEEKING, Items.REDSTONE, ModPotions.LONG_SEEKING));
        brewing.addRecipe(new ExtendRecipe(ModPotions.SEEKING, Items.GLOWSTONE_DUST, ModPotions.STRONG_SEEKING));
        brewing.addRecipe(new PlayerEssenceRecipe(Items.CARVED_PUMPKIN, ModPotions.DISGUISE));
        brewing.addRecipe(new ExtendRecipe(ModPotions.DISGUISE, Items.REDSTONE, ModPotions.LONG_DISGUISE));
        brewing.addRecipe(new PreserveContainerRecipe());
    }

    public static ItemStack latentBottle() {
        return brew(Items.POTION, ModPotions.LATENT_EYEDENTIFICATION, null, LATENT_COLOR);
    }

    public static boolean isLatent(ItemStack stack) {
        return isPotion(stack, ModPotions.LATENT_EYEDENTIFICATION);
    }

    /** A latent bottle the still has turned into the essence of an item, player, or mob. */
    public static boolean isEssence(ItemStack stack) {
        EssenceSubject subject = subject(stack);
        return isPotion(stack, ModPotions.ESSENCE)
                && subject != null
                && (subject.item().isPresent() || subject.playerName().isPresent() || subject.entity().isPresent());
    }

    public static boolean isPlayerEssence(ItemStack stack) {
        EssenceSubject subject = subject(stack);
        return isPotion(stack, ModPotions.ESSENCE) && subject != null && subject.playerName().isPresent();
    }

    public static boolean isNetheriteBlockEssence(ItemStack stack) {
        if (stack == null || !stack.is(Items.POTION)) {
            return false;
        }
        EssenceSubject subject = subject(stack);
        return isPotion(stack, ModPotions.ESSENCE)
                && subject != null
                && subject.item().filter(item -> item.value() == Items.NETHERITE_BLOCK).isPresent();
    }

    private static EssenceSubject subject(ItemStack stack) {
        return stack == null ? null : stack.get(ModDataComponents.ESSENCE.get());
    }

    /** Any ingredient in a latent bottle becomes an essence of that ingredient. The still is the only brewer. */
    public static ItemStack essenceOf(ItemStack input, ItemStack ingredient) {
        if (!input.is(Items.POTION) || !isPotion(input, ModPotions.LATENT_EYEDENTIFICATION) || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EssenceSubject subject = subjectOf(ingredient);
        return brew(input.getItem(), ModPotions.ESSENCE, subject, subject.liquidColor());
    }

    public static EssenceSubject subjectOf(ItemStack ingredient) {
        if (ingredient.is(Items.NAME_TAG)) {
            Component custom = ingredient.get(DataComponents.CUSTOM_NAME);
            if (custom != null && !custom.getString().isBlank()) {
                return EssenceSubject.ofPlayer(custom.getString());
            }
        }
        return EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(ingredient.getItem()));
    }

    private static boolean isPotion(ItemStack stack, Holder<Potion> potion) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(holder -> holder.unwrapKey().equals(potion.unwrapKey()))
                .orElse(false);
    }

    private static ItemStack brew(Item item, Holder<Potion> potion, EssenceSubject subject, Integer color) {
        ItemStack result = new ItemStack(item);
        result.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.of(potion),
                color == null ? Optional.empty() : Optional.of(color),
                List.of(),
                Optional.empty()
        ));
        if (subject != null) {
            result.set(ModDataComponents.ESSENCE.get(), subject);
        }
        return result;
    }

    private static final class LatentRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.is(Items.POTION) && isPotion(input, Potions.AWKWARD);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.ENDER_EYE);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), ModPotions.LATENT_EYEDENTIFICATION, null, LATENT_COLOR);
        }
    }

    private static final class BlockEssenceRecipe implements IBrewingRecipe {
        private final Item ingredient;
        private final Holder<Potion> potion;

        private BlockEssenceRecipe(Item ingredient, Holder<Potion> potion) {
            this.ingredient = ingredient;
            this.potion = potion;
        }

        @Override
        public boolean isInput(ItemStack input) {
            EssenceSubject subject = input.get(ModDataComponents.ESSENCE.get());
            return isPotion(input, ModPotions.ESSENCE) && subject != null && subject.block().isPresent();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), this.potion, input.get(ModDataComponents.ESSENCE.get()), null);
        }
    }

    private static final class ItemEssenceRecipe implements IBrewingRecipe {
        private final Item ingredient;
        private final Holder<Potion> potion;

        private ItemEssenceRecipe(Item ingredient, Holder<Potion> potion) {
            this.ingredient = ingredient;
            this.potion = potion;
        }

        @Override
        public boolean isInput(ItemStack input) {
            EssenceSubject subject = input.get(ModDataComponents.ESSENCE.get());
            return isPotion(input, ModPotions.ESSENCE) && subject != null && subject.item().isPresent();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), this.potion, input.get(ModDataComponents.ESSENCE.get()), null);
        }
    }

    private static final class PlayerEssenceRecipe implements IBrewingRecipe {
        private final Item ingredient;
        private final Holder<Potion> potion;

        private PlayerEssenceRecipe(Item ingredient, Holder<Potion> potion) {
            this.ingredient = ingredient;
            this.potion = potion;
        }

        @Override
        public boolean isInput(ItemStack input) {
            EssenceSubject subject = input.get(ModDataComponents.ESSENCE.get());
            return isPotion(input, ModPotions.ESSENCE) && subject != null && subject.playerName().isPresent();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), this.potion, input.get(ModDataComponents.ESSENCE.get()), null);
        }
    }

    private static final class AnyEssenceRecipe implements IBrewingRecipe {
        private final Item ingredient;
        private final Holder<Potion> potion;

        private AnyEssenceRecipe(Item ingredient, Holder<Potion> potion) {
            this.ingredient = ingredient;
            this.potion = potion;
        }

        @Override
        public boolean isInput(ItemStack input) {
            EssenceSubject subject = input.get(ModDataComponents.ESSENCE.get());
            if (!isPotion(input, ModPotions.ESSENCE) || subject == null) {
                return false;
            }
            if (subject.playerName().isPresent()) {
                return true;
            }
            if (subject.item().isEmpty()) {
                return false;
            }
            return subject.block().isPresent() || MobDrops.type(subject.item().get()).isPresent();
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), this.potion, input.get(ModDataComponents.ESSENCE.get()), null);
        }
    }

    private static final class ExtendRecipe implements IBrewingRecipe {
        private final Holder<Potion> from;
        private final Item ingredient;
        private final Holder<Potion> to;

        private ExtendRecipe(Holder<Potion> from, Item ingredient, Holder<Potion> to) {
            this.from = from;
            this.ingredient = ingredient;
            this.to = to;
        }

        @Override
        public boolean isInput(ItemStack input) {
            return isPotion(input, this.from);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return brew(input.getItem(), this.to, input.get(ModDataComponents.ESSENCE.get()), null);
        }
    }

    /**
     * Gunpowder and dragon's breath keep the essence subject. Vanilla container mixes would drop it.
     * A latent bottle is not splashed here; the still turns that ingredient into an essence instead.
     */
    private static final class PreserveContainerRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.is(Items.POTION) || input.is(Items.SPLASH_POTION);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.GUNPOWDER) || ingredient.is(Items.DRAGON_BREATH);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (isPotion(input, ModPotions.LATENT_EYEDENTIFICATION) || !input.has(ModDataComponents.ESSENCE.get())) {
                return ItemStack.EMPTY;
            }
            Item outputItem = null;
            if (input.is(Items.POTION) && ingredient.is(Items.GUNPOWDER)) {
                outputItem = Items.SPLASH_POTION;
            } else if (input.is(Items.SPLASH_POTION) && ingredient.is(Items.DRAGON_BREATH)
                    && !Incarnation.isIncarnation(input)
                    && !Formation.isFormation(input)) {
                outputItem = Items.LINGERING_POTION;
            }
            if (outputItem == null) {
                return ItemStack.EMPTY;
            }
            PotionContents contents = input.get(DataComponents.POTION_CONTENTS);
            if (contents == null || contents.potion().isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack result = new ItemStack(outputItem);
            result.set(DataComponents.POTION_CONTENTS, contents);
            result.set(ModDataComponents.ESSENCE.get(), input.get(ModDataComponents.ESSENCE.get()));
            return result;
        }
    }
}
