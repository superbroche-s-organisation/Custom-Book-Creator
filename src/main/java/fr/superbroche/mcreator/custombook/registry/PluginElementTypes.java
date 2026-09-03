package fr.superbroche.mcreator.custombook.registry;

import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import net.mcreator.element.ModElementType;
import net.mcreator.element.ModElementTypeLoader;
import net.mcreator.generator.GeneratorFlavor;

public final class PluginElementTypes {
    public static ModElementType<?> CUSTOMBOOK;

    private PluginElementTypes() {
    }

    public static void load() {
        CUSTOMBOOK = ModElementTypeLoader.register((ModElementType)new ModElementType("custombook", "custombook", (Character)null, CustomBookGUI::new, CustomBook.class)).coveredOn(GeneratorFlavor.baseLanguage((GeneratorFlavor.BaseLanguage)GeneratorFlavor.BaseLanguage.JAVA));
    }
}
