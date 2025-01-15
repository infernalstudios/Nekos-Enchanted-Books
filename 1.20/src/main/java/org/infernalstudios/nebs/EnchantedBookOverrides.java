package org.infernalstudios.nebs;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EnchantedBookOverrides extends ItemOverrides {
    public static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    public static ResourceLocation getEnchantedBookModel(Enchantment enchantment) {
        return new ResourceLocation(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.getDescriptionId().replace(".", "_"));
    }

    private final Map<String, BakedModel> overrides;

    public EnchantedBookOverrides(ModelBaker baker, UnbakedModel enchantedBook, List<ItemOverride> existing, Function<Material, TextureAtlasSprite> spriteGetter) {
        super(baker, enchantedBook, existing, spriteGetter);
        this.overrides = Util.make(new HashMap<>(), map -> {
            ForgeRegistries.ENCHANTMENTS.forEach(e -> {
                final var bakery = ObfuscationReflectionHelper.<ModelBakery, ModelBakery.ModelBakerImpl>getPrivateValue(ModelBakery.ModelBakerImpl.class, (ModelBakery.ModelBakerImpl) baker, "f_243927_");
                final var model = getEnchantedBookModel(e);
                if (!bakery.modelResources.containsKey(ModelBakery.MODEL_LISTER.idToFile(model))) {
                    NekosEnchantedBooks.LOGGER.warn("Enchanted book model for enchantment {} not found", e.getDescriptionId());
                    return;
                }

                baker.getModel(model).resolveParents(baker::getModel);
                map.put(e.getDescriptionId(), baker.bake(model, BlockModelRotation.X0_Y0, spriteGetter));
            });
        });

        NekosEnchantedBooks.LOGGER.info("Enchanted book overrides loaded for {} enchantments", overrides.size());
    }

    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        if (!enchantments.isEmpty()) {
            String key = enchantments.keySet().iterator().next().getDescriptionId();
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return super.resolve(model, stack, level, entity, seed);
    }
}
