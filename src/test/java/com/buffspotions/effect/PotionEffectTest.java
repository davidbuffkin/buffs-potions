package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.effect.AirStep;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EssenceNames;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.stack.StackBrewing;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.still.FlowerPotAssignments;
import com.buffspotions.still.StillBrewing;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.still.FlowerPotBrewing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirStepTest {
    @Test
    void airJumpsDoubleAcrossFourLevels() {
        assertEquals(12, AirStep.limit(0));
        assertEquals(24, AirStep.limit(1));
        assertEquals(48, AirStep.limit(2));
        assertEquals(96, AirStep.limit(3));
        assertEquals(96, AirStep.limit(8));
    }
}

class DurabilityEffectsTest {
    @Test
    void finesseAndFumblingScaleWithPotionLevel() {
        assertEquals(0.5, DurabilityEffects.skipChance(0), 1.0e-9);
        assertEquals(0.75, DurabilityEffects.skipChance(1), 1.0e-9);
        assertEquals(2, DurabilityEffects.damageMultiplier(0));
        assertEquals(4, DurabilityEffects.damageMultiplier(1));
        assertEquals(0.025, BloomfootedEffect.sproutChance(0), 1.0e-9);
        assertEquals(0.05, BloomfootedEffect.sproutChance(1), 1.0e-9);
        assertEquals(0.15, BloomfootedEffect.INTERESTING_CHANCE, 1.0e-9);
    }
}

class CrowdTuningTest {
    @Test
    void quietAndRiotScaleAcrossFourLevels() {
        assertEquals(0.50F, CrowdTuning.volumeScale(CrowdTuning.Kind.QUIET, 0), 1.0e-6);
        assertEquals(0.20F, CrowdTuning.volumeScale(CrowdTuning.Kind.QUIET, 1), 1.0e-6);
        assertEquals(0.10F, CrowdTuning.volumeScale(CrowdTuning.Kind.QUIET, 2), 1.0e-6);
        assertEquals(0.05F, CrowdTuning.volumeScale(CrowdTuning.Kind.QUIET, 3), 1.0e-6);
        assertEquals(0.05F, CrowdTuning.volumeScale(CrowdTuning.Kind.QUIET, 9), 1.0e-6);
        assertEquals(1.25F, CrowdTuning.volumeScale(CrowdTuning.Kind.RIOTOUS, 0), 1.0e-6);
        assertEquals(2.00F, CrowdTuning.volumeScale(CrowdTuning.Kind.RIOTOUS, 3), 1.0e-6);
        assertEquals(0.50F, CrowdTuning.skulkIgnoreChance(0), 1.0e-6);
        assertEquals(0.80F, CrowdTuning.skulkIgnoreChance(1), 1.0e-6);
        assertEquals(0.90F, CrowdTuning.skulkIgnoreChance(2), 1.0e-6);
        assertEquals(0.95F, CrowdTuning.skulkIgnoreChance(3), 1.0e-6);
        assertEquals(24.0, CrowdTuning.safeDistance(CrowdTuning.Kind.NONE, 0), 1.0e-9);
        assertEquals(32.0, CrowdTuning.safeDistance(CrowdTuning.Kind.QUIET, 0), 1.0e-9);
        assertEquals(56.0, CrowdTuning.safeDistance(CrowdTuning.Kind.QUIET, 9), 1.0e-9);
        assertEquals(20.0, CrowdTuning.safeDistance(CrowdTuning.Kind.RIOTOUS, 0), 1.0e-9);
        assertEquals(8.0, CrowdTuning.safeDistance(CrowdTuning.Kind.RIOTOUS, 3), 1.0e-9);
        assertEquals(53, CrowdTuning.localCap(MobCategory.MONSTER, CrowdTuning.Kind.QUIET, 0));
        assertEquals(18, CrowdTuning.localCap(MobCategory.MONSTER, CrowdTuning.Kind.QUIET, 3));
        assertEquals(15, CrowdTuning.localCap(MobCategory.CREATURE, CrowdTuning.Kind.QUIET, 0));
        assertEquals(140, CrowdTuning.localCap(MobCategory.MONSTER, CrowdTuning.Kind.RIOTOUS, 3));
        assertEquals(3, CrowdTuning.localCap(MobCategory.CREATURE, CrowdTuning.Kind.RIOTOUS, 3));
        assertEquals(3, CrowdTuning.spawnerNearbyCap(6, CrowdTuning.Kind.QUIET, 0));
        assertEquals(1, CrowdTuning.spawnerNearbyCap(6, CrowdTuning.Kind.QUIET, 3));
        assertEquals(9, CrowdTuning.spawnerNearbyCap(6, CrowdTuning.Kind.RIOTOUS, 0));
        assertEquals(18, CrowdTuning.spawnerNearbyCap(6, CrowdTuning.Kind.RIOTOUS, 3));
        assertEquals(0.75F, CrowdTuning.volumeScale(0, 0), 1.0e-6);
        assertEquals(0.50F, CrowdTuning.volumeScale(0, -1), 1.0e-6);
        assertEquals(1.25F, CrowdTuning.volumeScale(-1, 0), 1.0e-6);
        assertEquals(70, CrowdTuning.localCap(MobCategory.MONSTER, 0, 0));
        assertEquals(53, CrowdTuning.localCap(MobCategory.MONSTER, 0, -1));
        assertEquals(88, CrowdTuning.localCap(MobCategory.MONSTER, -1, 0));
        assertEquals(28.0, CrowdTuning.safeDistance(0, 0), 1.0e-9);
        assertEquals(6, CrowdTuning.spawnerNearbyCap(6, 0, 0));
    }
}

@ExtendWith(EphemeralTestServerProvider.class)
class PotionBrewingTest {
    @Test
    void newPotionsBrewFromTheRequestedIngredients(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);

        assertPotion(brewing.mix(new ItemStack(Items.RABBIT_HIDE), awkward.copy()), ModPotions.FINESSE);
        assertPotion(
                brewing.mix(new ItemStack(Items.REDSTONE), potion(ModPotions.FINESSE)),
                ModPotions.LONG_FINESSE
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(ModPotions.FINESSE)),
                ModPotions.STRONG_FINESSE
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), potion(ModPotions.FINESSE)),
                ModPotions.FUMBLING
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), potion(ModPotions.STRONG_FINESSE)),
                ModPotions.FUMBLING
        );

        ItemStack windJumping = brewing.mix(new ItemStack(ModItems.WING.get()), awkward.copy());
        assertPotion(windJumping, ModPotions.WIND_JUMPING);
        assertTrue(brewing.mix(new ItemStack(ModItems.WING.get()), splashAwkward()).is(Items.SPLASH_POTION));
        assertPotion(brewing.mix(new ItemStack(Items.BREEZE_ROD), awkward.copy()), Potions.WIND_CHARGED);
        assertPotion(
                brewing.mix(new ItemStack(Items.REDSTONE), potion(ModPotions.WIND_JUMPING)),
                ModPotions.LONG_WIND_JUMPING
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(ModPotions.WIND_JUMPING)),
                ModPotions.STRONG_WIND_JUMPING
        );
        ItemStack thicker = StackBrewing.mix(
                brewing, potion(ModPotions.STRONG_WIND_JUMPING), new ItemStack(Items.TORCHFLOWER));
        MobEffectInstance airStep = thicker.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().next();
        assertEquals(2, airStep.getAmplifier());
        assertFalse(brewing.hasMix(awkward, new ItemStack(Items.WIND_CHARGE)));
    }

    @Test
    void weatherPotionsBrewFromTheRequestedIngredients(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);

        assertFalse(brewing.hasMix(awkward, new ItemStack(Items.PINK_PETALS)));
        assertFalse(brewing.hasMix(potion(ModPotions.FLOWER_WALKING), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.FLOWER_WALKING), new ItemStack(Items.GLOWSTONE_DUST)));

        assertPotion(brewing.mix(new ItemStack(Items.HEART_OF_THE_SEA), awkward.copy()), ModPotions.RAIN_CALLING);
        assertFalse(brewing.hasMix(potion(ModPotions.RAIN_CALLING), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.RAIN_CALLING), new ItemStack(Items.GLOWSTONE_DUST)));
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), potion(ModPotions.RAIN_CALLING)),
                ModPotions.SUN_CALLING
        );
        assertFalse(brewing.hasMix(potion(ModPotions.SUN_CALLING), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.SUN_CALLING), new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(potion(ModPotions.RAIN_CALLING), new ItemStack(Items.GUNPOWDER)));
        assertFalse(brewing.hasMix(potion(ModPotions.RAIN_CALLING), new ItemStack(Items.DRAGON_BREATH)));
        assertFalse(brewing.hasMix(potion(ModPotions.SUN_CALLING), new ItemStack(Items.GUNPOWDER)));
        assertFalse(brewing.hasMix(potion(ModPotions.SUN_CALLING), new ItemStack(Items.DRAGON_BREATH)));

        assertPotion(brewing.mix(new ItemStack(Items.GLOW_BERRIES), awkward.copy()), ModPotions.HEIGHTS);
        assertFalse(brewing.hasMix(potion(ModPotions.HEIGHTS), new ItemStack(Items.REDSTONE)));
        assertPotion(
                brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(ModPotions.HEIGHTS)),
                ModPotions.STRONG_HEIGHTS
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), potion(ModPotions.HEIGHTS)),
                ModPotions.DEPTHS
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), potion(ModPotions.STRONG_HEIGHTS)),
                ModPotions.STRONG_DEPTHS
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(ModPotions.DEPTHS)),
                ModPotions.STRONG_DEPTHS
        );
        assertFalse(brewing.hasMix(potion(ModPotions.DEPTHS), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.STRONG_HEIGHTS), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(awkward, new ItemStack(Items.RESIN_CLUMP)));
    }

    @Test
    void quietingAndRiotingAreUnbrewableWhilePartialVisitationRemains(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);

        assertFalse(brewing.hasMix(awkward, new ItemStack(Items.WOOL.white())));
        assertFalse(brewing.hasMix(potion(ModPotions.QUIETING), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.QUIETING), new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(potion(ModPotions.QUIETING), new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        assertFalse(brewing.hasMix(potion(ModPotions.RIOTING), new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(potion(ModPotions.RIOTING), new ItemStack(Items.GLOWSTONE_DUST)));

        assertPotion(brewing.mix(new ItemStack(Items.CHORUS_FLOWER), awkward.copy()), ModPotions.PARTIAL_VISITATION);
        ItemStack splashVisit = brewing.mix(new ItemStack(Items.CHORUS_FLOWER), splashAwkward());
        assertTrue(splashVisit.is(Items.SPLASH_POTION));
        assertPotion(splashVisit, Potions.AWKWARD);
        assertPotion(
                brewing.mix(new ItemStack(Items.REDSTONE), potion(ModPotions.PARTIAL_VISITATION)),
                ModPotions.LONG_PARTIAL_VISITATION
        );
        assertPotion(
                brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(ModPotions.PARTIAL_VISITATION)),
                ModPotions.STRONG_PARTIAL_VISITATION
        );
        ItemStack splashPartial = brewing.mix(new ItemStack(Items.GUNPOWDER), potion(ModPotions.PARTIAL_VISITATION));
        assertTrue(splashPartial.is(Items.SPLASH_POTION));
        assertPotion(splashPartial, ModPotions.PARTIAL_VISITATION);
        ItemStack lingeringPartial = brewing.mix(new ItemStack(Items.DRAGON_BREATH), splashPartial);
        assertTrue(lingeringPartial.is(Items.LINGERING_POTION));
        assertPotion(lingeringPartial, ModPotions.PARTIAL_VISITATION);
        assertTrue(brewing.hasMix(potion(ModPotions.LONG_PARTIAL_VISITATION), new ItemStack(Items.GUNPOWDER)));
        ItemStack splashStrong = brewing.mix(new ItemStack(Items.GUNPOWDER), potion(ModPotions.STRONG_PARTIAL_VISITATION));
        assertTrue(splashStrong.is(Items.SPLASH_POTION));
        assertTrue(brewing.hasMix(splashStrong, new ItemStack(Items.DRAGON_BREATH)));
    }

    @Test
    void beaconOffersReachingAtTierThreeAndStopperedAtTierFour(MinecraftServer server) {
        assertTrue(BeaconBlockEntity.BEACON_EFFECTS.get(2).contains(ModEffects.REACHING));
        assertTrue(BeaconBlockEntity.BEACON_EFFECTS.get(3).contains(ModEffects.STOPPERED));
        assertFalse(BeaconBlockEntity.validateEffects(ModEffects.REACHING, null, 2));
        assertTrue(BeaconBlockEntity.validateEffects(ModEffects.REACHING, null, 3));
        assertTrue(BeaconBlockEntity.validateEffects(ModEffects.REACHING, ModEffects.REACHING, 4));
        assertFalse(BeaconBlockEntity.validateEffects(ModEffects.STOPPERED, null, 4));
        assertTrue(BeaconBlockEntity.validateEffects(MobEffects.STRENGTH, ModEffects.STOPPERED, 4));
        assertTrue(BeaconBlockEntity.validateEffects(ModEffects.REACHING, ModEffects.STOPPERED, 4));
    }

    @Test
    void wardingBrewsFromAMilkBottleAndOnlyExtends(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack milk = PotionContents.createItemStack(Items.POTION, ModPotions.MILK);

        assertPotion(brewing.mix(milk, awkward), ModPotions.WARDING);
        assertPotion(brewing.mix(new ItemStack(Items.REDSTONE), potion(ModPotions.WARDING)), ModPotions.LONG_WARDING);
        assertFalse(brewing.hasMix(potion(ModPotions.WARDING), new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(potion(ModPotions.LONG_WARDING), new ItemStack(Items.GLOWSTONE_DUST)));
    }

    @Test
    void remediesClearTheBrewedEffectAndCanInvertSplashAndLinger(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack milk = PotionContents.createItemStack(Items.POTION, ModPotions.MILK);

        assertFalse(brewing.hasMix(milk, new ItemStack(Items.BLAZE_POWDER)));
        ItemStack strength = StillBrewing.mix(brewing, new ItemStack(Items.BLAZE_POWDER), milk.copy());
        assertTrue(Remedies.effectOf(strength).unwrapKey().equals(MobEffects.STRENGTH.unwrapKey()));
        assertFalse(StillBrewing.hasMix(brewing, strength, new ItemStack(Items.REDSTONE)));
        assertFalse(StillBrewing.hasMix(brewing, strength, new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(StillBrewing.hasMix(brewing, milk, new ItemStack(Items.GLISTERING_MELON_SLICE)));
        assertFalse(StillBrewing.hasMix(brewing, milk, new ItemStack(Items.GLOW_BERRIES)));

        ItemStack weakness = StillBrewing.mix(brewing, new ItemStack(Items.FERMENTED_SPIDER_EYE), strength);
        assertTrue(Remedies.effectOf(weakness).unwrapKey().equals(MobEffects.WEAKNESS.unwrapKey()));

        ItemStack disguise = StillBrewing.mix(brewing, new ItemStack(Items.CARVED_PUMPKIN), milk.copy());
        assertTrue(Remedies.effectOf(disguise).unwrapKey().equals(ModEffects.DISGUISE.unwrapKey()));
        assertEquals(
                "effect.buffs_potions.disguise",
                ((TranslatableContents) ((Component) ((TranslatableContents) Remedies.name(disguise).getContents()).getArgs()[0]).getContents()).getKey()
        );
        assertFalse(StillBrewing.hasMix(brewing, disguise, new ItemStack(Items.FERMENTED_SPIDER_EYE)));

        ItemStack stacking = StillBrewing.mix(brewing, new ItemStack(Items.BRICK), milk.copy());
        assertTrue(Remedies.effectOf(stacking).unwrapKey().equals(ModEffects.BLOCK_STACKED.unwrapKey()));
        assertFalse(StillBrewing.hasMix(brewing, stacking, new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        ItemStack cracking = StillBrewing.mix(brewing, new ItemStack(Items.DIAMOND), milk.copy());
        assertTrue(Remedies.effectOf(cracking).unwrapKey().equals(ModEffects.BLOCK_CRACKED.unwrapKey()));

        assertFalse(brewing.hasMix(strength, new ItemStack(Items.GUNPOWDER)));
        ItemStack splash = StillBrewing.mix(brewing, new ItemStack(Items.GUNPOWDER), strength.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertTrue(Remedies.effectOf(splash).unwrapKey().equals(MobEffects.STRENGTH.unwrapKey()));
        ItemStack lingering = StillBrewing.mix(brewing, new ItemStack(Items.DRAGON_BREATH), splash);
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertTrue(Remedies.effectOf(lingering).unwrapKey().equals(MobEffects.STRENGTH.unwrapKey()));

        assertTrue(StackBrewing.mix(brewing, milk.copy(), new ItemStack(Items.GOLDEN_CARROT)).isEmpty());
        assertTrue(StackBrewing.mix(brewing, milk.copy(), new ItemStack(Items.BLAZE_POWDER)).isEmpty());
        assertTrue(FlagonContents.isDoseItem(strength));
    }

    @Test
    void perceptionAndStackingBrewFromBlockEssences(MinecraftServer server) {
        assertEquals(10, EyedTuning.radius(0));
        assertEquals(13, EyedTuning.radius(1));
        assertEquals(16, EyedTuning.radius(2));
        assertEquals(19, EyedTuning.radius(3));
        assertEquals(19, EyedTuning.radius(7));
        assertEquals(2, EyedTuning.airDistance(0));
        assertEquals(3, EyedTuning.airDistance(1));
        assertEquals(4, EyedTuning.airDistance(2));
        assertEquals(5, EyedTuning.airDistance(3));
        assertEquals(2, EyedTuning.crackDepth(0));
        assertEquals(4, EyedTuning.crackDepth(1));
        assertEquals(0.1F, EyedTuning.eatChance(0), 1.0e-6);
        assertEquals(0.2F, EyedTuning.eatChance(1), 1.0e-6);
        assertEquals(3, EyedTuning.attractDistance(0));
        assertEquals(6, EyedTuning.attractDistance(1));
        assertEquals(11, Insight.reach(0));
        assertEquals(14, Insight.reach(1));
        assertEquals(17, Insight.reach(2));
        assertEquals(20, Insight.reach(3));
        assertEquals(2, EyedTuning.pillarHeight(0));
        assertEquals(4, EyedTuning.pillarHeight(1));
        assertEquals(16, EyedTuning.pillarHeight(3));
        assertEquals(64, EyedTuning.pillarHeight(5));
        assertEquals(64, EyedTuning.pillarHeight(6));
        assertEquals(64, EyedTuning.pillarHeight(99));
        assertEquals(64, EyedTuning.crackDepth(99));
        assertEquals(EyedTuning.MAX_LEVEL, EyedTuning.level(99));
        assertEquals(0.08, EyedTuning.attractSpeed(0.0), 1.0e-9);
        assertEquals(0.88, EyedTuning.attractSpeed(1.0), 1.0e-9);

        EssenceSubject ironDose = EssenceSubject.ofPlayer("iron");
        EssenceSubject diamondDose = EssenceSubject.ofPlayer("diamond");
        var first = ParameterDoses.merge(
                java.util.List.of(),
                new ParameterDose("buffs_potions:item_eater", ironDose, 0, 500)
        );
        var both = ParameterDoses.merge(
                first,
                new ParameterDose("buffs_potions:item_eater", diamondDose, 1, 800)
        );
        assertEquals(2, both.size());
        var refreshed = ParameterDoses.merge(
                both,
                new ParameterDose("buffs_potions:item_eater", ironDose, 2, 400)
        );
        assertEquals(2, refreshed.size());
        ParameterDose kept = refreshed.get(0);
        assertEquals(2, kept.amplifier());
        assertEquals(500, kept.expiresAt());
        assertTrue(ParameterDoses.sameSubject(kept.subject(), ironDose));

        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        assertFalse(brewing.hasMix(potion(Potions.WATER), new ItemStack(Items.ENDER_EYE)));
        assertPotion(brewing.mix(new ItemStack(Items.BRICK), awkward.copy()), ModPotions.BLOCK_STACKING);
        assertPotion(brewing.mix(new ItemStack(Items.DIAMOND), awkward.copy()), ModPotions.BLOCK_CRACKING);
        assertTrue(brewing.isIngredient(new ItemStack(Items.DIAMOND)));
        assertPotion(brewing.mix(new ItemStack(Items.DIAMOND), potion(Potions.WATER)), Potions.MUNDANE);

        ItemStack latent = brewing.mix(new ItemStack(Items.ENDER_EYE), awkward.copy());
        assertPotion(latent, ModPotions.LATENT_EYEDENTIFICATION);
        assertEquals(EyedBrewing.LATENT_COLOR, latent.get(DataComponents.POTION_CONTENTS).customColor().orElseThrow());
        assertFalse(brewing.hasMix(splashAwkward(), new ItemStack(Items.ENDER_EYE)));
        ItemStack splashLatent = brewing.mix(new ItemStack(Items.ENDER_EYE), splashAwkward());
        assertTrue(splashLatent.is(Items.SPLASH_POTION));
        assertPotion(splashLatent, Potions.AWKWARD);
        assertFalse(brewing.hasMix(latent, new ItemStack(Items.GUNPOWDER)));
        assertFalse(brewing.hasMix(latent, new ItemStack(Items.DRAGON_BREATH)));
        ItemStack splashBottle = PotionContents.createItemStack(Items.SPLASH_POTION, ModPotions.LATENT_EYEDENTIFICATION);
        assertFalse(brewing.hasMix(splashBottle, new ItemStack(Items.DRAGON_BREATH)));

        ItemStack stoneEssence = StillBrewing.mix(brewing, new ItemStack(Items.STONE), latent.copy());
        assertFalse(brewing.hasMix(latent, new ItemStack(Items.STONE)));
        assertPotion(stoneEssence, ModPotions.ESSENCE);
        EssenceSubject stone = stoneEssence.get(ModDataComponents.ESSENCE.get());
        assertEquals(Items.STONE, stone.item().orElseThrow().value());
        assertEquals(Blocks.STONE, stone.block().orElseThrow().value());
        int diamondColor = EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND)).liquidColor();
        assertNotEquals(EssenceSubject.ITEM_COLOR, diamondColor);
        assertTrue((diamondColor & 0xFF) > ((diamondColor >> 16) & 0xFF));
        int grass = EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(Items.GRASS_BLOCK)).liquidColor();
        assertTrue(((grass >> 8) & 0xFF) > ((grass >> 16) & 0xFF));
        int leaves = EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(Items.OAK_LEAVES)).liquidColor();
        assertTrue(((leaves >> 8) & 0xFF) > ((leaves >> 16) & 0xFF));
        assertEquals(EssenceSubject.PLAYER_COLOR, EssenceSubject.ofPlayer("Buff").liquidColor());
        assertEquals(
                "item.buffs_potions.named.potion.essence",
                ((TranslatableContents) stoneEssence.getItem().getName(stoneEssence).getContents()).getKey()
        );
        assertEquals(
                "item.buffs_potions.named.flagon.essence",
                ((TranslatableContents) EssenceNames.named(stoneEssence, "flagon").getContents()).getKey()
        );

        ItemStack perception = brewing.mix(new ItemStack(Items.RESIN_CLUMP), stoneEssence.copy());
        assertPotion(perception, ModPotions.BLOCK_PERCEPTION);
        assertEquals(Blocks.STONE, perception.get(ModDataComponents.ESSENCE.get()).block().orElseThrow().value());
        assertEquals(
                "item.buffs_potions.named.potion.perception",
                ((TranslatableContents) perception.getItem().getName(perception).getContents()).getKey()
        );
        ItemStack longer = brewing.mix(new ItemStack(Items.REDSTONE), perception.copy());
        assertPotion(longer, ModPotions.LONG_BLOCK_PERCEPTION);
        assertEquals(Blocks.STONE, longer.get(ModDataComponents.ESSENCE.get()).block().orElseThrow().value());

        assertFalse(brewing.hasMix(stoneEssence, new ItemStack(Items.BRICK)));
        ItemStack stacking = brewing.mix(new ItemStack(Items.BRICK), awkward.copy());
        assertPotion(stacking, ModPotions.BLOCK_STACKING);
        assertTrue(stacking.get(ModDataComponents.ESSENCE.get()) == null);
        ItemStack stronger = brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), stacking.copy());
        assertPotion(stronger, ModPotions.STRONG_BLOCK_STACKING);
        assertFalse(brewing.hasMix(stacking, new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        assertFalse(brewing.hasMix(stronger, new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        ItemStack cracking = brewing.mix(new ItemStack(Items.DIAMOND), awkward.copy());
        assertPotion(cracking, ModPotions.BLOCK_CRACKING);
        assertTrue(cracking.get(ModDataComponents.ESSENCE.get()) == null);
        ItemStack strongCracking = brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), cracking.copy());
        assertPotion(strongCracking, ModPotions.STRONG_BLOCK_CRACKING);
        ItemStack plainMundane = PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
        ItemStack mundaneStacking = StillBrewing.mix(brewing, new ItemStack(Items.BRICK), plainMundane.copy());
        assertTrue(mundaneStacking.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(mundaneStacking, ModPotions.BLOCK_STACKING);
        ItemStack mundaneCracking = StillBrewing.mix(brewing, new ItemStack(Items.DIAMOND), plainMundane.copy());
        assertTrue(mundaneCracking.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(mundaneCracking, ModPotions.BLOCK_CRACKING);
        assertTrue(server.getRecipeManager().byKey(ResourceKey.create(
                Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", "still/mundane/minecraft/brick")
        )).isPresent());
        assertTrue(server.getRecipeManager().byKey(ResourceKey.create(
                Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", "still/mundane/minecraft/diamond")
        )).isPresent());
        assertTrue(server.getRecipeManager().byKey(ResourceKey.create(
                Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", "stack/base/minecraft/brick")
        )).isPresent());
        assertTrue(server.getRecipeManager().byKey(ResourceKey.create(
                Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", "stack/base/minecraft/diamond")
        )).isPresent());
        assertFalse(brewing.hasMix(perception, new ItemStack(Items.FERMENTED_SPIDER_EYE)));

        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), perception.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertPotion(splash, ModPotions.BLOCK_PERCEPTION);
        assertEquals(Blocks.STONE, splash.get(ModDataComponents.ESSENCE.get()).block().orElseThrow().value());

        ItemStack diamond = StillBrewing.mix(brewing, new ItemStack(Items.DIAMOND), latent.copy());
        assertPotion(diamond, ModPotions.ESSENCE);
        assertTrue(diamond.get(ModDataComponents.ESSENCE.get()).block().isEmpty());
        assertFalse(brewing.hasMix(stoneEssence, new ItemStack(Items.GLOW_BERRIES)));
        assertFalse(brewing.hasMix(diamond, new ItemStack(Items.GLOW_BERRIES)));
        assertFalse(brewing.hasMix(diamond, new ItemStack(Items.RESIN_CLUMP)));
        assertFalse(brewing.hasMix(diamond, new ItemStack(Items.BRICK)));
        ItemStack eating = brewing.mix(new ItemStack(Items.COOKIE), diamond.copy());
        assertPotion(eating, ModPotions.ITEM_EATING);
        assertEquals(Items.DIAMOND, eating.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());
        ItemStack stoneEating = brewing.mix(new ItemStack(Items.COOKIE), stoneEssence.copy());
        assertPotion(stoneEating, ModPotions.ITEM_EATING);
        assertEquals(Blocks.STONE, stoneEating.get(ModDataComponents.ESSENCE.get()).block().orElseThrow().value());

        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Steve"));
        ItemStack named = StillBrewing.mix(brewing, tag, latent.copy());
        EssenceSubject player = named.get(ModDataComponents.ESSENCE.get());
        assertEquals("Steve", player.playerName().orElseThrow());
        assertTrue(player.block().isEmpty());
        assertEquals("Steve", player.label().getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), player.label().getStyle().getColor());
        assertFalse(brewing.hasMix(named, new ItemStack(Items.GLOW_BERRIES)));
        assertFalse(brewing.hasMix(named, new ItemStack(Items.RESIN_CLUMP)));
        assertFalse(brewing.hasMix(named, new ItemStack(Items.COOKIE)));

        ItemStack plainTag = StillBrewing.mix(brewing, new ItemStack(Items.NAME_TAG), latent.copy());
        assertEquals(Items.NAME_TAG, plainTag.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());
        assertTrue(plainTag.get(ModDataComponents.ESSENCE.get()).playerName().isEmpty());

        ItemStack notSplashed = brewing.mix(new ItemStack(Items.GUNPOWDER), latent.copy());
        assertTrue(notSplashed.is(Items.POTION));
        assertPotion(notSplashed, ModPotions.LATENT_EYEDENTIFICATION);
        ItemStack gunpowder = StillBrewing.mix(brewing, new ItemStack(Items.GUNPOWDER), latent.copy());
        assertTrue(gunpowder.is(Items.POTION));
        assertPotion(gunpowder, ModPotions.ESSENCE);
        assertEquals(Items.GUNPOWDER, gunpowder.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());

        EyedDisplay.TOOLTIP.set(stone);
        try {
            assertEquals(
                    "effect.buffs_potions.block_sense.named",
                    ((TranslatableContents) PotionContents.getPotionDescription(ModEffects.BLOCK_SENSE, 0).getContents()).getKey()
            );
            assertEquals(
                    "effect.buffs_potions.block_stacked",
                    ((TranslatableContents) PotionContents.getPotionDescription(ModEffects.BLOCK_STACKED, 0).getContents()).getKey()
            );
            assertEquals(
                    "effect.buffs_potions.block_cracked",
                    ((TranslatableContents) PotionContents.getPotionDescription(ModEffects.BLOCK_CRACKED, 0).getContents()).getKey()
            );
            EyedDisplay.TOOLTIP.set(diamond.get(ModDataComponents.ESSENCE.get()));
            assertEquals(
                    "effect.buffs_potions.item_eater.named",
                    ((TranslatableContents) PotionContents.getPotionDescription(ModEffects.ITEM_EATER, 0).getContents()).getKey()
            );
            EyedDisplay.TOOLTIP.set(stone);
            assertEquals(
                    "potion.withAmplifier",
                    ((TranslatableContents) PotionContents.getPotionDescription(ModEffects.BLOCK_STACKED, 1).getContents()).getKey()
            );
        } finally {
            EyedDisplay.TOOLTIP.remove();
        }
    }

    @Test
    void incarnationVomitingAndSeekingBrewFromEssences(MinecraftServer server) {
        assertEquals(5, EyedTuning.vomitRange(0));
        assertEquals(10, EyedTuning.vomitRange(1));
        assertEquals(64, EyedTuning.vomitDepositInterval(0));
        assertEquals(16, EyedTuning.vomitDepositInterval(1));
        assertEquals(4, EyedTuning.vomitDepositInterval(2));
        assertEquals(1, EyedTuning.vomitDepositInterval(3));
        assertEquals(1, EyedTuning.vomitDepositInterval(9));
        assertEquals(16, EyedTuning.vomitThrowInterval(0));
        assertEquals(8, EyedTuning.vomitThrowInterval(1));
        assertEquals(4, EyedTuning.vomitThrowInterval(2));
        assertEquals(2, EyedTuning.vomitThrowInterval(3));
        assertEquals(16, EyedTuning.seekChunks(0));
        assertEquals(32, EyedTuning.seekChunks(1));
        assertEquals(48, EyedTuning.seekChunks(2));
        assertEquals(64, EyedTuning.seekChunks(3));
        assertEquals(64, EyedTuning.seekChunks(7));
        assertEquals(EntityTypes.WITHER_SKELETON, MobDrops.type(Items.COAL).orElseThrow());
        assertEquals(EntityTypes.ZOMBIE, MobDrops.type(Items.ROTTEN_FLESH).orElseThrow());
        assertEquals(EntityTypes.CREEPER, MobDrops.type(Items.GUNPOWDER).orElseThrow());
        assertEquals(EntityTypes.BLAZE, MobDrops.type(Items.BLAZE_ROD).orElseThrow());
        assertTrue(MobDrops.type(Items.DIAMOND).isEmpty());

        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack latent = brewing.mix(new ItemStack(Items.ENDER_EYE), awkward.copy());
        ItemStack flesh = StillBrewing.mix(brewing, new ItemStack(Items.ROTTEN_FLESH), latent.copy());
        ItemStack diamond = StillBrewing.mix(brewing, new ItemStack(Items.DIAMOND), latent.copy());
        ItemStack incarnation = brewing.mix(new ItemStack(Items.ECHO_SHARD), flesh.copy());
        assertPotion(incarnation, ModPotions.INCARNATION);
        assertEquals(Items.ROTTEN_FLESH, incarnation.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());
        assertEquals(
                "item.buffs_potions.named.potion.incarnation",
                ((TranslatableContents) EssenceNames.forStack(incarnation).getContents()).getKey()
        );
        ItemStack unknown = brewing.mix(new ItemStack(Items.ECHO_SHARD), diamond.copy());
        assertPotion(unknown, ModPotions.INCARNATION);
        assertEquals(
                "item.buffs_potions.named.potion.unknown_incarnation",
                ((TranslatableContents) EssenceNames.forStack(unknown).getContents()).getKey()
        );
        ItemStack camel = PotionContents.createItemStack(Items.SPLASH_POTION, ModPotions.INCARNATION);
        camel.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofEntity(EntityTypes.CAMEL.builtInRegistryHolder()));
        assertEquals(
                "item.buffs_potions.named.splash.incarnation",
                ((TranslatableContents) EssenceNames.forStack(camel).getContents()).getKey()
        );
        assertEquals(EntityTypes.CAMEL.getDescription(), ((TranslatableContents) EssenceNames.forStack(camel).getContents()).getArgs()[0]);
        assertFalse(brewing.hasMix(incarnation, new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(incarnation, new ItemStack(Items.GLOWSTONE_DUST)));
        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), incarnation.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertPotion(splash, ModPotions.INCARNATION);
        assertFalse(brewing.hasMix(splash, new ItemStack(Items.DRAGON_BREATH)));

        ItemStack eating = brewing.mix(new ItemStack(Items.COOKIE), diamond.copy());
        ItemStack vomiting = brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), eating.copy());
        assertPotion(vomiting, ModPotions.ITEM_VOMITING);
        assertEquals(Items.DIAMOND, vomiting.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());
        ItemStack strongEating = brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), eating.copy());
        assertPotion(
                brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), strongEating.copy()),
                ModPotions.ITEM_VOMITING
        );
        ItemStack strongVomiting = brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), vomiting.copy());
        assertPotion(strongVomiting, ModPotions.STRONG_ITEM_VOMITING);
        assertEquals(Items.DIAMOND, strongVomiting.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());

        assertFalse(brewing.hasMix(diamond, new ItemStack(Items.LODESTONE)));
        ItemStack stone = StillBrewing.mix(brewing, new ItemStack(Items.STONE), latent.copy());
        ItemStack stoneSeeking = brewing.mix(new ItemStack(Items.LODESTONE), stone.copy());
        assertPotion(stoneSeeking, ModPotions.SEEKING);
        assertEquals(Items.STONE, stoneSeeking.get(ModDataComponents.ESSENCE.get()).item().orElseThrow().value());
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Steve"));
        ItemStack named = StillBrewing.mix(brewing, tag, latent.copy());
        ItemStack nameSeeking = brewing.mix(new ItemStack(Items.LODESTONE), named.copy());
        assertPotion(nameSeeking, ModPotions.SEEKING);
        assertEquals("Steve", nameSeeking.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());
        assertFalse(brewing.hasMix(named, new ItemStack(Items.ECHO_SHARD)));
        ItemStack fleshSeeking = brewing.mix(new ItemStack(Items.LODESTONE), flesh.copy());
        assertPotion(fleshSeeking, ModPotions.SEEKING);
        assertEquals(
                EntityTypes.ZOMBIE.getDescription(),
                ((TranslatableContents) EssenceNames.forStack(fleshSeeking).getContents()).getArgs()[0]
        );
    }

    @Test
    void clericTradesOfferTwoOfThree(MinecraftServer server) {
        assertEquals(List.of(
                "buffs_potions:cleric/1/suspicious_brew_emerald",
                "minecraft:cleric/1/emerald_redstone",
                "minecraft:cleric/1/rotten_flesh_emerald"
        ), clericTradeIds(server, "minecraft:cleric/level_1"));
        assertEquals(List.of(
                "buffs_potions:cleric/2/emerald_gunpowder",
                "minecraft:cleric/2/emerald_lapis_lazuli",
                "minecraft:cleric/2/gold_ingot_emerald"
        ), clericTradeIds(server, "minecraft:cleric/level_2"));
        assertEquals(List.of(
                "buffs_potions:cleric/3/honey_bottle_emerald",
                "minecraft:cleric/3/emerald_glowstone",
                "minecraft:cleric/3/rabbit_foot_emerald"
        ), clericTradeIds(server, "minecraft:cleric/level_3"));

        CompoundTag flesh = clericTrade(server, "minecraft:cleric/1/rotten_flesh_emerald");
        assertEquals(18, flesh.getCompound("wants").orElseThrow().getIntOr("count", 0));

        CompoundTag brew = clericTrade(server, "buffs_potions:cleric/1/suspicious_brew_emerald");
        CompoundTag brewContents = brew.getCompound("wants").orElseThrow()
                .getCompound("components").orElseThrow()
                .getCompound("minecraft:potion_contents").orElseThrow();
        assertEquals("buffs_potions:suspicious_brew", brewContents.getStringOr("potion", ""));
        assertEquals(SuspiciousBrew.COLOR, brewContents.getIntOr("custom_color", 0));
        assertFalse(brew.contains("max_uses"));
        assertFalse(brew.contains("xp"));

        CompoundTag powder = clericTrade(server, "buffs_potions:cleric/2/emerald_gunpowder");
        assertEquals(2, powder.getCompound("gives").orElseThrow().getIntOr("count", 0));
        assertEquals(5, powder.getIntOr("xp", 0));
        assertFalse(powder.contains("max_uses"));

        CompoundTag honey = clericTrade(server, "buffs_potions:cleric/3/honey_bottle_emerald");
        assertEquals("minecraft:honey_bottle", honey.getCompound("wants").orElseThrow().getStringOr("id", ""));
        assertEquals(12, honey.getIntOr("xp", 0));
        assertFalse(honey.contains("max_uses"));
    }

    private static List<String> clericTradeIds(MinecraftServer server, String level) {
        TradeSet set = server.registryAccess()
                .lookupOrThrow(Registries.TRADE_SET)
                .getOrThrow(ResourceKey.create(Registries.TRADE_SET, Identifier.parse(level)))
                .value();
        assertEquals(2, set.calculateNumberOfTrades(null));
        return set.getTrades().stream()
                .map(holder -> holder.unwrapKey().orElseThrow().identifier().toString())
                .sorted()
                .toList();
    }

    private static CompoundTag clericTrade(MinecraftServer server, String id) {
        VillagerTrade trade = server.registryAccess()
                .lookupOrThrow(Registries.VILLAGER_TRADE)
                .getOrThrow(ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.parse(id)))
                .value();
        return (CompoundTag) VillagerTrade.CODEC.encodeStart(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                trade
        ).getOrThrow();
    }

    @Test
    void disguiseBrewsFromANamedPlayerAndOnlyExtends(MinecraftServer server) {
        assertEquals(0xAD5F1F, ModEffects.DISGUISE.value().getColor());
        PotionBrewing brewing = server.potionBrewing();
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack latent = brewing.mix(new ItemStack(Items.ENDER_EYE), awkward.copy());
        ItemStack stone = StillBrewing.mix(brewing, new ItemStack(Items.STONE), latent.copy());
        assertFalse(brewing.hasMix(stone, new ItemStack(Items.CARVED_PUMPKIN)));

        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Steve"));
        ItemStack named = StillBrewing.mix(brewing, tag, latent.copy());
        ItemStack disguise = brewing.mix(new ItemStack(Items.CARVED_PUMPKIN), named.copy());
        assertPotion(disguise, ModPotions.DISGUISE);
        assertEquals("Steve", disguise.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());
        assertEquals(
                "item.buffs_potions.named.potion.disguise",
                ((TranslatableContents) EssenceNames.forStack(disguise).getContents()).getKey()
        );
        assertFalse(brewing.hasMix(disguise, new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(disguise, new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        ItemStack longer = brewing.mix(new ItemStack(Items.REDSTONE), disguise.copy());
        assertPotion(longer, ModPotions.LONG_DISGUISE);
        assertEquals("Steve", longer.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());

        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), disguise.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertPotion(splash, ModPotions.DISGUISE);
        assertEquals("Steve", splash.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());
        ItemStack lingering = brewing.mix(new ItemStack(Items.DRAGON_BREATH), splash.copy());
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertPotion(lingering, ModPotions.DISGUISE);
        assertEquals("Steve", lingering.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());
    }

    @Test
    void durabilityEffectsApplyToDamageableItems(MinecraftServer server) {
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.DIAMOND_CHESTPLATE)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.BOW)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.SHIELD)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.ELYTRA)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.FLINT_AND_STEEL)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.SHEARS)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.BRUSH)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.FISHING_ROD)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.CARROT_ON_A_STICK)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.WARPED_FUNGUS_ON_A_STICK)));
        assertTrue(DurabilityEffects.affects(new ItemStack(Items.WOLF_ARMOR)));
        assertTrue(DurabilityEffects.affects(new ItemStack(ModItems.POTION_BELT.get())));
        assertTrue(DurabilityEffects.affects(new ItemStack(ModItems.FLAGON_BELT.get())));
        assertTrue(DurabilityEffects.affects(new ItemStack(ModItems.SPLASH_FLAGON_BELT.get())));
        assertTrue(DurabilityEffects.affects(new ItemStack(ModItems.LINGERING_FLAGON_BELT.get())));
        assertFalse(DurabilityEffects.affects(new ItemStack(Items.IRON_HORSE_ARMOR)));
        assertFalse(DurabilityEffects.affects(new ItemStack(Items.STICK)));
    }

    @Test
    void mundanePotionsBrewFromWaterAndVanillaDurationsFollowTheNewTable(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        assertFalse(brewing.hasMix(water, new ItemStack(Items.GOLDEN_CARROT)));
        assertFalse(brewing.hasMix(water, new ItemStack(Items.BEETROOT)));
        assertTrue(StillBrewing.mix(brewing, new ItemStack(Items.GOLDEN_CARROT), water.copy()).isEmpty());
        ItemStack stillMundane = StillBrewing.mix(brewing, new ItemStack(Items.REDSTONE), water.copy());
        assertPotion(stillMundane, Potions.MUNDANE);
        assertFalse(MundaneBrewing.isMundane(stillMundane));
        assertTrue(StillBrewing.isBottle(stillMundane));
        ItemStack plainMundane = PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
        ItemStack night = StillBrewing.mix(brewing, new ItemStack(Items.GOLDEN_CARROT), plainMundane.copy());
        assertTrue(night.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(night, Potions.NIGHT_VISION);
        assertEquals(3600, night.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().iterator().next().getDuration());
        assertFalse(StillBrewing.hasMix(brewing, night, new ItemStack(Items.REDSTONE)));
        assertFalse(StillBrewing.hasMix(brewing, night, new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(night, new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        assertFalse(brewing.hasMix(night, new ItemStack(Items.GUNPOWDER)));
        ItemStack inverted = StillBrewing.mix(brewing, new ItemStack(Items.FERMENTED_SPIDER_EYE), night.copy());
        assertTrue(inverted.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(inverted, Potions.INVISIBILITY);
        assertFalse(StillBrewing.hasMix(brewing, night, new ItemStack(Items.DRAGON_BREATH)));
        ItemStack splash = StillBrewing.mix(brewing, new ItemStack(Items.GUNPOWDER), night.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertTrue(splash.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(splash, Potions.NIGHT_VISION);
        assertTrue(StillBrewing.hasMix(brewing, splash, new ItemStack(Items.DRAGON_BREATH)));
        ItemStack lingering = StillBrewing.mix(brewing, new ItemStack(Items.DRAGON_BREATH), splash.copy());
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertTrue(lingering.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(lingering, Potions.NIGHT_VISION);
        ItemStack splashInverted = StillBrewing.mix(brewing, new ItemStack(Items.FERMENTED_SPIDER_EYE), splash.copy());
        assertTrue(splashInverted.is(Items.SPLASH_POTION));
        assertTrue(splashInverted.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(splashInverted, Potions.INVISIBILITY);

        ItemStack suspicious = StillBrewing.mix(brewing, new ItemStack(Items.BEETROOT), water.copy());
        assertTrue(SuspiciousBrew.is(suspicious));
        assertTrue(FlagonContents.isDoseItem(suspicious));
        assertFalse(StillBrewing.hasMix(brewing, water, new ItemStack(Items.NETHER_WART)));

        ItemStack finesse = StillBrewing.mix(brewing, new ItemStack(Items.RABBIT_HIDE), plainMundane.copy());
        assertTrue(finesse.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(finesse, ModPotions.FINESSE);
        assertFalse(StillBrewing.hasMix(brewing, finesse, new ItemStack(Items.REDSTONE)));
        assertTrue(StackBrewing.mix(brewing, water.copy(), new ItemStack(Items.GOLDEN_CARROT)).isEmpty());

        ItemStack awkward = brewing.mix(new ItemStack(Items.NETHER_WART), water.copy());
        assertPotion(awkward, Potions.AWKWARD);
        assertFalse(awkward.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertFalse(brewing.hasMix(water.copy(), new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        ItemStack weakness = brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), awkward.copy());
        assertPotion(weakness, Potions.WEAKNESS);
        assertFalse(weakness.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertTrue(brewing.hasMix(weakness, new ItemStack(Items.REDSTONE)));
        ItemStack vanillaMundane = brewing.mix(new ItemStack(Items.REDSTONE), water.copy());
        assertPotion(vanillaMundane, Potions.MUNDANE);
        assertFalse(vanillaMundane.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        ItemStack glowWater = brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), water.copy());
        assertPotion(glowWater, Potions.MUNDANE);
        assertFalse(glowWater.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertFalse(brewing.isBrewablePotion(Potions.THICK));

        ItemStack normal = brewing.mix(new ItemStack(Items.GOLDEN_CARROT), awkward.copy());
        assertPotion(normal, Potions.NIGHT_VISION);
        assertFalse(normal.getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertTrue(brewing.hasMix(normal, new ItemStack(Items.REDSTONE)));

        ItemStack splashWater = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER);
        assertFalse(brewing.mix(new ItemStack(Items.GOLDEN_CARROT), splashWater.copy()).getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertPotion(brewing.mix(new ItemStack(Items.DIAMOND), water.copy()), Potions.MUNDANE);

        assertEquals(7200, Potions.LONG_NIGHT_VISION.value().getEffects().getFirst().getDuration());
        assertEquals(3600, Potions.LONG_SLOWNESS.value().getEffects().getFirst().getDuration());
        assertEquals(3600, Potions.LONG_WEAKNESS.value().getEffects().getFirst().getDuration());
        assertEquals(3600, Potions.LONG_SLOW_FALLING.value().getEffects().getFirst().getDuration());
        assertEquals(1800, Potions.STRONG_SWIFTNESS.value().getEffects().getFirst().getDuration());
        assertEquals(900, Potions.POISON.value().getEffects().getFirst().getDuration());
        assertEquals(1800, Potions.LONG_POISON.value().getEffects().getFirst().getDuration());
        assertEquals(450, Potions.STRONG_POISON.value().getEffects().getFirst().getDuration());
        assertEquals(900, Potions.REGENERATION.value().getEffects().getFirst().getDuration());
        assertEquals(450, Potions.STRONG_REGENERATION.value().getEffects().getFirst().getDuration());
        assertEquals(7200, ModPotions.LONG_FINESSE.value().getEffects().getFirst().getDuration());
        assertEquals(1800, ModPotions.STRONG_FINESSE.value().getEffects().getFirst().getDuration());
    }

    @Test
    void vanillaPotionRecipesUseTheNewLevels(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack milk = FlagonContents.milkTemplate();

        ItemStack mundaneWeakness = StillBrewing.mix(brewing, eye.copy(), PotionContents.createItemStack(Items.POTION, Potions.MUNDANE));
        assertTrue(MundaneBrewing.isMundane(mundaneWeakness));
        assertPotion(mundaneWeakness, Potions.WEAKNESS);
        ItemStack weaknessRemedy = StillBrewing.mix(brewing, eye.copy(), milk.copy());
        assertTrue(Remedies.effectOf(weaknessRemedy).unwrapKey().equals(MobEffects.WEAKNESS.unwrapKey()));

        assertPotion(brewing.mix(new ItemStack(Items.GLOWSTONE_DUST), potion(Potions.WEAKNESS)), ModPotions.STRONG_WEAKNESS);
        MobEffectInstance strongWeakness = ModPotions.STRONG_WEAKNESS.value().getEffects().getFirst();
        assertEquals(1, strongWeakness.getAmplifier());
        assertEquals(900, strongWeakness.getDuration());
        ItemStack weaknessThree = StackBrewing.mix(brewing, potion(ModPotions.STRONG_WEAKNESS), new ItemStack(Items.TORCHFLOWER));
        MobEffectInstance weaknessLevelThree = weaknessThree.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().next();
        assertEquals(2, weaknessLevelThree.getAmplifier());
        assertEquals(450, weaknessLevelThree.getDuration());

        assertFalse(brewing.hasMix(awkward.copy(), new ItemStack(Items.SPIDER_EYE)));
        assertPotion(brewing.mix(new ItemStack(Items.POISONOUS_POTATO), awkward.copy()), Potions.POISON);
        assertFalse(brewing.hasMix(potion(Potions.POISON), eye.copy()));
        assertFalse(brewing.hasMix(potion(Potions.LONG_POISON), eye.copy()));
        assertFalse(brewing.hasMix(potion(Potions.STRONG_POISON), eye.copy()));
        assertTrue(brewing.hasMix(potion(Potions.HEALING), eye.copy()));
        assertFalse(brewing.hasMix(potion(Potions.LEAPING), eye.copy()));
        assertFalse(brewing.hasMix(potion(Potions.LONG_LEAPING), eye.copy()));
        assertTrue(brewing.hasMix(potion(Potions.SWIFTNESS), eye.copy()));
        ItemStack slownessTwo = brewing.mix(eye.copy(), potion(Potions.STRONG_SWIFTNESS));
        assertPotion(slownessTwo, Potions.STRONG_SLOWNESS);
        assertEquals(1, slownessTwo.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().iterator().next().getAmplifier());

        MobEffectInstance strongSlowness = Potions.STRONG_SLOWNESS.value().getEffects().getFirst();
        assertEquals(1, strongSlowness.getAmplifier());
        assertEquals(900, strongSlowness.getDuration());
        ItemStack slownessThree = StackBrewing.mix(brewing, potion(Potions.STRONG_SLOWNESS), new ItemStack(Items.TORCHFLOWER));
        assertEquals(2, slownessThree.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().next().getAmplifier());
        assertEquals(0.22, BuffsPotionsConfig.slownessPenalty(1), 0.0001);
        assertEquals(0.66, BuffsPotionsConfig.slownessPenalty(3), 0.0001);

        assertTurtle(Potions.TURTLE_MASTER.value().getEffects(), 1200, 0);
        assertTurtle(Potions.LONG_TURTLE_MASTER.value().getEffects(), 2400, 0);
        assertTurtle(Potions.STRONG_TURTLE_MASTER.value().getEffects(), 600, 1);
        ItemStack turtleThree = StackBrewing.mix(brewing, potion(Potions.STRONG_TURTLE_MASTER), new ItemStack(Items.TORCHFLOWER));
        for (MobEffectInstance effect : turtleThree.get(DataComponents.POTION_CONTENTS).getAllEffects()) {
            assertEquals(2, effect.getAmplifier());
            assertEquals(300, effect.getDuration());
        }

        assertExtended(brewing, Potions.WIND_CHARGED, ModPotions.LONG_WIND_CHARGED);
        assertExtended(brewing, Potions.WEAVING, ModPotions.LONG_WEAVING);
        assertExtended(brewing, Potions.OOZING, ModPotions.LONG_OOZING);
        assertExtended(brewing, Potions.INFESTED, ModPotions.LONG_INFESTED);
        ItemStack windTwice = StackBrewing.mix(
                brewing,
                potion(ModPotions.LONG_WIND_CHARGED),
                new ItemStack(Items.PITCHER_PLANT)
        );
        assertEquals(14400, windTwice.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().next().getDuration());
        assertFalse(brewing.hasMix(potion(Potions.WIND_CHARGED), new ItemStack(Items.GLOWSTONE_DUST)));
        assertTrue(brewing.hasMix(awkward, eye.copy()));
        assertPotion(brewing.mix(eye.copy(), awkward.copy()), Potions.WEAKNESS);
    }

    private static void assertTurtle(java.util.List<MobEffectInstance> effects, int duration, int amplifier) {
        assertEquals(2, effects.size());
        for (MobEffectInstance effect : effects) {
            assertEquals(duration, effect.getDuration());
            assertEquals(amplifier, effect.getAmplifier());
        }
    }

    private static void assertExtended(
            PotionBrewing brewing,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> base,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> extended
    ) {
        assertPotion(brewing.mix(new ItemStack(Items.REDSTONE), potion(base)), extended);
        assertEquals(7200, extended.value().getEffects().getFirst().getDuration());
        assertFalse(brewing.hasMix(potion(base), new ItemStack(Items.GLOWSTONE_DUST)));
    }

    @Test
    void milkBottleBrewsIntoSplashThenLingering(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack milk = FlagonContents.milkTemplate();
        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), milk.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertPotion(splash, ModPotions.MILK);
        ItemStack lingering = brewing.mix(new ItemStack(Items.DRAGON_BREATH), splash.copy());
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertPotion(lingering, ModPotions.MILK);
        assertFalse(brewing.hasMix(milk, new ItemStack(Items.DRAGON_BREATH)));
        assertFalse(brewing.hasMix(lingering, new ItemStack(Items.GUNPOWDER)));
    }

    private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    private static ItemStack splashAwkward() {
        return PotionContents.createItemStack(Items.SPLASH_POTION, Potions.AWKWARD);
    }

    private static void assertPotion(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        assertTrue(stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .orElseThrow()
                .unwrapKey()
                .equals(potion.unwrapKey()));
    }
}

@ExtendWith(EphemeralTestServerProvider.class)
class FlowerPotBrewTest {
    @Test
    void flowersTurnAPlainMundanePotionIntoTheirEffect(MinecraftServer server) {
        assertEquals(FlowerPotBrewing.PLANTS.size(), FlowerPotBrewing.EFFECTS.size());
        assertEquals(38, FlowerPotBrewing.PLANTS.size());
        assertTrue(FlowerPotBrewing.PLANTS.contains("minecraft:golden_dandelion"));
        assertFalse(FlowerPotBrewing.EFFECTS.contains("minecraft:blindness"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:block_stacking"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:block_cracking"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:warding"));
        assertFalse(FlowerPotBrewing.EFFECTS.contains("buffs_potions:latent_eyedentification"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:heights"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:depths"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("buffs_potions:harvesting"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains("minecraft:wind_charged"));
        assertFalse(FlowerPotBrewing.EFFECTS.contains("buffs_potions:flower_walking"));
        assertFalse(FlowerPotBrewing.EFFECTS.contains("buffs_potions:quieting"));
        assertFalse(FlowerPotBrewing.EFFECTS.contains("buffs_potions:rioting"));
        var retired = new HashMap<String, String>();
        retired.put("poppy", "buffs_potions:quieting");
        retired.put("dandelion", "buffs_potions:rioting");
        retired.put("cornflower", "buffs_potions:flower_walking");
        retired.put("oxeye_daisy", "minecraft:wind_charged");
        retired.put("allium", "minecraft:blindness");
        assertTrue(FlowerPotAssignments.retire(retired));
        assertEquals("buffs_potions:heights", retired.get("poppy"));
        assertEquals("buffs_potions:depths", retired.get("dandelion"));
        assertEquals("minecraft:wind_charged", retired.get("cornflower"));
        assertEquals("minecraft:wind_charged", retired.get("oxeye_daisy"));
        assertEquals("buffs_potions:block_stacking", retired.get("allium"));
        assertEquals("buffs_potions:block_cracking", retired.get("minecraft:golden_dandelion"));
        assertEquals(0.50F, GreenThumb.chance(0));
        assertEquals(0.65F, GreenThumb.chance(1));
        assertEquals(0.80F, GreenThumb.chance(2));
        assertEquals(1.0F, GreenThumb.chance(3));
        assertEquals(1.0F, GreenThumb.chance(8));
        long seed = 12345L;
        var first = FlowerPotBrewing.roll(seed);
        assertEquals(first, FlowerPotBrewing.roll(seed));
        assertNotEquals(first, FlowerPotBrewing.roll(seed + 1));
        assertEquals(38, new java.util.HashSet<>(first.values()).size());
        assertTrue(first.containsKey("minecraft:golden_dandelion"));
        assertTrue(FlowerPotBrewing.EFFECTS.containsAll(first.values()));

        assertTrue(FlowerPotBrewing.PLANTS.contains("minecraft:closed_eyeblossom"));
        assertTrue(FlowerPotBrewing.PLANTS.contains(FlowerPotBrewing.OPEN_EYEBLOSSOM));
        assertTrue(first.containsKey("minecraft:closed_eyeblossom"));
        assertTrue(first.containsKey(FlowerPotBrewing.OPEN_EYEBLOSSOM));

        ItemStack open = FlowerPotBrewing.brew(seed, Blocks.OPEN_EYEBLOSSOM);
        assertTrue(MundaneBrewing.isMundane(open));
        assertFalse(open.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.LATENT_EYEDENTIFICATION));
        assertEquals(
                FlowerPotBrewing.fromTarget(first.get(FlowerPotBrewing.OPEN_EYEBLOSSOM)).get(DataComponents.POTION_CONTENTS),
                open.get(DataComponents.POTION_CONTENTS)
        );

        ItemStack closed = FlowerPotBrewing.brew(seed, Blocks.CLOSED_EYEBLOSSOM);
        assertTrue(MundaneBrewing.isMundane(closed));
        var moved = new java.util.HashMap<String, String>();
        moved.put(FlowerPotBrewing.OPEN_EYEBLOSSOM, "minecraft:swiftness");
        assertTrue(FlowerPotAssignments.retire(moved));
        assertEquals("minecraft:swiftness", moved.get("minecraft:closed_eyeblossom"));
        assertFalse(moved.containsKey(FlowerPotBrewing.OPEN_EYEBLOSSOM));

        ItemStack poppy = FlowerPotBrewing.brew(seed, Blocks.POPPY);
        assertTrue(MundaneBrewing.isMundane(poppy));
        assertFalse(poppy.isEmpty());
        assertEquals(
                FlowerPotBrewing.brew(seed, Blocks.POPPY).get(DataComponents.POTION_CONTENTS),
                poppy.get(DataComponents.POTION_CONTENTS)
        );

        ItemStack stone = FlowerPotBrewing.brew(seed, Blocks.STONE);
        assertFalse(stone.isEmpty());
        assertEquals(stone.get(DataComponents.POTION_CONTENTS), FlowerPotBrewing.brew(seed, Blocks.STONE).get(DataComponents.POTION_CONTENTS));
        String stoneEffect = FlowerPotBrewing.unknown(seed, "minecraft:stone");
        assertEquals(stoneEffect, FlowerPotBrewing.unknown(seed, "minecraft:stone"));
        assertTrue(FlowerPotBrewing.EFFECTS.contains(stoneEffect));
        ItemStack warding = FlowerPotBrewing.fromTarget("buffs_potions:warding");
        assertTrue(MundaneBrewing.isMundane(warding));
        assertTrue(warding.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.WARDING));
        ItemStack levitation = FlowerPotBrewing.fromTarget("minecraft:levitation");
        assertTrue(MundaneBrewing.isMundane(levitation));
        MobEffectInstance levitationEffect = levitation.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .getAllEffects()
                .iterator()
                .next();
        assertTrue(levitationEffect.is(MobEffects.LEVITATION));
        assertEquals(20 * 45, levitationEffect.getDuration());
        ItemStack golden = FlowerPotBrewing.brew(seed, Blocks.GOLDEN_DANDELION);
        assertFalse(golden.isEmpty());

        assertTrue(FlowerPotBrewing.fromTarget("buffs_potions:block_perception").isEmpty());
        assertTrue(FlowerPotBrewing.isSuspiciousBrew(PotionContents.createItemStack(Items.POTION, ModPotions.SUSPICIOUS_BREW)));
        assertFalse(FlowerPotBrewing.isSuspiciousBrew(PotionContents.createItemStack(Items.POTION, Potions.MUNDANE)));
        assertFalse(FlowerPotBrewing.isSuspiciousBrew(poppy));
    }
}
