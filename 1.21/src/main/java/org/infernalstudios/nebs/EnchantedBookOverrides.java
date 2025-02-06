package org.infernalstudios.nebs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
 *         <li>If there are any failed models for enchantments, a warning will be displayed to the console log for
 *         debugging purposes.</li>
 *     </ul>
 * </ul>
 * <h2>Usage for NEBs Developers</h2>
 * Apart from what has already been mentioned, you should read the documentation for each of the methods:
 * <ul>
 *     <li>{@link EnchantedBookOverrides#EnchantedBookOverrides(ModelBaker, UnbakedModel, List, Function)}</li>
 *     <li>{@link #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)}</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class EnchantedBookOverrides extends ItemOverrides {
    /** The name of the vanilla enchanted book model, used as a base for NEBs own models. */
    public static final String ENCHANTED_BOOK_UNBAKED_MODEL_NAME = "minecraft:item/enchanted_book";

    static ResourceLocation getEnchantedBookModel(String enchantment) {
        return ResourceLocation.fromNamespaceAndPath(NekosEnchantedBooks.MOD_ID, "item/" + enchantment.replace(".", "/"));
    }

    private static ModelResourceLocation modelFileToLocation(ResourceLocation modelFile) {
        return ModelResourceLocation.inventory(modelFile.withPath(s -> s.substring("models/".length(), s.length() - ".json".length())));
    }

    private static String modelToEnchantment(ModelResourceLocation model) {
        return model.id().getPath().substring("item/".length()).replace("/", ".");
    }

    private static final Set<String> TEXTURED_ENCHANTMENTS = new HashSet<>();
    private static final Set<ModelResourceLocation> PREPARED_MODELS = new HashSet<>();

    private final Map<String, BakedModel> overrides;

    /**
     * This constructor follows up on the initialization done in its super method,
     * {@link ItemOverrides#ItemOverrides(ModelBaker, UnbakedModel, List, Function)}. It calls the
     * {@link #setup(ModelBaker, Function)} method, where existing models and are queried for automatic model loading.
     * The process of taking advantage of automatic model loading was described in the documentation for the class in
     * {@link EnchantedBookOverrides}.
     * <p>
     * Also note that this class respects any existing overrides that might have been added to the base enchanted book
     * model. However, this is only the case if an enchanted book has an enchantment that is not saved in our own
     * overrides.
     *
     * @param baker         The model baker
     * @param enchantedBook The vanilla enchanted book unbaked model (ensured by
     *                      {@link org.infernalstudios.nebs.mixin.BlockModelMixin BlockModelMixin})
     * @param existing      Any existing item overrides that exist in the base enchanted book model
     * @param spriteGetter  The sprite getter for model baking
     * @see #resolve(BakedModel, ItemStack, ClientLevel, LivingEntity, int)
     * @see EnchantedBookOverrides
     */
    public EnchantedBookOverrides(ModelBaker baker, UnbakedModel enchantedBook, List<ItemOverride> existing, Function<Material, TextureAtlasSprite> spriteGetter) {
        super(baker, enchantedBook, existing, spriteGetter);
        this.overrides = this.setup(baker, spriteGetter);
    }

    /**
     * The setup as described in
     * {@link EnchantedBookOverrides#EnchantedBookOverrides(ModelBaker, UnbakedModel, List, Function)}. Use this to
     * assign {@link #overrides}.
     *
     * @param baker        The model baker
     * @param spriteGetter The sprite getter for model baking
     * @return The map of enchantment IDs to their respective baked models
     *
     * @see EnchantedBookOverrides#EnchantedBookOverrides(ModelBaker, UnbakedModel, List, Function)
     */
    private Map<String, BakedModel> setup(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter) {
        // bake overrides
        BakeResult result = bakeOverrides(baker, spriteGetter, PREPARED_MODELS.size());

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
    private static BakeResult bakeOverrides(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, int expected) {
        ImmutableMap.Builder<String, BakedModel> overrides = ImmutableMap.builderWithExpectedSize(expected);
        ImmutableSet.Builder<String> failed = ImmutableSet.builderWithExpectedSize(expected);
        PREPARED_MODELS.forEach(model -> {
            String enchantment = modelToEnchantment(model);

            // Now we are ready to bake the custom model and add it to our own overrides.
            BakedModel baked = baker.bake(model.id(), BlockModelRotation.X0_Y0, spriteGetter);
            if (baked == null) {
                failed.add(enchantment);
                return;
            }

            TEXTURED_ENCHANTMENTS.add(enchantment);
            overrides.put(enchantment, baked);
        });
        return new BakeResult(overrides, failed);
    }

    /**
     * Prepares all custom models to be used by NEBs. By registering them through the
     * {@link ModelEvent.RegisterAdditional} event, we can save the trouble of needing to manually resolve and bake them
     * and their parents ourselves.
     *
     * @param models The consumer to accept new models to be registered
     */
    static void prepare(Consumer<ModelResourceLocation> models) {
        Minecraft.getInstance().getResourceManager().listResources("models/item", id -> NekosEnchantedBooks.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("json")).forEach((modelFile, r) -> {
            // save enchantment
            ModelResourceLocation enchantment = modelFileToLocation(modelFile);

            PREPARED_MODELS.add(enchantment);
            models.accept(enchantment);
        });
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
     * Holds the result of the model baking done in {@link #bakeOverrides(ModelBaker, Function, int)}.
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
        for (Enchantment enchantment : getEnchantments(stack)) {
            String key = NekosEnchantedBooks.getIdOf(enchantment);
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
    private static Iterable<Enchantment> getEnchantments(ItemStack stack) {
        var enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.isEmpty() ? null : () -> new Iterator<>() {
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
