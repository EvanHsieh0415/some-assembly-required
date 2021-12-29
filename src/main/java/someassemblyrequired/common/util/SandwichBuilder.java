package someassemblyrequired.common.util;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.items.ItemStackHandler;
import someassemblyrequired.common.init.ModItems;

public class SandwichBuilder {

    private final Item bread;
    private final NonNullList<ItemStack> ingredients = NonNullList.create();

    private SandwichBuilder(Item bread) {
        this.bread = bread;
    }

    public static SandwichBuilder create() {
        return create(ModItems.BREAD_SLICE.get()).addBread();
    }

    public static SandwichBuilder create(ItemLike bread) {
        return new SandwichBuilder(bread.asItem());
    }

    public SandwichBuilder addBread() {
        return add(bread);
    }

    public SandwichBuilder addFakeSpread(int color) {
        return addFakeSpread(color, false);
    }

    public SandwichBuilder addFakeSpread(int color, boolean hasGlint) {
        ItemStack spread = new ItemStack(ModItems.SPREAD.get());
        CompoundTag tag = spread.getOrCreateTag();
        tag.putInt("Color", color);
        tag.putBoolean("HasGlint", hasGlint);
        return addStack(spread);
    }

    public SandwichBuilder add(ItemLike item) {
        return addStack(new ItemStack(item));
    }

    private SandwichBuilder addStack(ItemStack ingredient) {
        ingredient.getOrCreateTag().putBoolean("IsOnSandwich", true);
        ingredients.add(ingredient);
        return this;
    }

    public ItemStack build() {
        addBread();
        ItemStack sandwich = new ItemStack(ModItems.SANDWICH.get());
        sandwich.getOrCreateTagElement("BlockEntityTag").put("Ingredients", new ItemStackHandler(ingredients).serializeNBT());
        return sandwich;
    }

    public static ItemStack blt() {
        return create()
                .add(ModItems.LETTUCE_LEAF.get())
                .add(ModItems.BACON_STRIPS.get())
                .add(ModItems.TOMATO_SLICES.get())
                .build();
    }
}