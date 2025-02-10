var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');

function initializeCoreMod() {
    return {
        'nebs_ModelBlock_enchantedBookOverrides': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.renderer.model.ModelBlock'
            },
            'transformer': getOverrides
        }
    };
}

function getOverrides(clazz) {
    for (var i = 0; i < clazz.methods.size(); i++) {
        var method = clazz.methods.get(i);
        if (method.name.equals(ASMAPI.mapMethod('func_209568_a')) && method.desc.equals('(Lnet/minecraft/client/renderer/model/ModelBlock;Ljava/util/function/Function;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/ItemOverrideList;')) {
            transform(method, onVanilla);
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
    var instructions = new InsnList();
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
    instructions.add(new FieldInsnNode(Opcodes.GETFIELD, 'net/minecraft/client/renderer/model/ModelBlock', ASMAPI.mapField('field_178317_b'), 'Ljava/lang/String;')); // model.name
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // modelGetter
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 3)); // spriteGetter
    instructions.add(ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/renderer/model/ItemOverrideList;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;)Lnet/minecraft/client/renderer/model/ItemOverrideList;', ASMAPI.MethodType.STATIC));
    return instructions;
}
