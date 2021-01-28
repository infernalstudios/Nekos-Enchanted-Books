package com.cgessinger.nebs;

import com.cgessinger.nebs.config.NebsConfig;
import net.minecraft.data.DataGenerator;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks
{
	public static final String MOD_ID = "nebs";

	public NekosEnchantedBooks ()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NebsConfig.CLIENT_SPEC, "nebs_config.toml");

		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

		MinecraftForge.EVENT_BUS.register(this);
	}

	private void doClientStuff (final FMLClientSetupEvent event)
	{
		ItemModelsProperties.registerProperty(Items.ENCHANTED_BOOK, new ResourceLocation("nebs:enchant"), (stack, world, entity) -> {
			if(NebsConfig.enchantementMap == null)
				return 0.0F;

			String key = EnchantmentHelper.getEnchantments(stack).entrySet().iterator().next().getKey().getName();
			return NebsConfig.enchantementMap.getOrDefault(key, 0.0F);
		});
	}

	@Mod.EventBusSubscriber(modid = NekosEnchantedBooks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class GatherDataSubscriber
	{
		@SubscribeEvent
		public static void gatherData (GatherDataEvent event)
		{
			System.out.println("gather mod data");
			DataGenerator gen = event.getGenerator();

			if (event.includeClient())
			{
				gen.addProvider(new ModItemModelProvider(gen, event.getExistingFileHelper()));
			}
		}
}
}
