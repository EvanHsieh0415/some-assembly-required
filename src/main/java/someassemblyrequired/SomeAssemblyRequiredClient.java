package someassemblyrequired;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import someassemblyrequired.block.SandwichBlockRenderer;
import someassemblyrequired.registry.ModBlockEntityTypes;
import someassemblyrequired.registry.ModItems;

public class SomeAssemblyRequiredClient {

    public SomeAssemblyRequiredClient() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterColorHandlers);
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BlockEntityRenderers.register(ModBlockEntityTypes.SANDWICH.get(), SandwichBlockRenderer::new));
    }

    public void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        event.register((itemStack, tintIndex) -> {
            CompoundTag tag = itemStack.getTag();
            if (tag != null && tintIndex == 0 && tag.contains("Color", Tag.TAG_INT)) {
                return tag.getInt("Color");
            }
            return 0xFFFFFF;
        }, ModItems.SPREAD.get());
    }
}
