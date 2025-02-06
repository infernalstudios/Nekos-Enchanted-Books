package org.infernalstudios.nebs;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
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
 *     <li>{@link #EnchantedBookOverrides(ItemModel, ModelBaker)}</li>
 *     <li>{@link #update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity, int)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class EnchantedBookOverrides implements ItemModel {
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

    private final ItemModel base;
    private final Map<String, BakedModel> overrides;

    @SuppressWarnings("unused") // ModelBakeryCoreMod
    public static ItemModel of(ItemModel base, ResourceLocation location, ModelBaker baker) {
        return ENCHANTED_BOOK_LOCATION.equals(location) ? new EnchantedBookOverrides(base, baker) : base;
    }

    /**
     * This constructor follows up on the creation of the enchanted book item model. It calls the
     * {@link #setup(ModelBaker)} method, where existing models and are queried for automatic model loading.
     * The process of taking advantage of automatic model loading was described in the documentation for the class in
     * {@link EnchantedBookOverrides}.
     * <p>
     * Also note that this class respects any existing overrides that might have been added to the base enchanted book
     * model. However, this is only the case if an enchanted book has an enchantment that is not saved in our own
     * overrides.
     *
     * @param base  The base enchanted book model
     * @param baker The model baker
     * @see #update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity, int)
     * @see EnchantedBookOverrides
     */
    public EnchantedBookOverrides(ItemModel base, ModelBaker baker) {
        this.base = base;
        this.overrides = this.setup(baker);
    }

    /**
     * The setup as described in {@link #EnchantedBookOverrides(ItemModel, ModelBaker)}. Use this to assign
     * {@link #overrides}.
     *
     * @param baker The model baker
     * @return The map of enchantment IDs to their respective baked models
     *
     * @see #EnchantedBookOverrides(ItemModel, ModelBaker)
     */
    private Map<String, BakedModel> setup(ModelBaker baker) {
        // bake overrides
        return bakeOverrides(baker);
    }

    /**
     * Bakes the custom overrides used for the enchanted books.
     *
     * @param baker The model baker
     * @return The map of enchantment IDs to their respective baked models
     */
    private static Map<String, BakedModel> bakeOverrides(ModelBaker baker) {
        ImmutableMap.Builder<String, BakedModel> overrides = ImmutableMap.builderWithExpectedSize(PREPARED_MODELS.size());
        PREPARED_MODELS.forEach(model -> {
            String enchantment = modelToEnchantment(model);
            BakedModel baked = baker.bake(model, BlockModelRotation.X0_Y0);

            TEXTURED_ENCHANTMENTS.add(enchantment);
            overrides.put(enchantment, baked);
        });
        return overrides.build();
    }

    /**
     * Prepares all custom models to be used by NEBs. By registering them through the
     * {@link ModelEvent.RegisterAdditional} event, we can save the trouble of needing to manually resolve and bake them
     * and their parents ourselves.
     *
     * @param models The consumer to accept new models to be registered
     */
    @SuppressWarnings({"unused", "JavadocReference"}) // ModelDiscoveryCoreMod, ModelEvent.RegisterAdditional has not yet been re-added
    public static void prepare(Map<ResourceLocation, UnbakedModel> models, ResolvableModel.Resolver resolver) {
        for (var model : models.keySet()) {
            if (model.getNamespace().equals(NekosEnchantedBooks.MOD_ID)) {
                // save enchantment
                PREPARED_MODELS.add(model);

                // resolve model to load textures
                resolver.resolve(model);
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


    /* BAKED MODEL RESOLUTION */

    /**
     * Resolves and renders the baked model based on the given stack's enchantment. If the enchantment is not found in
     * the custom overrides, we default back to updating the base model, which will likely render the base enchanted
     * book model.
     *
     * @param state    The render state to update
     * @param stack    The item stack to render and/or get the override for
     * @param resolver The item model resolver
     * @param context  The display context the model is being rendered in
     * @param level    The level the model is being rendered in
     * @param entity   The entity that is linked to, or using, the model
     * @param seed     The seed for random calculations
     */
    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        for (Enchantment enchantment : getEnchantments(stack)) {
            String key = NekosEnchantedBooks.getIdOf(enchantment);
            if (this.overrides.containsKey(key)) {
                this.render(this.overrides.get(key), state, stack);
                return;
            }
        }

        this.base.update(state, stack, resolver, context, level, entity, seed);
    }

    /**
     * Renders the given baked model with the given state and stack. This method is called from
     * {@link #update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity,
     * int)} if an override model has been found for the enchanted book item stack.
     *
     * @param model The baked model to render
     * @param state The render state to update
     * @param stack The item stack to render
     *
     * @see net.minecraft.client.renderer.item.BlockModelWrapper#update(ItemStackRenderState, ItemStack,
     * ItemModelResolver, ItemDisplayContext, ClientLevel, LivingEntity, int)
     */
    private void render(BakedModel model, ItemStackRenderState state, ItemStack stack) {
        ItemStackRenderState.LayerRenderState layer = state.newLayer();
        if (stack.hasFoil()) layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);

        RenderType rendertype = ItemBlockRenderTypes.getRenderType(stack);
        layer.setupBlockModel(model, rendertype);
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
