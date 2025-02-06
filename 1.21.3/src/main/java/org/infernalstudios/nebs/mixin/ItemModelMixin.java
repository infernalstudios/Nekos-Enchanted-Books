package org.infernalstudios.nebs.mixin;

import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ItemModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.infernalstudios.nebs.NekosEnchantedBooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Function;

/**
 * This mixin injects the custom item overrides for the enchanted books provided in {@link EnchantedBookOverrides}.
 *
 * @implNote There is unfortunately no Forge event for this. Even if there was, we would have to give up on supporting
 * older Forge versions that some people might use in their modpacks. Since this is a small mod, this is a sacrifice
 * that is worth making.
 */
@Mixin(ItemModel.class)
public class ItemModelMixin {
    @Shadow private @Final ResourceLocation id;
    @Shadow private List<ItemOverride> overrides;

    /**
     * Checks if the model we are getting overrides for is Minecraft's Enchanted Book (defined by
     * {@link EnchantedBookOverrides#ENCHANTED_BOOK_UNBAKED_MODEL_NAME}). If it is, stop whatever the method was going
     * to do an immediately return our custom Item Overrides instead.
     * <p>
     * It's important to note that it wouldn't be enough to redirect the construction of {@link ItemModel.BakedModelWithOverrides} that
     * already exists in this method, because that is only used if the model has no overrides. Since NEBs 2.0, we no
     * longer add overrides to the base model, so this way we can guarantee that even if no resource packs add any
     * overrides, our custom ones can still be used.
     *
     * @param baker        The model baker
     * @param spriteGetter The sprite getter for model baking
     * @param callback     The callback for Mixin, which is used if we are cancelling the original method
     */
    @Inject(
        method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "net/minecraft/client/resources/model/ModelBaker.bake(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;",
            shift = At.Shift.AFTER // shifting after brings us to the label instruction where the bakedmodel variable is accessible
        ),
        locals = LocalCapture.CAPTURE_FAILHARD, // no mixinextras, so we use capture local
        cancellable = true
    )
    private void getOverrides(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, CallbackInfoReturnable<BakedModel> callback, BakedModel bakedmodel) {
        if (EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(this.id))
            callback.setReturnValue(new ItemModel.BakedModelWithOverrides(bakedmodel, new EnchantedBookOverrides(baker, this.overrides, spriteGetter)));
    }
}
