package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.mojang.serialization.Codec;
import com.buffspotions.bellows.GolemBellowsJob;
import com.buffspotions.effect.BoundlessNight;
import com.buffspotions.effect.DislocationAnchor;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.ParameterDose;
import com.buffspotions.effect.SeekAim;
import com.buffspotions.effect.TrackedBlock;
import com.buffspotions.effect.TrackedItem;
import com.buffspotions.equipment.BeltSlotData;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.belt.BeltUseState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BuffsPotions.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BeltSlotData>> BELT_SLOT =
            ATTACHMENTS.register("belt_slot", () -> AttachmentType
                    .builder(() -> new BeltSlotData())
                    .serialize(BeltSlotData.CODEC)
                    .sync((holder, player) -> holder == player, BeltSlotData.STREAM_CODEC)
                    .build());

    /** Increments each time a bellows golem stokes a machine. The client starts a head spin when it changes. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> GOLEM_HEAD_SPIN =
            ATTACHMENTS.register("golem_head_spin", () -> AttachmentType.builder(() -> 0L)
                    .sync((holder, player) -> true, ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemBellowsJob>> GOLEM_BELLOWS =
            ATTACHMENTS.register("golem_bellows", () -> AttachmentType
                    .builder(GolemBellowsJob::new)
                    .serialize(GolemBellowsJob.CODEC.fieldOf("job"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<DislocationAnchor>> DISLOCATION =
            ATTACHMENTS.register("dislocation", () -> AttachmentType
                    .builder(() -> new DislocationAnchor(Level.OVERWORLD, Util.NIL_UUID, 0.0, 0.0, 0.0, 0.0F, 0.0F, 0))
                    .serialize(DislocationAnchor.CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<java.util.UUID>> ENDER_SHADOW =
            ATTACHMENTS.register("ender_shadow", () -> AttachmentType
                    .builder(() -> Util.NIL_UUID)
                    .serialize(UUIDUtil.CODEC.fieldOf("owner"))
                    .sync((holder, player) -> true, UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TrackedBlock>> BLOCK_SENSE =
            ATTACHMENTS.register("block_sense", () -> AttachmentType
                    .builder(() -> TrackedBlock.EMPTY)
                    .serialize(TrackedBlock.CODEC.fieldOf("block"))
                    .sync((holder, player) -> holder == player, TrackedBlock.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TrackedBlock>> BLOCK_STACKED =
            ATTACHMENTS.register("block_stacked", () -> AttachmentType
                    .builder(() -> TrackedBlock.EMPTY)
                    .serialize(TrackedBlock.CODEC.fieldOf("block"))
                    .sync((holder, player) -> holder == player, TrackedBlock.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TrackedBlock>> BLOCK_CRACKED =
            ATTACHMENTS.register("block_cracked", () -> AttachmentType
                    .builder(() -> TrackedBlock.EMPTY)
                    .serialize(TrackedBlock.CODEC.fieldOf("block"))
                    .sync((holder, player) -> holder == player, TrackedBlock.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TrackedItem>> ITEM_VOMITING =
            ATTACHMENTS.register("item_vomiting", () -> AttachmentType
                    .builder(() -> TrackedItem.EMPTY)
                    .serialize(TrackedItem.CODEC.fieldOf("item"))
                    .sync((holder, player) -> holder == player, TrackedItem.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EssenceSubject>> SEEKING =
            ATTACHMENTS.register("seeking", () -> AttachmentType
                    .builder(() -> EssenceSubject.EMPTY)
                    .serialize(EssenceSubject.CODEC.fieldOf("subject"))
                    .sync((holder, player) -> holder == player, EssenceSubject.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SeekAim>> SEEK_AIM =
            ATTACHMENTS.register("seek_aim", () -> AttachmentType
                    .builder(() -> SeekAim.NONE)
                    .serialize(SeekAim.CODEC.fieldOf("aim"))
                    .sync((holder, player) -> holder == player, SeekAim.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<ParameterDose>>> PARAMETER_DOSES =
            ATTACHMENTS.register("parameter_doses", () -> AttachmentType
                    .<List<ParameterDose>>builder(() -> List.of())
                    .serialize(ParameterDose.CODEC.listOf().fieldOf("doses"))
                    .sync(
                            (holder, player) -> holder == player,
                            ByteBufCodecs.<net.minecraft.network.RegistryFriendlyByteBuf, ParameterDose>list()
                                    .apply(ParameterDose.STREAM_CODEC)
                    )
                    .build());

    /** The player name whose skin is worn. Empty when no disguise is active. Synced to every client that can see the wearer. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> DISGUISE =
            ATTACHMENTS.register("disguise", () -> AttachmentType
                    .builder(() -> "")
                    .serialize(Codec.STRING.fieldOf("name"))
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<TrackedItem>> ITEM_EATER =
            ATTACHMENTS.register("item_eater", () -> AttachmentType
                    .builder(() -> TrackedItem.EMPTY)
                    .serialize(TrackedItem.CODEC.fieldOf("item"))
                    .sync((holder, player) -> holder == player, TrackedItem.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EssenceSubject>> LINGERING_SUBJECT =
            ATTACHMENTS.register("lingering_subject", () -> AttachmentType
                    .builder(() -> EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(Items.STONE)))
                    .serialize(EssenceSubject.CODEC.fieldOf("subject"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FlagonContents>> ENDER_FLAGON =
            ATTACHMENTS.register("ender_flagon", () -> AttachmentType
                    .builder(() -> FlagonContents.EMPTY)
                    .serialize(FlagonContents.CODEC.fieldOf("contents"))
                    .copyOnDeath()
                    .sync((holder, player) -> holder == player, FlagonContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> AIR_JUMPS =
            ATTACHMENTS.register("air_jumps", () -> AttachmentType.builder(() -> 0).build());

    /** Crops harvested during the current mundane Harvesting drink. {@code -1} means the count is not running. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> VERMILLION =
            ATTACHMENTS.register("vermillion", () -> AttachmentType
                    .builder(() -> -1)
                    .serialize(Codec.INT.fieldOf("crops"))
                    .build());

    /** Items that have been in the player's inventory. Essence cycles through these. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<Item>>> HELD_ITEMS =
            ATTACHMENTS.register("held_items", () -> AttachmentType.<Set<Item>>builder(() -> new HashSet<>())
                    .serialize(BuiltInRegistries.ITEM.byNameCodec().listOf().xmap(
                            list -> (Set<Item>) new HashSet<>(list),
                            set -> new ArrayList<>(set)
                    ).fieldOf("items"))
                    .sync(
                            (holder, player) -> holder == player,
                            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.registry(Registries.ITEM))
                    )
                    .build());

    /**
     * Mob effects the player has held in any potion form.
     * One effect covers every variant, including parameterized subjects.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Set<String>>> KNOWN_EFFECTS =
            ATTACHMENTS.register("known_effects", () -> AttachmentType.<Set<String>>builder(() -> new HashSet<>())
                    .serialize(Codec.STRING.listOf().xmap(
                            list -> (Set<String>) new HashSet<>(list),
                            set -> new ArrayList<>(set)
                    ).fieldOf("effects"))
                    .sync(
                            (holder, player) -> holder == player,
                            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8)
                    )
                    .build());

    /** Set once the recipe book has been read for effects this player already unlocked. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> KNOWN_EFFECTS_SEEDED =
            ATTACHMENTS.register("known_effects_seeded", () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("seeded"))
                    .build());

    /** A chick from a 64-egg hatch. It still pushes, and it does not take cramming damage. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> NO_CRAMMING =
            ATTACHMENTS.register("no_cramming", () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("exempt"))
                    .build());

    /** The player has carried a potion of latent eyedentification. Essence recipes stay unlocked after that. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> HELD_LATENT =
            ATTACHMENTS.register("held_latent", () -> AttachmentType.builder(() -> Boolean.FALSE)
                    .serialize(Codec.BOOL.fieldOf("held"))
                    .sync((holder, player) -> holder == player, ByteBufCodecs.BOOL)
                    .build());

    /** Last redstone strength applied to an armor stand's pose. {@code -1} until the stand has been seen once. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ARMOR_POSE_SIGNAL =
            ATTACHMENTS.register("armor_pose_signal", () -> AttachmentType.builder(() -> -1)
                    .serialize(Codec.INT.fieldOf("signal"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> BELT_KEY =
            ATTACHMENTS.register("belt_key", () -> AttachmentType.builder(() -> Boolean.FALSE).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> ARROW_TIP =
            ATTACHMENTS.register("arrow_tip", () -> AttachmentType
                    .builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.MAP_CODEC)
                    .build());

    /** A splash potion released where this arrow lands. The arrow itself stays a normal arrow. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> ARROW_SPLASH =
            ATTACHMENTS.register("arrow_splash", () -> AttachmentType
                    .builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.MAP_CODEC)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BoundlessNight.Watch>> BOUNDLESS_WATCH =
            ATTACHMENTS.register("boundless_watch", () -> AttachmentType
                    .builder(() -> BoundlessNight.Watch.IDLE)
                    .serialize(BoundlessNight.Watch.CODEC.fieldOf("watch"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BeltUseState>> BELT_USE =
            ATTACHMENTS.register("belt_use", () -> AttachmentType
                    .builder(() -> new BeltUseState())
                    .serialize(BeltUseState.CODEC.fieldOf("use"))
                    .sync((holder, player) -> holder == player, BeltUseState.STREAM_CODEC)
                    .build());

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    private ModAttachments() {
    }
}
