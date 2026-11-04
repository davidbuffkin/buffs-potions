package com.buffspotions.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * What a latent potion was brewed into. A named name tag identifies a player.
 * Anything else identifies that item, and block items can become perception or stacking.
 */
public record EssenceSubject(
        Optional<Holder<Item>> item,
        Optional<String> playerName,
        Optional<Holder<EntityType<?>>> entity
) {
    public static final EssenceSubject EMPTY = new EssenceSubject(Optional.empty(), Optional.empty(), Optional.empty());
    public static final int ITEM_COLOR = 0xB7A0FF;
    public static final int PLAYER_COLOR = 0x5B8CFF;
    public static final Codec<EssenceSubject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("item").forGetter(EssenceSubject::item),
            Codec.STRING.optionalFieldOf("player").forGetter(EssenceSubject::playerName),
            BuiltInRegistries.ENTITY_TYPE.holderByNameCodec().optionalFieldOf("entity").forGetter(EssenceSubject::entity)
    ).apply(instance, EssenceSubject::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceSubject> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM)),
            EssenceSubject::item,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            EssenceSubject::playerName,
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ENTITY_TYPE)),
            EssenceSubject::entity,
            EssenceSubject::new
    );

    public EssenceSubject {
        playerName = playerName.map(String::trim).filter(name -> !name.isEmpty());
        if (playerName.isPresent()) {
            item = Optional.empty();
            entity = Optional.empty();
        }
    }

    public static EssenceSubject ofItem(Holder<Item> item) {
        return new EssenceSubject(Optional.of(item), Optional.empty(), Optional.empty());
    }

    public static EssenceSubject ofPlayer(String name) {
        return new EssenceSubject(Optional.empty(), Optional.of(name), Optional.empty());
    }

    public static EssenceSubject ofEntity(Holder<EntityType<?>> entity) {
        return new EssenceSubject(Optional.empty(), Optional.empty(), Optional.of(entity));
    }

    public Optional<Holder<Block>> block() {
        return item.flatMap(holder -> holder.value() instanceof BlockItem blockItem
                ? Optional.of(blockItem.getBlock().builtInRegistryHolder())
                : Optional.empty());
    }

    public Component label() {
        if (playerName.isPresent()) {
            return Component.literal(playerName.get()).withStyle(ChatFormatting.BLUE);
        }
        if (entity.isPresent()) {
            return entity.get().value().getDescription();
        }
        return item.map(holder -> Component.translatable(holder.value().getDescriptionId()))
                .orElseGet(() -> Component.literal("Unknown"));
    }

    public Component blockLabel() {
        return block().<Component>map(holder -> holder.value().getName()).orElseGet(this::label);
    }

    public int liquidColor() {
        if (playerName.isPresent()) {
            return PLAYER_COLOR;
        }
        return item.map(holder -> EssenceGraphicColor.of(holder.value())).orElse(ITEM_COLOR);
    }
}
