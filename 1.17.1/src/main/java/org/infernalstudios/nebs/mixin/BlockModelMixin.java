package org.infernalstudios.nebs.mixin;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

/**
 * This mixin injects the custom item overrides for the enchanted books provided in {@link EnchantedBookOverrides}.
 *
 * @implNote There is unfortunately no Forge event for this. Even if there was, we would have to give up on supporting
 * older Forge versions that some people might use in their modpacks. Since this is a small mod, this is a sacrifice
 * that is worth making.
 */
@Mixin(BlockModel.class)
public class BlockModelMixin {
    @Shadow private @Final List<ItemOverride> overrides;

    /**
     * Checks if the model we are getting overrides for is Minecraft's Enchanted Book (defined by
     * {@link EnchantedBookOverrides#ENCHANTED_BOOK_UNBAKED_MODEL_NAME}). If it is, stop whatever the method was going
     * to do an immediately return our custom Item Overrides instead.
     * <p>
     * It's important to note that it wouldn't be enough to redirect the construction of {@link ItemOverrides} that
     * already exists in this method, because that is only used if the model has no overrides. Since NEBs 2.0, we no
     * longer add overrides to the base model, so this way we can guarantee that even if no resource packs add any
     * overrides, our custom ones can still be used.
     *
     * @param bakery       The model bakery
     * @param model        The unbaked model to get overrides for
     * @param spriteGetter The sprite getter for model baking
     * @param callback     The callback for Mixin, which is used if we are cancelling the original method
     */
    @Inject(
        method = "getOverrides(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;",
        remap = false, // this method was patched in by Forge, due to the spriteGetter being artificially inserted
        at = @At("HEAD"),
        cancellable = true
    )
    private void getOverrides(ModelBakery bakery, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<ItemOverrides> callback) {
        if (EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(model.name))
            callback.setReturnValue(new EnchantedBookOverrides(bakery, model, bakery::getModel, spriteGetter, this.overrides));
    }

    /**
     * This injector exists for backwards compatibility with older versions of Minecraft and Forge that do not strictly
     * use Forge's sprite getter.
     *
     * @param bakery   The model bakery
     * @param model    The unbaked model to get overrides for
     * @param callback The callback for Mixin, which is used if we are cancelling the original method
     * @see #getOverrides(ModelBakery, BlockModel, Function, CallbackInfoReturnable)
     */
    @Inject(
        method = "getItemOverrides(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;",
        at = @At("HEAD"),
        cancellable = true
    )
    @SuppressWarnings("deprecation")
    private void getOverrides(ModelBakery bakery, BlockModel model, CallbackInfoReturnable<ItemOverrides> callback) {
        if (EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(model.name))
            callback.setReturnValue(new EnchantedBookOverrides(bakery, model, bakery::getModel, this.overrides));
    }
}
