package org.infernalstudios.nebs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, NekosEnchantedBooks.MOD_ID, existingFileHelper);
    }

    public ModelFile generateModel(String name) {
        ResourceLocation location = modLoc("item/" + name);
        if (!existingFileHelper.exists(location, PackType.CLIENT_RESOURCES, ".png", "textures")) {
            NekosEnchantedBooks.LOGGER.debug(name + " book texture not found, defaulting...");
            location = mcLoc("item/enchanted_book");
        }

        return getBuilder(name).parent(new ModelFile.UncheckedModelFile("item/generated")).texture("layer0", location);
    }

    @Override
    protected void registerModels() {
        if (NekosEnchantedBooks.enchantmentMap == null) {
            InputStreamReader input = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("assets/nebs/models/properties.json")), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, Float>>(){}.getType();
            NekosEnchantedBooks.enchantmentMap = new Gson().fromJson(new BufferedReader(input), type);
        }

        ItemModelBuilder enchanted_book = getBuilder("minecraft:item/enchanted_book")
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", mcLoc("item/enchanted_book"));

        for (Map.Entry<String, Float> entry : NekosEnchantedBooks.enchantmentMap.entrySet()) {
            ModelFile file = generateModel(entry.getKey().split("\\.")[2]);
            enchanted_book.override()
                    .predicate(modLoc("enchant"), entry.getValue())
                    .model(file)
                    .end();
        }
    }
}
