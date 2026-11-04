package com.buffspotions.still;

import com.buffspotions.BuffsPotions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The flower-to-effect shuffle for one world, rolled once from the overworld seed. */
public final class FlowerPotAssignments extends SavedData {
    private static final Codec<FlowerPotAssignments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(data -> data.seed),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("plants", Map.of()).forGetter(data -> Map.copyOf(data.plants)),
            Codec.BOOL.optionalFieldOf("ready", false).forGetter(data -> data.ready)
    ).apply(instance, FlowerPotAssignments::from));
    public static final SavedDataType<FlowerPotAssignments> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "flower_pots"),
            level -> new FlowerPotAssignments(),
            level -> CODEC
    );

    private long seed;
    private final Map<String, String> plants = new HashMap<>();
    private boolean ready;
    private boolean migrated;

    private FlowerPotAssignments() {
    }

    public static FlowerPotAssignments get(MinecraftServer server) {
        FlowerPotAssignments data = server.getDataStorage().computeIfAbsent(TYPE);
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            data.bind(overworld.getSeed());
        }
        if (data.migrated) {
            data.migrated = false;
            data.setDirty();
        }
        return data;
    }

    public long seed() {
        return seed;
    }

    public boolean ready() {
        return ready;
    }

    public Map<String, String> plants() {
        return plants;
    }

    private void bind(long worldSeed) {
        if (ready) {
            return;
        }
        seed = worldSeed;
        plants.clear();
        plants.putAll(FlowerPotBrewing.roll(worldSeed));
        ready = true;
        setDirty();
    }

    private static FlowerPotAssignments from(long seed, Map<String, String> plants, boolean ready) {
        FlowerPotAssignments data = new FlowerPotAssignments();
        data.seed = seed;
        data.plants.putAll(plants);
        data.ready = ready;
        data.migrated = retire(data.plants);
        return data;
    }

    /**
     * Saved worlds keep their shuffle. Quiet and riot plants brew heights and depths.
     * Flower Walking plants brew Wind Charged. The random effect that used to belong to
     * open eyeblossom moves to closed eyeblossom. Blindness becomes Stacking, and a
     * missing golden dandelion brews Cracking.
     */
    public static boolean retire(Map<String, String> plants) {
        boolean changed = false;
        for (Map.Entry<String, String> entry : plants.entrySet()) {
            if ("buffs_potions:quieting".equals(entry.getValue())) {
                entry.setValue("buffs_potions:heights");
                changed = true;
            } else if ("buffs_potions:rioting".equals(entry.getValue())) {
                entry.setValue("buffs_potions:depths");
                changed = true;
            } else if ("buffs_potions:flower_walking".equals(entry.getValue())) {
                entry.setValue("minecraft:wind_charged");
                changed = true;
            } else if ("minecraft:blindness".equals(entry.getValue())) {
                entry.setValue("buffs_potions:block_stacking");
                changed = true;
            }
        }
        if (!plants.containsKey("minecraft:golden_dandelion")) {
            plants.put("minecraft:golden_dandelion", "buffs_potions:block_cracking");
            changed = true;
        }
        String open = FlowerPotBrewing.OPEN_EYEBLOSSOM;
        String closed = "minecraft:closed_eyeblossom";
        if (plants.containsKey(open) && !plants.containsKey(closed)) {
            plants.put(closed, plants.remove(open));
            changed = true;
        }
        return changed;
    }
}
