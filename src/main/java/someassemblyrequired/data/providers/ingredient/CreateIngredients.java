package someassemblyrequired.data.providers.ingredient;

import com.simibubi.create.AllItems;
import someassemblyrequired.data.providers.Ingredients;

public class CreateIngredients {

    public static void addIngredients(Ingredients ingredients) {
        ingredients.builder(AllItems.BUILDERS_TEA.get()).setBottled().setSpread(0xdf8367).setMoistSound();
    }
}
