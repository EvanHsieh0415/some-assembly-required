package someassemblyrequired.item.sandwich;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.block.SandwichBlock;
import someassemblyrequired.ingredient.Ingredients;
import someassemblyrequired.init.ModAdvancementTriggers;
import someassemblyrequired.init.ModFoods;
import someassemblyrequired.init.ModItems;
import someassemblyrequired.init.ModTags;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SandwichItem extends BlockItem {

    public SandwichItem(Block block, Properties builder) {
        super(block, builder);
    }

    public static ItemStack makeSandwich(Potion potion) {
        ItemStack item = new ItemStack(Items.POTION);
        PotionUtils.setPotion(item, potion);
        return makeSandwich(item);
    }

    public static ItemStack makeSandwich(ItemLike... items) {
        return makeSandwich(Arrays.stream(items)
                .map(ItemStack::new)
                .toArray(ItemStack[]::new));
    }

    public static ItemStack makeSandwich(ItemStack... items) {
        ArrayList<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.BREAD_SLICE.get()));
        list.addAll(Arrays.asList(items));
        list.add(new ItemStack(ModItems.BREAD_SLICE.get()));
        return of(list);
    }

    public static ItemStack of(ItemStack... items) {
        return of(Arrays.asList(items));
    }

    public static ItemStack of(List<ItemStack> items) {
        List<ItemStack> flattenedItems = new ArrayList<>();
        for (ItemStack item : items) {
            if (item.getCount() != 1) {
                throw new IllegalArgumentException();
            }
            if (!item.is(ModItems.SANDWICH.get())) {
                flattenedItems.add(item);
            } else {
                SandwichItemHandler.get(item).stream()
                        .map(SandwichItemHandler::getItems)
                        .flatMap(Collection::stream)
                        .forEach(flattenedItems::add);
            }
        }

        ItemStack result = new ItemStack(ModItems.SANDWICH.get());
        result.getOrCreateTagElement("BlockEntityTag").put("Sandwich", SandwichItemHandler.serializeItems(flattenedItems));
        return result;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack sandwich, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(sandwich, world, tooltip, flag);
        SandwichItemHandler.get(sandwich).ifPresent(handler -> handler.getItems()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                item -> Ingredients.getFullName(item).plainCopy(),
                                LinkedHashMap::new,
                                Collectors.counting()
                        )
                )
                .forEach(
                        (item, count) -> {
                            if (count > 1) {
                                tooltip.add(SomeAssemblyRequired.translate("tooltip.ingredient_count", item, count).withStyle(ChatFormatting.GRAY));
                            } else {
                                tooltip.add(item.withStyle(ChatFormatting.GRAY));
                            }
                        }
                ));
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        boolean isPlacingOnTable = context.getLevel().getBlockState(context.getClickedPos().below()).is(ModTags.SANDWICHING_STATIONS);
        if (context.getPlayer() != null && (context.getPlayer().isShiftKeyDown() || isPlacingOnTable)) {
            return super.placeBlock(context, state);
        }
        return false;
    }

    public InteractionResult place(UseOnContext useOnContext, BlockPos pos, ItemStack sandwich) {
        BlockPos clickedPos = pos.below();
        BlockPlaceContext placeContext = BlockPlaceContext.at(new BlockPlaceContext(useOnContext), clickedPos, Direction.UP);
        if (!placeContext.canPlace()) {
            return InteractionResult.FAIL;
        }
        placeContext = this.updatePlacementContext(placeContext);
        if (placeContext == null) {
            return InteractionResult.FAIL;
        }
        BlockState blockstate = this.getPlacementState(placeContext);
        if (blockstate == null) {
            return InteractionResult.FAIL;
        }

        Optional<SandwichItemHandler> itemHandler = SandwichItemHandler.get(sandwich);
        if (itemHandler.isEmpty()) {
            return InteractionResult.FAIL;
        }
        int size = SandwichBlock.getSizeFromSandwich(itemHandler.get());
        blockstate = blockstate.setValue(SandwichBlock.SIZE, size);

        if (!placeBlock(placeContext, blockstate)) {
            return InteractionResult.FAIL;
        }
        Level level = placeContext.getLevel();
        Player player = placeContext.getPlayer();
        BlockState placedState = level.getBlockState(pos);
        if (placedState.is(blockstate.getBlock())) {
            updateCustomBlockEntityTag(pos, level, player, sandwich, placedState);
            placedState.getBlock().setPlacedBy(level, pos, placedState, player, sandwich);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, sandwich);
            }
        }

        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        SoundType soundType = placedState.getSoundType(level, pos, player);
        if (player != null) {
            level.playSound(player, pos, getPlaceSound(placedState, level, pos, player), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
        }
        if (player == null || !player.getAbilities().instabuild) {
            sandwich.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return SandwichItemHandler.get(stack)
                .map(SandwichItemHandler::getFoodProperties)
                .orElse(ModFoods.EMPTY);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (entity instanceof Player player) {
            triggerAdvancements(stack, player);
        }

        SandwichItemHandler.get(stack).ifPresent(sandwich -> {
            for (ItemStack item : sandwich.items) {
                Ingredients.onFoodEaten(item, entity);
            }
        });

        return super.finishUsingItem(stack, world, entity);
    }

    private void triggerAdvancements(ItemStack stack, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        SandwichItemHandler.get(stack).ifPresent(sandwich -> {
            if (sandwich.isDoubleDeckerSandwich()) {
                ModAdvancementTriggers.CONSUME_DOUBLE_DECKER_SANDWICH.trigger(serverPlayer, stack);
            }
            for (ItemStack ingredient : sandwich) {
                if (ingredient.is(Items.POTION) && PotionUtils.getPotion(ingredient) != Potions.WATER) {
                    ModAdvancementTriggers.CONSUME_POTION_SANDWICH.trigger(serverPlayer, stack);
                }
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            return super.getName(stack);
        }
        return SandwichNameHelper.getSandwichDisplayName(stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private final BlockEntityWithoutLevelRenderer renderer = new SandwichItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag compoundNBT) {
        return new ICapabilityProvider() {

            private final LazyOptional<SandwichItemHandler> handler = LazyOptional.of(this::createHandler);

            private SandwichItemHandler createHandler() {
                SandwichItemHandler handler = new ItemHandler(stack);
                handler.deserializeNBT(stack.getOrCreateTagElement("BlockEntityTag").getList("Sandwich", Tag.TAG_COMPOUND));
                return handler;
            }

            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                if (capability == ForgeCapabilities.ITEM_HANDLER) {
                    return handler.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    private static class ItemHandler extends SandwichItemHandler {

        private final ItemStack sandwich;

        private ItemHandler(ItemStack sandwich) {
            this.sandwich = sandwich;
        }

        @Override
        protected void onContentsChanged() {
            super.onContentsChanged();
            sandwich.getOrCreateTagElement("BlockEntityTag").put("Sandwich", serializeNBT());
        }
    }
}
