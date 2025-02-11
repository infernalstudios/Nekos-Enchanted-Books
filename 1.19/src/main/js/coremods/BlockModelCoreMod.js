/*
 * This is a coremod that wraps the return value of BlockModel#getItemOverrides and BlockModel#getOverrides with
 * EnchantedBookOverrides#of. The reason this is used over a mixin is that CallbackInfoReturnable#setReturnValue is not
 * friendly with other mixins that modify the return value of the method.
 */

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');

function initializeCoreMod() {
    return {
        'wrap_enchanted_book_overrides': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.renderer.block.model.BlockModel'
            },
            'transformer': getOverrides
        }
    };
}

function getOverrides(clazz) {
    for (var i = 0; i < clazz.methods.size(); i++) {
        var method = clazz.methods.get(i);

        if (method.name.equals(ASMAPI.mapMethod('m_111446_')) && method.desc.equals('(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;')) {
            transform(method, onVanilla);
        } else if (method.name.equals('getOverrides') && method.desc.equals('(Lnet/minecraft/client/resources/model/ModelBakery;Lnet/minecraft/client/renderer/block/model/BlockModel;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;')) {
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
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/block/model/BlockModel', ASMAPI.mapField('f_111416_'), 'Ljava/lang/String;'), // p_111448_.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_111447_
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/block/model/ItemOverrides;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelBakery;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;', ASMAPI.MethodType.STATIC)
    );
}

function onForge() {
    return ASMAPI.listOf(
        new VarInsnNode(Opcodes.ALOAD, 2),
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/block/model/BlockModel', ASMAPI.mapField('f_111416_'), 'Ljava/lang/String;'), // p_111448_.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_111447_
        new VarInsnNode(Opcodes.ALOAD, 3), // textureGetter
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/block/model/ItemOverrides;Ljava/lang/String;Lnet/minecraft/client/resources/model/ModelBakery;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/block/model/ItemOverrides;', ASMAPI.MethodType.STATIC)
    );
}
