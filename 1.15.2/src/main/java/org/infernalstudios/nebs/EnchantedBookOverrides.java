package org.infernalstudios.nebs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
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
 *     <li>{@link #EnchantedBookOverrides(ItemOverrideList, ModelBaker)}</li>
 *     <li>{@link #resolve(IBakedModel, ItemStack, World, LivingEntity)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
@SuppressWarnings("deprecation") // We are wrapping things that use deprecated methods
public final class EnchantedBookOverrides extends ItemOverrideList {
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    static ResourceLocation locationFrom(String enchantment) {
        return new ResourceLocation(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static final Set<String> PREPARED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final ItemOverrideList base;
    private final Map<String, IBakedModel> overrides;

    @SuppressWarnings("unused") // BlockModelCoreMod
    public static ItemOverrideList of(ItemOverrideList base, String location, ModelBakery bakery) {
        if (!EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location)) return base;

        try {
            return new EnchantedBookOverrides(base, bakery::bake);
        } catch (RuntimeException e) {
            NekosEnchantedBooks.LOGGER.error("Failed to bake custom enchanted book overrides!", e);
            return base;
        }
    }

    @SuppressWarnings("unused") // BlockModelCoreMod
    public static ItemOverrideList of(ItemOverrideList base, String location, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter) {
        if (!EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location)) return base;

        try {
            return new EnchantedBookOverrides(base, (model, state) -> bakery.getBakedModel(model, state, spriteGetter));
        } catch (RuntimeException e) {
            NekosEnchantedBooks.LOGGER.error("Failed to bake custom enchanted book overrides!", e);
            return base;
        }
    }

    /**
     * This constructor follows up on the baking of the enchanted book item model's overrides. It calls the
     * {@link #bakeOverrides(ModelBaker)} method, where existing models are queried for automatic model loading.
     * The process of taking advantage of automatic model loading was described in the documentation for the class in
     * {@link EnchantedBookOverrides}.
     *
     * @param base  Any existing item overrides that exist in the base enchanted book model
     * @param baker The model baker
     * @see #resolve(IBakedModel, ItemStack, World, LivingEntity)
     * @see EnchantedBookOverrides
     */
    private EnchantedBookOverrides(ItemOverrideList base, ModelBaker baker) {
        this.base = base;
        this.overrides = bakeOverrides(baker);
    }

    /**
     * Bakes the custom overrides used for the enchanted books.
     *
     * @param baker The model baker to use with the bakery
     * @return The map of enchantment IDs to their respective baked models
     */
    private static Map<String, IBakedModel> bakeOverrides(ModelBaker baker) {
        Map<String, IBakedModel> overrides = new HashMap<>(PREPARED_ENCHANTMENTS.size());
        Set<String> failed = new TreeSet<>();
        PREPARED_ENCHANTMENTS.forEach(enchantment -> {
            ResourceLocation model = locationFrom(enchantment);
            if (!PREPARED_MODELS.contains(model)) {
                if (!NekosEnchantedBooks.NON_ENCHANTMENTS.contains(enchantment))
                    failed.add(enchantment);
                return;
            }

            // Now we are ready to bake the custom model and add it to our own overrides.
            IBakedModel baked = baker.bake(model, ModelRotation.X0_Y0);
            if (baked == null) {
                failed.add(enchantment);
                return;
            }

            overrides.put(enchantment, baked);
        });

        // log missing models
        if (!failed.isEmpty()) {
            NekosEnchantedBooks.LOGGER.warn("Missing, or failed to load, enchanted book models for the following enchantments: [{}]", String.join(", ", failed));
        } else {
            NekosEnchantedBooks.LOGGER.info("Successfully loaded enchanted book models for all available enchantments");
        }

        return overrides;
    }

    /**
     * Prepares all custom models to be used by NEBs. This includes resolving models so that their textures can be
     * referenced even though it doesn't exist in a model file that is directly tied to an item.
     *
     * @param enchantments All registered enchantments
     * @param resolver     The model resolver
     */
    static void prepare(Iterable<Enchantment> enchantments, Consumer<ResourceLocation> resolver) {
        enchantments.forEach(e -> {
            // save enchantment
            String enchantment = NekosEnchantedBooks.idOf(e);
            PREPARED_ENCHANTMENTS.add(enchantment);

            // try and find model for enchantment
            ResourceLocation model = locationFrom(enchantment);
            if (!Minecraft.getInstance().getResourceManager().hasResource(new ResourceLocation(model.getNamespace(), "models/" + model.getPath() + ".json"))) {
                return;
            }

            PREPARED_MODELS.add(model);
            resolver.accept(model);
        });
    }

    /**
     * A functional interface that allows us to use {@link ModelBakery#bake(ResourceLocation, IModelTransform)} or
     * {@link ModelBakery#getBakedModel(ResourceLocation, IModelTransform, Function)} without needing to arbitrarily
     * pass around any additional attributes that might be needed. It effectively acts as a wrapper around 1.20's
     * {@code ModelBaker}.
     */
    @FunctionalInterface
    private interface ModelBaker extends BiFunction<ResourceLocation, IModelTransform, IBakedModel> {
        IBakedModel bake(ResourceLocation location, IModelTransform state);

        @Override
        default IBakedModel apply(ResourceLocation location, IModelTransform state) {
            return this.bake(location, state);
        }
    }


    /* BAKED MODEL RESOLUTION */

    /**
     * Resolves the baked model based on the given stack's enchantment. If the enchantment is not found in the custom
     * overrides, we default back to the super method
     * {@link ItemOverrideList#resolve(IBakedModel, ItemStack, World, LivingEntity)} which will likely return the base
     * enchanted book model.
     *
     * @param model  The model to get the override for
     * @param stack  The item stack to get the override for
     * @param level  The level the model is being rendered in
     * @param entity The entity that is linked to, or using, the model
     * @return The resolved model
     */
    @Override
    public @Nullable IBakedModel resolve(IBakedModel model, ItemStack stack, @Nullable World level, @Nullable LivingEntity entity) {
        for (Enchantment enchantment : getEnchantments(stack)) {
            String key = NekosEnchantedBooks.idOf(enchantment);
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return this.base.resolve(model, stack, level, entity);
    }

    /**
     * Gets the enchantment from the given stack. If the stack has no enchantments, then this method returns null. If
     * the stack has multiple enchantments, then the first key found is what will be used.
     *
     * @param stack The stack to get the enchantment from
     * @return The enchantment of the stack, or {@code null} if it does not have any
     */
    private static Iterable<Enchantment> getEnchantments(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).keySet();
    }
}
