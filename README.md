# Neko's Enchanted Books
Neko's Enchanted Books is a Minecraft Forge mod, that adds all kinds of different textures to enchanted books.

## How to add more textures?
This mod is mostly created dynamically using an extended class of ```ItemModelProvider```. This is used to create dynamic models for each enchantment book and one to override the vanilla enchanted book model to make it use property overrides.

But how does it know which models to add? Since minecraft only uses floats for property overrides we want to map each supported enchantment to a float value. This is done in the [properties.json](https://github.com/CGessinger/Nekos-Enchanted-Books/tree/master/src/main/resources/assets/nebs/models/properties.json). To make this file easy to read and expand, please add 1 for each modid. Then every enchantment within this mod is numbered from 0.01 to 0.99. Vanilla enchantments for example are numbered from 1.00 to 1.37. Ensorcellation is numbered from 2.00 to 2.32. Only the value 0.00 should never be assign to an enchantment, since it is meant for the default enchantment book texture.

After adding the correct enchantment name to the properties, we want to add the according texture file to ```resources/assets/nebs/textures/items```. We want to name this file exactly like the enchantment name, without the extra ```enchantment.modid``` part. The protection texture for example is just called ```protection.png```.

And, that's it. The new texture is now successfully added. And don't worry: If the according texture cannot be found, it will just default to the vanila texture.

Note: If you run this mod in your development environment, rememeber to always run ```runData```, when you changed the [properties.json](https://github.com/CGessinger/Nekos-Enchanted-Books/tree/master/src/main/resources/assets/nebs/models/properties.json), before doing ```runClient```.