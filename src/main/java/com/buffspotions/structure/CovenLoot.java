package com.buffspotions.structure;

import com.buffspotions.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Trapped chest in the coven: lapis, a bundle, then a few distinct ingredients. */
public final class CovenLoot {
    private CovenLoot() {
    }

    public static List<ItemStack> chest(RandomSource random) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Items.LAPIS_LAZULI, 5 + random.nextInt(4)));
        stacks.add(new ItemStack(Items.BUNDLE));
        List<Item> extras = new ArrayList<>(List.of(
                Items.SPIDER_EYE,
                ModItems.WING.get(),
                Items.RABBIT_HIDE,
                Items.GLOW_BERRIES,
                Items.CHORUS_FLOWER,
                Items.RABBIT_FOOT,
                Items.SLIME_BALL,
                Items.POISONOUS_POTATO,
                Items.PHANTOM_MEMBRANE
        ));
        int picks = 3 + random.nextInt(3);
        for (int i = 0; i < picks; i++) {
            int swap = i + random.nextInt(extras.size() - i);
            Item item = extras.get(swap);
            extras.set(swap, extras.get(i));
            extras.set(i, item);
            stacks.add(new ItemStack(item, 1 + random.nextInt(4)));
        }
        return stacks;
    }
}
