package com.buffspotions.stand;

import com.buffspotions.recipebook.InversionUnlock;
import com.buffspotions.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** Grants stand recipes once their ingredient, and any essence subject, has been obtained. */
public final class StandRecipeUnlocks {
    private static final Map<ServerPlayer, Integer> STAMPS = new WeakHashMap<>();
    private static final Map<ResourceKey<Recipe<?>>, Set<Item>> CRAFTED = new HashMap<>();

    private StandRecipeUnlocks() {
    }

    public static void survey(ServerPlayer player) {
        StandRecipeCatalog catalog = StandRecipeCatalog.get();
        if (catalog.entries().isEmpty()) {
            return;
        }
        int stamp = player.getInventory().getTimesChanged();
        if (stamp == STAMPS.getOrDefault(player, Integer.MIN_VALUE) && player.tickCount % 20 != 0) {
            return;
        }
        STAMPS.put(player, stamp);

        Set<Item> held = new HashSet<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                held.add(stack.getItem());
            }
        }
        if (!player.getOffhandItem().isEmpty()) {
            held.add(player.getOffhandItem().getItem());
        }

        Set<Item> crafted = new HashSet<>();
        RecipeManager recipes = player.level().getServer().getRecipeManager();
        var context = SlotDisplayContext.fromLevel(player.level());
        for (ResourceKey<Recipe<?>> id : player.getRecipeBook().pack().known()) {
            if (catalog.contains(id)) {
                continue;
            }
            Set<Item> cached = CRAFTED.get(id);
            if (cached == null) {
                cached = crafted(recipes.byKey(id).orElse(null), context);
                CRAFTED.put(id, cached);
            }
            crafted.addAll(cached);
        }

        Set<Item> everHeld = new HashSet<>(player.getData(ModAttachments.HELD_ITEMS));
        everHeld.addAll(held);
        Set<String> known = player.getData(ModAttachments.KNOWN_EFFECTS);
        List<RecipeHolder<?>> grant = new ArrayList<>();
        for (Item item : held) {
            collect(player, catalog, recipes, grant, catalog.forItem(item), held, crafted, everHeld, known);
        }
        for (Item item : crafted) {
            if (!held.contains(item)) {
                collect(player, catalog, recipes, grant, catalog.forItem(item), held, crafted, everHeld, known);
            }
        }
        for (Item item : everHeld) {
            if (!held.contains(item) && !crafted.contains(item)) {
                collect(player, catalog, recipes, grant, catalog.forSubject(item), held, crafted, everHeld, known);
            }
        }
        if (everHeld.contains(Items.DRAGON_BREATH) || crafted.contains(Items.DRAGON_BREATH)) {
            collect(player, catalog, recipes, grant, catalog.lingering(), held, crafted, everHeld, known);
        }
        if (!grant.isEmpty()) {
            player.awardRecipes(grant);
        }
        InversionUnlock.revoke(player, catalog.entries(), recipes);
    }

    public static void forget() {
        CRAFTED.clear();
        STAMPS.clear();
    }

    private static void collect(
            ServerPlayer player,
            StandRecipeCatalog catalog,
            RecipeManager recipes,
            List<RecipeHolder<?>> grant,
            List<StandRecipeCatalog.Entry> entries,
            Set<Item> held,
            Set<Item> crafted,
            Set<Item> everHeld,
            Set<String> known
    ) {
        for (StandRecipeCatalog.Entry entry : entries) {
            if (!catalog.shouldUnlock(entry, held, crafted, everHeld, known)) {
                continue;
            }
            if (player.getRecipeBook().contains(entry.id()) || alreadyGranted(grant, entry.id())) {
                continue;
            }
            recipes.byKey(entry.id()).ifPresent(grant::add);
        }
    }

    private static boolean alreadyGranted(List<RecipeHolder<?>> grant, ResourceKey<Recipe<?>> id) {
        for (RecipeHolder<?> holder : grant) {
            if (holder.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Item> crafted(RecipeHolder<?> holder, net.minecraft.util.context.ContextMap context) {
        Set<Item> items = new HashSet<>();
        if (holder == null) {
            return items;
        }
        for (RecipeDisplay display : holder.value().display()) {
            for (ItemStack stack : display.result().resolveForStacks(context)) {
                if (!stack.isEmpty()) {
                    items.add(stack.getItem());
                }
            }
        }
        return Set.copyOf(items);
    }
}
