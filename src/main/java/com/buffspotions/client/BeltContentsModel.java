package com.buffspotions.client;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.mixin.client.ItemStackRenderStateAccess;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class BeltContentsModel implements ItemModel {
    private final ItemModel closed;
    private final ItemModel open;
    /** The open frame is drawn after the dose, so the bottle shows through the frame. */
    private final boolean coverDose;
    /** Inventory icons only. In the hand this shift reads as the dose sitting outside the belt. */
    private static final float COVER_FORWARD = 0.25F;
    /** Shown over the closed belt once it holds any bottle and the active slot is empty. */
    private final @Nullable ItemModel oneBottle;
    /** Shown over the closed belt once it holds five bottles and the active slot is empty. */
    private final @Nullable ItemModel fiveBottles;

    public BeltContentsModel(
            ItemModel closed,
            ItemModel open,
            boolean coverDose,
            @Nullable ItemModel oneBottle,
            @Nullable ItemModel fiveBottles
    ) {
        this.closed = closed;
        this.open = open;
        this.coverDose = coverDose;
        this.oneBottle = oneBottle;
        this.fiveBottles = fiveBottles;
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        ItemStack dose = BeltContents.get(item, BeltContents.activeSlot(item));
        boolean showDose = !dose.isEmpty() && !(dose.getItem() instanceof PotionBeltItem);
        if (!showDose) {
            this.closed.update(output, item, resolver, displayContext, level, owner, seed);
            if (dose.isEmpty() && (this.oneBottle != null || this.fiveBottles != null)) {
                int stored = storedBottles(item);
                if (stored >= 1 && this.oneBottle != null) {
                    this.oneBottle.update(output, item, resolver, displayContext, level, owner, seed);
                }
                if (stored >= 5 && this.fiveBottles != null) {
                    this.fiveBottles.update(output, item, resolver, displayContext, level, owner, seed);
                }
            }
            return;
        }
        if (this.coverDose) {
            resolver.appendItemLayers(output, dose, displayContext, level, owner, seed);
            int coverStart = ((ItemStackRenderStateAccess) output).buffsPotions$activeLayerCount();
            this.open.update(output, item, resolver, displayContext, level, owner, seed);
            if (displayContext == ItemDisplayContext.GUI) {
                bringCoverForward(output, coverStart);
            }
            return;
        }
        this.open.update(output, item, resolver, displayContext, level, owner, seed);
        resolver.appendItemLayers(output, dose, displayContext, level, owner, seed);
    }

    /** The cover is a flat icon in the same plane as the dose, so translucent doses paint over it. */
    private static void bringCoverForward(ItemStackRenderState output, int coverStart) {
        ItemStackRenderStateAccess access = (ItemStackRenderStateAccess) output;
        ItemStackRenderState.LayerRenderState[] layers = access.buffsPotions$layers();
        int count = access.buffsPotions$activeLayerCount();
        for (int layer = coverStart; layer < count; layer++) {
            List<BakedQuad> quads = layers[layer].prepareQuadList();
            for (int index = 0; index < quads.size(); index++) {
                quads.set(index, forward(quads.get(index)));
            }
        }
    }

    private static BakedQuad forward(BakedQuad quad) {
        return new BakedQuad(
                nudge(quad.position0()),
                nudge(quad.position1()),
                nudge(quad.position2()),
                nudge(quad.position3()),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                quad.materialInfo(),
                quad.bakedNormals(),
                quad.bakedColors()
        );
    }

    private static Vector3fc nudge(Vector3fc position) {
        return new Vector3f(position.x(), position.y(), position.z() + COVER_FORWARD);
    }

    /** Occupied belt slots, other than a nested belt. Each stored potion or glass bottle counts as one. */
    private static int storedBottles(ItemStack belt) {
        int limit = Math.min(BeltEnchantments.capacity(belt), BeltContents.MAX_SLOTS);
        int count = 0;
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stored = BeltContents.get(belt, slot);
            if (!stored.isEmpty() && !(stored.getItem() instanceof PotionBeltItem)) {
                count++;
            }
        }
        return count;
    }

    public record Unbaked(
            ItemModel.Unbaked closed,
            ItemModel.Unbaked open,
            boolean coverDose,
            Optional<ItemModel.Unbaked> oneBottle,
            Optional<ItemModel.Unbaked> fiveBottles
    ) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemModels.CODEC.fieldOf("closed").forGetter(Unbaked::closed),
                ItemModels.CODEC.fieldOf("open").forGetter(Unbaked::open),
                Codec.BOOL.optionalFieldOf("cover_dose", false).forGetter(Unbaked::coverDose),
                ItemModels.CODEC.optionalFieldOf("one_bottle").forGetter(Unbaked::oneBottle),
                ItemModels.CODEC.optionalFieldOf("five_bottles").forGetter(Unbaked::fiveBottles)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.closed.resolveDependencies(resolver);
            this.open.resolveDependencies(resolver);
            this.oneBottle.ifPresent(model -> model.resolveDependencies(resolver));
            this.fiveBottles.ifPresent(model -> model.resolveDependencies(resolver));
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new BeltContentsModel(
                    this.closed.bake(context, transformation),
                    this.open.bake(context, transformation),
                    this.coverDose,
                    this.oneBottle.map(model -> model.bake(context, transformation)).orElse(null),
                    this.fiveBottles.map(model -> model.bake(context, transformation)).orElse(null)
            );
        }
    }
}
