var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function initializeCoreMod() {
    return {
        'nebs_bakeModels_wrapEnchantedBook': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.resources.model.ModelBakery',
                'methodName': 'm_371696_', // lambda$bakeModels$6
                'methodDesc': '(Lnet/minecraft/client/resources/model/ModelBakery$TextureGetter;Lnet/minecraft/client/renderer/item/ItemModel;Ljava/util/Map;Ljava/util/Map;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/item/ClientItem;)V'
            },
            'transformer': wrapModel
        }
    };
}

// TODO: Eventually replace with new API to wrap baked item models
function wrapModel(method) {
    const bake = ASMAPI.findFirstMethodCall(method, ASMAPI.MethodType.INTERFACE, 'net/minecraft/client/renderer/item/ItemModel$Unbaked', ASMAPI.mapMethod('m_372419_'), '(Lnet/minecraft/client/renderer/item/ItemModel$BakingContext;)Lnet/minecraft/client/renderer/item/ItemModel;'); // bake
    const list = ASMAPI.listOf(
        // itemmodel1
        new VarInsnNode(Opcodes.ALOAD, 5), // p_374705_
        new VarInsnNode(Opcodes.ALOAD, 8), // modelbakery$modelbakerimpl
        ASMAPI.buildMethodCall(ASMAPI.MethodType.STATIC, 'org/infernalstudios/nebs/EnchantedBookItemModel', 'of', '(Lnet/minecraft/client/renderer/item/ItemModel;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/resources/model/ModelBaker;)Lnet/minecraft/client/renderer/item/ItemModel;')
    );

    // ItemModel itemmodel1 = EnchantedBookItemModel.of(p_374706_.model().bake(itemmodel$bakingcontext), p_374705_, modelbakery$modelbakerimpl);
    ASMAPI.insertInsnList(method, bake, list, ASMAPI.InsertMode.INSERT_AFTER);

    return method;
}
