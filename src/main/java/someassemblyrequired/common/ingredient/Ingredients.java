package someassemblyrequired.common.ingredient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.common.ingredient.behavior.*;
import someassemblyrequired.common.init.ModItems;
import someassemblyrequired.integration.ModCompat;
import someassemblyrequired.integration.farmersdelight.FarmersDelightCompat;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Ingredients {

    private static final HashMap<Item, IngredientBehavior> INGREDIENT_BEHAVIORS = new HashMap<>();

    public static void addBehavior(Item item, IngredientBehavior properties) {
        if (INGREDIENT_BEHAVIORS.get(item) != null) {
            SomeAssemblyRequired.LOGGER.error("Multiple ingredient behaviors for item " + item.getRegistryName());
        } else {
            INGREDIENT_BEHAVIORS.put(item, properties);
        }
    }

    public static void addBehaviors() {
        addBehavior(Items.CHORUS_FRUIT, new ChorusFruitBehavior());
        addBehavior(Items.SUSPICIOUS_STEW, new SuspiciousStewBehavior());
        addBehavior(Items.MILK_BUCKET, new MilkBucketBehavior());
        addBehavior(Items.HONEY_BOTTLE, new HoneyBottleBehavior());
        addBehavior(Items.POTION, new PotionBehavior());
    }

    public static boolean canAddToSandwich(ItemStack item) {
        return !item.isEmpty() && !item.is(ModItems.SANDWICH.get()) && (item.isEdible() || IngredientPropertiesManager.get(item) != null || ModCompat.isFarmersDelightLoaded() && FarmersDelightCompat.canAddToSandwich(item));
    }

    public static FoodProperties getFood(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getFood(item);
    }

    public static void onFoodEaten(ItemStack item, LivingEntity entity) {
        if (ModCompat.isFarmersDelightLoaded()) {
            FarmersDelightCompat.onFoodEaten(item, entity);
        }
        if (INGREDIENT_BEHAVIORS.containsKey(item.getItem())) {
            INGREDIENT_BEHAVIORS.get(item.getItem()).onEaten(item, entity);
        }
    }

    public static Component getDisplayName(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getDisplayName(item);
    }

    public static Component getFullName(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getFullName(item);
    }

    public static ItemStack getDisplayItem(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getDisplayItem(item);
    }

    public static ItemStack getContainer(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getContainer(item);
    }

    public static boolean hasContainer(ItemStack item) {
        return !getContainer(item).isEmpty();
    }

    public static void playApplySound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        IngredientPropertiesManager.getOrDefault(item).playApplySound(item, level, player, pos);
    }

    public static void playRemoveSound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        IngredientPropertiesManager.getOrDefault(item).playRemoveSound(item, level, player, pos);
    }
}