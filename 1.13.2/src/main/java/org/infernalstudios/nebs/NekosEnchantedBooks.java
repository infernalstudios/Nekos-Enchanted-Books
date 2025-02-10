package org.infernalstudios.nebs;

import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * <h1>Neko's Enchanted Books</h1>
 * <p>
 * This is the main class for the Neko's Enchanted Books (shortened to NEBs) mod, loaded by Forge. The mod itself does
 * not interface much with Forge itself, but rather uses coremods to inject the custom item overrides for the enchanted
 * books provided in {@link EnchantedBookOverrides}.
 */
@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    /** The Mod ID for this mod. Note that this variable is in-lined at compile time, so it is safe to reference. */
    public static final String MOD_ID = "nebs";
    /** The logger for this mod. Package-private since it doesn't need to be accessed in many places. */
    static final Logger LOGGER = LogManager.getLogger();

    /**
     * A set of enchantments that are known to not actually be enchantments or do not have an associated enchanted book.
     * You should add to this set during {@link FMLClientSetupEvent} if you have any custom enchantments that fall under
     * this category.
     */
    @Deprecated // gotta replace this with a config
    public static final Set<String> NON_ENCHANTMENTS = new HashSet<>();

    /**
     * Gets the NEBs ID of the given enchantment, which is the base {@linkplain Enchantment#getName()
     * description} while removing the {@code enchantment.} prefix if it exists.
     *
     * @param enchantment The enchantment to get the ID of
     * @return The NEBs ID of the enchantment
     */
    static String idOf(Enchantment enchantment) {
        String id = enchantment.getName();
        return id.startsWith("enchantment.") ? id.substring("enchantment.".length()) : id;
    }

    public NekosEnchantedBooks() {
        ModLoadingContext context = ModLoadingContext.get();

        // If this mod is loaded on a server, don't require clients to have it
        context.registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> "", (a, b) -> true));

        // If we're on a server, stop now
        DistExecutor.runWhenOn(Dist.CLIENT, () -> Client::new);
    }

    /**
     * Due to issues with Java 8's class verifier, it is necessary to move our setup code into a separate class. This
     * way, the {@link NekosEnchantedBooks} class can be loaded on a server without issue.
     */
    private static final class Client {
        private Client() {
            ModLoadingContext context = ModLoadingContext.get();

            this.setupListeners(context.<FMLJavaModLoadingContext>extension().getModEventBus());
        }

        private void setupListeners(IEventBus modBus) {
            modBus.<FMLClientSetupEvent>addListener(event -> this.setup());
            //modBus.<ModelRegistryEvent>addListener(event -> EnchantedBookOverrides.prepare(ForgeRegistries.ENCHANTMENTS, ModelLoader::addSpecialModel));
        }

        @Deprecated // gotta replace this with a config
        private void setup() {
            NON_ENCHANTMENTS.add("apotheosis.infusion");
        }
    }
}
