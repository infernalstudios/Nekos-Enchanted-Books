module org.infernalstudios.nebs {
    exports org.infernalstudios.nebs;
    exports org.infernalstudios.nebs.mixin;

    requires net.minecraftforge.eventbus;
    requires net.minecraftforge.fmlcore;
    requires net.minecraftforge.forge;
    requires net.minecraftforge.javafmlmod;
    requires net.minecraftforge.mergetool.api;

    requires org.spongepowered.mixin;
    requires com.google.gson;
    requires com.google.common;
    requires org.slf4j;
    requires static org.jetbrains.annotations;
}