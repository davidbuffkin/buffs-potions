package com.buffspotions.flagon;

import com.buffspotions.registry.ModItems;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class FlagonDispense {
    private FlagonDispense() {
    }

    public static void register(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DispenserBlock.registerBehavior(ModItems.FLAGON.get(), new ShulkerBoxDispenseBehavior());
        });
    }
}
