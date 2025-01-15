package org.infernalstudios.nebs;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * <h1>Enchanted Book Model Provider</h1>
 * This data generator automatically generates models for texture files of enchanted books. It reads the textures from
 * the {@code assets/nebs/textures/item} directory and generates the models for them with the same structure.
 * <h2>Usage for Modders</h2>
 * If you are interested in using an implementation of this provider for your own mod, make sure that you have the same
 * directory structure that is stated above. Since all the paths will remain the same, {@link #registerModels()} is
 * marked as final to prevent any changes that might lead to incorrect model generation.
 *
 * @apiNote This class was previously named {@code ModItemModelProvider}, but has since been renamed with a more generic
 * name to show that is available for other modders to use.
 * @implNote This class grabs resources in {@link #listResources(String)} using the instance's class loader. This means
 * that <strong>modders must use an anonymous class or explicitly extend this class</strong> so that NEBs own files
 * aren't discovered by the provider. This is an implementation detail in texture location, but it makes the class
 * significantly less complex. The constructor is set to protected to reflect this.
 */
public class EnchantedBookModelProvider extends ItemModelProvider {
    private static final String EXPECTED_PATH = "assets/nebs/textures/item";

    private static final ModelFile ENCHANTED_BOOK_MODEL = new ModelFile.UncheckedModelFile(EnchantedBookOverrides.ENCHANTED_BOOK_UNBAKED_MODEL_NAME);
    private static final String ENCHANTED_BOOK_TEXTURE_KEY = "layer0";

    /**
     * Creates a new provider for the given output, mod ID, and existing file helper.
     *
     * @param output             The output to write the generated data to
     * @param modId              The mod ID to generate the data for (does not effect the model location)
     * @param existingFileHelper The existing file helper to use for this provider
     * @implNote Modders, see {@link EnchantedBookModelProvider} for implementation details.
     */
    protected EnchantedBookModelProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        super(output, modId, existingFileHelper);
    }

    /**
     * Registers all models for the enchanted books. See {@link EnchantedBookModelProvider} for details.
     *
     * @apiNote This method is marked as final to prevent any changes that might lead to incorrect model generation.
     */
    @Override
    protected final void registerModels() {
        this.listResources(EXPECTED_PATH)
            .map(Path::toString).filter(s -> s.endsWith(".png"))
            .map(s -> s.substring((EXPECTED_PATH + "/").length(), s.length() - ".png".length()))

            .forEach(this::generateModel);
    }

    private void generateModel(String name) {
        ResourceLocation location = EnchantedBookOverrides.getEnchantedBookModel(name);
        if (!existingFileHelper.exists(location, PackType.CLIENT_RESOURCES, ".png", "textures")) {
            throw new IllegalStateException(name + " book texture not found, yet it was found as a resource earlier...");
        }

        this.getBuilder(location.getPath())
            .parent(ENCHANTED_BOOK_MODEL)
            .texture(ENCHANTED_BOOK_TEXTURE_KEY, location);
    }

    /**
     * Lists all resources in the given path. This will not include directories, only files with opaque content.
     *
     * @param path The path to get the resources of
     * @return The stream of resources in the given path
     *
     * @apiNote This is a technically unsafe method as it does not close the file walkers, but it is only used during
     * data generation so it doesn't matter too much.
     * @see #registerModels()
     */
    @SuppressWarnings("resource") // the walkers cannot be closed because we need to read the files in registerModels()
    private Stream<Path> listResources(String path) {
        try {
            // make sure the path exists in the first place
            URL resourceUrl = this.getClass().getClassLoader().getResource(path);
            if (resourceUrl == null) {
                throw new FileSystemNotFoundException("No resources found in \"%s\"".formatted(path));
            }

            Path resourcePath = Paths.get(resourceUrl.toURI());
            if (Files.isDirectory(resourcePath)) {
                // we are in dev environment, so walk the directory
                return Files.walk(resourcePath).filter(Files::isRegularFile);
            } else if (resourceUrl.getProtocol().equals("jar")) {
                return Files.walk(FileSystems.newFileSystem(resourceUrl.toURI(), Collections.emptyMap()).getPath(path)).filter(Files::isRegularFile);
            } else {
                throw new IOException("Unsupported resource protocol \"%s\"".formatted(resourceUrl.getProtocol()));
            }
        } catch (FileSystemNotFoundException | URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to get resources in \"%s\"".formatted(path), e);
        }
    }
}
