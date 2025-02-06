package org.infernalstudios.nebs.mixin;

import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {
    @Inject(
        method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "java/util/concurrent/CompletableFuture.thenCombineAsync(Ljava/util/concurrent/CompletionStage;Ljava/util/function/BiFunction;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
            ordinal = 0
        )
    )
    private void reload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor finalExecutor, CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        EnchantedBookOverrides.prepare(resourceManager);
    }
}
