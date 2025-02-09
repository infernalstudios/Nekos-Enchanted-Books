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
        } else if (method.name.equals('getOverrides') && method.desc.equals('(Lnet/minecraft/client/renderer/model/ModelBakery;Lnet/minecraft/client/renderer/model/BlockModel;Ljava/util/function/Function;Lnet/minecraft/client/renderer/vertex/VertexFormat;)Lnet/minecraft/client/renderer/model/ItemOverrideList;')) {
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
        new VarInsnNode(Opcodes.ALOAD, 2), // p_217646_2_
        new VarInsnNode(Opcodes.ALOAD, 2),
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/model/BlockModel', ASMAPI.mapField('field_178317_b'), 'Ljava/lang/String;'), // model.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_217646_1_
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/model/ItemOverrideList;Lnet/minecraft/client/renderer/model/BlockModel;Ljava/lang/String;Lnet/minecraft/client/renderer/model/ModelBakery;)Lnet/minecraft/client/renderer/model/ItemOverrideList;', ASMAPI.MethodType.STATIC)
    );
}

function onForge() {
    return ASMAPI.listOf(
        new VarInsnNode(Opcodes.ALOAD, 2), // p_217646_2_
        new VarInsnNode(Opcodes.ALOAD, 2),
        new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/model/BlockModel', ASMAPI.mapField('field_178317_b'), 'Ljava/lang/String;'), // model.name
        new VarInsnNode(Opcodes.ALOAD, 1), // p_217646_1_
        new VarInsnNode(Opcodes.ALOAD, 3), // textureGetter
        new VarInsnNode(Opcodes.ALOAD, 4), // format
        ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/model/ItemOverrideList;Lnet/minecraft/client/renderer/model/BlockModel;Ljava/lang/String;Lnet/minecraft/client/renderer/model/ModelBakery;Ljava/util/function/Function;Lnet/minecraft/client/renderer/vertex/VertexFormat;)Lnet/minecraft/client/renderer/model/ItemOverrideList;', ASMAPI.MethodType.STATIC)
    );
}
