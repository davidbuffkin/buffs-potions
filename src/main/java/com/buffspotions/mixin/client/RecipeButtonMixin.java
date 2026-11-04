package com.buffspotions.mixin.client;

import com.buffspotions.client.RecipeMysteryView;
import com.buffspotions.client.StandSubjectClicks;
import com.buffspotions.recipebook.RecipeMystery;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stack.StackFlagons;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
import com.buffspotions.still.StillRecipeDisplay;
import com.buffspotions.still.StillStacks;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.SlotSelectTime;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grouped still recipes cycle on left click. They do not offer the vanilla
 * "right click for more" list, and essence cycles in a shuffled order.
 */
@Mixin(RecipeButton.class)
abstract class RecipeButtonMixin {
    @Shadow
    private RecipeCollection collection;

    @Shadow
    private List selectedEntries;

    @Shadow
    private boolean allRecipesHaveSameResultDisplay;

    @Shadow
    public abstract ItemStack getDisplayStack();

    @Shadow
    private SlotSelectTime slotSelectTime;

    @Inject(method = "init", at = @At("RETURN"))
    private void buffsPotions$expandSubjects(
            RecipeCollection collection, boolean isFiltering, RecipeBookPage page, ContextMap context, CallbackInfo ci
    ) {
        boolean shuffle = expandSubjects(context, isFiltering);
        alignModifiers(context, isFiltering);
        if (shuffle && this.selectedEntries.size() >= 2) {
            List copy = new ArrayList(this.selectedEntries);
            Collections.shuffle(copy, new Random(seed(collection)));
            this.selectedEntries = copy;
        }
        retainKnown();
    }

    @Inject(method = "getDisplayStack", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$mysteryIcon(CallbackInfoReturnable<ItemStack> callback) {
        if (!RecipeMystery.ENABLED || !concealedShown()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ItemStack ingredient = RecipeMysteryView.ingredient(
                this.collection,
                currentId(),
                net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level)
        );
        if (ingredient.isEmpty()) {
            ingredient = new ItemStack(Items.PAPER);
        }
        ingredient.set(DataComponents.CUSTOM_NAME, Component.translatable(RecipeMystery.LABEL));
        callback.setReturnValue(ingredient);
    }

    @Inject(method = "getTooltipText", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$mysteryTip(ItemStack stack, CallbackInfoReturnable<List<Component>> callback) {
        if (RecipeMystery.ENABLED && concealedShown()) {
            callback.setReturnValue(List.of(Component.translatable(RecipeMystery.LABEL)));
        }
    }

    @Inject(method = "extractWidgetRenderState", at = @At("RETURN"))
    private void buffsPotions$mysteryMark(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (!RecipeMystery.ENABLED || !concealedShown()) {
            return;
        }
        AbstractWidget widget = (AbstractWidget) (Object) this;
        Font font = Minecraft.getInstance().font;
        int itemX = widget.getX() + 4;
        int itemY = widget.getY() + 4;
        int right = itemX + 17;
        int top = itemY - 1;
        graphics.text(font, "?", right - font.width("?"), top, 0xFFCC2222, true);
    }

    @Inject(method = "getCurrentRecipe", at = @At("RETURN"))
    private void buffsPotions$armSubject(CallbackInfoReturnable<RecipeDisplayId> callback) {
        RecipeDisplayId current = callback.getReturnValue();
        boolean template = false;
        for (RecipeDisplayEntry entry : this.collection.getRecipes()) {
            if (entry.id().equals(current) && entry.display() instanceof StandRecipeDisplay stand
                    && stand.subjects() != StandSubjects.Kind.NONE) {
                template = true;
                break;
            }
        }
        ItemStack shown = this.getDisplayStack();
        EssenceSubject subject = shown.get(ModDataComponents.ESSENCE.get());
        if (!template || subject == null || subject.item().isEmpty()) {
            StandSubjectClicks.clear();
            return;
        }
        StandSubjectClicks.arm(current, subject.item().get().value());
    }

    @Inject(method = "isOnlyOption", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$noOverlay(CallbackInfoReturnable<Boolean> callback) {
        if (bookCycle()) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getTooltipText", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$plainName(ItemStack stack, CallbackInfoReturnable<List<Component>> callback) {
        if (!bookCycle()) {
            return;
        }
        List<Component> texts = new ArrayList<>();
        for (Component line : callback.getReturnValue()) {
            if (line.getContents() instanceof TranslatableContents contents
                    && "gui.recipebook.moreRecipes".equals(contents.getKey())) {
                continue;
            }
            texts.add(line);
        }
        callback.setReturnValue(texts);
    }

    @Inject(method = "updateWidgetNarration", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$plainNarration(NarrationElementOutput output, CallbackInfo ci) {
        if (!bookCycle()) {
            return;
        }
        output.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", this.getDisplayStack().getHoverName()));
        output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        ci.cancel();
    }

    /**
     * @return whether every shown result is an essence, so the cycle should be shuffled
     */
    private boolean expandSubjects(ContextMap context, boolean filtering) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.selectedEntries.size() < 1) {
            return false;
        }
        List<RecipeDisplayEntry> shown = this.collection.getSelectedRecipes(
                filtering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY
        );
        if (shown.size() != this.selectedEntries.size()) {
            return false;
        }
        boolean template = false;
        for (RecipeDisplayEntry entry : shown) {
            if (entry.display() instanceof StandRecipeDisplay stand && stand.subjects() != StandSubjects.Kind.NONE) {
                template = true;
                break;
            }
        }
        if (!template) {
            return this.selectedEntries.size() > 1 && essenceResults(this.collection.getRecipes(), context);
        }
        List next = new ArrayList();
        boolean onlyEssence = true;
        for (int index = 0; index < shown.size(); index++) {
            RecipeDisplayEntry entry = shown.get(index);
            if (!(entry.display() instanceof StandRecipeDisplay stand) || stand.subjects() == StandSubjects.Kind.NONE) {
                next.add(this.selectedEntries.get(index));
                onlyEssence &= resultIsEssence(entry, context);
                continue;
            }
            ItemStack sampleBottle = StandSubjects.resolve(stand.bottle(), context);
            ItemStack sampleResult = StandSubjects.resolve(stand.result(), context);
            boolean added = false;
            for (Item subject : StandSubjects.matching(minecraft.player, stand.subjects())) {
                if (filtering && !carrying(minecraft.player, StandSubjects.retarget(sampleBottle, subject))) {
                    continue;
                }
                Object resolved = resolvedEntry(entry.id(), List.of(StandSubjects.retarget(sampleResult, subject)));
                if (resolved == null) {
                    return false;
                }
                next.add(resolved);
                added = true;
            }
            if (!added && !filtering) {
                next.add(this.selectedEntries.get(index));
                onlyEssence = false;
            }
        }
        if (next.isEmpty()) {
            return false;
        }
        this.selectedEntries = next;
        if (next.size() > 1) {
            this.allRecipesHaveSameResultDisplay = false;
        }
        return onlyEssence && next.size() > 1 && standBook();
    }

    /**
     * Modifier buttons on one tab share one ordered list of base potions,
     * so the same index shows the same effect on every button.
     */
    private void alignModifiers(ContextMap context, boolean filtering) {
        if (!modifierGroup(this.collection, context)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.collection.getRecipes().isEmpty()) {
            return;
        }
        net.minecraft.world.item.crafting.RecipeBookCategory category = this.collection.getRecipes().get(0).category();
        if (category != ModRecipes.STAND_UTILITY.get() && category != ModRecipes.STACK_UTILITY.get()) {
            return;
        }
        List<Set<String>> groups = new ArrayList<>();
        for (RecipeCollection collection : minecraft.player.getRecipeBook().getCollection(category)) {
            if (!modifierGroup(collection, context)) {
                continue;
            }
            Set<String> keys = keysFor(collection, context);
            if (!keys.isEmpty()) {
                groups.add(keys);
            }
        }
        if (groups.isEmpty()) {
            return;
        }
        Set<String> shared = new HashSet<>(groups.get(0));
        for (int index = 1; index < groups.size(); index++) {
            shared.retainAll(groups.get(index));
        }
        if (shared.isEmpty()) {
            return;
        }
        List<String> master = new ArrayList<>(shared);
        Collections.sort(master);
        List<RecipeDisplayEntry> shown = this.collection.getSelectedRecipes(
                filtering ? RecipeCollection.CraftableStatus.CRAFTABLE : RecipeCollection.CraftableStatus.ANY
        );
        if (shown.size() != this.selectedEntries.size()) {
            return;
        }
        if (alternating(this.collection, context)) {
            alignAlternating(shown, master, context);
            return;
        }
        Map<String, Object> byKey = new HashMap<>();
        for (int index = 0; index < shown.size(); index++) {
            String key = cycleKey(shown.get(index), context);
            if (shared.contains(key)) {
                byKey.put(key, this.selectedEntries.get(index));
            }
        }
        if (byKey.size() != master.size()) {
            return;
        }
        List next = new ArrayList();
        for (String key : master) {
            next.add(byKey.get(key));
        }
        this.selectedEntries = next;
        this.allRecipesHaveSameResultDisplay = false;
    }

    /** The combined button flips between torchflower and pitcher plant as the shared list advances. */
    private void alignAlternating(List<RecipeDisplayEntry> shown, List<String> master, ContextMap context) {
        Map<String, Object> torchflower = new HashMap<>();
        Map<String, Object> pitcher = new HashMap<>();
        for (int index = 0; index < shown.size(); index++) {
            RecipeDisplayEntry entry = shown.get(index);
            String key = cycleKey(entry, context);
            if (!master.contains(key)) {
                continue;
            }
            if (ingredientIs(entry, Items.TORCHFLOWER, context)) {
                torchflower.put(key, this.selectedEntries.get(index));
            } else if (ingredientIs(entry, Items.PITCHER_PLANT, context)) {
                pitcher.put(key, this.selectedEntries.get(index));
            }
        }
        if (torchflower.size() != master.size() || pitcher.size() != master.size()) {
            return;
        }
        List next = new ArrayList();
        for (int index = 0; index < master.size(); index++) {
            String key = master.get(index);
            next.add(index % 2 == 0 ? torchflower.get(key) : pitcher.get(key));
        }
        this.selectedEntries = next;
        this.allRecipesHaveSameResultDisplay = false;
    }

    private static Set<String> keysFor(RecipeCollection collection, ContextMap context) {
        if (!alternating(collection, context)) {
            Set<String> keys = new HashSet<>();
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                String key = cycleKey(entry, context);
                if (!key.isEmpty()) {
                    keys.add(key);
                }
            }
            return keys;
        }
        Set<String> torchflower = new HashSet<>();
        Set<String> pitcher = new HashSet<>();
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            String key = cycleKey(entry, context);
            if (key.isEmpty()) {
                continue;
            }
            if (ingredientIs(entry, Items.TORCHFLOWER, context)) {
                torchflower.add(key);
            } else if (ingredientIs(entry, Items.PITCHER_PLANT, context)) {
                pitcher.add(key);
            }
        }
        torchflower.retainAll(pitcher);
        return torchflower;
    }

    private static boolean alternating(RecipeCollection collection, ContextMap context) {
        return ingredientIs(collection, Items.TORCHFLOWER, context) && ingredientIs(collection, Items.PITCHER_PLANT, context);
    }

    private static boolean ingredientIs(RecipeCollection collection, Item item, ContextMap context) {
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (ingredientIs(entry, item, context)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ingredientIs(RecipeDisplayEntry entry, Item item, ContextMap context) {
        if (!(entry.display() instanceof StandRecipeDisplay stand)) {
            return false;
        }
        for (ItemStack stack : stand.ingredient().resolveForStacks(context)) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean modifierGroup(RecipeCollection collection, ContextMap context) {
        List<RecipeDisplayEntry> recipes = collection.getRecipes();
        if (recipes.isEmpty()) {
            return false;
        }
        for (RecipeDisplayEntry entry : recipes) {
            if (!(entry.display() instanceof StandRecipeDisplay stand) || stand.subjects() != StandSubjects.Kind.NONE) {
                return false;
            }
            for (ItemStack result : stand.result().resolveForStacks(context)) {
                if (MundaneBrewing.isPlain(result)) {
                    return false;
                }
            }
            boolean modifier = false;
            for (ItemStack stack : stand.ingredient().resolveForStacks(context)) {
                if (stack.is(Items.GLOWSTONE_DUST)
                        || stack.is(Items.REDSTONE)
                        || stack.is(Items.GUNPOWDER)
                        || stack.is(Items.DRAGON_BREATH)
                        || stack.is(Items.TORCHFLOWER)
                        || stack.is(Items.PITCHER_PLANT)) {
                    modifier = true;
                    break;
                }
            }
            if (!modifier) {
                return false;
            }
        }
        return true;
    }

    private static String cycleKey(RecipeDisplayEntry entry, ContextMap context) {
        for (ItemStack stack : entry.resultItems(context)) {
            String key = StandSubjects.potionCycleKey(stack);
            if (!key.isEmpty()) {
                return key;
            }
        }
        return "";
    }

    private boolean bookCycle() {
        return this.selectedEntries.size() > 1 && standBook();
    }

    private boolean standBook() {
        List<RecipeDisplayEntry> recipes = this.collection.getRecipes();
        if (recipes.isEmpty()) {
            return false;
        }
        for (RecipeDisplayEntry entry : recipes) {
            if (!(entry.display() instanceof StillRecipeDisplay) && !(entry.display() instanceof StandRecipeDisplay)) {
                return false;
            }
        }
        return true;
    }

    private static boolean essenceResults(List<RecipeDisplayEntry> entries, ContextMap context) {
        if (entries.size() < 2) {
            return false;
        }
        for (RecipeDisplayEntry entry : entries) {
            if (!resultIsEssence(entry, context)) {
                return false;
            }
        }
        return true;
    }

    private static boolean resultIsEssence(RecipeDisplayEntry entry, ContextMap context) {
        boolean any = false;
        for (ItemStack stack : entry.resultItems(context)) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack shown = ModItems.fitsBrewingStack(stack) ? FlagonContents.of(stack).dose() : stack;
            if (shown.isEmpty() || (!EyedBrewing.isEssence(shown) && !shown.has(ModDataComponents.ESSENCE.get()))) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private static boolean carrying(Player player, ItemStack want) {
        if (ModItems.fitsBrewingStack(want)) {
            for (Slot slot : player.containerMenu.slots) {
                if (StackFlagons.sameFluid(slot.getItem(), want)) {
                    return true;
                }
            }
            return StackFlagons.sameFluid(player.getOffhandItem(), want);
        }
        for (Slot slot : player.containerMenu.slots) {
            if (StillStacks.satisfies(slot.getItem(), want)) {
                return true;
            }
        }
        return false;
    }

    private static Object resolvedEntry(RecipeDisplayId id, List<ItemStack> stacks) {
        try {
            Class<?> type = Class.forName("net.minecraft.client.gui.screens.recipebook.RecipeButton$ResolvedEntry");
            Constructor<?> constructor = type.getDeclaredConstructor(RecipeDisplayId.class, List.class);
            constructor.setAccessible(true);
            return constructor.newInstance(id, stacks);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static long seed(RecipeCollection collection) {
        long value = 0x9E3779B97F4A7C15L;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            value ^= minecraft.player.getUUID().getLeastSignificantBits();
        }
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            value = value * 31 + entry.id().index();
        }
        return value;
    }

    /** Cycling buttons drop unknown effects. A single unknown recipe stays, and shows a question mark. */
    private void retainKnown() {
        if (!RecipeMystery.ENABLED || this.selectedEntries.size() < 2) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        List kept = new ArrayList();
        for (Object entry : this.selectedEntries) {
            boolean show = false;
            for (ItemStack stack : displayItems(entry)) {
                if (!RecipeMystery.concealed(minecraft.player, stack)) {
                    show = true;
                    break;
                }
            }
            if (show || displayItems(entry).isEmpty()) {
                kept.add(entry);
            }
        }
        if (!kept.isEmpty() && kept.size() != this.selectedEntries.size()) {
            this.selectedEntries = kept;
            if (kept.size() == 1) {
                this.allRecipesHaveSameResultDisplay = true;
            }
        }
    }

    private boolean concealedShown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || this.selectedEntries.isEmpty()) {
            return false;
        }
        RecipeDisplayId id = currentId();
        net.minecraft.util.context.ContextMap context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(minecraft.level);
        for (RecipeDisplayEntry entry : this.collection.getRecipes()) {
            if (!entry.id().equals(id)) {
                continue;
            }
            for (ItemStack stack : entry.resultItems(context)) {
                if (RecipeMystery.concealed(minecraft.player, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private RecipeDisplayId currentId() {
        int count = this.selectedEntries.size();
        Object entry = this.selectedEntries.get(Math.floorMod(this.slotSelectTime.currentIndex(), count));
        try {
            return (RecipeDisplayId) entry.getClass().getMethod("id").invoke(entry);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> displayItems(Object entry) {
        try {
            Method method = entry.getClass().getMethod("displayItems");
            return (List<ItemStack>) method.invoke(entry);
        } catch (ReflectiveOperationException exception) {
            return List.of();
        }
    }
}
