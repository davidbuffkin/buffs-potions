package com.buffspotions.stand;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.MobDrops;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/** Which held items a parameterized stand recipe can name, and how a sample stack is retargeted. */
public final class StandSubjects {
    public enum Kind {
        NONE,
        BLOCK,
        ITEM,
        SEEKING,
        /** An item that the mob-drop list maps to a mob. Incarnation uses this. */
        MOB;

        public static Kind byOrdinal(int ordinal) {
            Kind[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }
    }

    private StandSubjects() {
    }

    public static boolean accepts(Kind kind, Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        return switch (kind) {
            case NONE -> false;
            case BLOCK -> item instanceof BlockItem;
            case ITEM -> true;
            case SEEKING -> item instanceof BlockItem || MobDrops.type(item).isPresent();
            case MOB -> MobDrops.type(item).isPresent();
        };
    }

    /** Ever-held items, plus anything in the inventory that has not been recorded yet. */
    public static List<Item> matching(Player player, Kind kind) {
        if (player == null || kind == Kind.NONE) {
            return List.of();
        }
        Set<Item> items = new HashSet<>(player.getData(ModAttachments.HELD_ITEMS));
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                items.add(stack.getItem());
            }
        }
        if (!player.getOffhandItem().isEmpty()) {
            items.add(player.getOffhandItem().getItem());
        }
        List<Item> accepted = new ArrayList<>();
        for (Item item : items) {
            if (accepts(kind, item)) {
                accepted.add(item);
            }
        }
        accepted.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return accepted;
    }

    public static boolean known(Player player, Item item) {
        if (player == null || item == null || item == Items.AIR) {
            return false;
        }
        if (player.getData(ModAttachments.HELD_ITEMS).contains(item)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return true;
            }
        }
        return player.getOffhandItem().is(item);
    }

    public static ItemStack retarget(ItemStack sample, Item item) {
        if (sample.isEmpty()) {
            return sample.copy();
        }
        if (ModItems.fitsBrewingStack(sample)) {
            FlagonContents contents = FlagonContents.of(sample);
            if (contents.isEmpty() || !contents.dose().has(ModDataComponents.ESSENCE.get())) {
                return sample.copy();
            }
            ItemStack copy = sample.copy();
            copy.set(ModDataComponents.FLAGON_CONTENTS.get(), new FlagonContents(retarget(contents.dose(), item), contents.doses()));
            return copy;
        }
        if (!sample.has(ModDataComponents.ESSENCE.get())) {
            return sample.copy();
        }
        EssenceSubject subject = EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(item));
        ItemStack copy = sample.copy();
        copy.set(ModDataComponents.ESSENCE.get(), subject);
        if (EyedBrewing.isEssence(copy)) {
            PotionContents contents = copy.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            copy.set(DataComponents.POTION_CONTENTS, new PotionContents(
                    contents.potion(),
                    Optional.of(subject.liquidColor()),
                    contents.customEffects(),
                    contents.customName()
            ));
        }
        return copy;
    }

    public static ItemStack resolve(SlotDisplay display, Level level) {
        List<ItemStack> stacks = display.resolveForStacks(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level));
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst().copy();
    }

    /**
     * Shared id for a thicken, extend, splash, and linger form of one potion.
     * {@code strong_swiftness} and {@code long_swiftness} both become {@code swiftness}.
     */
    public static String potionCycleKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if (ModItems.fitsBrewingStack(stack)) {
            FlagonContents contents = FlagonContents.of(stack);
            if (contents.isEmpty()) {
                return "";
            }
            stack = contents.dose();
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        String path = contents.potion()
                .flatMap(holder -> holder.unwrapKey())
                .map(key -> key.identifier().getPath())
                .orElseGet(() -> contents.customName().orElse(""));
        if (path.startsWith("strong_")) {
            return path.substring("strong_".length());
        }
        if (path.startsWith("long_")) {
            return path.substring("long_".length());
        }
        return path;
    }

    public static ItemStack resolve(SlotDisplay display, ContextMap context) {
        List<ItemStack> stacks = display.resolveForStacks(context);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst().copy();
    }
}
