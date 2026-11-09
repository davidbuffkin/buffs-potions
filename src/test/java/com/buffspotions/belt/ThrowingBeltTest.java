package com.buffspotions.belt;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.menu.BeltItemContainer;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.List;
import net.minecraft.core.Holder;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class ThrowingBeltTest {
    @Test
    void throwingBeltsRejectPotionsTheyCannotThrow(MinecraftServer server) {
        ItemStack splash = ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance();
        ItemStack linger = ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance();
        ItemStack rain = flagon(PotionContents.createItemStack(Items.POTION, ModPotions.RAIN_CALLING));
        ItemStack water = flagon(PotionContents.createItemStack(Items.POTION, Potions.WATER));
        ItemStack incarnation = flagon(PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION));
        ItemStack formation = flagon(PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION));
        ItemStack honey = flagon(new ItemStack(Items.HONEY_BOTTLE));

        assertTrue(BeltContents.accepts(splash, rain));
        assertTrue(BeltContents.accepts(linger, rain));
        assertTrue(BeltContents.accepts(splash, water));
        assertTrue(BeltContents.accepts(linger, water));
        assertTrue(BeltContents.accepts(splash, incarnation));
        assertTrue(BeltContents.accepts(linger, incarnation));
        assertTrue(BeltContents.accepts(ModItems.FLAGON_BELT.get().getDefaultInstance(), formation));
        assertTrue(BeltContents.accepts(splash, formation));
        assertTrue(BeltContents.accepts(linger, formation));
        assertFalse(BeltContents.accepts(splash, honey));
        assertFalse(BeltContents.accepts(linger, honey));
        assertEquals(0, BeltContents.insertFirst(splash, rain, BeltContents.BASE_SLOTS));
        assertEquals(0, rain.getCount());
        assertEquals(1, ((FlagonBeltItem) splash.getItem()).throwMode(splash));
        FlagonBeltItem.setThrowing(splash, false);
        assertEquals(0, ((FlagonBeltItem) splash.getItem()).throwMode(splash));
        assertEquals(0, BeltContents.bottleSlotCount(splash));
        assertTrue(BeltContents.accepts(splash, honey));
        FlagonBeltItem.setThrowing(splash, true);
        assertEquals(3, BeltContents.bottleSlotCount(splash));
        assertFalse(BeltContents.accepts(splash, honey));

        FlagonBeltItem dragon = (FlagonBeltItem) linger.getItem();
        assertEquals(2, dragon.throwMode(linger));
        assertTrue(dragon.lingers(linger));
        FlagonBeltItem.setMode(linger, 0);
        assertFalse(dragon.throwsPotions(linger));
        assertEquals(0, BeltContents.bottleSlotCount(linger));
        FlagonBeltItem.setMode(linger, 1);
        assertTrue(dragon.throwsPotions(linger));
        assertFalse(dragon.lingers(linger));
        assertTrue(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION), false));
        assertFalse(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION), true));
        assertTrue(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION), false));
        assertFalse(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION), true));
        assertFalse(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.RAIN_CALLING), false));
        assertFalse(BeltConsumptionService.canTipArrow(PotionContents.createItemStack(Items.POTION, ModPotions.SUN_CALLING), true));
        FlagonBeltItem.setMode(linger, 2);
        assertTrue(dragon.lingers(linger));
    }

    @Test
    void emptyBottlesStayInTheBeltSlots(MinecraftServer server) {
        ItemStack belt = ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance();
        ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, 3);
        assertEquals(0, BeltContents.insertFirst(belt, bottles, BeltContents.BASE_SLOTS));
        assertEquals(0, bottles.getCount());
        assertTrue(BeltContents.get(belt, 0).isEmpty());
        assertEquals(3, BeltContents.bottle(belt, 0).getCount());
        assertEquals(3, BeltContents.bottleCount(belt));
        assertTrue(BeltContents.hasBottle(belt));
        assertTrue(BeltContents.takeBottle(belt));
        assertEquals(2, BeltContents.bottleCount(belt));
        assertEquals(0, BeltContents.bottleSlotCount(ModItems.FLAGON_BELT.get().getDefaultInstance()));

        ItemStack overflow = new ItemStack(Items.GLASS_BOTTLE, 140);
        ItemStack lingering = ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance();
        BeltContents.setBottle(lingering, 0, new ItemStack(Items.GLASS_BOTTLE, 60));
        assertEquals(0, BeltContents.insertFirst(lingering, overflow, BeltContents.BASE_SLOTS));
        assertEquals(8, overflow.getCount());
        assertEquals(64, BeltContents.bottle(lingering, 0).getCount());
        assertEquals(64, BeltContents.bottle(lingering, 1).getCount());
        assertEquals(64, BeltContents.bottle(lingering, 2).getCount());
    }

    @Test
    void gulpFunctionsOnThrowingBelts(MinecraftServer server) {
        Holder<Enchantment> gulp = server.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(BeltEnchantments.GULP_REFLEX);
        for (ItemStack belt : List.of(
                ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance(),
                ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance()
        )) {
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mutable.set(gulp, 1);
            belt.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
            assertEquals(1, BeltEnchantments.level(belt, BeltEnchantments.GULP_REFLEX));
            assertTrue(BeltEnchantments.hasGulpReflex(belt));
            FlagonBeltItem.setMode(belt, 1);
            assertTrue(BeltEnchantments.hasGulpReflex(belt));
        }
        ItemStack belt = ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance();
        BeltEnchantments.strip(belt, BeltEnchantments.GULP_REFLEX);
        assertEquals(0, BeltEnchantments.level(belt, BeltEnchantments.GULP_REFLEX));
        assertTrue(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(BeltEnchantments.TIPPING).isPresent());
        assertFalse(BeltEnchantments.hasTipping(belt));
    }

    @Test
    void tippingBakesBeltEnchantmentsBeforeArrowScale(MinecraftServer server) {
        ItemStack belt = ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance();
        var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantments.getOrThrow(BeltEnchantments.PROTRACTION), 3);
        mutable.set(enchantments.getOrThrow(BeltEnchantments.AMPLIFICATION), 1);
        mutable.set(enchantments.getOrThrow(BeltEnchantments.TIPPING), 1);
        belt.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        assertTrue(BeltEnchantments.hasTipping(belt));

        ItemStack dose = PotionContents.createItemStack(Items.POTION, Potions.STRENGTH);
        int base = new PotionContents(Potions.STRENGTH).getAllEffects().iterator().next().getDuration();
        ItemStack arrow = BeltTipping.tippedArrow(dose, belt);
        assertTrue(arrow.is(Items.TIPPED_ARROW));
        assertEquals(BeltEnchantments.TIPPED_ARROW_DURATION_SCALE, arrow.get(DataComponents.POTION_DURATION_SCALE));
        PotionContents tipped = arrow.get(DataComponents.POTION_CONTENTS);
        MobEffectInstance effect = tipped.getAllEffects().iterator().next();
        assertEquals(1, effect.getAmplifier());
        assertEquals(Math.round(base * 2.0F), effect.getDuration());
        assertEquals(java.util.Optional.of("strength"), tipped.customName());

        PotionContents unchanged = BeltEnchantments.modifiedContents(new PotionContents(Potions.STRENGTH), ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance());
        assertTrue(unchanged.is(Potions.STRENGTH));
    }

    @Test
    void beltOpenedFromAnotherContainerKeepsItsBottleRow(MinecraftServer server) {
        SimpleContainer chest = new SimpleContainer(1);
        assertSameSlotCount(server, chest, ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance());
        assertSameSlotCount(server, chest, ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance());
        ItemStack drinking = ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance();
        FlagonBeltItem.setThrowing(drinking, false);
        assertSameSlotCount(server, chest, drinking);
    }

    private static void assertSameSlotCount(MinecraftServer server, SimpleContainer chest, ItemStack belt) {
        chest.setItem(0, belt.copy());
        BeltItemContainer opened = new BeltItemContainer(null, chest, 0);
        RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        BeltItemContainer.writeOpenData(data, chest.getItem(0));
        BeltItemContainer received = BeltItemContainer.fromNetwork(null, data);
        assertEquals(opened.getContainerSize(), received.getContainerSize());
        assertEquals(BeltContents.BASE_SLOTS + BeltContents.bottleSlotCount(chest.getItem(0)), received.getContainerSize());
        data.release();
    }

    @Test
    void beltOpenedFromThePlayerKeepsTheServerBottleRow(MinecraftServer server) {
        ItemStack dragon = ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance();
        RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        BeltItemContainer.writeOpenData(data, BeltItemContainer.MAIN_HAND, dragon);
        BeltItemContainer received = BeltItemContainer.fromNetwork(null, data);
        assertEquals(BeltContents.BASE_SLOTS + BeltContents.BOTTLE_SLOTS, received.getContainerSize());
        assertEquals(BeltContents.BOTTLE_SLOTS, received.bottleSlots());
        data.release();

        ItemStack drinking = dragon.copy();
        FlagonBeltItem.setMode(drinking, 0);
        RegistryFriendlyByteBuf drinkData = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        BeltItemContainer.writeOpenData(drinkData, BeltItemContainer.MAIN_HAND, drinking);
        BeltItemContainer drinkMenu = BeltItemContainer.fromNetwork(null, drinkData);
        assertEquals(BeltContents.BASE_SLOTS, drinkMenu.getContainerSize());
        drinkData.release();
    }

    private static ItemStack flagon(ItemStack dose) {
        ItemStack flagon = new ItemStack(ModItems.FLAGON.get());
        flagon.set(ModDataComponents.FLAGON_CONTENTS.get(), new FlagonContents(dose, 1));
        return flagon;
    }
}
