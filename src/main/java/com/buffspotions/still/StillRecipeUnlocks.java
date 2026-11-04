package com.buffspotions.still;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.recipebook.InversionUnlock;
import com.buffspotions.registry.ModAttachments;
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

/** Grants still recipes once their ingredient has been held or its crafting recipe is known. */
public final class StillRecipeUnlocks {
    private static final Map<ServerPlayer, Integer> STAMPS = new WeakHashMap<>();
    private static final Map<ResourceKey<Recipe<?>>, Crafted> CRAFTED = new HashMap<>();

    private record Crafted(Set<Item> items, boolean milk) {
    }

    private StillRecipeUnlocks() {
    }

    public static void survey(ServerPlayer player) {
        StillRecipeCatalog catalog = StillRecipeCatalog.get();
        if (catalog.entries().isEmpty()) {
            return;
        }
        boolean latentNow = holdsLatent(player);
        if (latentNow) {
            player.setData(ModAttachments.HELD_LATENT, true);
        }
        int stamp = player.getInventory().getTimesChanged() * 2 + (player.getData(ModAttachments.HELD_LATENT) ? 1 : 0);
        if (stamp == STAMPS.getOrDefault(player, Integer.MIN_VALUE) && player.tickCount % 20 != 0) {
            return;
        }
        STAMPS.put(player, stamp);

        Set<Item> held = new HashSet<>();
        boolean heldMilk = false;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            heldMilk |= note(held, stack);
        }
        heldMilk |= note(held, player.getOffhandItem());

        Set<Item> crafted = new HashSet<>();
        boolean craftedMilk = false;
        RecipeManager recipes = player.level().getServer().getRecipeManager();
        var context = SlotDisplayContext.fromLevel(player.level());
        for (ResourceKey<Recipe<?>> id : player.getRecipeBook().pack().known()) {
            if (catalog.contains(id)) {
                continue;
            }
            Crafted cached = CRAFTED.get(id);
            if (cached == null) {
                cached = crafted(recipes.byKey(id).orElse(null), context);
                CRAFTED.put(id, cached);
            }
            crafted.addAll(cached.items);
            craftedMilk |= cached.milk;
        }

        boolean heldLatent = player.getData(ModAttachments.HELD_LATENT);
        Set<Item> everHeld = remember(player, held);
        boolean milk = heldMilk || craftedMilk;
        Set<String> known = player.getData(ModAttachments.KNOWN_EFFECTS);
        List<RecipeHolder<?>> grant = new java.util.ArrayList<>();
        for (Item item : held) {
            collect(player, catalog, recipes, grant, catalog.forItem(item), held, crafted, everHeld, heldLatent, milk, known);
        }
        for (Item item : crafted) {
            if (!held.contains(item)) {
                collect(player, catalog, recipes, grant, catalog.forItem(item), held, crafted, everHeld, heldLatent, milk, known);
            }
        }
        if (heldLatent) {
            for (Item item : everHeld) {
                if (!held.contains(item) && !crafted.contains(item)) {
                    collect(
                            player, catalog, recipes, grant, catalog.forItem(item),
                            held, crafted, everHeld, true, milk, known
                    );
                }
            }
        }
        if (milk) {
            collect(player, catalog, recipes, grant, catalog.splashMilk(), held, crafted, everHeld, heldLatent, true, known);
        }
        if (everHeld.contains(Items.DRAGON_BREATH) || crafted.contains(Items.DRAGON_BREATH)) {
            collect(player, catalog, recipes, grant, catalog.lingering(), held, crafted, everHeld, heldLatent, milk, known);
        }
        if (!grant.isEmpty()) {
            player.awardRecipes(grant);
        }
        InversionUnlock.revoke(player, catalog.entries(), recipes);
        if (!heldLatent) {
            revokeEssence(player, catalog, recipes);
        }
    }

    /** Essence entries stay out of the book until the player has carried a latent eyedentification potion. */
    private static void revokeEssence(ServerPlayer player, StillRecipeCatalog catalog, RecipeManager recipes) {
        List<RecipeHolder<?>> revoke = new java.util.ArrayList<>();
        for (StillRecipeCatalog.Entry entry : catalog.entries()) {
            if (entry.kind() != StillRecipeCatalog.Kind.ESSENCE || !player.getRecipeBook().contains(entry.id())) {
                continue;
            }
            recipes.byKey(entry.id()).ifPresent(revoke::add);
        }
        if (!revoke.isEmpty()) {
            player.getRecipeBook().removeRecipes(revoke, player);
        }
    }

    public static void forget() {
        CRAFTED.clear();
        STAMPS.clear();
    }

    private static void collect(
            ServerPlayer player,
            StillRecipeCatalog catalog,
            RecipeManager recipes,
            List<RecipeHolder<?>> grant,
            List<StillRecipeCatalog.Entry> entries,
            Set<Item> held,
            Set<Item> crafted,
            Set<Item> everHeld,
            boolean heldLatent,
            boolean heldMilk,
            Set<String> known
    ) {
        for (StillRecipeCatalog.Entry entry : entries) {
            if (!catalog.shouldUnlock(entry, held, crafted, everHeld, heldLatent, heldMilk, known)) {
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

    private static Set<Item> remember(ServerPlayer player, Set<Item> held) {
        Set<Item> ever = new HashSet<>(player.getData(ModAttachments.HELD_ITEMS));
        if (ever.addAll(held)) {
            player.setData(ModAttachments.HELD_ITEMS, ever);
        }
        return ever;
    }

    private static boolean note(Set<Item> held, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        held.add(stack.getItem());
        return FlagonContents.isMilkDose(stack);
    }

    private static boolean holdsLatent(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (EyedBrewing.isLatent(stack)) {
                return true;
            }
        }
        return EyedBrewing.isLatent(player.getOffhandItem());
    }

    private static Crafted crafted(RecipeHolder<?> holder, net.minecraft.util.context.ContextMap context) {
        Set<Item> items = new HashSet<>();
        boolean milk = false;
        if (holder == null) {
            return new Crafted(items, false);
        }
        for (RecipeDisplay display : holder.value().display()) {
            for (ItemStack stack : display.result().resolveForStacks(context)) {
                if (stack.isEmpty()) {
                    continue;
                }
                items.add(stack.getItem());
                milk |= FlagonContents.isMilkDose(stack);
            }
        }
        return new Crafted(Set.copyOf(items), milk);
    }
}
