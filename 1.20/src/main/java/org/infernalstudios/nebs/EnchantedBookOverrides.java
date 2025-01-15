package org.infernalstudios.nebs;

import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;

import java.util.List;
import java.util.function.Function;

public class EnchantedBookOverrides extends ItemOverrides {
    public static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    public EnchantedBookOverrides(ModelBaker baker, UnbakedModel model, List<ItemOverride> existing, Function<Material, TextureAtlasSprite> spriteGetter) {
        super(baker, model, existing, spriteGetter);
    }
}
