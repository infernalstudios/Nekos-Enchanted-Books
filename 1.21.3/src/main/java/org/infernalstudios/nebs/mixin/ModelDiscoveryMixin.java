package org.infernalstudios.nebs.mixin;

import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Set;

@Mixin(ModelDiscovery.class)
public abstract class ModelDiscoveryMixin {
    @ModifyVariable(
        method = "registerStandardModels(Lnet/minecraft/client/resources/model/BlockStateModelLoader$LoadedModels;)V",
        at = @At("STORE"),
        index = 2
    )
    private Set<ModelResourceLocation> registerStandardModels(Set<ModelResourceLocation> set, BlockStateModelLoader.LoadedModels loadedModels) {
        EnchantedBookOverrides.prepare(set::add);
        return set;
    }
}
