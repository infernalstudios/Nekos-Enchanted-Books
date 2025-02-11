var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function initializeCoreMod() {
    return {
        'wrap_enchanted_book_model': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.resources.model.ItemModel',
                'methodName': 'm_7611_', // bake
                'methodDesc': '(Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;Lnet/minecraft/client/resources/model/ModelState;)Lnet/minecraft/client/resources/model/BakedModel;'
            },
            'transformer': wrapModel
        }
    };
}

// TODO: Eventually replace with new API to wrap baked item models
function wrapModel(method) {
    const list = () => ASMAPI.listOf(
        // bakedmodel
        new VarInsnNode(Opcodes.ALOAD, 0),
        ASMAPI.buildFieldCall(Opcodes.GETFIELD, 'net/minecraft/client/resources/model/ItemModel', ASMAPI.mapField('f_347422_'), 'Lnet/minecraft/resources/ResourceLocation;'), // this.id
        new VarInsnNode(Opcodes.ALOAD, 1), // baker
        new VarInsnNode(Opcodes.ALOAD, 2), // spriteGetter
        ASMAPI.buildMethodCall(ASMAPI.MethodType.STATIC, 'org/infernalstudios/nebs/EnchantedBookOverrides', 'of', '(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelBaker;Ljava/util/function/Function;)Lnet/minecraft/client/resources/model/BakedModel;')
    );

    for (let insn of method.instructions)
        if (insn.getOpcode() === Opcodes.ARETURN)
            ASMAPI.insertInsnList(method, insn, list(), ASMAPI.InsertMode.INSERT_BEFORE);

    return method;
}
