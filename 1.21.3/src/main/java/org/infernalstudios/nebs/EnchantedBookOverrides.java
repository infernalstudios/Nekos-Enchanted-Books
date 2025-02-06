package org.infernalstudios.nebs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraftforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
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
 *         <li>If there are any failed models for enchantments, a warning will be displayed to the console log for
 *         debugging purposes.</li>
 *     </ul>
 * </ul>
 * <h2>Usage for NEBs Developers</h2>
 * Apart from what has already been mentioned, you should read the documentation for each of the methods:
 * <ul>
 *     <li>{@link #EnchantedBookOverrides(BakedModel, ModelBaker, Function, ModelState)}</li>
 *     <li>{@link #findOverride(ItemStack, ClientLevel, LivingEntity, int)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class EnchantedBookOverrides extends BakedOverrides {
    /** The resource location for the vanilla {@linkplain net.minecraft.world.item.Items#ENCHANTED_BOOK enchanted book}. */
    static final ResourceLocation ENCHANTED_BOOK_LOCATION = ResourceLocation.withDefaultNamespace("enchanted_book");
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    static final ResourceLocation ENCHANTED_BOOK_UNBAKED_MODEL_NAME = ENCHANTED_BOOK_LOCATION.withPrefix("item/");

    static ResourceLocation getEnchantedBookModel(String enchantment) {
        return ResourceLocation.fromNamespaceAndPath(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static String modelToEnchantment(ResourceLocation model) {
        return model.getPath().substring("item/".length()).replace("/", ".");
    }

    private static final Set<String> TEXTURED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final BakedModel base;
    private final Map<String, BakedModel> overrides;

    @SuppressWarnings("unused") // ItemModelCoreMod
    public static BakedModel of(BakedModel base, ResourceLocation location, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        return !EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(location) ? base : new DelegateBakedModel(base) {
            @Override
            public BakedOverrides overrides() {
                return new EnchantedBookOverrides(this.parent, baker, spriteGetter, state);
            }
        };
    }

    /**
     * This constructor follows up on the creation of the enchanted book item model. It calls the
     * {@link #setup(ModelBaker, Function, ModelState)} method, where existing models and are queried for automatic
     * model loading. The process of taking advantage of automatic model loading was described in the documentation for
     * the class in {@link EnchantedBookOverrides}.
     * <p>
     * Also note that this class respects any existing overrides that might have been added to the base enchanted book
     * model. However, this is only the case if an enchanted book has an enchantment that is not saved in our own
     * overrides.
     *
     * @param base         The base enchanted book model
     * @param baker        The model baker
     * @param spriteGetter The sprite getter for model baking
     * @param state        The model state
     * @see #findOverride(ItemStack, ClientLevel, LivingEntity, int)
     * @see EnchantedBookOverrides
     */
    public EnchantedBookOverrides(BakedModel base, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        this.base = base;
        this.overrides = this.setup(baker, spriteGetter, state);
    }

    /**
     * The setup as described in
     * {@link #EnchantedBookOverrides(BakedModel, ModelBaker, Function, ModelState)}. Use this to
     * assign {@link #overrides}.
     *
     * @param baker        The model baker
     * @param spriteGetter The sprite getter for model baking
     * @return The map of enchantment IDs to their respective baked models
     *
     * @see #EnchantedBookOverrides(BakedModel, ModelBaker, Function, ModelState)
     */
    private Map<String, BakedModel> setup(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        // bake overrides
        BakeResult result = bakeOverrides(baker, spriteGetter, state, PREPARED_MODELS.size());

        // log failed models
        if (!result.failed.isEmpty()) {
            NekosEnchantedBooks.LOGGER.warn("Failed to load enchanted book models for the following enchantments: [{}]", String.join(", ", result.failed));
        }

        return result.overrides;
    }

    /**
     * Bakes the custom overrides used for the enchanted books.
     *
     * @param baker        The model baker
     * @param spriteGetter The sprite getter for model baking
     * @param expected     The expected number of enchantments to load models for
     * @return The map of enchantment IDs to their respective baked models
     */
    private static BakeResult bakeOverrides(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, int expected) {
        ImmutableMap.Builder<String, BakedModel> overrides = ImmutableMap.builderWithExpectedSize(expected);
        ImmutableSet.Builder<String> failed = ImmutableSet.builderWithExpectedSize(expected);
        PREPARED_MODELS.forEach(model -> {
            String enchantment = modelToEnchantment(model);

            // Now we are ready to bake the custom model and add it to our own overrides.
            BakedModel baked = baker.bake(model, state, spriteGetter);
            if (baked == null) {
                failed.add(enchantment);
                return;
            }

            TEXTURED_ENCHANTMENTS.add(enchantment);
            overrides.put(enchantment, baked);
        });
        return new BakeResult(overrides, failed);
    }

    @Deprecated // this is a stop-gap until ModelEvent.RegisterAdditional is re-added
    @SuppressWarnings("unused") // ModelDiscoveryCoreMod
    public static void prepare(Map<ResourceLocation, UnbakedModel> models, UnbakedModel.Resolver resolver) {
        prepare(resolver::resolve, models.keySet());
    }

    /**
     * Prepares all custom models to be used by NEBs. By registering them through the
     * {@link ModelEvent.RegisterAdditional} event, we can save the trouble of needing to manually resolve and bake them
     * and their parents ourselves.
     *
     * @param models The consumer to accept new models to be registered
     */
    @SuppressWarnings("JavadocReference") // ModelEvent.RegisterAdditional has not yet been re-added
    static void prepare(Consumer<ResourceLocation> resolver, Set<ResourceLocation> models) {
        for (var model : models) {
            if (model.getNamespace().equals(NekosEnchantedBooks.MOD_ID)) {
                // save enchantment
                PREPARED_MODELS.add(model);

                // resolve model to load textures
                resolver.accept(model);
            }
        }
    }

    static void validate(Iterable<Enchantment> enchantments) {
        Set<String> missing = new TreeSet<>(Comparator.naturalOrder());
        enchantments.forEach(enchantment -> {
            String id = NekosEnchantedBooks.getIdOf(enchantment);
            if (id != null && !TEXTURED_ENCHANTMENTS.contains(id) && !NekosEnchantedBooks.NON_ENCHANTMENTS.contains(id))
                missing.add(id);
        });

        if (!missing.isEmpty()) {
            NekosEnchantedBooks.LOGGER.warn("Missing enchanted book models for the following enchantments: [{}]", String.join(", ", missing));
        } else {
            NekosEnchantedBooks.LOGGER.info("Successfully loaded enchanted book models for all available enchantments");
        }
    }

    /**
     * Holds the result of the model baking done in {@link #bakeOverrides(ModelBaker, Function, ModelState, int)}.
     *
     * @param overrides The baked overrides to be used by {@link EnchantedBookOverrides}
     * @param failed   The enchantments that are failed models
     */
    private record BakeResult(Map<String, BakedModel> overrides, Set<String> failed) {
        private BakeResult(ImmutableMap.Builder<String, BakedModel> overrides, ImmutableSet.Builder<String> failed) {
            this(overrides.build(), failed.build());
        }
    }


    /* BAKED MODEL RESOLUTION */

    /**
     * Resolves the baked model based on the given stack's enchantment. If the enchantment is not found in the custom
     * overrides, we default back to the super method
     * {@link BakedOverrides#findOverride(ItemStack, ClientLevel, LivingEntity, int)} which will likely return the
     * base enchanted book model.
     *
     * @param stack  The item stack to get the override for
     * @param level  The level the model is being rendered in
     * @param entity The entity that is linked to, or using, the model
     * @param seed   The seed for random calculations
     * @return The resolved model
     */
    @Override
    public @Nullable BakedModel findOverride(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        for (Enchantment enchantment : getEnchantments(stack)) {
            String key = NekosEnchantedBooks.getIdOf(enchantment);
            if (this.overrides.containsKey(key)) {
                return this.overrides.get(key);
            }
        }

        return this.base.overrides().findOverride(stack, level, entity, seed);
    }

    /**
     * Gets the enchantment from the given stack. If the stack has no enchantments, then this method returns null. If
     * the stack has multiple enchantments, then the first key found is what will be used.
     *
     * @param stack The stack to get the enchantment from
     * @return The enchantment of the stack, or {@code null} if it does not have any
     */
    private static Iterable<Enchantment> getEnchantments(ItemStack stack) {
        var enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
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
        };
    }
}
