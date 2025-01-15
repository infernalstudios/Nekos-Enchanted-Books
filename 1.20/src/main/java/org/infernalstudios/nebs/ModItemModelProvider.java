package org.infernalstudios.nebs;

import net.minecraft.data.DataGenerator;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

public class ModItemModelProvider extends ItemModelProvider {
    private static final ModelFile ENCHANTED_BOOK_MODEL = new ModelFile.UncheckedModelFile("item/enchanted_book");
    private static final String ENCHANTED_BOOK_TEXTURE_KEY = "layer0";

    public ModItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator.getPackOutput(), NekosEnchantedBooks.MOD_ID, existingFileHelper);
    }

    private void generateModel(String name) {
        final var location = modLoc("item/nebs/" + name);
        if (!existingFileHelper.exists(location, PackType.CLIENT_RESOURCES, ".png", "textures")) {
            throw new IllegalStateException(name + " book texture not found, yet it was found as a resource earlier...");
        }

        this.getBuilder(name.replace("/", "_"))
            .parent(ENCHANTED_BOOK_MODEL)
            .texture(ENCHANTED_BOOK_TEXTURE_KEY, location);
    }

    @Override
    protected void registerModels() {
        this.listResources("assets/nebs/textures/item/nebs")
            .map(Path::toString).filter(s -> s.endsWith(".png"))
            .map(s -> s.substring("assets/nebs/textures/item/nebs/".length(), s.length() - ".png".length()))
            .forEach(this::generateModel);
    }

    @SuppressWarnings("resource") // the walkers cannot be closed because we need to read the files in registerModels()
    private Stream<Path> listResources(String path) {
        try {
            var resourceUrl = this.getClass().getClassLoader().getResource(path);
            if (resourceUrl == null) {
                throw new FileSystemNotFoundException("No resources found in \"%s\"".formatted(path));
            }

            var resourcePath = Paths.get(resourceUrl.toURI());
            if (Files.isDirectory(resourcePath)) {
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
