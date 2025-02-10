package org.infernalstudios.nebs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <h1>Enchanted Book Overrides</h1>
 * This class is effectively the heart of NEBs, handling the custom models that are to be used for the enchanted books.
 * <p>
 * <h2>Usage for Modders and Modpackers</h2>
 * If you are a modder, you do not need to worry about this class. This class implements the automatic model loading
 * introduced in NEBs 2.0 for you to take advantage of. Here is what you need to know about loading models for your own
 * enchantments or the enchantments of other mods (in the case of modpackers):
 * <ul>
 *     <li>All models are automatically loaded from the root folder {@code assets/nebs/models/item}. Each model is
 *     organized into the {@linkplain NekosEnchantedBooks#idOf(Enchantment) enchantment's NEBs ID} where each point
 *     is a folder separation.</li>
 *     <ul>
 *         <li>For example, if you want to load a model for your enchantment of key
 *         {@code enchantment.mymod.overpowered}, your model must exist in
 *         {@code assets/nebs/models/item/enchantment/mymod/overpowered.json}.</li>
 *         <li><strong>It is strongly recommended</strong> that your model parents off of
 *         {@code minecraft:item/enchanted_book} instead of {@code minecraft:item/generated}, so any custom additions
 *         made to the base model are reflected in yours.</li>
 *     </ul>
 *     <li>The placement of the texture you would like to use does not matter, as long as it is properly referenced in
 *     your model file. If you look at any of NEBs's own models as an example, you will see that the {@code layer0}
 *     texture simply points to a texture image that is in the same structure as the model files are. This makes it easy
 *     for NEBs to generate its own models, but is not a requirement for you.</li>
 *     <li>If a model does not exist for a registered enchantment when models are baked, then your enchantment is simply
 *     ignored and the base {@code minecraft:item/enchanted_book} model is used instead. There is no override or fake
 *     model, the vanilla model is used directly.</li>
 *     <ul>
 *         <li>If there are any missing models for enchantments, a warning will be displayed to the console log for
 *         debugging purposes.</li>
 *     </ul>
 * </ul>
 * <strong>It is important to note</strong> that this class respects any existing overrides that might have been added
 * to the base enchanted book model. However, this is only the case if an enchanted book has an enchantment that is not
 * saved in our own overrides, so it merely acts as a fallback.
 * <h2>Usage for NEBs Developers</h2>
 * Apart from what has already been mentioned, you should read the documentation for each of the methods:
 * <ul>
 *     <li>{@link #EnchantedBookOverrides(ItemOverrides, ModelBaker)}</li>
 *     <li>{@link #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
@SuppressWarnings("deprecation") // We are wrapping things that use deprecated methods
public final class EnchantedBookOverrides extends ItemOverrides {
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    static ResourceLocation locationFrom(String enchantment) {
        return ResourceLocation.fromNamespaceAndPath(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static String idFromModel(ModelResourceLocation model) {
        return model.id().getPath().substring("item/".length()).replace("/", ".");
    }

    private static ModelResourceLocation modelFromFile(ResourceLocation modelFile) {
        return ModelResourceLocation.inventory(modelFile.withPath(s -> s.substring("models/".length(), s.length() - ".json".length())));
    }

    private static final Set<String> TEXTURED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ModelResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final ItemOverrides base;
    private final Map<String, BakedModel> overrides;

    @SuppressWarnings("unused") // BlockModelCoreMod
    public static @Nullable ItemOverrides of(ItemOverrides base, String location, ModelBaker baker) {
        if (!EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location)) return base;

        try {
            return new EnchantedBookOverrides(base, baker);
        } catch (RuntimeException e) {
            NekosEnchantedBooks.LOGGER.error("Failed to bake custom enchanted book overrides!", e);
            return base;
        }
    }

    @SuppressWarnings("unused") // BlockModelCoreMod
    public static @Nullable ItemOverrides of(ItemOverrides base, String location, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter) {
        if (!EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location)) return base;

        try {
            return new EnchantedBookOverrides(base, new ModelBaker() {
                @Override
                public UnbakedModel getModel(ResourceLocation location) {
                    return baker.getModel(location);
                }

                @Override
                public @Nullable BakedModel bake(ResourceLocation location, ModelState state) {
                    return baker.bake(location, state, this.getModelTextureGetter());
                }

                @Override
                public @Nullable BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
                    return baker.bake(location, state, sprites);
                }

                @Override
                public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
                    return spriteGetter;
                }
            });
        } catch (RuntimeException e) {
            NekosEnchantedBooks.LOGGER.error("Failed to bake custom enchanted book overrides!", e);
            return base;
        }
    }

    /**
     * This constructor follows up on the initialization done in its super method,
     * {@link ItemOverrides#ItemOverrides(ModelBaker, UnbakedModel, List, Function)}. It calls the
     * {@link #bakeOverrides(ModelBaker)} method, where existing models are queried for automatic model loading. The
     * enchantments are later validated in {@link #validate(Iterable)} when a world is loaded, since enchantments are a
     * data pack registry. The process of taking advantage of automatic model loading was described in the documentation
     * for the class in {@link EnchantedBookOverrides}.
     *
     * @param base  Any existing item overrides that exist in the base enchanted book model
     * @param baker The model baker
     * @see #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)
     * @see EnchantedBookOverrides
     */
    private EnchantedBookOverrides(ItemOverrides base, ModelBaker baker) {
        this.base = base;
        this.overrides = bakeOverrides(baker);
    }

    /**
     * Bakes the custom overrides used for the enchanted books.
     *
     * @param baker The model baker
     * @return The map of enchantment IDs to their respective baked models
     */
    private static Map<String, BakedModel> bakeOverrides(ModelBaker baker) {
        Map<String, BakedModel> overrides = new HashMap<>(PREPARED_MODELS.size());
        PREPARED_MODELS.forEach(model -> {
            String enchantment = idFromModel(model);
            BakedModel baked = baker.bake(model.id(), BlockModelRotation.X0_Y0);

            // null model? something went wrong, so stop now
            if (baked == null) return;

            TEXTURED_ENCHANTMENTS.add(enchantment);
            overrides.put(enchantment, baked);
        });

        return overrides;
    }

    /**
     * Prepares all custom models to be used by NEBs. This includes resolving models so that their textures can be
     * referenced even though it doesn't exist in a model file that is directly tied to an item.
     *
     * @param resolver The model resolver
     */
    static void prepare(Consumer<ModelResourceLocation> resolver) {
        Minecraft.getInstance().getResourceManager().listResources("models/item", id -> NekosEnchantedBooks.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("json")).forEach((modelFile, r) -> {
            // save enchantment
            ModelResourceLocation enchantment = modelFromFile(modelFile);

            PREPARED_MODELS.add(enchantment);
            resolver.accept(enchantment);
        });
    }

    static void validate(Iterable<Enchantment> enchantments) {
        Set<String> missing = new TreeSet<>();
        enchantments.forEach(enchantment -> {
            @Nullable String id = NekosEnchantedBooks.idOf(enchantment);
            if (id != null && !TEXTURED_ENCHANTMENTS.contains(id) && !NekosEnchantedBooks.NON_ENCHANTMENTS.contains(id))
                missing.add(id);
        });

        if (!missing.isEmpty()) {
            NekosEnchantedBooks.LOGGER.warn("Missing, or failed to load, enchanted book models for the following enchantments: [{}]", String.join(", ", missing));
        } else {
            NekosEnchantedBooks.LOGGER.info("Successfully loaded enchanted book models for all available enchantments");
        }
    }


    /* BAKED MODEL RESOLUTION */

    /**
     * Resolves the baked model based on the given stack's enchantment. If the enchantment is not found in the custom
     * overrides, we default back to the super method
     * {@link ItemOverrides#resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)} which will likely return the
     * base enchanted book model.
     *
     * @param model  The model to get the override for
     * @param stack  The item stack to get the override for
     * @param level  The level the model is being rendered in
     * @param entity The entity that is linked to, or using, the model
     * @param seed   The seed for random calculations
     * @return The resolved model
     */
    @Override
    public @Nullable BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        for (Enchantment enchantment : getEnchantments(stack)) {
            @Nullable String key = NekosEnchantedBooks.idOf(enchantment);
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return this.base.resolve(model, stack, level, entity, seed);
    }

    /**
     * Gets the enchantment from the given stack. If the stack has no enchantments, then this method returns null. If
     * the stack has multiple enchantments, then the first key found is what will be used.
     *
     * @param stack The stack to get the enchantment from
     * @return The enchantment of the stack, or {@code null} if it does not have any
     */
    private static Iterable<Enchantment> getEnchantments(ItemStack stack) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return () -> new Iterator<>() {
            private final Iterator<Holder<Enchantment>> iterator = enchantments.keySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Enchantment next() {
                return iterator.next().get();
            }

            @Override
            public void forEachRemaining(Consumer<? super Enchantment> action) {
                iterator.forEachRemaining(holder -> action.accept(holder.get()));
            }
        };
    }
}
