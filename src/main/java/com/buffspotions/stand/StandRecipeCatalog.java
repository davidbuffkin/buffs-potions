package com.buffspotions.stand;

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
import com.buffspotions.stack.StackRecipeCatalog;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Brewing-stand book. Base potions match the still book's healing and harming tabs.
 * Inversions are only the fermented-spider-eye mix from that base potion.
 * Parameterized potions are one template per potion. The book fills in subjects the player has held.
 */
public final class StandRecipeCatalog {
    public static final String THICKEN_GROUP = "thicken";
    public static final String EXTEND_GROUP = "extend";
    public static final String SPLASH_GROUP = "splash";
    public static final String LINGER_GROUP = "linger";

    public enum Kind {
        BASE,
        INVERT,
        PARAMETER,
        DISGUISE,
        AWKWARD,
        MUNDANE,
        THICKEN,
        EXTEND,
        SPLASH,
        LINGER
    }

    public record Entry(
            ResourceKey<Recipe<?>> id, Kind kind, @Nullable Item unlockItem, @Nullable Item subject, Set<String> requiresKnown
    ) implements InversionUnlock.Gated {
    }

    private static final StandRecipeCatalog EMPTY = new StandRecipeCatalog(List.of(), Set.of());
    private static StandRecipeCatalog current = EMPTY;

    private final List<Entry> entries;
    private final Set<ResourceKey<Recipe<?>>> ids;
    private final Map<Item, List<Entry>> byItem = new HashMap<>();
    private final Map<Item, List<Entry>> bySubject = new HashMap<>();
    private final List<Entry> lingering = new ArrayList<>();

    private StandRecipeCatalog(List<Entry> entries, Set<ResourceKey<Recipe<?>>> ids) {
        this.entries = entries;
        this.ids = ids;
        for (Entry entry : entries) {
            if (entry.unlockItem != null) {
                byItem.computeIfAbsent(entry.unlockItem, key -> new ArrayList<>()).add(entry);
            }
            if (entry.subject != null) {
                bySubject.computeIfAbsent(entry.subject, key -> new ArrayList<>()).add(entry);
            }
            if (entry.kind == Kind.LINGER) {
                lingering.add(entry);
            }
        }
    }

    public static StandRecipeCatalog get() {
        return current;
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
            if (holder.value() instanceof StandBrewRecipe brew && !StackRecipeCatalog.owns(brew)) {
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

    public List<Entry> lingering() {
        return lingering;
    }

    public boolean shouldUnlock(Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld) {
        return shouldUnlock(entry, held, crafted, everHeld, Set.of());
    }

    public boolean shouldUnlock(Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld, Set<String> known) {
        if (!InversionUnlock.allowed(entry.requiresKnown, known)) {
            return false;
        }
        if (entry.kind == Kind.LINGER && !obtainedDragonBreath(held, crafted, everHeld)) {
            return false;
        }
        if (entry.subject != null && !everHeld.contains(entry.subject)) {
            return false;
        }
        return unlocks(entry.unlockItem, held, crafted);
    }

    private static boolean obtainedDragonBreath(Set<Item> held, Set<Item> crafted, Set<Item> everHeld) {
        return everHeld.contains(Items.DRAGON_BREATH)
                || held.contains(Items.DRAGON_BREATH)
                || crafted.contains(Items.DRAGON_BREATH);
    }

    private static boolean unlocks(@Nullable Item item, Set<Item> held, Set<Item> crafted) {
        return item != null && (held.contains(item) || crafted.contains(item));
    }

    private record Built(StandRecipeCatalog catalog, List<RecipeHolder<?>> holders) {
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
        ItemStack gunpowder = new ItemStack(Items.GUNPOWDER);
        ItemStack breath = new ItemStack(Items.DRAGON_BREATH);
        RecipeBookCategory helpful = ModRecipes.STAND_HELPFUL.get();
        RecipeBookCategory harmful = ModRecipes.STAND_HARMFUL.get();
        RecipeBookCategory parameter = ModRecipes.STAND_PARAMETER.get();
        RecipeBookCategory utility = ModRecipes.STAND_UTILITY.get();

        ActiveBrewing.using(brewing, () -> {
            addIf(entries, holders, paths, "stand/awkward", Kind.AWKWARD, Items.NETHER_WART, null, utility, "", true, false,
                    changed(brewing, new ItemStack(Items.NETHER_WART), water), water, new ItemStack(Items.NETHER_WART));
            add(entries, holders, paths, "stand/mundane/redstone", Kind.MUNDANE, Items.REDSTONE, null, utility, "", true, false,
                    water, redstone, PotionContents.createItemStack(Items.POTION, Potions.MUNDANE));

            List<BaseBrew> bases = new ArrayList<>();
            Map<ResourceKey<Potion>, Integer> byPotion = new HashMap<>();
            BuiltInRegistries.ITEM.forEach(item -> {
                if (!item.isEnabled(flags) || item == Items.AIR || MundaneBrewing.distilled(brewing, item).isEmpty()) {
                    return;
                }
                ItemStack result = changed(brewing, new ItemStack(item), awkward);
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
                add(entries, holders, paths, "stand/base/" + path, Kind.BASE, brew.item, null, category, "", true, false,
                        awkward, new ItemStack(brew.item), brew.result);
                addModifiers(entries, holders, paths, brewing, brew.result, path, brew.item, null, glowstone, redstone, gunpowder, breath, utility);
                ItemStack inverted = changed(brewing, eye, brew.result);
                if (inverted.isEmpty()) {
                    continue;
                }
                add(entries, holders, paths, "stand/invert/" + path, Kind.INVERT, brew.item, null,
                        categoryFor(inverted, helpful, harmful), "", true, false,
                        brew.result, eye, inverted);
                addModifiers(entries, holders, paths, brewing, inverted, "invert/" + path, brew.item, null, glowstone, redstone, gunpowder, breath, utility, RecipeMystery.effectIds(brew.result));
            }

            ItemStack stoneEssence = EyedBrewing.essenceOf(EyedBrewing.latentBottle(), new ItemStack(Items.STONE));
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.RESIN_CLUMP, StandSubjects.Kind.BLOCK, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.SCAFFOLDING, StandSubjects.Kind.BLOCK, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.COOKIE, StandSubjects.Kind.ITEM, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.ECHO_SHARD, StandSubjects.Kind.MOB, parameter, eye);
            addFamily(entries, holders, paths, brewing, stoneEssence, Items.LODESTONE, StandSubjects.Kind.SEEKING, parameter, eye);

            ItemStack sample = playerEssenceSample();
            ItemStack disguise = changed(brewing, new ItemStack(Items.CARVED_PUMPKIN), sample);
            if (!disguise.isEmpty()) {
                ItemStack shown = disguise.copy();
                shown.remove(ModDataComponents.ESSENCE.get());
                add(entries, holders, paths, "stand/param/disguise", Kind.DISGUISE, Items.CARVED_PUMPKIN, null, parameter, "", true, true,
                        sample, new ItemStack(Items.CARVED_PUMPKIN), shown);
            }
        });

        Set<ResourceKey<Recipe<?>>> ids = new HashSet<>();
        for (Entry entry : entries) {
            ids.add(entry.id);
        }
        return new Built(new StandRecipeCatalog(entries, ids), holders);
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
        ItemStack result = changed(brewing, catalystStack, essence);
        if (result.isEmpty() || !parameterized(result)) {
            return;
        }
        String potion = potionPath(result);
        if (potion == null) {
            return;
        }
        add(entries, holders, paths, "stand/param/" + potion, Kind.PARAMETER, catalyst, null, category, "param_" + potion, false, false,
                subjects, essence, catalystStack, result);
        ItemStack inverted = changed(brewing, eye, result);
        if (inverted.isEmpty() || !parameterized(inverted)) {
            return;
        }
        String invertedPotion = potionPath(inverted);
        if (invertedPotion == null) {
            return;
        }
        add(entries, holders, paths, "stand/param/" + invertedPotion, Kind.PARAMETER, catalyst, null, category,
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
            @Nullable Item subject,
            ItemStack glowstone,
            ItemStack redstone,
            ItemStack gunpowder,
            ItemStack breath,
            RecipeBookCategory utility
    ) {
        addModifiers(entries, holders, paths, brewing, base, suffix, unlockItem, subject, glowstone, redstone, gunpowder, breath, utility, Set.of());
    }

    private static void addModifiers(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            PotionBrewing brewing,
            ItemStack base,
            String suffix,
            Item unlockItem,
            @Nullable Item subject,
            ItemStack glowstone,
            ItemStack redstone,
            ItemStack gunpowder,
            ItemStack breath,
            RecipeBookCategory utility,
            Set<String> requiresKnown
    ) {
        addModifiers(entries, holders, paths, brewing, base, suffix, unlockItem, subject, glowstone, redstone, gunpowder, breath, utility, StandSubjects.Kind.NONE, requiresKnown);
    }

    private static void addModifiers(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            Set<String> paths,
            PotionBrewing brewing,
            ItemStack base,
            String suffix,
            Item unlockItem,
            @Nullable Item subject,
            ItemStack glowstone,
            ItemStack redstone,
            ItemStack gunpowder,
            ItemStack breath,
            RecipeBookCategory utility,
            StandSubjects.Kind subjects,
            Set<String> requiresKnown
    ) {
        ItemStack thick = changed(brewing, glowstone, base);
        if (!thick.isEmpty()) {
            add(entries, holders, paths, "stand/thicken/" + suffix, Kind.THICKEN, unlockItem, subject, utility, THICKEN_GROUP, false, false,
                    subjects, base, glowstone, thick, requiresKnown);
        }
        ItemStack extended = changed(brewing, redstone, base);
        if (!extended.isEmpty()) {
            add(entries, holders, paths, "stand/extend/" + suffix, Kind.EXTEND, unlockItem, subject, utility, EXTEND_GROUP, false, false,
                    subjects, base, redstone, extended, requiresKnown);
        }
        ItemStack splash = changed(brewing, gunpowder, base);
        if (splash.isEmpty() || !splash.is(Items.SPLASH_POTION)) {
            return;
        }
        add(entries, holders, paths, "stand/splash/" + suffix, Kind.SPLASH, unlockItem, subject, utility, SPLASH_GROUP, false, false,
                subjects, base, gunpowder, splash, requiresKnown);
        ItemStack lingering = changed(brewing, breath, splash);
        if (!lingering.isEmpty() && lingering.is(Items.LINGERING_POTION)) {
            add(entries, holders, paths, "stand/linger/" + suffix, Kind.LINGER, unlockItem, subject, utility, LINGER_GROUP, false, false,
                    subjects, splash, breath, lingering, requiresKnown);
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
                matching(bottle),
                matching(ingredient),
                ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1)),
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

    private static ItemStack changed(PotionBrewing brewing, ItemStack ingredient, ItemStack bottle) {
        if (ingredient.isEmpty() || bottle.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = brewing.mix(ingredient.copy(), bottle.copy());
        if (result == null || result.isEmpty() || ItemStack.isSameItemSameComponents(result, bottle)) {
            return ItemStack.EMPTY;
        }
        if (result.is(bottle.getItem()) && samePotion(result, bottle)) {
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
            } else if (negative(effect)) {
                bad = true;
            }
        }
        return bad && !good ? harmful : helpful;
    }

    private static boolean negative(MobEffectInstance effect) {
        if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return true;
        }
        return effect.is(ModEffects.INSTANT_CONFUSION) || effect.is(ModEffects.INSTANT_DESCENT);
    }

    private static boolean samePotion(ItemStack left, ItemStack right) {
        return left.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .equals(right.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion());
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
        if (stack.getItem() instanceof net.minecraft.world.item.PotionItem) {
            return DataComponentIngredient.of(false, stack.copyWithCount(1));
        }
        return Ingredient.of(stack.getItem());
    }

    private static String itemPath(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace() + "/" + id.getPath();
    }
}
