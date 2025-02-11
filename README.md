# Neko's Enchanted Books

[![banner](https://i.imgur.com/lEmYwq9.png)](https://www.curseforge.com/minecraft/mc-mods/nekos-enchanted-books)

Neko's Enchanted Books is a small mod that enables adding unique textures for Enchanted Books.

## Usage for Modders and Modpackers

Since 2.0, Neko's Enchanted Books now features an automatic model loading system, where models are loaded automatically from a specific path for the enchantment you want to add. Here is a detailed description of how this system works and how you can use it yourself:

- All models are automatically loaded from the root folder `assets/nebs/models/item`. Each model is organized into the enchantment's description ID (`Enchantment.getDescriptionId()`) where each point is a folder separation. The prefix `enchantment.` is automatically removed from this ID if it exists.
  - For example, if you want to load a model for your enchantment of key `enchantment.mymod.overpowered`, your model must exist in `assets/nebs/models/item/mymod/overpowered.json`. **It is strongly recommended** that your model parents off of `minecraft:item/enchanted_book` instead of `minecraft:item/generated`, so any custom additions made to the base model are reflected in yours.
- The placement of the texture you would like to use does not matter, as long as it is properly referenced in your model file. If you look at any of NEBs's own models as an example, you will see that the `layer0` texture simply points to a texture image that is in the same structure as the model files are. This makes it easy for NEBs to generate its own models, but is not a requirement for you.
  - If you are a modder and are interested in a data generator for your textures, you should read the documentation found in `EnchantedBookModelProvider`.
- If a model does not exist for a registered enchantment when models are baked, then your enchantment is simply ignored and the base `minecraft:item/enchanted_book` is used instead. There is no override or fake model, the vanilla model is used directly.
  - If there are any missing models for enchantments, a warning will be displayed to the console log for debugging purposes.

If you are interested in learning more about how the custom item model system works, you can read the documentation found in `EnchantedBookOverrides`.

## Adding Native Compatibility

To add native compatibility for a mod in NEBs itself, the process is mostly the same. Make sure the texture exists in the correct path in `assets/nebs/textures/item`, run the data generator with `runData`, and the accompanying model files should now exist.

If you want to re-use an existing texture for other enchantments, you should add to the `overrides.json` file located in `assets/nebs`.

> [!WARNING]
> If you run this mod in your development environment, remember to always run `runData` if you have added, moved, or deleted any texture files before doing `runClient`. If you don't, the model files won't exist and your textures will not load into the game.

## Technical Details for Nerds

> [!NOTE]
> This section describes the general process that takes place inside of NEBs. There are exceptions for specific versions, such as 1.21.3 and 1.21.4 where the `ItemModel` is wrapped using (`DelegateItemModel` or a custom implementation respectively) instead of `ItemOverrides`. Also, although some mod loader APIs are used in NEBs, the overall process is mod loader agnostic.

This mod uses a special extension of `ItemOverrides` that gives us complete control over what baked models are sent to the renderer. What normally happens in `ItemOverrides` is the item property functions are run through and a baked model for the specified override is given back to the player. However the overrides instance already has access to the item stack, and property functions usually query the item stack to begin with to get their desired value. This is effectively used as an exploit which allows NEBs can easily override this functionality to allow us to work with the enchantment directly and load any additional models with minimal impact to performance.

To do this, some custom model baking is done inside of `EnchantedBookOverrides`. This is done by taking the original item overrides instance that was baked by the game, but additional baking is applied on top of it so NEBs models can be used without destroying item rendering. There are javadocs and comments littered throughout the mod detailing this process, so if it interests you, feel free to take a look. There is unfortunately no Forge event to supply your own `ItemOverrides` instance for an existing model, so a bytecode injection using `BlockModelCoreMod` allows us to conditionally use own overrides instance if the model the game is getting it for is `minecraft:item/enchanted_book`.

> [!NOTE]
> For 1.21.1 and lower, the modified class in question is named `BlockModel` (in Mojang mappings). It deals with almost all simple block and item models that are rendered in the player's inventory, as well as several other cases. The class name is a little misleading.

When models are baked, they are stored in a map of Enchantment description IDs and baked models. The item stack is then queried for any enchantments. If it has one, then it will try to get the baked model from the map. If there is no baked model for the enchantment, it is defaulted back to the original `ItemOverrides` functionality, which makes the game use the base `minecraft:item/enchanted_book` model instead. This is effectively the same exact function that NEBs 1.0 used, so there is no additional computational performance hit in 2.0, and we get to have a lot more control over how the baked models are resolved for the enchanted books.

> [!IMPORTANT]
> For Forge: CoreMods are used over mixins because, in general, Mixin does not give enough context or compatibility when injecting the overrides system. In all cases, the coremods are carefully written to retain compatibility with other coremods and mixins.
> - In 1.14.4 and older, Forge does not ship Mixin at all.
> - Forge does not currently ship MixinExtras.
>   - In 1.21.1 and older, the coremods mimic the usage of `@ModifyReturnValue`, targeting the `ARETURN` instruction.
>   - In 1.21.3 and later, the coremods allow local variable context to be used alongside `@ModifyVariable`, since `@Local` cannot be.
