package com.cgessinger.nebs;

import com.cgessinger.nebs.config.NebsConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import sun.net.ResourceManager;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ModItemModelProvider extends ItemModelProvider
{
	public ModItemModelProvider (DataGenerator generator, ExistingFileHelper existingFileHelper)
	{
		super(generator, NekosEnchantedBooks.MOD_ID, existingFileHelper);
	}

	public ModelFile generateModel (String name)
	{
		ResourceLocation location = modLoc("items/" + name);
		if(!existingFileHelper.exists(location, ResourcePackType.CLIENT_RESOURCES, ".png", "textures"))
		{
			location = mcLoc("item/enchanted_book");
		}

		return getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile("item/generated"))
				.texture("layer0", location);
	}

	@Override
	protected void registerModels ()
	{
		System.out.println("register entries model");
		if(NebsConfig.enchantementMap == null)
		{
			InputStreamReader input = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("assets/nebs/models/properties.json")), StandardCharsets.UTF_8);
			Type type = new TypeToken<Map<String, Float>>(){}.getType();
			NebsConfig.enchantementMap = new Gson().fromJson(new BufferedReader(input), type);
		}

		for(Map.Entry<String, Float> entry : NebsConfig.enchantementMap.entrySet())
		{
			System.out.println("adding entries model" + entry);
			ModelFile file = generateModel(entry.getKey().split("\\.")[2]);
			System.out.println("added model at" + file.getLocation());
		}
	}
}
