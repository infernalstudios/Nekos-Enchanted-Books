package org.infernalstudios.nebs;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * <h1>Neko's Enchanted Books</h1>
 * <p>
 * This is the main class for the Neko's Enchanted Books (shortened to NEBs) mod, loaded by Forge. The mod itself does
 * not interface much with Forge itself, but rather uses
 * {@link org.infernalstudios.nebs.mixin.BlockModelMixin BlockModelMixin} to inject the custom item overrides for the
 * enchanted books provided in {@link EnchantedBookOverrides}.
 */
@Mod(NekosEnchantedBooks.MOD_ID)
public class NekosEnchantedBooks {
    /** The Mod ID for this mod. Note that this variable is in-lined at compile time, so it is safe to reference. */
    public static final String MOD_ID = "nebs";
    /** The logger for this mod. Package-private since it doesn't need to be accessed in many places. */
    static final Logger LOGGER = LogManager.getLogger();

    /**
     * A set of enchantments that are known to not actually be enchantments or do not have an associated enchanted book.
     * You should add to this set during {@link FMLClientSetupEvent} using
     * {@link FMLClientSetupEvent#enqueueWork(Runnable) event.enqueueWork(Runnable)} if you have any custom enchantments
     * that fall under this category.
     */
    @Deprecated(forRemoval = true, since = "2.0.3") // gotta replace this with a config
    public static final Set<String> NON_ENCHANTMENTS = new HashSet<>();

    /**
     * Gets the NEBs ID of the given enchantment, which is the base {@linkplain Enchantment#description() description}
     * while removing the {@code enchantment.} prefix if it exists.
     *
     * @param enchantment The enchantment to get the ID of
     * @return The NEBs ID of the enchantment
     */
    static @Nullable String idOf(Enchantment enchantment) {
        if (!(enchantment.description().getContents() instanceof TranslatableContents contents))
            return null;

        String id = contents.getKey();
        return id.startsWith("enchantment.") ? id.substring("enchantment.".length()) : id;
    }

    public NekosEnchantedBooks() {
        this.setupListeners(FMLJavaModLoadingContext.get().getModEventBus(), MinecraftForge.EVENT_BUS);
    }

    private void setupListeners(IEventBus modBus, IEventBus forgeBus) {
        modBus.<FMLClientSetupEvent>addListener(event -> event.enqueueWork(this::setup));
        modBus.<ModelEvent.RegisterAdditional>addListener(event -> EnchantedBookOverrides.prepare(event::register));
        forgeBus.<ClientPlayerNetworkEvent.LoggingIn>addListener(event -> EnchantedBookOverrides.validate(event.getPlayer().registryAccess().registryOrThrow(Registries.ENCHANTMENT)));
        modBus.addListener(this::gatherData);
    }

    @Deprecated(forRemoval = true, since = "2.0.3") // gotta replace this with a config
    private void setup() {
        NON_ENCHANTMENTS.add("apotheosis.infusion");
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
        generator.addProvider(event.includeClient(), new EnchantedBookModelProvider(generator.getPackOutput(), NekosEnchantedBooks.MOD_ID, event.getExistingFileHelper()));
    }
}
