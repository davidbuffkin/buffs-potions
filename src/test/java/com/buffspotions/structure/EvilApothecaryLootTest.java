package com.buffspotions.structure;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import com.mojang.authlib.GameProfile;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class EvilApothecaryLootTest {
    @Test
    void shelvesAndFlagonsStayInsideTheirPools(MinecraftServer server) {
        RandomSource random = RandomSource.create(11);
        boolean empty = false;
        boolean regeneration = false;
        boolean latent = false;
        boolean piglin = false;
        boolean strider = false;
        boolean quartz = false;
        boolean obsidian = false;
        for (int i = 0; i < 500; i++) {
            ItemStack slot = EvilApothecaryLoot.shelfSlot(random);
            if (slot.isEmpty()) {
                empty = true;
            } else {
                assertTrue(slot.is(Items.POTION));
                if (slot.get(DataComponents.POTION_CONTENTS).is(Potions.STRONG_REGENERATION)) {
                    regeneration = true;
                    assertEquals(4.0F, slot.get(DataComponents.POTION_DURATION_SCALE));
                }
                if (slot.get(DataComponents.POTION_CONTENTS).is(ModPotions.LATENT_EYEDENTIFICATION)) {
                    latent = true;
                    assertEquals(EyedBrewing.LATENT_COLOR, slot.get(DataComponents.POTION_CONTENTS).customColor().orElseThrow());
                }
            }
            FlagonContents contents = EvilApothecaryLoot.flagon(random);
            assertTrue(contents.doses() >= 1 && contents.doses() <= 4);
            EssenceSubject essence = contents.dose().get(ModDataComponents.ESSENCE.get());
            if (essence != null && essence.entity().isPresent()) {
                String id = essence.entity().get().getRegisteredName();
                if ("minecraft:piglin_brute".equals(id)) {
                    piglin = true;
                    assertTrue(contents.dose().get(DataComponents.POTION_CONTENTS).is(ModPotions.INCARNATION));
                }
                if ("minecraft:strider".equals(id)) {
                    strider = true;
                }
            }
            if (essence != null && essence.item().isPresent()) {
                if (essence.item().get().value() == Items.NETHER_QUARTZ_ORE) {
                    quartz = true;
                    assertTrue(contents.dose().get(DataComponents.POTION_CONTENTS).is(ModPotions.STRONG_BLOCK_PERCEPTION));
                }
                if (essence.item().get().value() == Items.OBSIDIAN) {
                    obsidian = true;
                    assertTrue(contents.dose().get(DataComponents.POTION_CONTENTS).is(ModPotions.STRONG_SEEKING));
                }
            }
        }
        assertTrue(empty);
        assertTrue(regeneration);
        assertTrue(latent);
        assertTrue(piglin);
        assertTrue(strider);
        assertTrue(quartz);
        assertTrue(obsidian);
    }

    @Test
    void masterTradeSellsTheTradingPlayersHead(MinecraftServer server) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 3), new ItemStack(Items.EXPERIENCE_BOTTLE), 12, 30, 0.05F));
        EvilCleric.prepare(offers, new GameProfile(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Ada"));
        MerchantOffer offer = offers.get(0);
        assertEquals(1, offers.size());
        assertTrue(offer.getBaseCostA().is(Items.EMERALD));
        assertEquals(30, offer.getItemCostA().count());
        assertEquals(1, offer.getMaxUses());
        assertEquals(1, offer.getResult().getCount());
        assertTrue(offer.getResult().is(Items.PLAYER_HEAD));
        assertEquals("Ada", offer.getResult().get(DataComponents.PROFILE).name().orElseThrow());

        offer.increaseUses();
        EvilCleric.prepare(offers, new GameProfile(UUID.fromString("22222222-2222-2222-2222-222222222222"), "Bea"));
        assertEquals(1, offers.size());
        assertEquals(1, offers.get(0).getUses());
        assertEquals("Bea", offers.get(0).getResult().get(DataComponents.PROFILE).name().orElseThrow());
    }

    @Test
    void containersAndClericArePrepared(MinecraftServer server) {
        CompoundTag shelf = ApothecaryProcessor.shelfItems(
                server.registryAccess(),
                new CompoundTag(),
                RandomSource.create(2),
                EvilApothecaryLoot.SHELF_SLOTS,
                EvilApothecaryLoot::shelfSlot
        );
        List<ItemStackWithSlot> slots = ItemStackWithSlot.CODEC.listOf().parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                shelf.get("Items")
        ).getOrThrow();
        assertTrue(slots.size() <= EvilApothecaryLoot.SHELF_SLOTS);
        assertTrue(slots.stream().allMatch(slot -> slot.slot() >= 0 && slot.slot() < EvilApothecaryLoot.SHELF_SLOTS));

        CompoundTag chest = ApothecaryProcessor.chestLoot(new CompoundTag(), EvilApothecaryLoot.CHEST, RandomSource.create(1));
        assertEquals("buffs_potions:chests/evil_apothecary", chest.getStringOr("LootTable", ""));
        assertTrue(server.reloadableRegistries().lookup().lookupOrThrow(net.minecraft.core.registries.Registries.LOOT_TABLE)
                .get(EvilApothecaryLoot.CHEST).isPresent());

        CompoundTag zombie = new CompoundTag();
        zombie.putString("id", "minecraft:zombie_villager");
        zombie.putBoolean("NoAI", true);
        zombie.put("Brain", new CompoundTag());
        zombie.put("Offers", new CompoundTag());
        CompoundTag woken = new EvilApothecaryProcessor().processEntity(
                null,
                BlockPos.ZERO,
                new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, zombie),
                new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, zombie),
                null,
                null
        ).nbt;
        assertFalse(woken.getBooleanOr("NoAI", false));
        assertTrue(woken.getCompound("Brain").isEmpty());
        assertTrue(woken.getCompound("Offers").isEmpty());
        assertTrue(woken.getBooleanOr("PersistenceRequired", false));
        assertEquals(1, woken.getIntOr("Xp", 0));
        CompoundTag data = woken.getCompound("VillagerData").orElseThrow();
        assertEquals("minecraft:cleric", data.getStringOr("profession", ""));
        assertEquals(1, data.getIntOr("level", 0));
        boolean marked = false;
        for (net.minecraft.nbt.Tag tag : woken.getList("Tags").orElseThrow()) {
            if (EvilCleric.TAG.equals(tag.asString().orElse(""))) {
                marked = true;
            }
        }
        assertTrue(marked);
    }

    @Test
    void floorUsesOpenNetherGroundAndSkipsLavaLakes() {
        BlockState[] column = new BlockState[128];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        for (int y = 0; y <= 4; y++) {
            column[y] = Blocks.BEDROCK.defaultBlockState();
        }
        for (int y = 123; y <= 127; y++) {
            column[y] = Blocks.BEDROCK.defaultBlockState();
        }
        column[40] = Blocks.CRIMSON_NYLIUM.defaultBlockState();
        column[41] = Blocks.CRIMSON_ROOTS.defaultBlockState();
        assertEquals(40, EvilApothecaryStructure.floorY(new NoiseColumn(0, column)));

        column[40] = Blocks.SOUL_SAND.defaultBlockState();
        column[41] = Blocks.AIR.defaultBlockState();
        assertEquals(40, EvilApothecaryStructure.floorY(new NoiseColumn(0, column)));

        column[40] = Blocks.NETHERRACK.defaultBlockState();
        assertEquals(40, EvilApothecaryStructure.floorY(new NoiseColumn(0, column)));

        column[41] = Blocks.LAVA.defaultBlockState();
        assertEquals(-1, EvilApothecaryStructure.floorY(new NoiseColumn(0, column)));
    }
}
