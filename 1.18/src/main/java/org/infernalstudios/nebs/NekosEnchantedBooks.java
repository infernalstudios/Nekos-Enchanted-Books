package org.infernalstudios.nebs;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.infernalstudios.nebs.mixin.BlockModelMixin;

/**
 * <h1>Neko's Enchanted Books</h1>
 * <p>
 * This is the main class for the Neko's Enchanted Books (shortened to NEBs) mod, loaded by Forge. The mod itself does
 * not interface much with Forge itself, but rather uses
 * {@link BlockModelMixin BlockModelMixin} to inject the custom item overrides for the
 * enchanted books provided in {@link EnchantedBookOverrides}.
 */
@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    /** The Mod ID for this mod. Note that this variable is in-lined at compile time, so it is safe to reference. */
    public static final String MOD_ID = "nebs";
    /** The logger for this mod. Package-private since it doesn't need to be accessed in many places. */
    static final Logger LOGGER = LogManager.getLogger();

    /**
     * The constructor for the mod. This does two things:
     *
     * <ol>
     *     <li>Register the display test extension point, which tells the game to ignore this mod when polling servers
     *     for mod compatibility.</li>
     *     <li>Add our data generator as a listener to the {@link GatherDataEvent}. See
     *     {@link #gatherData(GatherDataEvent)}</li>
     * </ol>
     */
    public NekosEnchantedBooks() {
        // Newer Forge versions make this simpler, but here the old way is used for backwards compatibility with older Forge versions
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        // The rest is client-only stuff
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::gatherData);
        modBus.<ModelRegistryEvent>addListener(event -> EnchantedBookOverrides.prepare());
    }

    /**
     * Gets the NEBs ID of the given enchantment, which is the base {@link Enchantment#getDescriptionId()} while
     * removing the {@code enchantment.} prefix if it exists.
     *
     * @param enchantment The enchantment to get the ID of
     * @return The NEBs ID of the enchantment
     */
    static String getIdOf(Enchantment enchantment) {
        String id = enchantment.getDescriptionId();
        return id.startsWith("enchantment.") ? id.substring("enchantment.".length()) : id;
    }

    /**
     * Adds our data generator, {@link EnchantedBookModelProvider}, to the {@link GatherDataEvent} event. This is used
     * to generate the item models for the enchanted books that NEBs natively supports.
     *
     * @param event The event to add our generator to
     */
    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();

        // native enchanted book models
        if (event.includeClient()) generator.addProvider(new EnchantedBookModelProvider(generator, NekosEnchantedBooks.MOD_ID, event.getExistingFileHelper()));
    }
}
