package someassemblyrequired.ingredient;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import someassemblyrequired.SomeAssemblyRequired;
import someassemblyrequired.network.IngredientSyncPacket;
import someassemblyrequired.network.NetworkHandler;
import someassemblyrequired.integration.ModCompat;
import someassemblyrequired.integration.jei.JEIUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class IngredientPropertiesManager extends SimpleJsonResourceReloadListener {

    private static final IngredientProperties DEFAULT_PROPERTIES = new IngredientProperties();
    private static final IngredientProperties POTION_PROPERTIES = new PotionProperties();

    private static Map<Item, IngredientProperties> properties = new HashMap<>();

    private IngredientPropertiesManager() {
        super(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(), SomeAssemblyRequired.MODID + "/ingredients");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        properties.clear();
        resources.forEach((resourceLocation, element) -> {
            try {
                if (element.isJsonObject() && !CraftingHelper.processConditions(element.getAsJsonObject(), "conditions", ICondition.IContext.EMPTY)) {
                    SomeAssemblyRequired.LOGGER.debug("Skipping loading ingredient {} as it's conditions were not met", resourceLocation);
                    return;
                }

                JsonObject object = GsonHelper.convertToJsonObject(element, "ingredient");
                IngredientProperties ingredient = IngredientProperties.fromJson(object);
                ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(object, "item"));
                if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                    throw new JsonParseException("Unknown item: " + itemId);
                }
                Item item = ForgeRegistries.ITEMS.getValue(itemId);

                if (properties.containsKey(item)) {
                    SomeAssemblyRequired.LOGGER.error("Multiple ingredients found for item " + itemId);
                }
                properties.put(item, ingredient);
            } catch (IllegalArgumentException | JsonParseException exception) {
                SomeAssemblyRequired.LOGGER.error("Parsing error loading sandwich ingredient {}: {}", resourceLocation, exception.getMessage());
            }
        });

        SomeAssemblyRequired.LOGGER.info("Loaded {} sandwich ingredients", properties.size());
    }

    protected static boolean hasIngredientFor(Item item) {
        return properties.containsKey(item);
    }

    @Nullable
    protected static IngredientProperties get(ItemStack item) {
        if (item.getItem() == Items.POTION) {
            return POTION_PROPERTIES;
        }
        return properties.get(item.getItem());
    }

    protected static IngredientProperties getOrDefault(ItemStack item) {
        if (get(item) != null) {
            return get(item);
        }
        return DEFAULT_PROPERTIES;
    }

    public static void syncIngredientProperties(Map<Item, IngredientProperties> map) {
        properties = map;
        if (ModCompat.isJEILoaded()) {
            JEIUtil.refresh();
        }
    }

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new IngredientPropertiesManager());
    }

    public static void onDataPackReload(OnDatapackSyncEvent event) {
        if (Environment.get().getDist().isClient()) {
            JEIUtil.refresh();
            return;
        }
        if (event.getPlayer() != null) {
            sync(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(IngredientPropertiesManager::sync);
        }
    }

    private static void sync(ServerPlayer player) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new IngredientSyncPacket(properties));
    }
}
