package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.potion.DrinkableAwkwardRecipe;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.PotionTypeRecipe;
import com.buffspotions.potion.WardingRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class ModPotions {
    private static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, BuffsPotions.MOD_ID);

    public static final DeferredHolder<Potion, Potion> MILK = POTIONS.register(
            "milk",
            () -> new Potion("milk", new MobEffectInstance(ModEffects.MILK, 1))
    );
    public static final DeferredHolder<Potion, Potion> INSIGHT = POTIONS.register(
            "insight",
            () -> new Potion("insight", new MobEffectInstance(ModEffects.INSIGHTFUL, BuffsPotionsConfig.ticks("insight", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_INSIGHT = POTIONS.register(
            "long_insight",
            () -> new Potion("insight", new MobEffectInstance(ModEffects.INSIGHTFUL, BuffsPotionsConfig.ticks("insight", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_INSIGHT = POTIONS.register(
            "strong_insight",
            () -> new Potion("insight", new MobEffectInstance(ModEffects.INSIGHTFUL, BuffsPotionsConfig.ticks("insight", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> INSTANT_CONFUSION = POTIONS.register(
            "instant_confusion",
            () -> new Potion("instant_confusion", new MobEffectInstance(ModEffects.INSTANT_CONFUSION, 1))
    );
    public static final DeferredHolder<Potion, Potion> REACHING = POTIONS.register(
            "reaching",
            () -> new Potion("reaching", new MobEffectInstance(ModEffects.REACHING, BuffsPotionsConfig.ticks("reaching", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_REACHING = POTIONS.register(
            "long_reaching",
            () -> new Potion("reaching", new MobEffectInstance(ModEffects.REACHING, BuffsPotionsConfig.ticks("reaching", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_REACHING = POTIONS.register(
            "strong_reaching",
            () -> new Potion("reaching", new MobEffectInstance(ModEffects.REACHING, BuffsPotionsConfig.ticks("reaching", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> FINESSE = POTIONS.register(
            "finesse",
            () -> new Potion("finesse", new MobEffectInstance(ModEffects.FINESSE, BuffsPotionsConfig.ticks("finesse", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_FINESSE = POTIONS.register(
            "long_finesse",
            () -> new Potion("finesse", new MobEffectInstance(ModEffects.FINESSE, BuffsPotionsConfig.ticks("finesse", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_FINESSE = POTIONS.register(
            "strong_finesse",
            () -> new Potion("finesse", new MobEffectInstance(ModEffects.FINESSE, BuffsPotionsConfig.ticks("finesse", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> FUMBLING = POTIONS.register(
            "fumbling",
            () -> new Potion("fumbling", new MobEffectInstance(ModEffects.FUMBLING, BuffsPotionsConfig.ticks("fumbling", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_FUMBLING = POTIONS.register(
            "long_fumbling",
            () -> new Potion("fumbling", new MobEffectInstance(ModEffects.FUMBLING, BuffsPotionsConfig.ticks("fumbling", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_FUMBLING = POTIONS.register(
            "strong_fumbling",
            () -> new Potion("fumbling", new MobEffectInstance(ModEffects.FUMBLING, BuffsPotionsConfig.ticks("fumbling", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> WIND_JUMPING = POTIONS.register(
            "wind_jumping",
            () -> new Potion("wind_jumping", new MobEffectInstance(ModEffects.WIND_JUMPING, BuffsPotionsConfig.ticks("wind_jumping", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_WIND_JUMPING = POTIONS.register(
            "long_wind_jumping",
            () -> new Potion("wind_jumping", new MobEffectInstance(ModEffects.WIND_JUMPING, BuffsPotionsConfig.ticks("wind_jumping", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_WIND_JUMPING = POTIONS.register(
            "strong_wind_jumping",
            () -> new Potion("wind_jumping", new MobEffectInstance(ModEffects.WIND_JUMPING, BuffsPotionsConfig.ticks("wind_jumping", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> FLOWER_WALKING = POTIONS.register(
            "flower_walking",
            () -> new Potion("flower_walking", new MobEffectInstance(ModEffects.BLOOMFOOTED, BuffsPotionsConfig.ticks("flower_walking", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_FLOWER_WALKING = POTIONS.register(
            "long_flower_walking",
            () -> new Potion("flower_walking", new MobEffectInstance(ModEffects.BLOOMFOOTED, BuffsPotionsConfig.ticks("flower_walking", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_FLOWER_WALKING = POTIONS.register(
            "strong_flower_walking",
            () -> new Potion("flower_walking", new MobEffectInstance(ModEffects.BLOOMFOOTED, BuffsPotionsConfig.ticks("flower_walking", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> HARVESTING = POTIONS.register(
            "harvesting",
            () -> new Potion("harvesting", new MobEffectInstance(ModEffects.GREEN_THUMB, BuffsPotionsConfig.ticks("harvesting", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_HARVESTING = POTIONS.register(
            "long_harvesting",
            () -> new Potion("harvesting", new MobEffectInstance(ModEffects.GREEN_THUMB, BuffsPotionsConfig.ticks("harvesting", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_HARVESTING = POTIONS.register(
            "strong_harvesting",
            () -> new Potion("harvesting", new MobEffectInstance(ModEffects.GREEN_THUMB, BuffsPotionsConfig.ticks("harvesting", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> RAIN_CALLING = POTIONS.register(
            "rain_calling",
            () -> new Potion("rain_calling", new MobEffectInstance(ModEffects.INSTANT_RAIN, 1))
    );
    public static final DeferredHolder<Potion, Potion> SUN_CALLING = POTIONS.register(
            "sun_calling",
            () -> new Potion("sun_calling", new MobEffectInstance(ModEffects.INSTANT_SUN, 1))
    );
    public static final DeferredHolder<Potion, Potion> HEIGHTS = POTIONS.register(
            "heights",
            () -> new Potion("heights", new MobEffectInstance(ModEffects.INSTANT_ASCENT, 1))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_HEIGHTS = POTIONS.register(
            "strong_heights",
            () -> new Potion("heights", new MobEffectInstance(ModEffects.INSTANT_ASCENT, 1, 1))
    );
    public static final DeferredHolder<Potion, Potion> DEPTHS = POTIONS.register(
            "depths",
            () -> new Potion("depths", new MobEffectInstance(ModEffects.INSTANT_DESCENT, 1))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_DEPTHS = POTIONS.register(
            "strong_depths",
            () -> new Potion("depths", new MobEffectInstance(ModEffects.INSTANT_DESCENT, 1, 1))
    );
    public static final DeferredHolder<Potion, Potion> QUIETING = POTIONS.register(
            "quieting",
            () -> new Potion("quieting", new MobEffectInstance(ModEffects.QUIET, BuffsPotionsConfig.ticks("quieting", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_QUIETING = POTIONS.register(
            "long_quieting",
            () -> new Potion("quieting", new MobEffectInstance(ModEffects.QUIET, BuffsPotionsConfig.ticks("quieting", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_QUIETING = POTIONS.register(
            "strong_quieting",
            () -> new Potion("quieting", new MobEffectInstance(ModEffects.QUIET, BuffsPotionsConfig.ticks("quieting", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> RIOTING = POTIONS.register(
            "rioting",
            () -> new Potion("rioting", new MobEffectInstance(ModEffects.RIOTOUS, BuffsPotionsConfig.ticks("rioting", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_RIOTING = POTIONS.register(
            "long_rioting",
            () -> new Potion("rioting", new MobEffectInstance(ModEffects.RIOTOUS, BuffsPotionsConfig.ticks("rioting", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_RIOTING = POTIONS.register(
            "strong_rioting",
            () -> new Potion("rioting", new MobEffectInstance(ModEffects.RIOTOUS, BuffsPotionsConfig.ticks("rioting", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> PARTIAL_VISITATION = POTIONS.register(
            "partial_visitation",
            () -> new Potion("partial_visitation", new MobEffectInstance(ModEffects.DISLOCATED, BuffsPotionsConfig.ticks("partial_visitation", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_PARTIAL_VISITATION = POTIONS.register(
            "long_partial_visitation",
            () -> new Potion("partial_visitation", new MobEffectInstance(ModEffects.DISLOCATED, BuffsPotionsConfig.ticks("partial_visitation", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_PARTIAL_VISITATION = POTIONS.register(
            "strong_partial_visitation",
            () -> new Potion("partial_visitation", new MobEffectInstance(ModEffects.DISLOCATED, BuffsPotionsConfig.ticks("partial_visitation", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> SUSPICIOUS_BREW = POTIONS.register(
            "suspicious_brew",
            () -> new Potion(
                    "suspicious_brew",
                    new MobEffectInstance(ModEffects.SUSPICIOUS_BREW, 1, 0, false, false, false)
            )
    );
    public static final DeferredHolder<Potion, Potion> LATENT_EYEDENTIFICATION = POTIONS.register(
            "latent_eyedentification",
            () -> new Potion("latent_eyedentification")
    );
    public static final DeferredHolder<Potion, Potion> ESSENCE = POTIONS.register(
            "essence",
            () -> new Potion("essence")
    );
    public static final DeferredHolder<Potion, Potion> BLOCK_PERCEPTION = POTIONS.register(
            "block_perception",
            () -> new Potion("block_perception", new MobEffectInstance(ModEffects.BLOCK_SENSE, BuffsPotionsConfig.ticks("block_perception", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_BLOCK_PERCEPTION = POTIONS.register(
            "long_block_perception",
            () -> new Potion("block_perception", new MobEffectInstance(ModEffects.BLOCK_SENSE, BuffsPotionsConfig.ticks("block_perception", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_BLOCK_PERCEPTION = POTIONS.register(
            "strong_block_perception",
            () -> new Potion("block_perception", new MobEffectInstance(ModEffects.BLOCK_SENSE, BuffsPotionsConfig.ticks("block_perception", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> BLOCK_STACKING = POTIONS.register(
            "block_stacking",
            () -> new Potion("block_stacking", new MobEffectInstance(ModEffects.BLOCK_STACKED, BuffsPotionsConfig.ticks("block_stacking", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_BLOCK_STACKING = POTIONS.register(
            "long_block_stacking",
            () -> new Potion("block_stacking", new MobEffectInstance(ModEffects.BLOCK_STACKED, BuffsPotionsConfig.ticks("block_stacking", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_BLOCK_STACKING = POTIONS.register(
            "strong_block_stacking",
            () -> new Potion("block_stacking", new MobEffectInstance(ModEffects.BLOCK_STACKED, BuffsPotionsConfig.ticks("block_stacking", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> BLOCK_CRACKING = POTIONS.register(
            "block_cracking",
            () -> new Potion("block_cracking", new MobEffectInstance(ModEffects.BLOCK_CRACKED, BuffsPotionsConfig.ticks("block_cracking", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_BLOCK_CRACKING = POTIONS.register(
            "long_block_cracking",
            () -> new Potion("block_cracking", new MobEffectInstance(ModEffects.BLOCK_CRACKED, BuffsPotionsConfig.ticks("block_cracking", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_BLOCK_CRACKING = POTIONS.register(
            "strong_block_cracking",
            () -> new Potion("block_cracking", new MobEffectInstance(ModEffects.BLOCK_CRACKED, BuffsPotionsConfig.ticks("block_cracking", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> ITEM_EATING = POTIONS.register(
            "item_eating",
            () -> new Potion("item_eating", new MobEffectInstance(ModEffects.ITEM_EATER, BuffsPotionsConfig.ticks("item_eating", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_ITEM_EATING = POTIONS.register(
            "long_item_eating",
            () -> new Potion("item_eating", new MobEffectInstance(ModEffects.ITEM_EATER, BuffsPotionsConfig.ticks("item_eating", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_ITEM_EATING = POTIONS.register(
            "strong_item_eating",
            () -> new Potion("item_eating", new MobEffectInstance(ModEffects.ITEM_EATER, BuffsPotionsConfig.ticks("item_eating", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> INCARNATION = POTIONS.register(
            "incarnation",
            () -> new Potion("incarnation", new MobEffectInstance(ModEffects.INSTANT_INCARNATION, 1))
    );
    public static final DeferredHolder<Potion, Potion> FORMATION = POTIONS.register(
            "formation",
            () -> new Potion("formation", new MobEffectInstance(ModEffects.INSTANT_FORMATION, 1))
    );
    public static final DeferredHolder<Potion, Potion> ITEM_VOMITING = POTIONS.register(
            "item_vomiting",
            () -> new Potion("item_vomiting", new MobEffectInstance(ModEffects.ITEM_VOMITING, BuffsPotionsConfig.ticks("item_vomiting", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_ITEM_VOMITING = POTIONS.register(
            "long_item_vomiting",
            () -> new Potion("item_vomiting", new MobEffectInstance(ModEffects.ITEM_VOMITING, BuffsPotionsConfig.ticks("item_vomiting", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_ITEM_VOMITING = POTIONS.register(
            "strong_item_vomiting",
            () -> new Potion("item_vomiting", new MobEffectInstance(ModEffects.ITEM_VOMITING, BuffsPotionsConfig.ticks("item_vomiting", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> SEEKING = POTIONS.register(
            "seeking",
            () -> new Potion("seeking", new MobEffectInstance(ModEffects.SEEKING, BuffsPotionsConfig.ticks("seeking", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_SEEKING = POTIONS.register(
            "long_seeking",
            () -> new Potion("seeking", new MobEffectInstance(ModEffects.SEEKING, BuffsPotionsConfig.ticks("seeking", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_SEEKING = POTIONS.register(
            "strong_seeking",
            () -> new Potion("seeking", new MobEffectInstance(ModEffects.SEEKING, BuffsPotionsConfig.ticks("seeking", "strongDuration"), 1))
    );
    public static final DeferredHolder<Potion, Potion> DISGUISE = POTIONS.register(
            "disguise",
            () -> new Potion("disguise", new MobEffectInstance(ModEffects.DISGUISE, BuffsPotionsConfig.ticks("disguise", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_DISGUISE = POTIONS.register(
            "long_disguise",
            () -> new Potion("disguise", new MobEffectInstance(ModEffects.DISGUISE, BuffsPotionsConfig.ticks("disguise", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> WARDING = POTIONS.register(
            "warding",
            () -> new Potion("warding", new MobEffectInstance(ModEffects.STOPPERED, BuffsPotionsConfig.ticks("warding", "baseDuration")))
    );
    public static final DeferredHolder<Potion, Potion> LONG_WARDING = POTIONS.register(
            "long_warding",
            () -> new Potion("warding", new MobEffectInstance(ModEffects.STOPPERED, BuffsPotionsConfig.ticks("warding", "longDuration")))
    );
    public static final DeferredHolder<Potion, Potion> STRONG_WEAKNESS = POTIONS.register(
            "strong_weakness",
            () -> new Potion(
                    "weakness",
                    new MobEffectInstance(MobEffects.WEAKNESS, BuffsPotionsConfig.ticks("weakness", "strongDuration"), 1)
            )
    );
    /** 9600 is vanilla's stored length for one redstone extend of a 3:00 potion. Durations then follow the configured base. */
    private static final int EXTENDED_MARKER = 9600;
    public static final DeferredHolder<Potion, Potion> LONG_WIND_CHARGED = extended("long_wind_charged", "wind_charged", MobEffects.WIND_CHARGED);
    public static final DeferredHolder<Potion, Potion> LONG_WEAVING = extended("long_weaving", "weaving", MobEffects.WEAVING);
    public static final DeferredHolder<Potion, Potion> LONG_OOZING = extended("long_oozing", "oozing", MobEffects.OOZING);
    public static final DeferredHolder<Potion, Potion> LONG_INFESTED = extended("long_infested", "infested", MobEffects.INFESTED);

    private static DeferredHolder<Potion, Potion> extended(String id, String name, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return POTIONS.register(id, () -> new Potion(name, new MobEffectInstance(effect, EXTENDED_MARKER)));
    }

    private ModPotions() {
    }

    public static void register(IEventBus bus) {
        POTIONS.register(bus);
    }

    @SubscribeEvent
    public static void hideRetiredPotions(BuildCreativeModeTabContentsEvent event) {
        event.removeIf(ModPotions::retiredPotion, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::thickPotion, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::unsplashableWeather, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::unsplashableLatent, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::plainLatent, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::plainEssence, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::incarnationArrow, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::lingeringIncarnation, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::formationArrow, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        event.removeIf(ModPotions::lingeringFormation, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    private static boolean thickPotion(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.THICK);
    }

    private static boolean retiredPotion(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(QUIETING.unwrapKey())
                        || potion.unwrapKey().equals(LONG_QUIETING.unwrapKey())
                        || potion.unwrapKey().equals(STRONG_QUIETING.unwrapKey())
                        || potion.unwrapKey().equals(RIOTING.unwrapKey())
                        || potion.unwrapKey().equals(LONG_RIOTING.unwrapKey())
                        || potion.unwrapKey().equals(STRONG_RIOTING.unwrapKey())
                        || potion.unwrapKey().equals(FLOWER_WALKING.unwrapKey())
                        || potion.unwrapKey().equals(LONG_FLOWER_WALKING.unwrapKey())
                        || potion.unwrapKey().equals(STRONG_FLOWER_WALKING.unwrapKey()))
                .orElse(false);
    }

    private static boolean lingeringIncarnation(ItemStack stack) {
        if (!stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(INCARNATION.unwrapKey()))
                .orElse(false);
    }

    private static boolean lingeringFormation(ItemStack stack) {
        return lingeringPotion(stack, FORMATION);
    }

    private static boolean formationArrow(ItemStack stack) {
        return tippedArrow(stack, FORMATION);
    }

    private static boolean lingeringPotion(ItemStack stack, DeferredHolder<Potion, Potion> potion) {
        if (!stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(holder -> holder.unwrapKey().equals(potion.unwrapKey()))
                .orElse(false);
    }

    private static boolean tippedArrow(ItemStack stack, DeferredHolder<Potion, Potion> potion) {
        if (!stack.is(Items.TIPPED_ARROW)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(holder -> holder.unwrapKey().equals(potion.unwrapKey()))
                .orElse(false);
    }

    private static boolean incarnationArrow(ItemStack stack) {
        if (!stack.is(Items.TIPPED_ARROW)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(INCARNATION.unwrapKey()))
                .orElse(false);
    }

    /**
     * The potions tab lists every registered potion. Latent eyedentification has no effect color,
     * so that copy looks like a water bottle. The colored bottle in this mod's tab is the real one.
     */
    private static boolean plainLatent(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!contents.is(LATENT_EYEDENTIFICATION)) {
            return false;
        }
        return contents.customColor().orElse(-1) != EyedBrewing.LATENT_COLOR;
    }

    /** Essence has no effect color, so the potions-tab copy looks like water. The tinted bottle is the catalog one. */
    private static boolean plainEssence(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!contents.is(ESSENCE)) {
            return false;
        }
        return contents.customColor().orElse(-1) != EssenceSubject.ITEM_COLOR;
    }

    /** Latent eyedentification stays drinkable. Splash, lingering, and tipped arrows are not offered. */
    private static boolean unsplashableLatent(ItemStack stack) {
        if (stack.is(Items.POTION)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(LATENT_EYEDENTIFICATION);
    }

    /** Rain and sun calling stay drinkable. Splash, lingering, and tipped arrows are not offered. */
    private static boolean unsplashableWeather(ItemStack stack) {
        if (stack.is(Items.POTION)) {
            return false;
        }
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(RAIN_CALLING.unwrapKey())
                        || potion.unwrapKey().equals(SUN_CALLING.unwrapKey()))
                .orElse(false);
    }

    @SubscribeEvent
    public static void registerBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder brewing = event.getBuilder();
        brewing.addMix(Potions.AWKWARD, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS);
        brewing.addMix(Potions.STRONG_SWIFTNESS, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_SLOWNESS);
        brewing.addMix(Potions.WEAKNESS, Items.GLOWSTONE_DUST, STRONG_WEAKNESS);
        brewing.addMix(Potions.WIND_CHARGED, Items.REDSTONE, LONG_WIND_CHARGED);
        brewing.addMix(Potions.WEAVING, Items.REDSTONE, LONG_WEAVING);
        brewing.addMix(Potions.OOZING, Items.REDSTONE, LONG_OOZING);
        brewing.addMix(Potions.INFESTED, Items.REDSTONE, LONG_INFESTED);
        brewing.addStartMix(Items.EXPERIENCE_BOTTLE, INSIGHT);
        brewing.addMix(INSIGHT, Items.REDSTONE, LONG_INSIGHT);
        brewing.addMix(INSIGHT, Items.GLOWSTONE_DUST, STRONG_INSIGHT);
        brewing.addMix(INSIGHT, Items.FERMENTED_SPIDER_EYE, INSTANT_CONFUSION);
        brewing.addMix(LONG_INSIGHT, Items.FERMENTED_SPIDER_EYE, INSTANT_CONFUSION);
        brewing.addMix(STRONG_INSIGHT, Items.FERMENTED_SPIDER_EYE, INSTANT_CONFUSION);
        brewing.addStartMix(ModItems.CRAB_CLAW.get(), REACHING);
        brewing.addMix(REACHING, Items.REDSTONE, LONG_REACHING);
        brewing.addMix(REACHING, Items.GLOWSTONE_DUST, STRONG_REACHING);
        brewing.addStartMix(Items.RABBIT_HIDE, FINESSE);
        brewing.addMix(FINESSE, Items.REDSTONE, LONG_FINESSE);
        brewing.addMix(FINESSE, Items.GLOWSTONE_DUST, STRONG_FINESSE);
        brewing.addMix(FINESSE, Items.FERMENTED_SPIDER_EYE, FUMBLING);
        brewing.addMix(LONG_FINESSE, Items.FERMENTED_SPIDER_EYE, FUMBLING);
        brewing.addMix(STRONG_FINESSE, Items.FERMENTED_SPIDER_EYE, FUMBLING);
        brewing.addMix(FUMBLING, Items.REDSTONE, LONG_FUMBLING);
        brewing.addMix(FUMBLING, Items.GLOWSTONE_DUST, STRONG_FUMBLING);
        brewing.addRecipe(new PotionTypeRecipe(Potions.AWKWARD, ModItems.WING.get(), WIND_JUMPING));
        brewing.addMix(WIND_JUMPING, Items.REDSTONE, LONG_WIND_JUMPING);
        brewing.addMix(WIND_JUMPING, Items.GLOWSTONE_DUST, STRONG_WIND_JUMPING);
        brewing.addStartMix(Items.BRICK, BLOCK_STACKING);
        brewing.addMix(BLOCK_STACKING, Items.REDSTONE, LONG_BLOCK_STACKING);
        brewing.addMix(BLOCK_STACKING, Items.GLOWSTONE_DUST, STRONG_BLOCK_STACKING);
        brewing.addStartMix(Items.DIAMOND, BLOCK_CRACKING);
        brewing.addMix(BLOCK_CRACKING, Items.REDSTONE, LONG_BLOCK_CRACKING);
        brewing.addMix(BLOCK_CRACKING, Items.GLOWSTONE_DUST, STRONG_BLOCK_CRACKING);
        brewing.addStartMix(Items.BONE_MEAL, HARVESTING);
        brewing.addMix(HARVESTING, Items.REDSTONE, LONG_HARVESTING);
        brewing.addMix(HARVESTING, Items.GLOWSTONE_DUST, STRONG_HARVESTING);
        brewing.addStartMix(Items.HEART_OF_THE_SEA, RAIN_CALLING);
        brewing.addMix(RAIN_CALLING, Items.FERMENTED_SPIDER_EYE, SUN_CALLING);
        brewing.addStartMix(Items.GLOW_BERRIES, HEIGHTS);
        brewing.addMix(HEIGHTS, Items.GLOWSTONE_DUST, STRONG_HEIGHTS);
        brewing.addMix(HEIGHTS, Items.FERMENTED_SPIDER_EYE, DEPTHS);
        brewing.addMix(STRONG_HEIGHTS, Items.FERMENTED_SPIDER_EYE, STRONG_DEPTHS);
        brewing.addMix(DEPTHS, Items.GLOWSTONE_DUST, STRONG_DEPTHS);
        brewing.addRecipe(new WardingRecipe());
        brewing.addMix(WARDING, Items.REDSTONE, LONG_WARDING);
        brewing.addRecipe(new DrinkableAwkwardRecipe(Potions.AWKWARD, Items.CHORUS_FLOWER, PARTIAL_VISITATION));
        brewing.addMix(PARTIAL_VISITATION, Items.REDSTONE, LONG_PARTIAL_VISITATION);
        brewing.addMix(PARTIAL_VISITATION, Items.GLOWSTONE_DUST, STRONG_PARTIAL_VISITATION);
        EyedBrewing.addRecipes(brewing);
    }
}
