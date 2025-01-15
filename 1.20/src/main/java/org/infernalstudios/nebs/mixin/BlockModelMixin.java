package org.infernalstudios.nebs.mixin;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

@Mixin(BlockModel.class)
public class BlockModelMixin {
    @Shadow private @Final List<ItemOverride> overrides;

    @Inject(
        method = "getOverrides(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;",
        remap = false, // this method was patched in by Forge
        at = @At("HEAD"),
        cancellable = true
    )
    private void getOverrides(ModelBaker baker, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<ItemOverrides> callback) {
        if (EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME.equals(model.name))
            callback.setReturnValue(new EnchantedBookOverrides(baker, model, this.overrides, spriteGetter));
    }
}
