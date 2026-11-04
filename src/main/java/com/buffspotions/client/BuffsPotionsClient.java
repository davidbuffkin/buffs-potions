package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.belt.BeltClickPriority;
import com.buffspotions.belt.BeltConsumptionService;
import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltKey;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.belt.BeltSource;
import com.buffspotions.network.ToolSwapPayload;
import com.buffspotions.belt.BeltUseState;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.equipment.FallbackBeltContainer;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.flagon.FlagonDrinkLatch;
import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.potion.ActiveBrewing;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.menu.BeltItemContainer;
import com.buffspotions.mixin.client.AbstractContainerScreenAccessor;
import com.buffspotions.network.BeltActionPayload;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModBlockEntities;
import com.buffspotions.registry.ModBlocks;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModEntities;
import com.buffspotions.registry.ModMenus;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stand.StandSearchCategory;
import com.buffspotions.still.StillSearchCategory;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRecipeBookSearchCategoriesEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@Mod(value = BuffsPotions.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BuffsPotions.MOD_ID, value = Dist.CLIENT)
public final class BuffsPotionsClient {
    private static final Identifier BELT_OVERLAY =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belt_overlay");
    private static final boolean[] PREVIOUS_NUMBER_KEYS = new boolean[BeltContents.BASE_SLOTS];
    private static boolean heldUseSent;
    private static boolean heldChain;
    /** One drink per press for a flagon belt in drink mode. Cleared when right-click is released. */
    private static boolean drinkLatched;
    /** Set on the click that starts a drink, and applied on the next press so both hands of one click still run. */
    private static boolean drinkArm;
    private static boolean equippedUseSent;
    private static boolean equippedWasBusy;
    private static boolean rowSwitchSent;
    /** One inventory open per right-click while a held potion belt has nothing to drink. */
    private static boolean beltMenuSent;
    private static boolean shiftWasDown;
    private static int potionNameTimer;
    private static ItemStack potionNameStack = ItemStack.EMPTY;

    public BuffsPotionsClient(ModContainer container) {
        container.getEventBus().addListener(BeltKeys::register);
        container.getEventBus().addListener(FallingRodParticle::register);
        container.getEventBus().addListener(BuffsPotionsClient::registerStillSearch);
        BeltKey.bindClient(BeltKeys::isDown);
        ActiveBrewing.bindClient(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            return level == null ? null : level.potionBrewing();
        });
        EnderFlagons.setClientView(() -> {
            if (Minecraft.getInstance().player == null) {
                return FlagonContents.EMPTY;
            }
            return EnderFlagons.of(Minecraft.getInstance().player);
        });
        EnderFlagons.setClientIdentity(
                () -> Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getUUID(),
                () -> {
                    if (Minecraft.getInstance().player == null) {
                        return "";
                    }
                    String name = Minecraft.getInstance().player.getGameProfile().name();
                    return name == null || name.isBlank()
                            ? Minecraft.getInstance().player.getName().getString()
                            : name;
                }
        );
        EnderFlagons.setClientNames(uuid -> {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(uuid)) {
                String name = Minecraft.getInstance().player.getGameProfile().name();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
            if (Minecraft.getInstance().getConnection() != null) {
                var info = Minecraft.getInstance().getConnection().getPlayerInfo(uuid);
                if (info != null && info.getProfile().name() != null && !info.getProfile().name().isBlank()) {
                    return info.getProfile().name();
                }
            }
            return "";
        });
    }

    private static void registerStillSearch(RegisterRecipeBookSearchCategoriesEvent event) {
        event.register(
                StillSearchCategory.STILL,
                ModRecipes.MUNDANE.get(),
                ModRecipes.HARMFUL.get(),
                ModRecipes.OTHER.get()
        );
        event.register(
                StandSearchCategory.STAND,
                ModRecipes.STAND_HELPFUL.get(),
                ModRecipes.STAND_HARMFUL.get(),
                ModRecipes.STAND_PARAMETER.get(),
                ModRecipes.STAND_UTILITY.get()
        );
        event.register(
                com.buffspotions.stack.StackSearchCategory.STACK,
                ModRecipes.STACK_HELPFUL.get(),
                ModRecipes.STACK_HARMFUL.get(),
                ModRecipes.STACK_PARAMETER.get(),
                ModRecipes.STACK_UTILITY.get()
        );
    }

    @SubscribeEvent
    public static void registerClueTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(InsightClueTooltip.class, InsightClueTooltip.Client::new);
    }

    @SubscribeEvent
    public static void registerSeekingIcon(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new SeekingIcon(), ModEffects.SEEKING.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.POTION_BELT.get(), PotionBeltScreen::new);
        event.register(ModMenus.BREWING_STILL.get(), BrewingStillScreen::new);
        event.register(ModMenus.BREWING_STACK.get(), BrewingStackScreen::new);
        event.register(ModMenus.BREWING_STAND_BOOK.get(), BrewingStandBookScreen::new);
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belt_contents"),
                BeltContentsModel.Unbaked.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerFlagonDoses(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "flagon_doses"), FlagonDoses.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerFlagonTint(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "flagon_fluid"), FlagonFluidTint.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "remedy_band"), RemedyBandTint.MAP_CODEC);
        event.register(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "anchor_stopper"), AnchorStopperTint.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerAnchorStopper(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new AnchorStopperBlockTint()), ModBlocks.ANCHORED_ENDER_FLAGON.get());
    }

    @SubscribeEvent
    public static void registerRemedyDose(RegisterConditionalItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "remedy_dose"), RemedyDose.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerDisguiseArmor(EntityRenderersEvent.AddLayers event) {
        DisguiseStandModel.init(event.getContext());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLAGON.get(), FlagonRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENDER_FLAGON.get(), EnderFlagonRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ANCHORED_ENDER_FLAGON.get(), AnchoredEnderFlagonRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BREWING_STACK.get(), BrewingStackRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAB.get(), CrabRenderer::new);
    }

    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(
                BrewingStackRenderer.FLAGON_OPEN,
                SimpleUnbakedStandaloneModel.simpleModelWrapper(BrewingStackRenderer.flagonModel())
        );
        event.register(
                BrewingStackRenderer.ANCHORED_FLAGON,
                SimpleUnbakedStandaloneModel.simpleModelWrapper(BrewingStackRenderer.anchoredModel())
        );
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CrabModel.LAYER, CrabModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, BELT_OVERLAY, BuffsPotionsClient::renderBeltOverlay);
    }

    @SubscribeEvent
    public static void disguiseNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        String name = DisguiseSkins.subjectName(player);
        if (name == null) {
            return;
        }
        event.setContent(Component.literal(name));
    }

    @SubscribeEvent
    public static void disguiseBodyNameTag(RenderNameTagEvent.CanRender event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !disguisedBody(living)) {
            return;
        }
        String name = DisguiseSkins.subjectName(living);
        if (name == null) {
            return;
        }
        if (!living.hasCustomName()) {
            event.setCanRender(TriState.FALSE);
            return;
        }
        event.setContent(Component.literal(name));
        double reach = Mth.square(64.0F);
        event.setCanRender(event.getEntityRenderState().distanceToCameraSq < reach ? TriState.TRUE : TriState.FALSE);
    }

    private static boolean disguisedBody(LivingEntity entity) {
        if (entity instanceof ArmorStand) {
            return true;
        }
        if (entity instanceof Villager villager) {
            return !villager.isBaby();
        }
        return entity instanceof Zombie zombie && !zombie.isBaby();
    }

    @SubscribeEvent
    public static void onAttackPress(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }
        if (!minecraft.options.keyAttack.matches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            return;
        }
        requestToolSwap(minecraft);
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !minecraft.mouseHandler.isMouseGrabbed()) {
            return;
        }
        if (!minecraft.options.keyAttack.matches(event.getKeyEvent())) {
            return;
        }
        requestToolSwap(minecraft);
    }

    private static void requestToolSwap(Minecraft minecraft) {
        if (minecraft.player.isShiftKeyDown()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (minecraft.level == null || minecraft.level.getBlockState(hit.getBlockPos()).isAir()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ToolSwapPayload(hit.getBlockPos()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.keyUse.isDown()) {
            FlagonDrinkLatch.clear();
        }
        if (minecraft.player == null) {
            releaseAll();
            BeltDrinkAnim.clear();
            BeltKeys.sync(minecraft);
            return;
        }
        SkinMemory.harvest(minecraft);
        BeltDrinkAnim.tick(minecraft.player);
        BeltKeys.sync(minecraft);
        boolean typing = minecraft.gui.screen() != null && minecraft.gui.screen().getFocused() instanceof EditBox;
        if (!typing) {
            selectBeltColumn(minecraft);
        }
        if (minecraft.gui.screen() != null) {
            releaseUseKeys();
            return;
        }

        releaseUseIfNeeded(minecraft);
        tickPotionName(minecraft);
        releaseHeldConsumable(minecraft);
    }

    private static void selectBeltColumn(Minecraft minecraft) {
        boolean useBelt = BeltKeys.isDown();
        ItemStack preferred = BeltSource.shown(minecraft.player);
        for (int slot = 0; slot < BeltContents.BASE_SLOTS; slot++) {
            boolean clicked = false;
            if (useBelt && !preferred.isEmpty()) {
                while (minecraft.options.keyHotbarSlots[slot].consumeClick()) {
                    clicked = true;
                }
            }
            if (clicked) {
                int target = BeltEnchantments.rowStart(preferred) + slot;
                if (minecraft.gui.screen() == null
                        && BeltSource.heldHand(minecraft.player) == null
                        && BeltContents.activeSlot(preferred) == target
                        && !minecraft.player.isSpectator()) {
                    minecraft.player.getInventory().setSelectedSlot(slot);
                }
                BeltContents.setActiveSlot(preferred, target);
                ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.SELECT, slot));
            }
            PREVIOUS_NUMBER_KEYS[slot] = useBelt && minecraft.options.keyHotbarSlots[slot].isDown() && !preferred.isEmpty();
        }
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) {
            return;
        }
        BeltKeys.sync(minecraft);
        if (event.isAttack()) {
            switchRow(event, minecraft);
            return;
        }
        if (!event.isUseItem()) {
            return;
        }
        if (FlagonDrinkLatch.isArmed()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND && drinkArm) {
            drinkLatched = true;
            drinkArm = false;
        }
        boolean useBelt = BeltKeys.isDown();
        boolean air = minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS;
        ItemStack main = minecraft.player.getMainHandItem();
        ItemStack offhand = minecraft.player.getOffhandItem();
        ItemStack thisHand = event.getHand() == InteractionHand.MAIN_HAND ? main : offhand;
        if (PotionBeltItem.opensWhenUndrinkable(minecraft.player, thisHand)) {
            ItemStack shown = useBelt ? BeltSource.shown(minecraft.player) : ItemStack.EMPTY;
            boolean shownActs = useBelt
                    && (PotionBeltItem.canDrink(minecraft.player, shown) || repeatsThrow(minecraft.player, shown));
            if (!shownActs) {
                event.setCanceled(true);
                event.setSwingHand(false);
                if (!beltMenuSent && minecraft.gameMode != null) {
                    beltMenuSent = true;
                    minecraft.gameMode.useItem(minecraft.player, event.getHand());
                }
                return;
            }
        }
        if (!useBelt) {
            boolean mainBelt = main.getItem() instanceof PotionBeltItem;
            if (air && (mainBelt || offhand.getItem() instanceof PotionBeltItem)) {
                ItemStack belt = mainBelt ? main : offhand;
                InteractionHand hand = mainBelt ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                if (repeatsThrow(minecraft.player, belt)) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    if (event.getHand() == InteractionHand.MAIN_HAND) {
                        throwHeld(minecraft, belt, hand);
                    }
                    return;
                }
                if (drinksOnce(belt) && drinkLatched) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    return;
                }
                if (PotionBeltItem.canDrink(minecraft.player, belt)) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    if (event.getHand() != InteractionHand.MAIN_HAND) {
                        return;
                    }
                    drinkLatched = drinksOnce(belt);
                    activateHeld(minecraft, belt, hand);
                    return;
                }
            }
            if (drinksOnce(thisHand) && PotionBeltItem.canDrink(minecraft.player, thisHand)) {
                if (drinkLatched) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                    return;
                }
                drinkArm = true;
            }
            syncCrossbowShot(minecraft, event.getHand());
            return;
        }
        ItemStack belt = BeltSource.shown(minecraft.player);
        boolean throwing = repeatsThrow(minecraft.player, belt);
        boolean yields = BeltClickPriority.allows(minecraft.player, minecraft.hitResult, main, offhand);
        boolean drinkFirst = !BeltKeys.boundToSneak(minecraft) && !throwing;
        if (throwing && !yields) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            if (main.getItem() instanceof PotionBeltItem && main == belt) {
                throwHeld(minecraft, belt, InteractionHand.MAIN_HAND);
            } else if (offhand.getItem() instanceof PotionBeltItem && offhand == belt) {
                throwHeld(minecraft, belt, InteractionHand.OFF_HAND);
            } else if (!minecraft.player.getCooldowns().isOnCooldown(belt)) {
                ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.START_EQUIPPED, 0));
            }
            return;
        }
        if (drinksOnce(belt) && drinkLatched && (drinkFirst || !yields)) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!PotionBeltItem.canDrink(minecraft.player, belt) || (!drinkFirst && yields)) {
            syncCrossbowShot(minecraft, event.getHand());
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (drinksOnce(belt)) {
            drinkLatched = true;
        }
        if (main.getItem() instanceof PotionBeltItem && main == belt) {
            activateHeld(minecraft, main, InteractionHand.MAIN_HAND);
        } else if (offhand.getItem() instanceof PotionBeltItem && offhand == belt) {
            activateHeld(minecraft, offhand, InteractionHand.OFF_HAND);
        } else {
            continueEquipped(minecraft);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        BeltKeys.sync(minecraft);
        if (minecraft.player == null
                || !BeltKeys.isDown()
                || BeltSource.preferred(minecraft.player).isEmpty()
                || event.getScrollDeltaY() == 0) {
            return;
        }
        int direction = event.getScrollDeltaY() > 0 ? -1 : 1;
        ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.SCROLL, direction));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1
                || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        var slot = ((AbstractContainerScreenAccessor) screen).buffsPotions$getHoveredSlot();
        if (slot == null || !(slot.getItem().getItem() instanceof PotionBeltItem) || !screen.getMenu().getCarried().isEmpty()) {
            return;
        }
        boolean fallbackSlot = slot.container instanceof FallbackBeltContainer;
        boolean curiosSlot = slot.getClass().getName().startsWith("top.theillusivec4.curios");
        if (fallbackSlot || curiosSlot) {
            ClientPacketDistributor.sendToServer(
                    new BeltActionPayload(BeltActionPayload.Action.OPEN_EQUIPPED, 0)
            );
            event.setCanceled(true);
            return;
        }
        if (!(screen instanceof CreativeModeInventoryScreen) || !(slot.container instanceof Inventory inventory)) {
            return;
        }
        int index = slot.getSlotIndex();
        if (index < 0 || index >= inventory.getContainerSize()) {
            return;
        }
        ClientPacketDistributor.sendToServer(
                new BeltActionPayload(BeltActionPayload.Action.OPEN_HAND, BeltItemContainer.inventorySource(index))
        );
        event.setCanceled(true);
    }

    private static void switchRow(InputEvent.InteractionKeyMappingTriggered event, Minecraft minecraft) {
        if (!BeltKeys.isDown()) {
            return;
        }
        ItemStack belt = BeltSource.preferred(minecraft.player);
        if (belt.getItem() instanceof FlagonBeltItem flagon && flagon.hasThrowMode()) {
            if (!rowSwitchSent) {
                rowSwitchSent = true;
                int next = (flagon.throwMode(belt) + 1) % flagon.modeCount();
                FlagonBeltItem.setMode(belt, next);
                BeltSource.saveShown(minecraft.player, belt);
                ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.SWITCH_MODE, next));
            }
            return;
        }
        if (!BeltEnchantments.hasGauge(belt)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (rowSwitchSent) {
            return;
        }
        rowSwitchSent = true;
        ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.SWITCH_ROW, 0));
    }

    /** Flagon, creeper, and dragon belts in drink mode take one dose per right-click. */
    private static boolean drinksOnce(ItemStack belt) {
        return belt.getItem() instanceof FlagonBeltItem flagon && !flagon.throwsPotions(belt);
    }

    /** Creeper and dragon belts in splash or linger mode throw again each time the held click clears the cooldown. */
    private static boolean repeatsThrow(Player player, ItemStack belt) {
        if (!(belt.getItem() instanceof FlagonBeltItem flagon) || !flagon.hasThrowMode() || !flagon.throwsPotions(belt)) {
            return false;
        }
        return BeltConsumptionService.shouldSplash(player, belt, BeltContents.get(belt, BeltContents.activeSlot(belt)));
    }

    private static void throwHeld(Minecraft minecraft, ItemStack belt, InteractionHand hand) {
        if (minecraft.gameMode == null || minecraft.player.getCooldowns().isOnCooldown(belt)) {
            return;
        }
        minecraft.gameMode.useItem(minecraft.player, hand);
    }

    private static void activateHeld(Minecraft minecraft, ItemStack belt, InteractionHand hand) {
        if (minecraft.player == null) {
            return;
        }
        if (beltBusy(minecraft, belt)) {
            if (!drinksOnce(belt)) {
                heldUseSent = false;
            }
            heldChain = true;
            return;
        }
        if (heldUseSent) {
            return;
        }
        heldUseSent = true;
        if (!PotionBeltItem.canDrink(minecraft.player, belt)) {
            return;
        }
        heldChain = true;
        if (minecraft.gameMode != null) {
            minecraft.gameMode.useItem(minecraft.player, hand);
        }
    }

    private static void continueEquipped(Minecraft minecraft) {
        ItemStack belt = BeltSource.equipped(minecraft.player);
        if (beltBusy(minecraft, belt)) {
            if (!drinksOnce(belt)) {
                equippedWasBusy = true;
            }
            return;
        }
        if (equippedWasBusy) {
            equippedUseSent = false;
            equippedWasBusy = false;
        }
        if (equippedUseSent) {
            return;
        }
        equippedUseSent = true;
        ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.START_EQUIPPED, 0));
    }

    private static boolean beltBusy(Minecraft minecraft, ItemStack belt) {
        BeltUseState state = minecraft.player.getData(ModAttachments.BELT_USE);
        return minecraft.player.isUsingItem()
                || state.isDrinking()
                || state.pendingAdvance()
                || minecraft.player.getCooldowns().isOnCooldown(belt);
    }

    private static void releaseUseIfNeeded(Minecraft minecraft) {
        if (!minecraft.options.keyAttack.isDown()) {
            rowSwitchSent = false;
        }
        if (minecraft.options.keyUse.isDown()) {
            return;
        }
        heldUseSent = false;
        heldChain = false;
        drinkLatched = false;
        drinkArm = false;
        equippedWasBusy = false;
        beltMenuSent = false;
        if (equippedUseSent) {
            ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.STOP_DRINK, 0));
            equippedUseSent = false;
        }
    }

    private static void tickPotionName(Minecraft minecraft) {
        boolean useBelt = BeltKeys.isDown();
        ItemStack belt = useBelt ? BeltSource.preferred(minecraft.player) : ItemStack.EMPTY;
        ItemStack dose = ItemStack.EMPTY;
        if (!belt.isEmpty()) {
            dose = BeltContents.get(belt, BeltEnchantments.displayedActive(belt));
        }
        boolean changed = potionNameStack.isEmpty()
                ? !dose.isEmpty()
                : dose.isEmpty()
                || !dose.is(potionNameStack.getItem())
                || !dose.getHoverName().equals(potionNameStack.getHoverName());
        if (!useBelt || dose.isEmpty()) {
            potionNameTimer = 0;
            potionNameStack = ItemStack.EMPTY;
        } else if (!shiftWasDown || changed) {
            potionNameTimer = (int) (40.0 * minecraft.options.notificationDisplayTime().get());
            potionNameStack = dose.copy();
        } else if (potionNameTimer > 0) {
            potionNameTimer--;
        }
        shiftWasDown = useBelt;
    }

    private static void releaseHeldConsumable(Minecraft minecraft) {
        if (!BeltKeys.isDown()
                || minecraft.player.getMainHandItem().getItem() instanceof PotionBeltItem
                || !minecraft.player.isUsingItem()
                || minecraft.gameMode == null) {
            return;
        }
        if (!PotionBeltItem.canDrink(minecraft.player, BeltSource.shown(minecraft.player))) {
            return;
        }
        ItemStack using = minecraft.player.getUseItem();
        if (using.has(DataComponents.CONSUMABLE) && !(using.getItem() instanceof PotionBeltItem)) {
            minecraft.gameMode.releaseUsingItem(minecraft.player);
        }
    }

    private static void releaseUseKeys() {
        heldUseSent = false;
        heldChain = false;
        drinkLatched = false;
        drinkArm = false;
        equippedWasBusy = false;
        beltMenuSent = false;
        rowSwitchSent = false;
        shiftWasDown = false;
        potionNameTimer = 0;
        potionNameStack = ItemStack.EMPTY;
        if (equippedUseSent && Minecraft.getInstance().getConnection() != null) {
            ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.STOP_DRINK, 0));
        }
        equippedUseSent = false;
    }

    private static void releaseAll() {
        for (int slot = 0; slot < PREVIOUS_NUMBER_KEYS.length; slot++) {
            PREVIOUS_NUMBER_KEYS[slot] = false;
        }
        releaseUseKeys();
    }

    private static void renderBeltOverlay(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.hud.isHidden() || !BeltKeys.isDown()) {
            return;
        }
        ItemStack belt = BeltSource.preferred(minecraft.player);
        if (belt.isEmpty()) {
            return;
        }

        int width = 182;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 72;
        int origin = BeltEnchantments.rowStart(belt);
        int column = BeltEnchantments.displayedActive(belt) - origin;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace("hud/hotbar"), x, y, width, 22);
        blitSelection(graphics, x - 1 + column * 20, y - 1);
        Identifier emptySlot = belt.getItem() instanceof FlagonBeltItem
                ? Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/flagon")
                : Identifier.withDefaultNamespace("container/slot/potion");
        float cooldown = minecraft.player.getCooldowns().getCooldownPercent(
                belt,
                partialTick.getGameTimeDeltaPartialTick(true)
        );
        for (int slot = 0; slot < BeltContents.BASE_SLOTS; slot++) {
            int slotX = x + 3 + slot * 20;
            int slotY = y + 3;
            ItemStack dose = BeltContents.get(belt, origin + slot);
            if (dose.isEmpty()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, emptySlot, slotX, slotY, 16, 16);
            } else {
                graphics.item(dose, slotX, slotY);
            }
            if (cooldown > 0.0F) {
                int top = slotY + Mth.floor(16.0F * (1.0F - cooldown));
                int bottom = top + Mth.ceil(16.0F * cooldown);
                graphics.fill(RenderPipelines.GUI, slotX, top, slotX + 16, bottom, Integer.MAX_VALUE);
            }
        }
        renderThrowMode(graphics, belt, x, y);
        ItemStack activeDose = BeltContents.get(belt, origin + column);
        if (potionNameTimer > 0 && !activeDose.isEmpty()) {
            MutableComponent name = Component.empty()
                    .append(activeDose.getHoverName())
                    .withStyle(activeDose.getRarity().color());
            if (activeDose.has(DataComponents.CUSTOM_NAME)) {
                name.withStyle(ChatFormatting.ITALIC);
            }
            int alpha = (int) (potionNameTimer * 256.0F / 10.0F);
            if (alpha > 255) {
                alpha = 255;
            }
            if (alpha > 0) {
                int textWidth = minecraft.font.width(name);
                int textX = (graphics.guiWidth() - textWidth) / 2;
                graphics.textWithBackdrop(minecraft.font, name, textX, y - 14, textWidth, ARGB.white(alpha));
            }
        }
    }

    private static void syncCrossbowShot(Minecraft minecraft, InteractionHand hand) {
        if (minecraft.player == null || hand == null) {
            return;
        }
        ItemStack weapon = minecraft.player.getItemInHand(hand);
        if (!(weapon.getItem() instanceof CrossbowItem) || !CrossbowItem.isCharged(weapon)) {
            return;
        }
        ItemStack belt = BeltSlotAccess.getEquipped(minecraft.player);
        if (!(belt.getItem() instanceof FlagonBeltItem flagon) || !BeltEnchantments.hasTipping(belt)) {
            return;
        }
        int packed = 256 + flagon.throwMode(belt) * 32 + BeltContents.activeSlot(belt);
        ClientPacketDistributor.sendToServer(new BeltActionPayload(BeltActionPayload.Action.SWITCH_MODE, packed));
    }

    private static void renderThrowMode(GuiGraphicsExtractor graphics, ItemStack belt, int beltX, int beltY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(belt.getItem() instanceof FlagonBeltItem flagon) || !flagon.hasThrowMode()) {
            return;
        }
        int modes = flagon.modeCount();
        int active = flagon.throwMode(belt);
        int modeX = beltX + 182 + 4;
        int modeY = beltY + (22 - modes * 24) / 2;
        Identifier frame = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
        ItemStack stored = BeltContents.get(belt, BeltContents.activeSlot(belt));
        int color = doseColor(minecraft.player, stored);
        for (int mode = 0; mode < modes; mode++) {
            int slotY = modeY + mode * 24;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, modeX, slotY, 29, 24);
            if (mode == active) {
                blitSelection(graphics, modeX + 6, slotY);
            }
            boolean filled = color >= 0 && modeAcceptsDose(stored, minecraft.player, mode);
            graphics.item(filled ? filledIcon(mode, color) : emptyIcon(mode), modeX + 10, slotY + 4);
        }
    }

    /** The vanilla selection sprite has a dark top and sides. One extra black row closes the bottom. */
    private static void blitSelection(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                Identifier.withDefaultNamespace("hud/hotbar_selection"),
                x,
                y,
                24,
                23
        );
        graphics.fill(RenderPipelines.GUI, x, y + 23, x + 24, y + 24, 0xFF000000);
    }

    private static int doseColor(Player player, ItemStack stored) {
        if (!(stored.getItem() instanceof FlagonItem flagon) || !flagon.hasDrinkableDose(player, stored)) {
            return -1;
        }
        int color = flagon.playerContents(player, stored).fluidColor();
        return color == 0 ? -1 : color & 0xFFFFFF;
    }

    /** Rain, sun, and honey cannot be thrown. Incarnation and formation can be splashed, and cannot linger. */
    private static boolean modeAcceptsDose(ItemStack stored, Player player, int mode) {
        if (mode <= 0) {
            return true;
        }
        if (!(stored.getItem() instanceof FlagonItem flagon)) {
            return true;
        }
        FlagonContents contents = flagon.playerContents(player, stored);
        ItemStack dose = contents.dose();
        if (contents.isHoney() || BeltConsumptionService.isWeatherCalling(dose)) {
            return false;
        }
        if (Formation.isFormation(dose)) {
            return mode == 1;
        }
        if (!Incarnation.isIncarnation(dose)) {
            return true;
        }
        return mode == 1;
    }

    private static ItemStack filledIcon(int mode, int color) {
        Item item = mode == 0 ? Items.POTION : mode == 1 ? Items.SPLASH_POTION : Items.LINGERING_POTION;
        ItemStack icon = new ItemStack(item);
        icon.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
        return icon;
    }

    private static ItemStack emptyIcon(int mode) {
        if (mode <= 0) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStack icon = new ItemStack(mode == 1 ? Items.SPLASH_POTION : Items.LINGERING_POTION);
        icon.set(
                DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, mode == 1 ? "empty_splash_bottle" : "empty_lingering_bottle")
        );
        return icon;
    }
}
