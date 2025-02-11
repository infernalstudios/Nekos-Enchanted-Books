package org.infernalstudios.nebs;

import net.minecraft.data.DataGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * <h1>Neko's Enchanted Books</h1>
 * <p>
 * This is the main class for the Neko's Enchanted Books (shortened to NEBs) mod, loaded by Forge. The mod itself does
 * not interface much with Forge itself, but rather uses coremods to ensure the injection of the custom item overrides
 * for the enchanted books provided in {@link EnchantedBookOverrides}.
 */
@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    /** The Mod ID for this mod. Note that this variable is in-lined at compile time, so it is safe to reference. */
    public static final String MOD_ID = "nebs";
    /** The logger for this mod. Package-private since it doesn't need to be accessed in many places. */
    static final Logger LOGGER = LogManager.getLogger();

    @Deprecated // gotta replace this with a config
    public static final Set<String> NON_ENCHANTMENTS = Util.make(new HashSet<>(), set -> set.add("apotheosis.infusion"));

    /**
     * Gets the NEBs ID of the given enchantment, which is the base {@linkplain Enchantment#getDescriptionId()}
     * description} while removing the {@code enchantment.} prefix if it exists.
     *
     * @param enchantment The enchantment to get the ID of
     * @return The NEBs ID of the enchantment
     */
    static String idOf(Enchantment enchantment) {
        String id = enchantment.getDescriptionId();
        return id.startsWith("enchantment.") ? id.substring("enchantment.".length()) : id;
    }

    public NekosEnchantedBooks() {
        ModLoadingContext context = ModLoadingContext.get();

        // If this mod is loaded on a server, don't require clients to have it
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> "", (a, b) -> true));

        // If we're on a server, stop now
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> Client::new);
    }

    /**
     * Due to issues with Java 8's class verifier, it is necessary to move our setup code into a separate class. This
     * way, the {@link NekosEnchantedBooks} class can be loaded on a server without issue.
     *
     * @see DistExecutor.SafeReferent
     */
    private static final class Client {
        private Client() {
            ModLoadingContext context = ModLoadingContext.get();

            this.setupListeners(context.<FMLJavaModLoadingContext>extension().getModEventBus());
        }

        private void setupListeners(IEventBus modBus) {
            modBus.<ModelRegistryEvent>addListener(event -> EnchantedBookOverrides.prepare(ForgeRegistries.ENCHANTMENTS, ModelLoader::addSpecialModel));
            modBus.addListener(this::gatherData);
        }

        /**
         * Adds our data generator, {@link EnchantedBookModelProvider}, to the {@link GatherDataEvent} event. This is
         * used to generate the item models for the enchanted books that NEBs natively supports.
         *
         * @param event The event to add our generator to
         */
        private void gatherData(GatherDataEvent event) {
            DataGenerator generator = event.getGenerator();

            if (event.includeClient()) {
                // native enchanted book models
                generator.addProvider(new EnchantedBookModelProvider(generator, NekosEnchantedBooks.MOD_ID, event.getExistingFileHelper()));
            }
        }
    }
}
