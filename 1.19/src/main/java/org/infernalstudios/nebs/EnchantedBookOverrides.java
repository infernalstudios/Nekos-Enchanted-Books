package org.infernalstudios.nebs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
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
 *     <li>{@link EnchantedBookOverrides#EnchantedBookOverrides(ModelBakery, UnbakedModel, Function, Function, List)}</li>
 *     <li>{@link #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class EnchantedBookOverrides extends ItemOverrides {
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    public static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    static ResourceLocation getEnchantedBookModel(String enchantment) {
        return new ResourceLocation(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static final Set<String> PREPARED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final Map<String, BakedModel> overrides;

    /**
     * This constructor acts as a bouncer for legacy vanilla model baking.
     *
     * @param bakery        The model bakery
     * @param enchantedBook The vanilla enchanted book unbaked model (ensured by
     *                      {@link org.infernalstudios.nebs.mixin.BlockModelMixin BlockModelMixin})
     * @param modelGetter   The model getter
     * @param existing      Any existing item overrides that exist in the base enchanted book model
     * @see EnchantedBookOverrides#EnchantedBookOverrides(ModelBakery, UnbakedModel, Function, Function, List)
     * @see EnchantedBookOverrides
     * @deprecated For use by vanilla only.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public EnchantedBookOverrides(ModelBakery bakery, BlockModel enchantedBook, Function<ResourceLocation, UnbakedModel> modelGetter, List<ItemOverride> existing) {
        super(bakery, enchantedBook, modelGetter, existing);
        this.overrides = this.setup(bakery, ModelBaker.of());
    }

    /**
     * This constructor follows up on the initialization done in its super method,
     * {@link ItemOverrides#ItemOverrides(ModelBakery, UnbakedModel, Function, Function, List)}. It calls the
     * {@link #setup(ModelBakery, ModelBaker)} method, where all the registered enchantments are grabbed from the
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
     * @see #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)
     * @see EnchantedBookOverrides
     */
    public EnchantedBookOverrides(ModelBakery bakery, UnbakedModel enchantedBook, Function<ResourceLocation, UnbakedModel> modelGetter, Function<Material, TextureAtlasSprite> spriteGetter, List<ItemOverride> existing) {
        super(bakery, enchantedBook, modelGetter, spriteGetter, existing);
        this.overrides = setup(bakery, ModelBaker.of(spriteGetter));
    }

    /**
     * The setup as described in
     * {@link EnchantedBookOverrides#EnchantedBookOverrides(ModelBakery, UnbakedModel, Function, Function, List)}. Use
     * this to assign {@link #overrides}.
     *
     * @param bakery The model bakery
     * @param baker  The model baker
     * @return The map of enchantment IDs to their respective baked models
     *
     * @see EnchantedBookOverrides#EnchantedBookOverrides(ModelBakery, UnbakedModel, Function, Function, List)
     */
    private Map<String, BakedModel> setup(ModelBakery bakery, ModelBaker baker) {
        // bake overrides
        Set<String> enchantments = PREPARED_ENCHANTMENTS;
        BakeResult result = bakeOverrides(bakery, baker, enchantments, enchantments.size());

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
     * @param bakery       The model bakery
     * @param modelBaker   The model baker to use with the bakery
     * @param enchantments The enchantments to automatically load models for
     * @param expected     The expected number of enchantments to load models for
     * @return The map of enchantment IDs to their respective baked models
     */
    private static BakeResult bakeOverrides(ModelBakery bakery, ModelBaker modelBaker, Iterable<String> enchantments, int expected) {
        ImmutableMap.Builder<String, BakedModel> overrides = ImmutableMap.builderWithExpectedSize(expected);
        ImmutableSet.Builder<String> missing = ImmutableSet.builderWithExpectedSize(expected);
        enchantments.forEach(enchantment -> {
            ResourceLocation model = getEnchantedBookModel(enchantment);
            if (!PREPARED_MODELS.contains(model)) {
                missing.add(enchantment);
                return;
            }

            // Now we are ready to bake the custom model and add it to our own overrides.
            BakedModel baked = modelBaker.bake(bakery, model, BlockModelRotation.X0_Y0);
            if (baked == null) {
                missing.add(enchantment);
                return;
            }

            overrides.put(enchantment, baked);
        });
        return new BakeResult(overrides, missing);
    }

    /**
     * Prepares all custom models to be used by NEBs. By registering them through the
     * {@link ModelEvent.RegisterAdditional} event, we can save the trouble of needing to manually resolve and bake them
     * and their parents ourselves.
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
            if (Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(model.getNamespace(), "models/" + model.getPath() + ".json")).isEmpty()) {
                return;
            }

            // model exists? prepare it
            PREPARED_MODELS.add(model);
            models.accept(model);
        });
    }

    /**
     * A functional interface that allows us to use {@link ModelBakery#bake(ResourceLocation, ModelState)} or
     * {@link ModelBakery#bake(ResourceLocation, ModelState, Function)} without needing to arbitrarily pass around any
     * additional attributes that might be needed. It effectively acts as a wrapper around 1.20's {@code ModelBaker}.
     */
    @FunctionalInterface
    private interface ModelBaker {
        /** @see ModelBakery#bake(ResourceLocation, ModelState) */
        @SuppressWarnings("deprecation")
        static ModelBaker of() {
            return ModelBakery::bake;
        }

        /** @see ModelBakery#bake(ResourceLocation, ModelState, Function) */
        static ModelBaker of(Function<Material, TextureAtlasSprite> spriteGetter) {
            return (b, n, r) -> b.bake(n, r, spriteGetter);
        }

        /**
         * Bakes the model using the given bakery and parameters.
         *
         * @param bakery The model bakery to bake with
         * @param model  The model name to bake
         * @param state  The model state to use
         * @return The baked model
         */
        BakedModel bake(ModelBakery bakery, ResourceLocation model, ModelState state);
    }

    /**
     * Holds the result of the model baking done in {@link #bakeOverrides(ModelBakery, ModelBaker, Iterable, int)}.
     *
     * @param overrides The baked overrides to be used by {@link EnchantedBookOverrides}
     * @param missing   The enchantments that are missing models
     */
    private record BakeResult(Map<String, BakedModel> overrides, Set<String> missing) {
        private BakeResult(ImmutableMap.Builder<String, BakedModel> overrides, ImmutableSet.Builder<String> missing) {
            this(overrides.build(), Util.make(new TreeSet<>(Comparator.naturalOrder()), set -> set.addAll(missing.build())));
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
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        Enchantment enchantment = getEnchantment(stack);
        if (enchantment != null) {
            String key = enchantment.getDescriptionId();
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return super.resolve(model, stack, level, entity, seed);
    }

    /**
     * Gets the enchantment from the given stack. If the stack has no enchantments, then this method returns null. If
     * the stack has multiple enchantments, then the first key found is what will be used.
     *
     * @param stack The stack to get the enchantment from
     * @return The enchantment of the stack, or {@code null} if it does not have any
     */
    private static @Nullable Enchantment getEnchantment(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        return !enchantments.isEmpty() ? enchantments.keySet().iterator().next() : null;
    }
}
