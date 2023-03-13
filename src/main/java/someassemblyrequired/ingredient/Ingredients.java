package someassemblyrequired.ingredient;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.ingredient.behavior.*;
import someassemblyrequired.init.ModFoods;

import javax.annotation.Nullable;
import java.util.HashMap;

public class Ingredients {

    private static final HashMap<Item, IngredientBehavior> INGREDIENT_BEHAVIORS = new HashMap<>();

    public static void addBehavior(Item item, IngredientBehavior properties) {
        if (INGREDIENT_BEHAVIORS.get(item) != null) {
            SomeAssemblyRequired.LOGGER.error("Multiple ingredient behaviors for item " + ForgeRegistries.ITEMS.getKey(item));
        } else {
            INGREDIENT_BEHAVIORS.put(item, properties);
        }
    }

    public static void addBehaviors() {
        addBehavior(Items.CHORUS_FRUIT, new ChorusFruitBehavior());
        addBehavior(Items.SUSPICIOUS_STEW, new SuspiciousStewBehavior());
        addBehavior(Items.MILK_BUCKET, new MilkBucketBehavior());
        addBehavior(Items.HONEY_BOTTLE, new HoneyBottleBehavior());
    }

    public static boolean hasCustomIngredientProperties(Item item) {
        return IngredientPropertiesManager.hasIngredientFor(item);
    }

    public static boolean canAddToSandwich(ItemStack item) {
        return !item.isEmpty() && (item.isEdible() || IngredientPropertiesManager.get(item) != null);
    }

    public static FoodProperties getFood(ItemStack item, @Nullable LivingEntity entity) {
        FoodProperties result = IngredientPropertiesManager.getOrDefault(item).getFood(item, entity);
        return result == null ? ModFoods.EMPTY : result;
    }

    public static void onFoodEaten(ItemStack item, LivingEntity entity) {
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

    public static int getHeight(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).getHeight(item);
    }

    public static boolean shouldRenderAsItem(ItemStack item) {
        return IngredientPropertiesManager.getOrDefault(item).shouldRenderAsItem(item);
    }

    public static void playApplySound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        IngredientPropertiesManager.getOrDefault(item).playSound(level, player, pos, 1);
    }

    public static void playRemoveSound(ItemStack item, Level level, @Nullable Player player, BlockPos pos) {
        IngredientPropertiesManager.getOrDefault(item).playSound(level, player, pos, 1.2F);
    }
}
