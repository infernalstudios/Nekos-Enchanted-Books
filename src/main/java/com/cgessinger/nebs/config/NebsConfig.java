package com.cgessinger.nebs.config;

import com.cgessinger.nebs.NekosEnchantedBooks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Type;
import java.util.Map;

@Mod.EventBusSubscriber(modid = NekosEnchantedBooks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NebsConfig
{
	public static final ClientConfig CLIENT;
	public static final ForgeConfigSpec CLIENT_SPEC;

	public static Map<String, Float> enchantementMap;

	static {
		final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
		CLIENT_SPEC = specPair.getRight();
		CLIENT = specPair.getLeft();
	}

	public static class ClientConfig
	{
		public final ForgeConfigSpec.ConfigValue<String> enchantment_map;

		public ClientConfig (ForgeConfigSpec.Builder builder)
		{
			builder.comment("Neko's Enchanted Books: general").push(NekosEnchantedBooks.MOD_ID);

			enchantment_map = builder.comment(
					"This maps the enchantments to a given ResourceLocation")
					.translation("nebs.configgui.enchantmentMap").define("enchantment_map", defaultConfig);

			builder.pop();
		}

	}

	@SubscribeEvent
	public static void onModConfigEvent(final ModConfig.ModConfigEvent configEvent)
	{
		if (configEvent.getConfig().getSpec() == NebsConfig.CLIENT_SPEC)
		{
			bakeConfig();
		}
	}

	public static void bakeConfig()
	{
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, Float>>(){}.getType();
		enchantementMap = gson.fromJson(CLIENT.enchantment_map.get(), type);
	}

	private static final String defaultConfig = "{\"enchantment.minecraft.aqua_affinity\":1,\"enchantment.minecraft.bane_of_arthropods\":1.01,\"enchantment.minecraft.blast_protection\":1.02,\"enchantment.minecraft.channeling\":1.03,\"enchantment.minecraft.cleaving\":1.04,\"enchantment.minecraft.curse_of_binding\":1.05,\"enchantment.minecraft.curse_of_vanishing\":1.06,\"enchantment.minecraft.depth_strider\":1.07,\"enchantment.minecraft.efficiency\":1.08,\"enchantment.minecraft.feather_falling\":1.09,\"enchantment.minecraft.fire_aspect\":1.1,\"enchantment.minecraft.flame\":1.11,\"enchantment.minecraft.fortune\":1.12,\"enchantment.minecraft.frost_walker\":1.13,\"enchantment.minecraft.impaling\":1.14,\"enchantment.minecraft.infinity\":1.15,\"enchantment.minecraft.knockback\":1.16,\"enchantment.minecraft.looting\":1.17,\"enchantment.minecraft.loyalty\":1.18,\"enchantment.minecraft.luck_of_the_sea\":1.19,\"enchantment.minecraft.lure\":1.2,\"enchantment.minecraft.mending\":1.21,\"enchantment.minecraft.multishot\":1.22,\"enchantment.minecraft.piercing\":1.23,\"enchantment.minecraft.power\":1.24,\"enchantment.minecraft.projectile_protection\":1.25,\"enchantment.minecraft.protection\":1.26,\"enchantment.minecraft.punch\":1.27,\"enchantment.minecraft.quick_charge\":1.28,\"enchantment.minecraft.respiration\":1.29,\"enchantment.minecraft.riptide\":1.3,\"enchantment.minecraft.sharpness\":1.31,\"enchantment.minecraft.silk_touch\":1.32,\"enchantment.minecraft.smite\":1.33,\"enchantment.minecraft.soul_speed\":1.34,\"enchantment.minecraft.sweeping_edge\":1.35,\"enchantment.minecraft.thorns\":1.36,\"enchantment.minecraft.unbreaking\":1.37}";
}
