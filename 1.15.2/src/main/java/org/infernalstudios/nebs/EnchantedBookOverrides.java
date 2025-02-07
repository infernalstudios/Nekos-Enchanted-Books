package org.infernalstudios.nebs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
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
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashSet;
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
 *     organized into the {@linkplain NekosEnchantedBooks#getIdOf(Enchantment) enchantment's NEBs ID} where each point
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
 * <h2>Usage for NEBs Developers</h2>
 * Apart from what has already been mentioned, you should read the documentation for each of the methods:
 * <ul>
 *     <li>{@link #EnchantedBookOverrides(ModelBakery, BlockModel, Function, List, Function)}</li>
 *     <li>{@link #resolve(IBakedModel, ItemStack, World, LivingEntity)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
@SuppressWarnings("deprecation") // We are wrapping things that use deprecated methods
public final class EnchantedBookOverrides extends ItemOverrideList {
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    public static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    static ResourceLocation getEnchantedBookModel(String enchantment) {
        return new ResourceLocation(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static final Set<String> PREPARED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final Map<String, IBakedModel> overrides;

    public static @Nullable ItemOverrideList of(BlockModel base, String location, ModelBakery bakery, List<ItemOverride> existing) {
        return !EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location) ? null : new EnchantedBookOverrides(bakery, base, bakery::getModel, existing);
    }

    public static @Nullable ItemOverrideList of(BlockModel base, String location, ModelBakery bakery, List<ItemOverride> existing, Function<Material, TextureAtlasSprite> spriteGetter) {
        return !EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location) ? null : new EnchantedBookOverrides(bakery, base, bakery::getModel, existing, spriteGetter);
    }

    /**
     * This constructor acts as a bouncer for legacy vanilla model baking.
     *
     * @param bakery        The model bakery
     * @param enchantedBook The vanilla enchanted book unbaked model (ensured by
     *                      {@link org.infernalstudios.nebs.mixin.BlockModelMixin BlockModelMixin})
     * @param modelGetter   The model getter
     * @param existing      Any existing item overrides that exist in the base enchanted book model
     * @see #EnchantedBookOverrides(ModelBakery, BlockModel, Function, List, Function)
     * @see EnchantedBookOverrides
     */
    private EnchantedBookOverrides(ModelBakery bakery, BlockModel enchantedBook, Function<ResourceLocation, IUnbakedModel> modelGetter, List<ItemOverride> existing) {
        super(bakery, enchantedBook, modelGetter, existing);
        this.overrides = this.setup(bakery::bake);
    }

    /**
     * This constructor follows up on the initialization done in its super method,
     * {@link ItemOverrideList#ItemOverrideList(ModelBakery, IUnbakedModel, Function, Function, List)}. It calls the
     * {@link #setup(ModelBaker)} method, where all the registered enchantments are grabbed from the
     * {@linkplain ForgeRegistries#ENCHANTMENTS Enchantments registry} and are queried for automatic model loading. The
     * process of taking advantage of automatic model loading was described in the documentation for the class in
     * {@link EnchantedBookOverrides}.
     * <p>
     * Also note that this class respects any existing overrides that might have been added to the base enchanted book
     * model. However, this is only the case if an enchanted book has an enchantment that is not saved in our own
     * overrides.
     *
     * @param bakery        The model bakery
     * @param enchantedBook The vanilla enchanted book unbaked model (ensured by
     *                      {@link org.infernalstudios.nebs.mixin.BlockModelMixin BlockModelMixin})
     * @param modelGetter   The model getter
     * @param spriteGetter  The sprite getter for model baking
     * @param existing      Any existing item overrides that exist in the base enchanted book model
     * @see #resolve(IBakedModel, ItemStack, World, LivingEntity)
     * @see EnchantedBookOverrides
     */
    private EnchantedBookOverrides(ModelBakery bakery, BlockModel enchantedBook, Function<ResourceLocation, IUnbakedModel> modelGetter, List<ItemOverride> existing, Function<Material, TextureAtlasSprite> spriteGetter) {
        super(bakery, enchantedBook, modelGetter, spriteGetter, existing);
        this.overrides = setup((model, state) -> bakery.getBakedModel(model, state, spriteGetter));
    }

    /**
     * The setup as described in
     * {@link #EnchantedBookOverrides(ModelBakery, BlockModel, Function, List, Function)}. Use
     * this to assign {@link #overrides}.
     *
     * @param baker The model baker
     * @return The map of enchantment IDs to their respective baked models
     *
     * @see #EnchantedBookOverrides(ModelBakery, BlockModel, Function, List, Function)
     */
    private Map<String, IBakedModel> setup(ModelBaker baker) {
        // bake overrides
        BakeResult result = bakeOverrides(baker);

        // log missing models
        if (!result.missing.isEmpty()) {
            NekosEnchantedBooks.LOGGER.warn("Missing enchanted book models for the following enchantments: [{}]", String.join(", ", result.missing));
        } else {
            NekosEnchantedBooks.LOGGER.info("Successfully loaded enchanted book models for all available enchantments");
        }

        return result.overrides;
    }

    /**
     * Bakes the custom overrides used for the enchanted books.
     *
     * @param baker   The model baker to use with the bakery
     * @return The map of enchantment IDs to their respective baked models
     */
    private static BakeResult bakeOverrides(ModelBaker baker) {
        ImmutableMap.Builder<String, IBakedModel> overrides = ImmutableMap.builder();
        ImmutableSet.Builder<String> missing = ImmutableSet.builder();
        PREPARED_ENCHANTMENTS.forEach(enchantment -> {
            ResourceLocation model = getEnchantedBookModel(enchantment);
            if (!PREPARED_MODELS.contains(model)) {
                if (!NekosEnchantedBooks.NON_ENCHANTMENTS.contains(enchantment))
                    missing.add(enchantment);
                return;
            }

            // Now we are ready to bake the custom model and add it to our own overrides.
            IBakedModel baked = baker.bake(model, ModelRotation.X0_Y0);
            if (baked == null) {
                missing.add(enchantment);
                return;
            }

            overrides.put(enchantment, baked);
        });
        return new BakeResult(overrides, missing);
    }

    /**
     * Prepares all custom models to be used by NEBs. By registering them in {@link ModelLoader}, we can save the
     * trouble of needing to manually resolve and bake them and their parents ourselves.
     *
     * @param models The consumer to accept new models to be registered
     */
    static void prepare(Consumer<ResourceLocation> models) {
        ForgeRegistries.ENCHANTMENTS.forEach(e -> {
            // save enchantment
            String enchantment = NekosEnchantedBooks.getIdOf(e);
            PREPARED_ENCHANTMENTS.add(enchantment);

            // try and find model for enchantment
            ResourceLocation model = getEnchantedBookModel(enchantment);
            if (!Minecraft.getInstance().getResourceManager().hasResource(new ResourceLocation(model.getNamespace(), "models/" + model.getPath() + ".json"))) {
                return;
            }

            // model exists? prepare it
            PREPARED_MODELS.add(model);
            models.accept(model);
        });
    }

    /**
     * A functional interface that allows us to use {@link ModelBakery#bake(ResourceLocation, IModelTransform)} or
     * {@link ModelBakery#getBakedModel(ResourceLocation, IModelTransform, Function)} without needing to arbitrarily
     * pass around any additional attributes that might be needed. It effectively acts as a wrapper around 1.20's
     * {@code ModelBaker}.
     */
    @FunctionalInterface
    private interface ModelBaker {
        IBakedModel bake(ResourceLocation model, IModelTransform state);
    }

    /**
     * Holds the result of the model baking done in {@link #bakeOverrides(ModelBaker)}.
     *
     * @see BakeResult#BakeResult(ImmutableMap.Builder, ImmutableSet.Builder)
     */
    private static final class BakeResult {
        /** The baked overrides to be used by {@link EnchantedBookOverrides} */
        private final Map<String, IBakedModel> overrides;
        /** The enchantments that are missing models */
        private final Set<String> missing;

        private BakeResult(ImmutableMap.Builder<String, IBakedModel> overrides, ImmutableSet.Builder<String> missing) {
            this.overrides = overrides.build();
            this.missing = Util.make(new TreeSet<>(), set -> set.addAll(missing.build()));
        }
    }


    /* BAKED MODEL RESOLUTION */

    /**
     * Resolves the baked model based on the given stack's enchantment. If the enchantment is not found in the custom
     * overrides, we default back to the super method
     * {@link ItemOverrideList#resolve(IBakedModel, ItemStack, World, LivingEntity)} which will likely return the
     * base enchanted book model.
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
            String key = NekosEnchantedBooks.getIdOf(enchantment);
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return super.resolve(model, stack, level, entity);
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
