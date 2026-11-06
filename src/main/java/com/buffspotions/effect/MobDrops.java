package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;

/**
 * The item that stands in for a mob when brewing incarnation or seeking.
 * The config lists every mob. An empty item means that mob cannot be identified.
 */
public final class MobDrops {
    private static final Map<String, String> DEFAULT_ITEMS = Map.ofEntries(
            Map.entry("minecraft:armadillo", "minecraft:armadillo_scute"),
            Map.entry("minecraft:bat", "buffs_potions:wing"),
            Map.entry("minecraft:bee", "minecraft:honeycomb"),
            Map.entry("minecraft:blaze", "minecraft:blaze_rod"),
            Map.entry("minecraft:breeze", "minecraft:breeze_rod"),
            Map.entry("minecraft:cave_spider", "minecraft:string"),
            Map.entry("minecraft:chicken", "minecraft:chicken"),
            Map.entry("minecraft:cod", "minecraft:cod"),
            Map.entry("minecraft:cow", "minecraft:beef"),
            Map.entry("minecraft:creeper", "minecraft:gunpowder"),
            Map.entry("minecraft:drowned", "minecraft:copper_ingot"),
            Map.entry("minecraft:enderman", "minecraft:ender_pearl"),
            Map.entry("minecraft:evoker", "minecraft:totem_of_undying"),
            Map.entry("minecraft:ghast", "minecraft:ghast_tear"),
            Map.entry("minecraft:glow_squid", "minecraft:glow_ink_sac"),
            Map.entry("minecraft:goat", "minecraft:goat_horn"),
            Map.entry("minecraft:guardian", "minecraft:prismarine_shard"),
            Map.entry("minecraft:magma_cube", "minecraft:magma_cream"),
            Map.entry("minecraft:nautilus", "minecraft:nautilus_shell"),
            Map.entry("minecraft:phantom", "minecraft:phantom_membrane"),
            Map.entry("minecraft:pig", "minecraft:porkchop"),
            Map.entry("minecraft:pufferfish", "minecraft:pufferfish"),
            Map.entry("minecraft:rabbit", "minecraft:rabbit_hide"),
            Map.entry("minecraft:salmon", "minecraft:salmon"),
            Map.entry("minecraft:sheep", "minecraft:mutton"),
            Map.entry("minecraft:shulker", "minecraft:shulker_shell"),
            Map.entry("minecraft:skeleton", "minecraft:bone"),
            Map.entry("minecraft:slime", "minecraft:slime_ball"),
            Map.entry("minecraft:spider", "minecraft:spider_eye"),
            Map.entry("minecraft:squid", "minecraft:ink_sac"),
            Map.entry("minecraft:tropical_fish", "minecraft:tropical_fish"),
            Map.entry("minecraft:turtle", "minecraft:turtle_scute"),
            Map.entry("minecraft:witch", "minecraft:glowstone_dust"),
            Map.entry("minecraft:wither_skeleton", "minecraft:coal"),
            Map.entry("minecraft:zombie", "minecraft:rotten_flesh"),
            Map.entry("minecraft:zombified_piglin", "minecraft:gold_nugget"),
            Map.entry("buffs_potions:crab", "buffs_potions:crab_claw")
    );

    private static Map<Item, EntityType<?>> drops;
    private static boolean sawConfig;

    private MobDrops() {
    }

    public static Optional<EntityType<?>> type(Holder<Item> item) {
        return type(item.value());
    }

    public static Optional<EntityType<?>> type(Item item) {
        return Optional.ofNullable(drops().get(item));
    }

    public static List<String> defaultLines() {
        Map<String, String> lines = new TreeMap<>();
        boolean registryReady = false;
        try {
            for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.listElements().toList()) {
                Identifier id = holder.unwrapKey().map(key -> key.identifier()).orElse(null);
                if (id == null || !isMob(holder.value(), id)) {
                    continue;
                }
                registryReady = true;
                lines.put(id.toString(), knownItem(id.toString()));
            }
        } catch (RuntimeException ignored) {
            registryReady = false;
        }
        if (!registryReady) {
            for (String entity : DEFAULT_ITEMS.keySet()) {
                lines.put(entity, knownItem(entity));
            }
        }
        lines.putIfAbsent("buffs_potions:crab", knownItem("buffs_potions:crab"));
        List<String> result = new ArrayList<>();
        for (var entry : lines.entrySet()) {
            result.add(entry.getKey() + "=" + entry.getValue());
        }
        return List.copyOf(result);
    }

    private static Map<Item, EntityType<?>> drops() {
        boolean loaded = BuffsPotionsConfig.SPEC.isLoaded();
        if (drops == null || (loaded && !sawConfig)) {
            drops = build();
            sawConfig = loaded;
        }
        return drops;
    }

    private static Map<Item, EntityType<?>> build() {
        Map<Item, EntityType<?>> map = new HashMap<>();
        for (String line : BuffsPotionsConfig.mobItems()) {
            int split = line.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String entityId = line.substring(0, split).trim();
            String itemId = line.substring(split + 1).trim();
            if (itemId.isEmpty()) {
                continue;
            }
            try {
                var types = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId));
                var items = BuiltInRegistries.ITEM.get(Identifier.parse(itemId));
                if (types.isPresent() && items.isPresent()) {
                    map.put(items.get().value(), types.get().value());
                }
            } catch (RuntimeException ignored) {
                // A bad config line is skipped rather than breaking incarnation.
            }
        }
        return Map.copyOf(map);
    }

    private static boolean isMob(EntityType<?> type, Identifier id) {
        if (type.getCategory() != MobCategory.MISC) {
            return true;
        }
        if (DEFAULT_ITEMS.containsKey(id.toString())) {
            return true;
        }
        String egg = id.getNamespace() + ":" + id.getPath() + "_spawn_egg";
        try {
            return BuiltInRegistries.ITEM.get(Identifier.parse(egg)).isPresent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String knownItem(String entityId) {
        String item = DEFAULT_ITEMS.get(entityId);
        if (item == null || item.isEmpty()) {
            return "";
        }
        if (item.startsWith("buffs_potions:")) {
            return item;
        }
        try {
            if (BuiltInRegistries.ITEM.get(Identifier.parse("minecraft:stone")).isPresent()
                    && BuiltInRegistries.ITEM.get(Identifier.parse(item)).isEmpty()) {
                return "";
            }
        } catch (RuntimeException ignored) {
            return "";
        }
        return item;
    }
}
