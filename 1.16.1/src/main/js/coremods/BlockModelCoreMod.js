/*
 * This is a coremod that wraps the return value of BlockModel#getItemOverrides and BlockModel#getOverrides with
 * EnchantedBookOverrides#of. The reason this is used over a mixin is that CallbackInfoReturnable#setReturnValue is not
 * friendly with other mixins that modify the return value of the method.
 *
 * At the very bottom of this file, I have, in a comment, a translation of this entire coremod as a Mixin.
 */

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');

function initializeCoreMod() {
    return {
        'nebs_BlockModel_enchantedBookOverrides': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.renderer.model.BlockModel'
            },
            'transformer': getOverrides
        }
    };
}

function getOverrides(clazz) {
    for (var i = 0; i < clazz.methods.size(); i++) {
        var method = clazz.methods.get(i);

        if (method.name.equals(ASMAPI.mapMethod('func_217646_a')) && method.desc.equals('(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/client/renderer/model/BlockModel;)Lnet/minecraft/client/renderer/model/ItemOverrideList;')) {
            transform(method, onVanilla);
        } else if (method.name.equals('getOverrides') && method.desc.equals('(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/client/renderer/model/BlockModel;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/ItemOverrideList;')) {
            transform(method, onForge);
        }
    }

    return clazz;
}

function transform(method, instructions) {
    for (var i = 0; i < method.instructions.size(); i++) {
        var insn = method.instructions.get(i);
        if (insn.getOpcode() === Opcodes.ARETURN) {
            method.instructions.insertBefore(insn, instructions());
            i = method.instructions.indexOf(insn);
        }
    }
}

function onVanilla() {
    return ASMAPI.listOf(
        new VarInsnNode(Opcodes.ALOAD, 2),
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/model/BlockModel', ASMAPI.mapField('field_178317_b'), 'Ljava/lang/String;'), // model.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_217646_1_
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/model/ItemOverrideList;Ljava/lang/String;Lnet/minecraft/client/renderer/model/ModelBakery;)Lnet/minecraft/client/renderer/model/ItemOverrideList;', ASMAPI.MethodType.STATIC)
    );
}

function onForge() {
    return ASMAPI.listOf(
        new VarInsnNode(Opcodes.ALOAD, 2),
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/model/BlockModel', ASMAPI.mapField('field_178317_b'), 'Ljava/lang/String;'), // model.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_217646_1_
        new VarInsnNode(Opcodes.ALOAD, 3), // textureGetter
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/model/ItemOverrideList;Ljava/lang/String;Lnet/minecraft/client/renderer/model/ModelBakery;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/ItemOverrideList;', ASMAPI.MethodType.STATIC)
    );
}

/*
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.infernalstudios.nebs.EnchantedBookOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(BlockModel.class)
public abstract class BlockModelMixin {
    @Inject(
        method = "getItemOverrides(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/client/renderer/model/BlockModel;)Lnet/minecraft/client/renderer/model/ItemOverrideList;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void getOverrides(ModelBakery bakery, BlockModel model, CallbackInfoReturnable<ItemOverrideList> callback) {
        callback.setReturnValue(EnchantedBookOverrides.of(callback.getReturnValue(), model.name, bakery));
    }

    @Inject(
        method = "getOverrides(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/client/renderer/model/BlockModel;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/ItemOverrideList;",
        remap = false, // this method was patched in by Forge, due to the spriteGetter being artificially inserted
        at = @At("HEAD"),
        cancellable = true
    )
    private void getOverrides(ModelBakery bakery, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<ItemOverrideList> callback) {
        callback.setReturnValue(EnchantedBookOverrides.of(callback.getReturnValue(), model.name, bakery, spriteGetter));
    }
}
 */
