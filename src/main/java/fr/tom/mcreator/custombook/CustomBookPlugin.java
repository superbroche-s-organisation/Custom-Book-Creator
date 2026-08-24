package fr.tom.mcreator.custombook;

import fr.tom.mcreator.custombook.registry.PluginElementTypes;
import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.PreGeneratorsLoadingEvent;

public class CustomBookPlugin
extends JavaPlugin {
    public CustomBookPlugin(Plugin plugin) {
        super(plugin);
        this.addListener(PreGeneratorsLoadingEvent.class, preGeneratorsLoadingEvent -> PluginElementTypes.load());
    }
}
