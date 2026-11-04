package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.item.BellowsItem;
import com.buffspotions.item.ColoredSmithingTemplateItem;
import com.buffspotions.item.EnderFlagonItem;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.item.LingeringFlagonBeltItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.item.SplashFlagonBeltItem;
import com.buffspotions.belt.BeltContents;
import com.buffspotions.flagon.FlagonContents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.List;
import java.util.Optional;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final TooltipDisplay HIDDEN_DYE =
            TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BuffsPotions.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, BuffsPotions.MOD_ID);

    public static final DeferredItem<PotionBeltItem> POTION_BELT = ITEMS.registerItem(
            "potion_belt",
            PotionBeltItem::new,
            properties -> properties
                    .stacksTo(1)
                    .durability(64)
                    .enchantable(12)
                    .component(ModDataComponents.BELT_CONTENTS.get(), BeltContents.EMPTY_FIXED)
                    .component(ModDataComponents.ACTIVE_BELT_SLOT.get(), 0)
                    .component(ModDataComponents.BELT_DATA_VERSION.get(), 1)
                    .component(DataComponents.TOOLTIP_DISPLAY, HIDDEN_DYE)
                    .component(
                            DataComponents.USE_COOLDOWN,
                            new UseCooldown(
                                    1.0F,
                                    Optional.of(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belts"))
                            )
                    )
                    .repairable(Items.LEATHER)
    );

    public static final DeferredItem<FlagonBeltItem> FLAGON_BELT = ITEMS.registerItem(
            "flagon_belt",
            FlagonBeltItem::new,
            properties -> properties
                    .stacksTo(1)
                    .durability(512)
                    .enchantable(12)
                    .component(ModDataComponents.BELT_CONTENTS.get(), BeltContents.EMPTY_FIXED)
                    .component(ModDataComponents.ACTIVE_BELT_SLOT.get(), 0)
                    .component(ModDataComponents.BELT_DATA_VERSION.get(), 1)
                    .component(DataComponents.TOOLTIP_DISPLAY, HIDDEN_DYE)
                    .component(
                            DataComponents.USE_COOLDOWN,
                            new UseCooldown(
                                    1.0F,
                                    Optional.of(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belts"))
                            )
                    )
                    .repairable(Items.NETHERITE_SCRAP)
    );

    public static final DeferredItem<SplashFlagonBeltItem> SPLASH_FLAGON_BELT = ITEMS.registerItem(
            "splash_flagon_belt",
            SplashFlagonBeltItem::new,
            ModItems::throwingBelt
    );

    public static final DeferredItem<LingeringFlagonBeltItem> LINGERING_FLAGON_BELT = ITEMS.registerItem(
            "lingering_flagon_belt",
            LingeringFlagonBeltItem::new,
            ModItems::throwingBelt
    );

    public static final DeferredItem<SmithingTemplateItem> CREEPER_UPGRADE = ITEMS.registerItem(
            "creeper_upgrade",
            properties -> smithingTemplate(
                    properties,
                    "creeper_upgrade",
                    ChatFormatting.GREEN,
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/flagon_belt"),
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/creeper_head")
            ),
            properties -> properties.stacksTo(64)
    );

    public static final DeferredItem<SmithingTemplateItem> DRAGON_UPGRADE = ITEMS.registerItem(
            "dragon_upgrade",
            properties -> smithingTemplate(
                    properties,
                    "dragon_upgrade",
                    ChatFormatting.LIGHT_PURPLE,
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/splash_flagon_belt"),
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/dragon_head")
            ),
            properties -> properties.stacksTo(64)
    );

    public static final DeferredItem<FlagonItem> FLAGON = ITEMS.registerItem(
            "flagon",
            properties -> new FlagonItem(ModBlocks.FLAGON.get(), properties),
            properties -> properties
                    .stacksTo(64)
                    .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
                    .component(ModDataComponents.FLAGON_CONTENTS.get(), FlagonContents.EMPTY)
    );

    public static final DeferredItem<EnderFlagonItem> ENDER_FLAGON = ITEMS.registerItem(
            "ender_flagon",
            properties -> new EnderFlagonItem(ModBlocks.ENDER_FLAGON.get(), properties),
            properties -> properties
                    .stacksTo(1)
                    .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
    );

    public static final DeferredItem<AnchoredEnderFlagonItem> ANCHORED_ENDER_FLAGON = ITEMS.registerItem(
            "anchored_ender_flagon",
            properties -> new AnchoredEnderFlagonItem(ModBlocks.ANCHORED_ENDER_FLAGON.get(), properties),
            properties -> properties
                    .stacksTo(1)
                    .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
    );

    public static boolean fitsBrewingStack(ItemStack stack) {
        return stack.is(FLAGON.get()) || stack.is(ANCHORED_ENDER_FLAGON.get());
    }

    public static final DeferredItem<Item> WING = ITEMS.registerItem(
            "wing",
            Item::new,
            properties -> properties.stacksTo(64)
    );

    public static final DeferredItem<Item> CRAB_CLAW = ITEMS.registerItem(
            "crab_claw",
            Item::new,
            properties -> properties.stacksTo(64)
    );

    public static final DeferredItem<SpawnEggItem> CRAB_SPAWN_EGG = ITEMS.registerItem(
            "crab_spawn_egg",
            SpawnEggItem::new,
            properties -> properties.spawnEgg(ModEntities.CRAB.get())
    );

    public static final DeferredItem<BellowsItem> BELLOWS = ITEMS.registerItem(
            "bellows",
            BellowsItem::new,
            properties -> properties.stacksTo(1)
    );

    public static final DeferredItem<BlockItem> BREWING_STILL = ITEMS.registerItem(
            "brewing_still",
            properties -> new BlockItem(ModBlocks.BREWING_STILL.get(), properties)
    );

    public static final DeferredItem<BlockItem> BREWING_STACK = ITEMS.registerItem(
            "brewing_stack",
            properties -> new BlockItem(ModBlocks.BREWING_STACK.get(), properties)
    );

    public static final DeferredItem<BlockItem> CALIBRATED_SCULK_SHRIEKER = ITEMS.registerItem(
            "calibrated_sculk_shrieker",
            properties -> new BlockItem(ModBlocks.CALIBRATED_SCULK_SHRIEKER.get(), properties.useBlockDescriptionPrefix())
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.buffs_potions"))
                    .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
                    .icon(() -> POTION_BELT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(POTION_BELT.get());
                        output.accept(FLAGON_BELT.get());
                        output.accept(SPLASH_FLAGON_BELT.get());
                        output.accept(LINGERING_FLAGON_BELT.get());
                        output.accept(CREEPER_UPGRADE.get());
                        output.accept(DRAGON_UPGRADE.get());
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.MILK));
                        output.accept(PotionContents.createItemStack(Items.SPLASH_POTION, ModPotions.MILK));
                        output.accept(PotionContents.createItemStack(Items.LINGERING_POTION, ModPotions.MILK));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.SUSPICIOUS_BREW));
                        output.accept(FLAGON.get());
                        output.accept(ENDER_FLAGON.get());
                        output.accept(ANCHORED_ENDER_FLAGON.get());
                        output.accept(BREWING_STILL.get());
                        output.accept(BREWING_STACK.get());
                        output.accept(CALIBRATED_SCULK_SHRIEKER.get());
                        output.accept(BELLOWS.get());
                        output.accept(WING.get());
                        output.accept(CRAB_CLAW.get());
                        output.accept(CRAB_SPAWN_EGG.get());
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.INSIGHT));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_INSIGHT));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_INSIGHT));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.INSTANT_CONFUSION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.REACHING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_REACHING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_REACHING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.FINESSE));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_FINESSE));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_FINESSE));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.FUMBLING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_FUMBLING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_FUMBLING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.WIND_JUMPING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_WIND_JUMPING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_WIND_JUMPING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.HARVESTING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_HARVESTING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_HARVESTING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.RAIN_CALLING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.SUN_CALLING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.HEIGHTS));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_HEIGHTS));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.DEPTHS));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_DEPTHS));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.WARDING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_WARDING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.PARTIAL_VISITATION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_PARTIAL_VISITATION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_PARTIAL_VISITATION));
                        output.accept(EyedBrewing.latentBottle());
                        output.accept(essenceBottle());
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.BLOCK_PERCEPTION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_BLOCK_PERCEPTION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_BLOCK_PERCEPTION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.BLOCK_STACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_BLOCK_STACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_BLOCK_STACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.BLOCK_CRACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_BLOCK_CRACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_BLOCK_CRACKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.ITEM_EATING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_ITEM_EATING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_ITEM_EATING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.ITEM_VOMITING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_ITEM_VOMITING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_ITEM_VOMITING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.SEEKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_SEEKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.STRONG_SEEKING));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.DISGUISE));
                        output.accept(PotionContents.createItemStack(Items.POTION, ModPotions.LONG_DISGUISE));
                        acceptEnchantedBooks(parameters, output);
                    })
                    .build()
    );

    private static Item.Properties throwingBelt(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .durability(512)
                .enchantable(12)
                .component(ModDataComponents.BELT_CONTENTS.get(), BeltContents.EMPTY_FIXED)
                .component(ModDataComponents.BELT_BOTTLES.get(), BeltContents.EMPTY_FIXED)
                .component(ModDataComponents.ACTIVE_BELT_SLOT.get(), 0)
                .component(ModDataComponents.BELT_DATA_VERSION.get(), 1)
                .component(DataComponents.TOOLTIP_DISPLAY, HIDDEN_DYE)
                .component(
                        DataComponents.USE_COOLDOWN,
                        new UseCooldown(
                                1.0F,
                                Optional.of(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belts"))
                        )
                )
                .repairable(Items.NETHERITE_SCRAP);
    }

    private static SmithingTemplateItem smithingTemplate(
            Item.Properties properties,
            String name,
            ChatFormatting nameColor,
            Identifier baseIcon,
            Identifier additionIcon
    ) {
        return new ColoredSmithingTemplateItem(
                Component.translatable("item.buffs_potions." + name + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.buffs_potions." + name + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.buffs_potions." + name + ".base_slot_description"),
                Component.translatable("item.buffs_potions." + name + ".additions_slot_description"),
                List.of(baseIcon),
                List.of(additionIcon),
                nameColor,
                properties
        );
    }

    /** A subject-less essence has no effect color, so the catalog bottle uses the item-essence tint. */
    private static ItemStack essenceBottle() {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, ModPotions.ESSENCE);
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                contents.potion(),
                Optional.of(EssenceSubject.ITEM_COLOR),
                contents.customEffects(),
                contents.customName()
        ));
        return stack;
    }

    private static void acceptEnchantedBooks(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        var enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
        for (ResourceKey<Enchantment> enchantment : List.of(
                BeltEnchantments.PROTRACTION,
                BeltEnchantments.AMPLIFICATION,
                BeltEnchantments.QUICK_QUAFF,
                BeltEnchantments.TIPPING,
                BeltEnchantments.GULP_REFLEX
        )) {
            Holder<Enchantment> holder = enchantments.getOrThrow(enchantment);
            for (int level = 1; level <= holder.value().getMaxLevel(); level++) {
                output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(holder, level)));
            }
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private ModItems() {
    }
}
