package com.buffspotions.stack;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.potion.ActiveBrewing;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.recipebook.InversionUnlock;
import com.buffspotions.recipebook.RecipeMystery;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stand.StandBrewRecipe;
import com.buffspotions.stand.StandSubjects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Brewing-stack book. The same potions as the stand book, shown as flagons.
 * Splash and lingering are omitted. The utility tab adds the second thicken, the second extend,
 * and the combined thicken-and-extend.
 */
public final class StackRecipeCatalog {
    public static final String THICKEN_GROUP = "thicken";
    public static final String EXTEND_GROUP = "extend";
    public static final String TORCHFLOWER_GROUP = "torchflower";
    public static final String PITCHER_GROUP = "pitcher";
    public static final String CLARIFY_GROUP = "clarify";

    public enum Kind {
        BASE,
        INVERT,
        PARAMETER,
        DISGUISE,
        AWKWARD,
        MUNDANE,
        THICKEN,
        EXTEND,
        TORCHFLOWER,
        PITCHER,
        CLARIFY
    }

    public record Entry(
            ResourceKey<Recipe<?>> id, Kind kind, @Nullable Item unlockItem, @Nullable Item subject, Set<String> requiresKnown
    ) implements InversionUnlock.Gated {
    }

    private static final StackRecipeCatalog EMPTY = new StackRecipeCatalog(List.of(), Set.of());
    private static StackRecipeCatalog current = EMPTY;

    private final List<Entry> entries;
    private final Set<ResourceKey<Recipe<?>>> ids;
    private final Map<Item, List<Entry>> byItem = new HashMap<>();
    private final Map<Item, List<Entry>> bySubject = new HashMap<>();

    private StackRecipeCatalog(List<Entry> entries, Set<ResourceKey<Recipe<?>>> ids) {
        this.entries = entries;
        this.ids = ids;
        for (Entry entry : entries) {
            if (entry.unlockItem != null) {
                byItem.computeIfAbsent(entry.unlockItem, key -> new ArrayList<>()).add(entry);
            }
            if (entry.subject != null) {
                bySubject.computeIfAbsent(entry.subject, key -> new ArrayList<>()).add(entry);
            }
        }
    }

    public static StackRecipeCatalog get() {
        return current;
    }

    public static boolean owns(StandBrewRecipe recipe) {
        if (recipe == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(recipe.recipeBookCategory());
        return id != null && id.getPath().startsWith("stack_");
    }

    public static RecipeMap install(RecipeMap existing, FeatureFlagSet flags) {
        PotionBrewing brewing = ActiveBrewing.get();
        if (brewing == null) {
            brewing = PotionBrewing.bootstrap(flags);
        }
        Built built = build(brewing, flags);
        current = built.catalog;
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (RecipeHolder<?> holder : existing.values()) {
            if (holder.value() instanceof StandBrewRecipe brew && owns(brew)) {
                continue;
            }
            holders.add(holder);
        }
        holders.addAll(built.holders);
        return RecipeMap.create(holders);
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean contains(ResourceKey<Recipe<?>> id) {
        return ids.contains(id);
    }

    public List<Entry> forItem(Item item) {
        return byItem.getOrDefault(item, List.of());
    }

    public List<Entry> forSubject(Item item) {
        return bySubject.getOrDefault(item, List.of());
    }

    public boolean shouldUnlock(Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld) {
        return shouldUnlock(entry, held, crafted, everHeld, Set.of());
    }

    public boolean shouldUnlock(Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld, Set<String> known) {
        if (!InversionUnlock.allowed(entry.requiresKnown, known)) {
            return false;
        }
        if (entry.subject != null && !everHeld.contains(entry.subject)) {
            return false;
        }
        return entry.unlockItem != null && (held.contains(entry.unlockItem) || crafted.contains(entry.unlockItem));
    }

    private record Built(StackRecipeCatalog catalog, List<RecipeHolder<?>> holders) {
    }

    private record BaseBrew(Item item, ItemStack result) {
    }

    private static Built build(PotionBrewing brewing, FeatureFlagSet flags) {
        List<Entry> entries = new ArrayList<>();
        List<RecipeHolder<?>> holders = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        ItemStack glowstone = new ItemStack(Items.GLOWSTONE_DUST);
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack torchflower = new ItemStack(Items.TORCHFLOWER);
        ItemStack pitcher = new ItemStack(Items.PITCHER_PLANT);
        RecipeBookCategory helpful = ModRecipes.STACK_HELPFUL.get();
        RecipeBookCategory harmful = ModRecipes.STACK_HARMFUL.get();
        RecipeBookCategory parameter = ModRecipes.STACK_PARAMETER.get();
        RecipeBookCategory utility = ModRecipes.STACK_UTILITY.get();

        ActiveBrewing.using(brewing, () -> {
            addIf(entries, holders, paths, "stack/awkward", Kind.AWKWARD, Items.NETHER_WART, null, utility, "", true, false,
                    brewed(brewing, water, new ItemStack(Items.NETHER_WART)), water, new ItemStack(Items.NETHER_WART));
            addIf(entries, holders, paths, "stack/mundane/redstone", Kind.MUNDANE, Items.REDSTONE, null, utility, "", true, false,
                    brewed(brewing, water, redstone), water, redstone);

            List<BaseBrew> bases = new ArrayList<>();
            Map<ResourceKey<Potion>, Integer> byPotion = new HashMap<>();
            BuiltInRegistries.ITEM.forEach(item -> {
                if (!item.isEnabled(flags) || item == Items.AIR || MundaneBrewing.distilled(brewing, item).isEmpty()) {
                    return;
                }
                ItemStack result = brewed(brewing, awkward, new ItemStack(item));
                if (result.isEmpty() || parameterized(result)) {
                    return;
                }
                ResourceKey<Potion> potion = potionKey(result);
                if (potion == null) {
                    return;
                }
                Integer existing = byPotion.get(potion);
                if (existing == null) {
                    byPotion.put(potion, bases.size());
                    bases.add(new BaseBrew(item, result));
                } else if (item == Items.BREEZE_ROD && bases.get(existing).item == Items.WIND_CHARGE) {
                    bases.set(existing, new BaseBrew(item, result));
                }
            });
            for (BaseBrew brew : bases) {
                String path = itemPath(brew.item);
                RecipeBookCategory category = categoryFor(brew.result, helpful, harmful);
                add(entries, holders, paths, "stack/base/" + path, Kind.BASE, brew.item, null, category, "", true, false,
                        awkward, new ItemStack(brew.item), brew.result);
                addModifiers(entries, holders, paths, brewing, brew.result, path, brew.item, glowstone, redstone, torchflower, pitcher, utility);
                ItemStack inverted = brewed(brewing, brew.result, eye);
                if (inverted.isEmpty()) {
                    continue;
                }
                add(entries, holders, paths, "stack/invert/" + path, Kind.INVERT, brew.item, null,
                        categoryFor(inverted, helpful, harmful), "", true, false,
                        brew.result, eye, inverted);
                addModifiers(entries, holders, paths, brewing, inverted, "invert/" + path, brew.item, glowstone, redstone, torchflower, pitcher, utility, RecipeMystery.effectIds(brew.result));
            }

            ItemStack stoneEssence = EyedBrewing.essenceOf(EyedBrewing.latentBottle(), new ItemStack(Items.STONE));
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.RESIN_CLUMP, StandSubjects.Kind.BLOCK, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.SCAFFOLDING, StandSubjects.Kind.BLOCK, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.COOKIE, StandSubjects.Kind.ITEM, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.ECHO_SHARD, StandSubjects.Kind.MOB, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.LODESTONE, StandSubjects.Kind.SEEKING, parameter, eye);

            ItemStack sample = playerEssenceSample();
            ItemStack disguise = brewed(brewing, sample, new ItemStack(Items.CARVED_PUMPKIN));
            if (!disguise.isEmpty()) {
                ItemStack shown = disguise.copy();
                shown.remove(ModDataComponents.ESSENCE.get());
                add(entries, holders, paths, "stack/param/disguise", Kind.DISGUISE, Items.CARVED_PUMPKIN, null, parameter, "", true, true,
                        sample, new ItemStack(Items.CARVED_PUMPKIN), shown);
            }
        });

        Set<ResourceKey<Recipe<?>>> ids = new HashSet<>();
        for (Entry entry : entries) {
            ids.add(entry.id);
        }
        return new Built(new StackRecipeCatalog(entries, ids), holders);
    }

    private static void addFamily(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            PotionBrewing brewing,
            ItemStack essence,
            Item catalyst,
            StandSubjects.Kind subjects,
            RecipeBookCategory category,
            ItemStack eye
    ) {
        ItemStack catalystStack = new ItemStack(catalyst);
        ItemStack result = brewed(brewing, essence, catalystStack);
        if (result.isEmpty() || !parameterized(result)) {
            return;
        }
        String potion = potionPath(result);
        if (potion == null) {
            return;
        }
        add(entries, holders, paths, "stack/param/" + potion, Kind.PARAMETER, catalyst, null, category, "param_" + potion, false, false,
                subjects, essence, catalystStack, result);
        ItemStack inverted = brewed(brewing, result, eye);
        if (inverted.isEmpty() || !parameterized(inverted)) {
            return;
        }
        String invertedPotion = potionPath(inverted);
        if (invertedPotion == null) {
            return;
        }
        add(entries, holders, paths, "stack/param/" + invertedPotion, Kind.PARAMETER, catalyst, null, category,
                "param_" + invertedPotion, false, false, subjects, result, eye, inverted);
    }

    private static void addModifiers(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            PotionBrewing brewing,
            ItemStack base,
            String suffix,
            Item unlockItem,
            ItemStack glowstone,
            ItemStack redstone,
            ItemStack torchflower,
            ItemStack pitcher,
            RecipeBookCategory utility
    ) {
        addModifiers(entries, holders, paths, brewing, base, suffix, unlockItem, glowstone, redstone, torchflower, pitcher, utility, Set.of());
    }

    private static void addModifiers(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            PotionBrewing brewing,
            ItemStack base,
            String suffix,
            Item unlockItem,
            ItemStack glowstone,
            ItemStack redstone,
            ItemStack torchflower,
            ItemStack pitcher,
            RecipeBookCategory utility,
            Set<String> requiresKnown
    ) {
        ItemStack thick = brewed(brewing, base, glowstone);
        if (!thick.isEmpty()) {
            add(entries, holders, paths, "stack/thicken/" + suffix, Kind.THICKEN, unlockItem, null, utility, THICKEN_GROUP, false, false,
                    base, glowstone, thick, requiresKnown);
        }
        ItemStack extended = brewed(brewing, base, redstone);
        if (!extended.isEmpty()) {
            add(entries, holders, paths, "stack/extend/" + suffix, Kind.EXTEND, unlockItem, null, utility, EXTEND_GROUP, false, false,
                    base, redstone, extended, requiresKnown);
        }
        ItemStack levelThree = thick.isEmpty() ? ItemStack.EMPTY : brewed(brewing, thick, torchflower);
        if (!levelThree.isEmpty()) {
            add(entries, holders, paths, "stack/torchflower/" + suffix, Kind.TORCHFLOWER, unlockItem, null, utility, TORCHFLOWER_GROUP, false, false,
                    thick, torchflower, levelThree, requiresKnown);
        }
        ItemStack doubleExtended = extended.isEmpty() ? ItemStack.EMPTY : brewed(brewing, extended, pitcher);
        if (!doubleExtended.isEmpty()) {
            add(entries, holders, paths, "stack/pitcher/" + suffix, Kind.PITCHER, unlockItem, null, utility, PITCHER_GROUP, false, false,
                    extended, pitcher, doubleExtended, requiresKnown);
        }
        ItemStack clarifiedTorch = extended.isEmpty() ? ItemStack.EMPTY : brewed(brewing, extended, torchflower);
        ItemStack clarifiedPitch = thick.isEmpty() ? ItemStack.EMPTY : brewed(brewing, thick, pitcher);
        if (!clarifiedTorch.isEmpty()) {
            add(entries, holders, paths, "stack/clarify/torchflower/" + suffix, Kind.CLARIFY, unlockItem, null, utility, CLARIFY_GROUP, false, false,
                    extended, torchflower, clarifiedTorch, requiresKnown);
        }
        if (!clarifiedPitch.isEmpty()) {
            add(entries, holders, paths, "stack/clarify/pitcher/" + suffix, Kind.CLARIFY, unlockItem, null, utility, CLARIFY_GROUP, false, false,
                    thick, pitcher, clarifiedPitch, requiresKnown);
        }
    }

    private static void addIf(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            String path,
            Kind kind,
            Item unlockItem,
            @Nullable Item subject,
            RecipeBookCategory category,
            String group,
            boolean notify,
            boolean anyPlayer,
            ItemStack result,
            ItemStack bottle,
            ItemStack ingredient
    ) {
        if (result.isEmpty()) {
            return;
        }
        add(entries, holders, paths, path, kind, unlockItem, subject, category, group, notify, anyPlayer, bottle, ingredient, result);
    }

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            @Nullable Item subject,
            RecipeBookCategory category,
            String group,
            boolean notify,
            boolean anyPlayer,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result
    ) {
        add(entries, holders, paths, path, kind, unlockItem, subject, category, group, notify, anyPlayer, StandSubjects.Kind.NONE, bottle, ingredient, result, Set.of());
    }

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            @Nullable Item subject,
            RecipeBookCategory category,
            String group,
            boolean notify,
            boolean anyPlayer,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result,
            Set<String> inherited
    ) {
        add(entries, holders, paths, path, kind, unlockItem, subject, category, group, notify, anyPlayer, StandSubjects.Kind.NONE, bottle, ingredient, result, inherited);
    }

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            @Nullable Item subject,
            RecipeBookCategory category,
            String group,
            boolean notify,
            boolean anyPlayer,
            StandSubjects.Kind subjects,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result
    ) {
        add(entries, holders, paths, path, kind, unlockItem, subject, category, group, notify, anyPlayer, subjects, bottle, ingredient, result, Set.of());
    }

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            @Nullable Item subject,
            RecipeBookCategory category,
            String group,
            boolean notify,
            boolean anyPlayer,
            StandSubjects.Kind subjects,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result,
            Set<String> inherited
    ) {
        if (!paths.add(path) || bottle.isEmpty() || ingredient.isEmpty() || result.isEmpty()) {
            return;
        }
        ResourceKey<Recipe<?>> id = ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, path)
        );
        StandBrewRecipe recipe = new StandBrewRecipe(
                matching(StackFlagons.filled(bottle)),
                matching(ingredient),
                ItemStackTemplate.fromNonEmptyStack(StackFlagons.filled(result)),
                category,
                group,
                notify,
                anyPlayer,
                subjects
        );
        Set<String> requires = inherited.isEmpty() ? InversionUnlock.of(ingredient, bottle) : Set.copyOf(inherited);
        entries.add(new Entry(id, kind, unlockItem, subject, requires));
        holders.add(new RecipeHolder<>(id, recipe));
    }

    private static ItemStack brewed(PotionBrewing brewing, ItemStack dose, ItemStack ingredient) {
        ItemStack result = StackBrewing.mix(brewing, dose, ingredient);
        if (result.isEmpty() || ItemStack.isSameItemSameComponents(result, dose)) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    private static ItemStack playerEssenceSample() {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.of(ModPotions.ESSENCE),
                Optional.of(EssenceSubject.PLAYER_COLOR),
                List.of(),
                Optional.empty()
        ));
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Player"));
        return stack;
    }

    private static boolean parameterized(ItemStack stack) {
        for (MobEffectInstance effect : stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
            if (effect.is(ModEffects.BLOCK_SENSE)
                    || effect.is(ModEffects.ITEM_EATER)
                    || effect.is(ModEffects.INSTANT_INCARNATION)
                    || effect.is(ModEffects.INSTANT_FORMATION)
                    || effect.is(ModEffects.ITEM_VOMITING)
                    || effect.is(ModEffects.SEEKING)
                    || effect.is(ModEffects.DISGUISE)) {
                return true;
            }
        }
        return false;
    }

    private static RecipeBookCategory categoryFor(ItemStack result, RecipeBookCategory helpful, RecipeBookCategory harmful) {
        PotionContents contents = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.is(Potions.TURTLE_MASTER)) {
            return helpful;
        }
        boolean bad = false;
        boolean good = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                good = true;
            } else if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                    || effect.is(ModEffects.INSTANT_CONFUSION)
                    || effect.is(ModEffects.INSTANT_DESCENT)) {
                bad = true;
            }
        }
        return bad && !good ? harmful : helpful;
    }

    private static @Nullable ResourceKey<Potion> potionKey(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .flatMap(Holder::unwrapKey)
                .orElse(null);
    }

    private static @Nullable String potionPath(ItemStack stack) {
        ResourceKey<Potion> key = potionKey(stack);
        return key == null ? null : key.identifier().getPath();
    }

    private static Ingredient matching(ItemStack stack) {
        if (stack.getItem() instanceof PotionItem || com.buffspotions.registry.ModItems.fitsBrewingStack(stack)) {
            return DataComponentIngredient.of(false, stack.copyWithCount(1));
        }
        return Ingredient.of(stack.getItem());
    }

    private static String itemPath(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace() + "/" + id.getPath();
    }
}
