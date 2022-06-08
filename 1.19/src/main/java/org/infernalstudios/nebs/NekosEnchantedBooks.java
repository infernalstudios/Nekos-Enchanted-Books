package org.infernalstudios.nebs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.network.NetworkConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    public static final String MOD_ID = "nebs";
    public static Map<String, Float> enchantmentMap;

    public static final Logger LOGGER = LogManager.getLogger();

    public NekosEnchantedBooks() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::doClientStuff);
        bus.addListener(this::gatherData);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void doClientStuff (final FMLClientSetupEvent event) {
        InputStreamReader input = new InputStreamReader(Objects.requireNonNull(
                NekosEnchantedBooks.class.getClassLoader().getResourceAsStream("assets/nebs/models/properties.json")),
                StandardCharsets.UTF_8);
        Type type = new TypeToken<Map<String, Float>>(){}.getType();
        enchantmentMap = new Gson().fromJson(new BufferedReader(input), type);

        ItemProperties.register(Items.ENCHANTED_BOOK, new ResourceLocation("nebs:enchant"), (stack, world, entity, i) -> {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
            if (map.isEmpty() || enchantmentMap == null) {
                return 0.0F;
            }

            String key = map.entrySet().iterator().next().getKey().getDescriptionId();
            return enchantmentMap.getOrDefault(key, 0.0F);
        });
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();

        if (event.includeClient()) {
            gen.addProvider(true, new ModItemModelProvider(gen, event.getExistingFileHelper()));
        }
    }
}
