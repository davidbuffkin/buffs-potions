package com.buffspotions.still;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.ActiveBrewing;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.RemedyRecipe;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.recipebook.InversionUnlock;
import com.buffspotions.recipebook.RecipeMystery;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModRecipes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * Every still recipe the book can show. Mundane potions, remedies, and their splash forms
 * unlock from the ingredient. Lingering forms unlock once dragon's breath has been obtained,
 * for the splash recipes the player already knows. Essence unlocks only after a latent bottle
 * has been held, and then only for ingredients the player has also unlocked.
 */
public final class StillRecipeCatalog {
    public static final String SPLASH_GROUP = "splash";
    public static final String LINGERING_GROUP = "lingering";
    public static final String REMEDY_GROUP = "remedy";
    public static final String ESSENCE_GROUP = "essence";

    public enum Kind {
        MUNDANE,
        SUSPICIOUS,
        REMEDY,
        SPLASH,
        SPLASH_MILK,
        LINGERING,
        LINGERING_MILK,
        ESSENCE
    }

    public record Entry(ResourceKey<Recipe<?>> id, Kind kind, @Nullable Item unlockItem, Set<String> requiresKnown) implements InversionUnlock.Gated {
    }

    private static final StillRecipeCatalog EMPTY = new StillRecipeCatalog(List.of(), Set.of());
    private static StillRecipeCatalog current = EMPTY;

    private final List<Entry> entries;
    private final Set<ResourceKey<Recipe<?>>> ids;
    private final Map<Item, List<Entry>> byItem = new HashMap<>();
    private final List<Entry> splashMilk = new ArrayList<>();
    private final List<Entry> lingering = new ArrayList<>();

    private StillRecipeCatalog(List<Entry> entries, Set<ResourceKey<Recipe<?>>> ids) {
        this.entries = entries;
        this.ids = ids;
        for (Entry entry : entries) {
            if (entry.kind == Kind.SPLASH_MILK) {
                splashMilk.add(entry);
                continue;
            }
            if (entry.kind == Kind.LINGERING || entry.kind == Kind.LINGERING_MILK) {
                lingering.add(entry);
            }
            if (entry.unlockItem != null) {
                byItem.computeIfAbsent(entry.unlockItem, key -> new ArrayList<>()).add(entry);
            }
        }
    }

    public static StillRecipeCatalog get() {
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
            if (!(holder.value() instanceof StillDistillRecipe)) {
                holders.add(holder);
            }
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

    public List<Entry> splashMilk() {
        return splashMilk;
    }

    public List<Entry> lingering() {
        return lingering;
    }

    /**
     * @param heldLatent the player has had a latent eyedentification bottle in their inventory
     * @param heldMilk the player currently has, or has unlocked a recipe whose result is, a milk bottle
     */
    public boolean shouldUnlock(
            Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld, boolean heldLatent, boolean heldMilk
    ) {
        return shouldUnlock(entry, held, crafted, everHeld, heldLatent, heldMilk, Set.of());
    }

    public boolean shouldUnlock(
            Entry entry, Set<Item> held, Set<Item> crafted, Set<Item> everHeld, boolean heldLatent, boolean heldMilk, Set<String> known
    ) {
        if (!InversionUnlock.allowed(entry.requiresKnown, known)) {
            return false;
        }
        if (entry.kind == Kind.ESSENCE) {
            return heldLatent && entry.unlockItem != null && everHeld.contains(entry.unlockItem);
        }
        if (entry.kind == Kind.SPLASH_MILK) {
            return heldMilk;
        }
        if (entry.kind == Kind.LINGERING_MILK) {
            return heldMilk && obtainedDragonBreath(held, crafted, everHeld);
        }
        if (entry.kind == Kind.LINGERING) {
            return obtainedDragonBreath(held, crafted, everHeld) && unlocks(entry.unlockItem, held, crafted);
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

    private record Built(StillRecipeCatalog catalog, List<RecipeHolder<?>> holders) {
    }

    private static Built build(PotionBrewing brewing, FeatureFlagSet flags) {
        List<Entry> entries = new ArrayList<>();
        List<RecipeHolder<?>> holders = new ArrayList<>();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack plainMundane = PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
        ItemStack milk = FlagonContents.milkTemplate();
        ItemStack gunpowder = new ItemStack(Items.GUNPOWDER);
        ItemStack breath = new ItemStack(Items.DRAGON_BREATH);
        RecipeBookCategory mundaneCategory = ModRecipes.MUNDANE.get();
        RecipeBookCategory harmfulCategory = ModRecipes.HARMFUL.get();
        RecipeBookCategory other = ModRecipes.OTHER.get();
        RemedyRecipe remedies = new RemedyRecipe();

        add(entries, holders, "still/mundane_potion", Kind.MUNDANE, Items.REDSTONE, other, "",
                water, new ItemStack(Items.REDSTONE), plainMundane);
        add(entries, holders, "still/suspicious/beetroot", Kind.SUSPICIOUS, Items.BEETROOT, other, "",
                water, new ItemStack(Items.BEETROOT),
                new SuspiciousBrew.Recipe().getOutput(water, new ItemStack(Items.BEETROOT)));

        ItemStack splashMilk = brewing.mix(gunpowder.copy(), milk.copy());
        if (!splashMilk.isEmpty() && splashMilk.is(Items.SPLASH_POTION)) {
            add(entries, holders, "still/splash/milk", Kind.SPLASH_MILK, null, other, SPLASH_GROUP,
                    milk, gunpowder, splashMilk);
            ItemStack lingeringMilk = StillBrewing.mix(brewing, breath.copy(), splashMilk.copy());
            if (!lingeringMilk.isEmpty() && lingeringMilk.is(Items.LINGERING_POTION)) {
                add(entries, holders, "still/lingering/milk", Kind.LINGERING_MILK, null, other, LINGERING_GROUP,
                        splashMilk, breath, lingeringMilk);
            }
        }

        ActiveBrewing.using(brewing, () -> {
            List<MundaneBrew> mundanes = new ArrayList<>();
            Map<ResourceKey<Potion>, Integer> byPotion = new HashMap<>();
            BuiltInRegistries.ITEM.forEach(item -> {
                if (!item.isEnabled(flags) || item == Items.AIR) {
                    return;
                }
                String path = itemPath(item);
                ItemStack ingredient = new ItemStack(item);
                ItemStack mundane = MundaneBrewing.distilled(brewing, item);
                if (!mundane.isEmpty()) {
                    ResourceKey<Potion> potion = potionKey(mundane);
                    if (potion != null) {
                        Integer existing = byPotion.get(potion);
                        if (existing == null) {
                            byPotion.put(potion, mundanes.size());
                            mundanes.add(new MundaneBrew(item, mundane));
                        }
                    }
                }
                ItemStack remedy = remedies.getOutput(milk.copy(), ingredient.copy());
                if (!remedy.isEmpty()) {
                    add(entries, holders, "still/remedy/" + path, Kind.REMEDY, item, other, REMEDY_GROUP,
                            milk, ingredient, remedy);
                    ItemStack splash = remedies.getOutput(remedy.copy(), gunpowder.copy());
                    if (!splash.isEmpty()) {
                        add(entries, holders, "still/splash/remedy/" + path, Kind.SPLASH, item, other, SPLASH_GROUP,
                                remedy, gunpowder, splash);
                        ItemStack lingering = remedies.getOutput(splash.copy(), breath.copy());
                        if (!lingering.isEmpty() && lingering.is(Items.LINGERING_POTION)) {
                            add(entries, holders, "still/lingering/remedy/" + path, Kind.LINGERING, item, other, LINGERING_GROUP,
                                    splash, breath, lingering);
                        }
                    }
                }
                ItemStack essence = EyedBrewing.essenceOf(EyedBrewing.latentBottle(), ingredient);
                if (!essence.isEmpty()) {
                    add(entries, holders, "still/essence/" + path, Kind.ESSENCE, item, other, ESSENCE_GROUP,
                            EyedBrewing.latentBottle(), ingredient, essence);
                }
            });
            ItemStack eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
            for (MundaneBrew brew : mundanes) {
                String path = itemPath(brew.item());
                ItemStack ingredient = new ItemStack(brew.item());
                add(entries, holders, "still/mundane/" + path, Kind.MUNDANE, brew.item(),
                        categoryFor(brew.result(), mundaneCategory, harmfulCategory), "",
                        plainMundane, ingredient, brew.result());
                addSplash(entries, holders, "still/splash/mundane/" + path, "still/lingering/mundane/" + path,
                        brew.item(), other, brew.result(), gunpowder, breath, brewing);
                ItemStack inverted = MundaneBrewing.mix(brewing, eye.copy(), brew.result().copy());
                if (inverted == null || inverted.isEmpty() || samePotion(brew.result(), inverted)) {
                    continue;
                }
                add(entries, holders, "still/mundane/invert/" + path, Kind.MUNDANE, brew.item(),
                        categoryFor(inverted, mundaneCategory, harmfulCategory), "",
                        brew.result(), eye, inverted);
                addSplash(entries, holders, "still/splash/mundane/invert/" + path, "still/lingering/mundane/invert/" + path,
                        brew.item(), other, inverted, gunpowder, breath, brewing, RecipeMystery.effectIds(brew.result()));
            }
        });

        Set<ResourceKey<Recipe<?>>> ids = new HashSet<>();
        for (Entry entry : entries) {
            ids.add(entry.id);
        }
        return new Built(new StillRecipeCatalog(entries, ids), holders);
    }

    private record MundaneBrew(Item item, ItemStack result) {
    }

    private static void addSplash(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            String splashPath,
            String lingeringPath,
            Item unlockItem,
            RecipeBookCategory category,
            ItemStack mundane,
            ItemStack gunpowder,
            ItemStack breath,
            PotionBrewing brewing
    ) {
        addSplash(entries, holders, splashPath, lingeringPath, unlockItem, category, mundane, gunpowder, breath, brewing, Set.of());
    }

    private static void addSplash(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            String splashPath,
            String lingeringPath,
            Item unlockItem,
            RecipeBookCategory category,
            ItemStack mundane,
            ItemStack gunpowder,
            ItemStack breath,
            PotionBrewing brewing,
            Set<String> inherited
    ) {
        ItemStack splash = MundaneBrewing.mix(brewing, gunpowder.copy(), mundane.copy());
        if (splash == null || splash.isEmpty() || !splash.is(Items.SPLASH_POTION)) {
            return;
        }
        add(entries, holders, splashPath, Kind.SPLASH, unlockItem, category, SPLASH_GROUP, mundane, gunpowder, splash, inherited);
        ItemStack lingering = MundaneBrewing.mix(brewing, breath.copy(), splash.copy());
        if (lingering != null && !lingering.isEmpty() && lingering.is(Items.LINGERING_POTION)) {
            add(entries, holders, lingeringPath, Kind.LINGERING, unlockItem, category, LINGERING_GROUP, splash, breath, lingering, inherited);
        }
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

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            RecipeBookCategory category,
            String group,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result
    ) {
        add(entries, holders, path, kind, unlockItem, category, group, bottle, ingredient, result, Set.of());
    }

    private static void add(
            List<Entry> entries,
            List<RecipeHolder<?>> holders,
            String path,
            Kind kind,
            @Nullable Item unlockItem,
            RecipeBookCategory category,
            String group,
            ItemStack bottle,
            ItemStack ingredient,
            ItemStack result,
            Set<String> inherited
    ) {
        if (bottle.isEmpty() || ingredient.isEmpty() || result.isEmpty()) {
            return;
        }
        ResourceKey<Recipe<?>> id = ResourceKey.create(
                Registries.RECIPE,
                Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, path)
        );
        StillDistillRecipe recipe = new StillDistillRecipe(
                matching(bottle),
                matching(ingredient),
                ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1)),
                category,
                group,
                kind == Kind.MUNDANE || kind == Kind.SUSPICIOUS
        );
        Set<String> requires = inherited.isEmpty() ? InversionUnlock.of(ingredient, bottle) : Set.copyOf(inherited);
        entries.add(new Entry(id, kind, unlockItem, requires));
        holders.add(new RecipeHolder<>(id, recipe));
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
