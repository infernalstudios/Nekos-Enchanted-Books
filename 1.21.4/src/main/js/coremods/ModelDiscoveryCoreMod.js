var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

function initializeCoreMod() {
    return {
        'nebs_discoverDependencies_resolveEnchantedBooks': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.resources.model.ModelDiscovery',
                'methodName': 'm_353401_', // discoverDependencies
                'methodDesc': '()V'
            },
            'transformer': registerAdditional
        }
    };
}

// TODO: Eventually replace with new ModelEvent.RegisterAdditional event
function registerAdditional(method) {
    const list = ASMAPI.listOf(
        new VarInsnNode(Opcodes.ALOAD, 0),
        ASMAPI.buildFieldCall(Opcodes.GETFIELD, 'net/minecraft/client/resources/model/ModelDiscovery', ASMAPI.mapField('f_347306_'), 'Ljava/util/Map;'), // this.inputModels
        new TypeInsnNode(Opcodes.NEW, 'net/minecraft/client/resources/model/ModelDiscovery$ResolverImpl'),
        new InsnNode(Opcodes.DUP),
        new VarInsnNode(Opcodes.ALOAD, 0), // this
        ASMAPI.buildMethodCall(ASMAPI.MethodType.SPECIAL, 'net/minecraft/client/resources/model/ModelDiscovery$ResolverImpl', '<init>', '(Lnet/minecraft/client/resources/model/ModelDiscovery;)V'),
        ASMAPI.buildMethodCall(ASMAPI.MethodType.STATIC, 'org/infernalstudios/nebs/EnchantedBookItemModel', 'prepare', '(Ljava/util/Map;Lnet/minecraft/client/resources/model/ResolvableModel$Resolver;)V')
    );

    // EnchantedBookItemModel.prepare(this.inputModels, new ModelDiscovery.ResolverImpl(/* this (implied) */));
    ASMAPI.insertInsnList(method, method.instructions.getFirst(), list, ASMAPI.InsertMode.INSERT_BEFORE);

    return method;
}
