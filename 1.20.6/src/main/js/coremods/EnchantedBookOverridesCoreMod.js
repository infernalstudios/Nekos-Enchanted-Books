var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var LocalVariableNode = Java.type('org.objectweb.asm.tree.LocalVariableNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');

function initializeCoreMod() {
    return {
        'override_forge_method': {
            'target': {
                'type': 'CLASS',
                'name': 'org.infernalstudios.nebs.EnchantedBookOverrides'
            },
            'transformer': transform
        }
    };
}

function transform(clazz) {
    // signature
    var getOverrides = new MethodNode(Opcodes.ACC_PUBLIC, 'getOverrides', '()Lcom/google/common/collect/ImmutableList;', '()Lcom/google/common/collect/ImmutableList<Lnet/minecraft/client/renderer/block/model/ItemOverrides$BakedOverride;>;', null);

    // labels
    var start = new LabelNode();
    var end = new LabelNode();

    // instructions
    getOverrides.instructions = ASMAPI.listOf(
        start,
        new VarInsnNode(Opcodes.ALOAD, 0),
        new FieldInsnNode(Opcodes.GETFIELD, clazz.name, 'base', 'Lnet/minecraft/client/renderer/block/model/ItemOverrides;'),
        ASMAPI.buildMethodCall('net/minecraft/client/renderer/block/model/ItemOverrides', 'getOverrides', '()Lcom/google/common/collect/ImmutableList;', ASMAPI.MethodType.VIRTUAL),
        new InsnNode(Opcodes.ARETURN),
        end
    );

    // footer
    getOverrides.localVariables = [new LocalVariableNode('this', 'L' + clazz.name + ';', null, start, end, 0)];
    getOverrides.maxStack = 1;
    getOverrides.maxLocals = 1;

    clazz.methods.add(getOverrides);
    return clazz;
}
