package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCriteria {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, BuffsPotions.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> EXPELLIARMUS =
            TRIGGERS.register("expelliarmus", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BELT_DRINK =
            TRIGGERS.register("belt_drink", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> FLAGON_HELD_DRINK =
            TRIGGERS.register("flagon_held_drink", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> FLAGON_PLACED_DRINK =
            TRIGGERS.register("flagon_placed_drink", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> CAULDRON_WATER =
            TRIGGERS.register("cauldron_water", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> MUNDANE_STILL =
            TRIGGERS.register("mundane_still", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> VERMILLION =
            TRIGGERS.register("vermillion", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BELLOWS_BOOST =
            TRIGGERS.register("bellows_boost", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BELLOWS_GOLEM =
            TRIGGERS.register("bellows_golem", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> SUSPICIOUS_BREW =
            TRIGGERS.register("suspicious_brew", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> MUNDANE_LUCK =
            TRIGGERS.register("mundane_luck", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> REMEDY_STILL =
            TRIGGERS.register("remedy_still", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> PASTEURIZATION =
            TRIGGERS.register("pasteurization", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> LEVEL_TWO =
            TRIGGERS.register("level_two", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> EXTENDED =
            TRIGGERS.register("extended", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BEGONE =
            TRIGGERS.register("begone", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> CONFUSE_RAY =
            TRIGGERS.register("confuse_ray", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> INSIGHT_THREE =
            TRIGGERS.register("insight_three", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> NIGHT_VISION_FAR =
            TRIGGERS.register("night_vision_far", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> STRENGTH_MEDIUM =
            TRIGGERS.register("strength_medium", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> LATENT_STAND =
            TRIGGERS.register("latent_stand", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> ESSENCE_STILL =
            TRIGGERS.register("essence_still", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> PLAYER_ESSENCE =
            TRIGGERS.register("player_essence", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> HALLOWEEN =
            TRIGGERS.register("halloween", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> HARD_TIMES =
            TRIGGERS.register("hard_times", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> NETHERITE_ESSENCE =
            TRIGGERS.register("netherite_essence", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> HOT_SWAPPING =
            TRIGGERS.register("hot_swapping", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> GLASS_MENAGERIE =
            TRIGGERS.register("glass_menagerie", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> PERFECT_TIMING =
            TRIGGERS.register("perfect_timing", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> AIR_BENDER =
            TRIGGERS.register("air_bender", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> PROTRACTED =
            TRIGGERS.register("protracted", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> AMPLIFIED =
            TRIGGERS.register("amplified", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> REACH_FOUR =
            TRIGGERS.register("reach_four", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> STACKS_ON_STACKS =
            TRIGGERS.register("stacks_on_stacks", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> WHOS_HUNGRY =
            TRIGGERS.register("whos_hungry", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> SWEET_TOOTH =
            TRIGGERS.register("sweet_tooth", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> ELIXIR_OF_LIFE =
            TRIGGERS.register("elixir_of_life", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> MOBS_ON_DEMAND =
            TRIGGERS.register("mobs_on_demand", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> TIP_JAR =
            TRIGGERS.register("tip_jar", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> NETHER_AGAIN =
            TRIGGERS.register("nether_again", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> ANCHORS_AWEIGH =
            TRIGGERS.register("anchors_aweigh", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> DONT_COUNT_YOUR_CHICKENS =
            TRIGGERS.register("dont_count_your_chickens", PlayerTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, PlayerTrigger> BEFORE_THEY_HATCH =
            TRIGGERS.register("before_they_hatch", PlayerTrigger::new);

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }

    private ModCriteria() {
    }
}
