package com.buffspotions.still;

import com.buffspotions.bellows.BellowsBoosts;
import com.buffspotions.bellows.BellowsTarget;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.menu.BrewingStillMenu;
import com.buffspotions.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public final class BrewingStillBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, BellowsTarget {
    public static final int SLOT_BOTTLE = 0;
    public static final int SLOT_INGREDIENT = 1;
    public static final int SLOT_FUEL = 2;
    public static final int SLOT_COUNT = 3;
    public static final int BREW_TIME = 400;
    public static final int FUEL_TIME = 1600;

    private static final int[] SLOTS_DOWN = {SLOT_BOTTLE, SLOT_INGREDIENT};
    private static final int[] SLOTS_UP = {SLOT_INGREDIENT};
    private static final int[] SLOTS_SIDE = {SLOT_BOTTLE, SLOT_FUEL};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    int brewTime;
    int litTime;
    int litDuration;
    private ItemStack brewingBottle = ItemStack.EMPTY;
    private ItemStack brewingIngredient = ItemStack.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> brewTime;
                case 1 -> litTime;
                case 2 -> litDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> brewTime = value;
                case 1 -> litTime = value;
                case 2 -> litDuration = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BrewingStillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREWING_STILL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrewingStillBlockEntity still) {
        boolean burning = still.tickBrew(level.potionBrewing(), level);
        if (BellowsBoosts.isActive(level, pos)) {
            burning = still.tickBrew(level.potionBrewing(), level);
            if (level instanceof net.minecraft.server.level.ServerLevel server && server.getGameTime() % 10 == 0) {
                BellowsBoosts.trickle(server, pos);
            }
        }
        boolean hasBottle = !still.getItem(SLOT_BOTTLE).isEmpty();
        if (state.getValue(BrewingStillBlock.LIT) != burning || state.getValue(BrewingStillBlock.HAS_BOTTLE) != hasBottle) {
            level.setBlock(
                    pos,
                    state.setValue(BrewingStillBlock.LIT, burning).setValue(BrewingStillBlock.HAS_BOTTLE, hasBottle),
                    3
            );
        }
    }

    @Override
    public boolean isWorking() {
        return litTime > 0 && brewTime > 0;
    }

    void tryFlowerBrew(ServerLevel level, BlockPos pos, RandomSource random) {
        if (brewTime > 0 || !(level.getBlockState(pos.above()).getBlock() instanceof FlowerPotBlock pot)) {
            return;
        }
        if (pot.getPotted() == Blocks.AIR || !FlowerPotBrewing.isSuspiciousBrew(getItem(SLOT_BOTTLE))) {
            return;
        }
        if (!FlowerPotBrewing.mapped(level, BuiltInRegistries.BLOCK.getKey(pot.getPotted()))) {
            return;
        }
        if (random.nextFloat() >= (float) BuffsPotionsConfig.flowerStillChance()) {
            return;
        }
        ItemStack brewed = FlowerPotBrewing.brew(level, BuiltInRegistries.BLOCK.getKey(pot.getPotted()));
        if (brewed.isEmpty()) {
            return;
        }
        setItem(SLOT_BOTTLE, brewed);
        brewingBottle = ItemStack.EMPTY;
        brewingIngredient = ItemStack.EMPTY;
        level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    boolean tickBrew(PotionBrewing brewing, @Nullable Level level) {
        int previousLit = litTime;
        int previousBrew = brewTime;
        if (litTime > 0) {
            litTime--;
        }
        boolean burning = litTime > 0;
        if (canBrew(brewing)) {
            if (brewTime > 0 && brewingBottle.isEmpty()) {
                rememberMix();
            } else if (brewTime > 0 && !sameMix()) {
                brewTime = 0;
            }
            if (!burning && consumeCharcoal()) {
                burning = true;
            }
            if (burning) {
                if (brewTime == 0) {
                    rememberMix();
                }
                brewTime++;
                if (brewTime >= BREW_TIME) {
                    brewTime = 0;
                    finishBrew(brewing, level);
                }
            } else {
                brewTime = 0;
                brewingBottle = ItemStack.EMPTY;
                brewingIngredient = ItemStack.EMPTY;
            }
        } else if (brewTime != 0 || !brewingBottle.isEmpty()) {
            brewTime = 0;
            brewingBottle = ItemStack.EMPTY;
            brewingIngredient = ItemStack.EMPTY;
        }
        if (litTime != previousLit || brewTime != previousBrew) {
            setChanged();
        }
        return burning;
    }

    private boolean canBrew(PotionBrewing brewing) {
        return StillBrewing.hasMix(brewing, items.get(SLOT_BOTTLE), items.get(SLOT_INGREDIENT));
    }

    private boolean sameMix() {
        return ItemStack.isSameItemSameComponents(items.get(SLOT_BOTTLE), brewingBottle)
                && ItemStack.isSameItemSameComponents(items.get(SLOT_INGREDIENT), brewingIngredient);
    }

    private void rememberMix() {
        brewingBottle = items.get(SLOT_BOTTLE).copyWithCount(1);
        brewingIngredient = items.get(SLOT_INGREDIENT).copyWithCount(1);
    }

    private boolean consumeCharcoal() {
        ItemStack fuel = items.get(SLOT_FUEL);
        if (!fuel.is(Items.CHARCOAL)) {
            return false;
        }
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (remainder != null && fuel.isEmpty()) {
            items.set(SLOT_FUEL, remainder.create());
        }
        litTime = FUEL_TIME;
        litDuration = FUEL_TIME;
        return true;
    }

    private void finishBrew(PotionBrewing brewing, @Nullable Level level) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
        stacks.set(0, items.get(SLOT_BOTTLE).copy());
        stacks.set(3, items.get(SLOT_INGREDIENT).copy());
        if (EventHooks.onPotionAttemptBrew(stacks)) {
            items.set(SLOT_BOTTLE, stacks.get(0));
            items.set(SLOT_INGREDIENT, stacks.get(3));
        } else {
            ItemStack ingredient = stacks.get(3);
            ItemStack brewed = StillBrewing.mix(brewing, ingredient, stacks.get(0));
            if (brewed.isEmpty()) {
                brewingBottle = ItemStack.EMPTY;
                brewingIngredient = ItemStack.EMPTY;
                setChanged();
                return;
            }
            stacks.set(0, brewed);
            EventHooks.onPotionBrewed(stacks);
            ItemStackTemplate remainder = ingredient.getCraftingRemainder();
            ingredient.shrink(1);
            if (remainder != null) {
                if (ingredient.isEmpty()) {
                    ingredient = remainder.create();
                } else if (level != null) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.create());
                }
            }
            items.set(SLOT_BOTTLE, stacks.get(0));
            items.set(SLOT_INGREDIENT, ingredient);
            if (level != null) {
                level.levelEvent(1035, worldPosition, 0);
            }
        }
        brewingBottle = ItemStack.EMPTY;
        brewingIngredient = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.buffs_potions.brewing_still");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BrewingStillMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        brewTime = input.getIntOr("BrewTime", 0);
        litTime = input.getIntOr("LitTime", 0);
        litDuration = input.getIntOr("LitDuration", 0);
        brewingBottle = ItemStack.EMPTY;
        brewingIngredient = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BrewTime", brewTime);
        output.putInt("LitTime", litTime);
        output.putInt("LitDuration", litDuration);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> stack.is(Items.CHARCOAL);
            case SLOT_INGREDIENT -> StillBrewing.isIngredient(stack);
            case SLOT_BOTTLE -> (StillBrewing.isBottle(stack) || stack.is(Items.GLASS_BOTTLE)) && getItem(slot).isEmpty();
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_DOWN;
        }
        if (direction == Direction.UP) {
            return SLOTS_UP;
        }
        return SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot != SLOT_INGREDIENT || stack.is(Items.GLASS_BOTTLE);
    }
}
