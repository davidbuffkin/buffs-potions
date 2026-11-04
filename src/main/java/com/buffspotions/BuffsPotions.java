package com.buffspotions;

import com.buffspotions.cauldron.CauldronFills;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.flagon.AnchoredFlagons;
import com.buffspotions.flagon.FlagonDispense;
import com.buffspotions.registry.ModBlockEntities;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModBlocks;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModEntities;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModParticles;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModMenus;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.registry.ModSounds;
import com.buffspotions.registry.ModStructureProcessors;
import com.buffspotions.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(BuffsPotions.MOD_ID)
public final class BuffsPotions {
    public static final String MOD_ID = "buffs_potions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BuffsPotions(IEventBus modBus, ModContainer container) {
        ModCriteria.register(modBus);
        ModEffects.register(modBus);
        ModPotions.register(modBus);
        ModDataComponents.register(modBus);
        ModAttachments.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModSounds.register(modBus);
        ModParticles.register(modBus);
        ModItems.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        ModStructureProcessors.register(modBus);
        ModStructures.register(modBus);
        modBus.addListener(FlagonDispense::register);
        modBus.addListener(AnchoredFlagons::registerTickets);
        modBus.addListener(CauldronFills::register);
        container.registerConfig(ModConfig.Type.SERVER, BuffsPotionsConfig.SPEC);
    }
}
