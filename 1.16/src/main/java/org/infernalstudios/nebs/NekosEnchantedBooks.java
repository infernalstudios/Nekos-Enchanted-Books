package org.infernalstudios.nebs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.data.DataGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;

@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    public static final String MOD_ID = "nebs";
    public static Map<String, Float> enchantmentMap;

    public static final Logger LOGGER = LogManager.getLogger();

    public NekosEnchantedBooks() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        // Only load the mod on the client side
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
            bus.addListener(this::doClientStuff);
            bus.addListener(this::gatherData);
            MinecraftForge.EVENT_BUS.register(this);
        });
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        InputStreamReader input = new InputStreamReader(Objects.requireNonNull(
                NekosEnchantedBooks.class.getClassLoader().getResourceAsStream("assets/nebs/models/properties.json")),
                StandardCharsets.UTF_8);
        Type type = new TypeToken<Map<String, Float>>(){}.getType();
        enchantmentMap = new Gson().fromJson(new BufferedReader(input), type);

        // enqueue this part so we don't concurrently modify the client item properties
        event.enqueueWork(() -> {
            ItemModelsProperties.register(Items.ENCHANTED_BOOK, new ResourceLocation("nebs:enchant"), (stack, world, entity) -> {
                Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(stack);
                if (map.isEmpty() || enchantmentMap == null) {
                    return 0.0F;
                }

                String key = map.keySet().iterator().next().getDescriptionId();
                return enchantmentMap.getOrDefault(key, 0.0F);
            });
        });
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();

        if (event.includeClient()) {
            gen.addProvider(new ModItemModelProvider(gen, event.getExistingFileHelper()));
        }
    }
}