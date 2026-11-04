package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonAnchor;
import com.buffspotions.flagon.FlagonContents;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, BuffsPotions.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> BELT_CONTENTS =
            COMPONENTS.<ItemContainerContents>registerComponentType("belt_contents", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .ignoreSwapAnimation());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> BELT_BOTTLES =
            COMPONENTS.<ItemContainerContents>registerComponentType("belt_bottles", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .ignoreSwapAnimation());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ACTIVE_BELT_SLOT =
            COMPONENTS.<Integer>registerComponentType("active_belt_slot", builder -> builder
                    .persistent(Codec.intRange(0, 17))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BELT_DATA_VERSION =
            COMPONENTS.<Integer>registerComponentType("belt_data_version", builder -> builder
                    .persistent(Codec.intRange(1, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .ignoreSwapAnimation());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlagonAnchor>> FLAGON_ANCHOR =
            COMPONENTS.<FlagonAnchor>registerComponentType("flagon_anchor", builder -> builder
                    .persistent(FlagonAnchor.CODEC)
                    .networkSynchronized(FlagonAnchor.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FlagonContents>> FLAGON_CONTENTS =
            COMPONENTS.<FlagonContents>registerComponentType("flagon_contents", builder -> builder
                    .persistent(FlagonContents.CODEC)
                    .networkSynchronized(FlagonContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EssenceSubject>> ESSENCE =
            COMPONENTS.<EssenceSubject>registerComponentType("essence", builder -> builder
                    .persistent(EssenceSubject.CODEC)
                    .networkSynchronized(EssenceSubject.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> MUNDANE =
            COMPONENTS.<Boolean>registerComponentType("mundane", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BELT_ADVANCE =
            COMPONENTS.<Integer>registerComponentType("belt_advance", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .ignoreSwapAnimation());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Holder<MobEffect>>> REMEDY =
            COMPONENTS.<Holder<MobEffect>>registerComponentType("remedy", builder -> builder
                    .persistent(BuiltInRegistries.MOB_EFFECT.holderByNameCodec())
                    .networkSynchronized(ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BELT_ROW =
            COMPONENTS.<Integer>registerComponentType("belt_row", builder -> builder
                    .persistent(Codec.intRange(0, 1))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .ignoreSwapAnimation());

    /** Absent means a Creeper Belt is in splash mode. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BELT_THROWING =
            COMPONENTS.<Boolean>registerComponentType("belt_throwing", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .ignoreSwapAnimation());

    /** 0 drink, 1 splash, 2 linger. Creeper belts only use 0 and 1. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BELT_MODE =
            COMPONENTS.<Integer>registerComponentType("belt_mode", builder -> builder
                    .persistent(Codec.intRange(0, 2))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .ignoreSwapAnimation());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {
    }
}
