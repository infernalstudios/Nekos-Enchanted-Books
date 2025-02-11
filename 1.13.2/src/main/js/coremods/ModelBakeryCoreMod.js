var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var InvokeDynamicInsnNode = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var LocalVariableNode = Java.type('org.objectweb.asm.tree.LocalVariableNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');

var Handle = Java.type('org.objectweb.asm.Handle');
var Type = Java.type('org.objectweb.asm.Type');

function initializeCoreMod() {
    return {
        'nebs_ModelBakery_prepare': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.renderer.model.ModelBakery'
            },
            'transformer': prepare
        }
    };
}

function prepare(clazz) {
    clazz.methods.add(makeLambda(clazz));

    for (var i = 0; i < clazz.methods.size(); i++) {
        var method = clazz.methods.get(i);
        if (method.name.equals('func_177570_a') && method.desc.equals('()Ljava/util/Map;')) {
            transform(clazz, method);
        }
    }

    return clazz;
}

function transform(clazz, method) {
    var label = null;

    var index;
    for (index = 0; index < method.instructions.size(); index++) {
        var insn = method.instructions.get(index);
        if (insn.getOpcode() === Opcodes.LDC && insn.cst.equals('minecraft:trident_in_hand#inventory')) {
            break;
        }
    }

    for (; index < method.instructions.size(); index++) {
        var insn = method.instructions.get(index);
        if (insn.getType() === AbstractInsnNode.LABEL) {
            label = insn;
            break;
        }
    }

    if (label === null) return;

    var instructions = new InsnList();
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
    instructions.add(new InvokeDynamicInsnNode(
        'accept',
        '(L' + clazz.name + ';Ljava/util/Map;)Ljava/util/function/Consumer;',
        new Handle(Opcodes.H_INVOKESTATIC, 'java/lang/invoke/LambdaMetafactory', 'metafactory', '(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;', false),
        Type.getMethodType('(Ljava/lang/Object;)V'),
        new Handle(Opcodes.H_INVOKESPECIAL, clazz.name, 'nebs$prepare', '(Ljava/util/Map;Lnet/minecraft/client/renderer/model/ModelResourceLocation;)V', false),
        Type.getMethodType('(Lnet/minecraft/client/renderer/model/ModelResourceLocation;)V')
    ));
    instructions.add(ASMAPI.buildMethodCall('org/infernalstudios/nebs/EnchantedBookOverrides', 'prepare', '(Ljava/util/function/Consumer;)V', ASMAPI.MethodType.STATIC));

    method.instructions.insert(label, instructions);
}

function makeLambda(clazz) {
    // signature
    var prepare = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, 'nebs$prepare', '(Ljava/util/Map;Lnet/minecraft/client/renderer/model/ModelResourceLocation;)V', null, null);

    // labels
    var start = new LabelNode();
    var end = new LabelNode();

    // instructions
    var instructions = new InsnList();
    instructions.add(start);
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
    instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
    instructions.add(ASMAPI.buildMethodCall('net/minecraft/client/renderer/model/ModelBakery', ASMAPI.mapMethod('func_209594_a'), '(Ljava/util/Map;Lnet/minecraft/client/renderer/model/ModelResourceLocation;)V', ASMAPI.MethodType.SPECIAL));
    instructions.add(new InsnNode(Opcodes.RETURN));
    instructions.add(end);

    prepare.instructions = instructions
    prepare.localVariables = [
        new LocalVariableNode('this', 'L' + clazz.name + ';', null, start, end, 0),
        new LocalVariableNode('map', 'Ljava/util/Map;', null, start, end, 1),
        new LocalVariableNode('location', 'Lnet/minecraft/client/renderer/model/ModelResourceLocation;', null, start, end, 2)
    ];
    prepare.maxStack = 3;
    prepare.maxLocals = 3;

    return prepare;
}
