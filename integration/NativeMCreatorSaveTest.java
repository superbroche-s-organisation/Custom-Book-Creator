import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.registry.PluginElementTypes;
import java.nio.file.*;
import net.mcreator.element.ModElementTypeLoader;
import net.mcreator.generator.*;
import net.mcreator.plugin.*;
import net.mcreator.plugin.events.PreGeneratorsLoadingEvent;
import net.mcreator.preferences.PreferencesManager;
import net.mcreator.ui.init.L10N;
import net.mcreator.workspace.*;
import net.mcreator.workspace.elements.*;
import net.mcreator.workspace.settings.WorkspaceSettings;

/** Runs the native plugin loader, generator coverage and storage/generation path in an isolated installation. */
public final class NativeMCreatorSaveTest {
    public static void main(String[] args) {
        try {
            var version = new java.util.Properties();
            try (var manifest = NativeMCreatorSaveTest.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
                version.setProperty("mcreator", new java.util.jar.Manifest(manifest).getMainAttributes().getValue("MCreator-Version"));
            }
            net.mcreator.Launcher.version = new net.mcreator.util.MCreatorVersionNumber(version);
            PreferencesManager.PREFERENCES = new net.mcreator.preferences.data.PreferencesData();
            PreferencesManager.PREFERENCES.hidden.enableJavaPlugins.set(true);
            PluginLoader.initInstance();
            if (!PluginLoader.INSTANCE.getFailedPlugins().isEmpty())
                throw new AssertionError("Plugin loading failed: " + PluginLoader.INSTANCE.getFailedPlugins());
            L10N.initTranslations();
            net.mcreator.ui.help.HelpLoader.preloadCache();
            net.mcreator.ui.help.IHelpContext context = () -> "custombook";
            for (String key : java.util.List.of("input_key", "input_mode", "input_default", "input_choices")) {
                var entry = context.withEntry("custombook/writing/" + key);
                if (!net.mcreator.ui.help.HelpLoader.hasFullHelp(entry)
                        || net.mcreator.ui.help.HelpLoader.loadHelpFor(entry, false).contains("No help entry"))
                    throw new AssertionError("Writing help did not load: " + key);
            }
            System.out.println("NATIVE_WRITING_HELP_OK");
            net.mcreator.ui.init.UIRES.preloadImages();
            VariableTypeLoader.loadVariableTypes();
            net.mcreator.blockly.data.BlocklyLoader.init();
            ModElementTypeLoader.loadModElements();
            MCREvent.event(new PreGeneratorsLoadingEvent(null));
            if (PluginElementTypes.CUSTOMBOOK == null) throw new AssertionError("Custom book not registered by plugin event");
            for (String profile : args) verify(profile);
            System.out.println("NATIVE_MCREATOR_SAVE_OK");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void verify(String profile) throws Exception {
        GeneratorConfiguration config = new GeneratorConfiguration(profile);
        Generator.GENERATOR_CACHE.put(profile, config);
        if (!config.getGeneratorStats().getSupportedModElementTypes().contains(PluginElementTypes.CUSTOMBOOK))
            throw new AssertionError(profile + ": Custom book is missing from the element menu");
        WorkspaceSettings settings = new WorkspaceSettings("book_audit");
        settings.setCurrentGenerator(profile);
        settings.setVersion("1.0.0");
        settings.setModName("Book audit");
        settings.setAuthor("Test");
        settings.setDescription("Native save regression");
        settings.setLicense("All Rights Reserved");
        settings.setModElementsPackage("com.example.audit");
        Path directory = Path.of("workspaces", profile).toAbsolutePath();
        Files.createDirectories(directory);
        Workspace workspace = Workspace.createWorkspace(directory.resolve("book_audit.mcreator").toFile(), settings);
        try {
            ModElement element = new ModElement(workspace, "AuditBook", PluginElementTypes.CUSTOMBOOK);
            CustomBook book = new CustomBook(element);
            book.name = "Audit book";
            book.bookTitle = "Audit book";
            workspace.addModElement(element);
            for (boolean writable : new boolean[]{false, true}) {
                if (writable) book.getBookCategories().get(0).pages.get(0).elements.add(CustomBook.BookElement.input());
                workspace.getModElementManager().storeModElement(book);
                if (!workspace.getGenerator().generateBase()) throw new AssertionError(profile + ": base generation failed");
                if (!workspace.getGenerator().generateElement(book)) throw new AssertionError(profile + ": book generation failed");
                workspace.getModElementManager().storeModElementPicture(book);
                element.reinit(workspace);
                workspace.getFileManager().saveWorkspaceDirectlyAndWait();
                workspace.getModElementManager().invalidateCache();
                if (!(element.getGeneratableElement() instanceof CustomBook)) throw new AssertionError("Stored book cannot reload");
                System.out.println("NATIVE_SAVE_OK " + profile + " writable=" + writable);
            }
        } finally { workspace.close(); }
    }
}
