package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.AnchoredEnderFlagonBlock;
import com.buffspotions.flagon.EnderFlagonBlock;
import com.buffspotions.flagon.FlagonBlock;
import com.buffspotions.shrieker.CalibratedSculkShriekerBlock;
import com.buffspotions.stack.BrewingStackBlock;
import com.buffspotions.still.BrewingStillBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BuffsPotions.MOD_ID);

    public static final DeferredBlock<FlagonBlock> FLAGON = BLOCKS.registerBlock(
            "flagon",
            FlagonBlock::new,
            properties -> properties
                    .strength(0.0F, 0.0F)
                    .noOcclusion()
                    .sound(SoundType.DECORATED_POT)
                    .forceSolidOn()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final DeferredBlock<EnderFlagonBlock> ENDER_FLAGON = BLOCKS.registerBlock(
            "ender_flagon",
            EnderFlagonBlock::new,
            properties -> properties
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(22.5F, 600.0F)
                    .noOcclusion()
                    .sound(SoundType.STONE)
                    .forceSolidOn()
                    .pushReaction(PushReaction.BLOCK)
    );

    public static final DeferredBlock<AnchoredEnderFlagonBlock> ANCHORED_ENDER_FLAGON = BLOCKS.registerBlock(
            "anchored_ender_flagon",
            AnchoredEnderFlagonBlock::new,
            properties -> properties
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(22.5F, 600.0F)
                    .noOcclusion()
                    .sound(SoundType.STONE)
                    .forceSolidOn()
                    .pushReaction(PushReaction.BLOCK)
    );

    public static final DeferredBlock<BrewingStillBlock> BREWING_STILL = BLOCKS.registerBlock(
            "brewing_still",
            BrewingStillBlock::new,
            properties -> properties
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .randomTicks()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(BrewingStillBlock.LIT) ? 13 : 0)
    );

    public static final DeferredBlock<CalibratedSculkShriekerBlock> CALIBRATED_SCULK_SHRIEKER = BLOCKS.registerBlock(
            "calibrated_sculk_shrieker",
            CalibratedSculkShriekerBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)
    );

    public static final DeferredBlock<BrewingStackBlock> BREWING_STACK = BLOCKS.registerBlock(
            "brewing_stack",
            BrewingStackBlock::new,
            properties -> properties
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
    );

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {
    }
}
