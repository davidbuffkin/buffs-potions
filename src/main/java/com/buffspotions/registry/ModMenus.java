package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.menu.BrewingStackMenu;
import com.buffspotions.menu.BrewingStillMenu;
import com.buffspotions.menu.PotionBeltMenu;
import com.buffspotions.menu.StandBookMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BuffsPotions.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PotionBeltMenu>> POTION_BELT =
            MENUS.register("potion_belt", () -> IMenuTypeExtension.create(PotionBeltMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BrewingStillMenu>> BREWING_STILL =
            MENUS.register("brewing_still", () -> IMenuTypeExtension.create(BrewingStillMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BrewingStackMenu>> BREWING_STACK =
            MENUS.register("brewing_stack", () -> IMenuTypeExtension.create(BrewingStackMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StandBookMenu>> BREWING_STAND_BOOK =
            MENUS.register("brewing_stand", () -> IMenuTypeExtension.create(StandBookMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }

    private ModMenus() {
    }
}
