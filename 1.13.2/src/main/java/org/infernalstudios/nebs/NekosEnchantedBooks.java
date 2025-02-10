package org.infernalstudios.nebs;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Util;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
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

    @Deprecated // gotta replace this with a config
    public static final Set<String> NON_ENCHANTMENTS = Util.make(new HashSet<>(), set -> set.add("apotheosis.infusion"));

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
    }
}
