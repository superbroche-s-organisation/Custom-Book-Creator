import com.google.gson.Gson;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import fr.superbroche.mcreator.custombook.registry.PluginElementTypes;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Legacy2022PackageTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("custombook.pluginRoot"));
        Path output = Path.of(args[0]);
        // These checks use MCreator 2022.2's actual classes, not replacement API stubs.
        check(CustomBook.class.getMethod("getTexture").getReturnType() == String.class, "2022 texture ABI");
        check(CustomBook.class.getMethod("getCreativeTab").getReturnType() == net.mcreator.element.parts.TabEntry.class, "2022 creative tab ABI");
        for (var method : net.mcreator.ui.modgui.ModElementGUI.class.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) continue;
            var implementation = CustomBookGUI.class.getMethod(method.getName(), method.getParameterTypes());
            check(!Modifier.isAbstract(implementation.getModifiers()), "missing GUI method: " + method);
        }
        boolean saveBridge = java.util.Arrays.stream(CustomBookGUI.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("getElementFromGUI") && m.isBridge()
                    && m.getReturnType() == net.mcreator.element.GeneratableElement.class);
        check(saveBridge, "missing save bridge method");
        // Plugin registration and its native BaseType drive the legacy item registry generator.
        var translations = new java.util.ListResourceBundle() {
            @Override protected Object[][] getContents() { return new Object[0][0]; }
        };
        for (String fieldName : List.of("rb", "rb_en")) {
            var translationField = net.mcreator.ui.init.L10N.class.getDeclaredField(fieldName);
            translationField.setAccessible(true);
            translationField.set(null, translations);
        }
        PluginElementTypes.load();
        check(PluginElementTypes.CUSTOMBOOK.getBaseType() == net.mcreator.element.BaseType.ITEM, "legacy item not registered");
        verifyNativeWorkspaceRoundTrip();

        CustomBook book = new CustomBook(null);
        book.startingBook = true;
        book.glow = true;
        book.isPiglinCurrency = true;
        book.destroyAnyBlock = true;
        book.enchantability = 8;
        book.rarity = "RARE";
        book.itemTexture = "book_icon";
        var page = book.getBookCategories().get(0).pages.get(0);
        page.pageSound = "chapter.intro";
        page.pageSoundTrigger = "BUTTON";
        page.titleDesign.position = "LEFT";
        page.titleDesign.content = "[b]Legacy title[/b]";
        var field = CustomBook.BookElement.input();
        field.inputKey = "terms";
        field.inputChoices = List.of("Accept", "Decline");
        page.elements.add(field);
        book.getWritingZones();
        Gson gson = new Gson();
        String saved = gson.toJson(book);
        CustomBook restored = gson.fromJson(saved, CustomBook.class);
        check(restored.getWritingZones().get(0).inputKey.equals("terms"), "writing key lost on reload");
        check(restored.getSafeStackSize() == 1, "writable book stacks");
        check(restored.writingPermission.getFixedValue(), "default permission lost");
        check(restored.getBookCategories().get(0).pages.get(0).titleDesign.position.equals("LEFT"), "title layout lost");

        Path templates = root.resolve("forge-1.17.1/templates/custombook");
        Configuration config = new Configuration(Configuration.VERSION_2_3_31);
        config.setDirectoryForTemplateLoading(templates.toFile());
        config.setDefaultEncoding("UTF-8");
        BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_31);
        wrapper.setExposeFields(true);
        config.setObjectWrapper(wrapper.build());
        Map<String, Object> data = new HashMap<>();
        data.put("data", restored);
        data.put("package", "com.example.audit");
        data.put("name", "Audit");
        data.put("modid", "audit_mod");
        data.put("registryname", "audit_book");
        data.put("JavaModName", "AuditMod");
        Map<String, String> classes = Map.ofEntries(
            Map.entry("item", "item/AuditItem"),
            Map.entry("screen", "client/gui/AuditBookScreen"),
            Map.entry("client_events", "client/AuditBookClientEvents"),
            Map.entry("writing_data", "book/AuditBookWritingData"),
            Map.entry("writing_server", "book/AuditBookWritingServer"),
            Map.entry("writing_client", "client/AuditBookWritingClient"),
            Map.entry("writing_screen", "client/gui/AuditBookWritingScreen"),
            Map.entry("writing_network", "network/AuditBookWritingMessage"),
            Map.entry("legacy_graphics", "client/gui/AuditBookLegacyGraphics"),
            Map.entry("legacy_textbox", "client/gui/AuditBookLegacyTextBox"),
            Map.entry("legacy_button", "client/gui/AuditBookLegacyButton"));
        for (var entry : classes.entrySet()) {
            String java = render(config, entry.getKey() + ".java.ftl", data);
            check(!java.contains("@@") && !java.contains("<#"), "unresolved " + entry.getKey());
            write(output.resolve("com/example/audit/" + entry.getValue() + ".java"), java);
        }
        for (String model : List.of("item_model", "item_model_custom_json", "item_model_custom_obj")) {
            String json = render(config, model + ".json.ftl", data);
            new com.google.gson.JsonParser().parse(json);
            check(json.contains("audit_mod:items/book_icon"), "legacy custom texture folder");
        }
        data.put("data", new StartingData());
        String starting = render(config, "starting_book_events.java.ftl", data);
        check(starting.contains("AuditModItems.AUDIT_BOOK)") && !starting.contains("AUDIT_BOOK.get()"), "legacy item registry reference");
        write(output.resolve("com/example/audit/event/AuditStartingBookEvents.java"), starting);
        String manifest = Files.readString(root.resolve("plugin.json"));
        check(manifest.contains("202200200000") && !manifest.contains("2026002"), "incorrect legacy version range");
        try (var children = Files.list(root)) {
            check(children.filter(p -> Files.exists(p.resolve("custombook.definition.yaml"))).count() == 1, "wrong generator profiles in legacy ZIP");
        }
        System.out.println("LEGACY_2022_PACKAGE_OK: ABI, registration, JSON reload and 12 generated Java templates");
    }
    public static final class StartingData {
        public Element getModElement() { return new Element(); }
    }
    private static void verifyNativeWorkspaceRoundTrip() throws Exception {
        Path directory = Files.createTempDirectory("custombook-legacy-workspace-");
        try {
            // Headless icon fixture only: do not run PluginLoader's constructor,
            // which would scan the user's installed plugins.
            Path icon = directory.resolve("test-icon.png");
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", icon.toFile());
            ResourceOnlyLoader.icon = icon.toUri().toURL();
            var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            var unsafe = (sun.misc.Unsafe)unsafeField.get(null);
            net.mcreator.plugin.PluginLoader.INSTANCE = (ResourceOnlyLoader)unsafe.allocateInstance(ResourceOnlyLoader.class);
            // 2022.2 has no public/protected Workspace constructor. Invoke its native
            // constructor without starting a generator or the autosave executor.
            var constructor = net.mcreator.workspace.Workspace.class.getDeclaredConstructor(net.mcreator.workspace.settings.WorkspaceSettings.class);
            constructor.setAccessible(true);
            var workspace = constructor.newInstance(new net.mcreator.workspace.settings.WorkspaceSettings("legacy_test"));
            workspace.getWorkspaceSettings().setWorkspace(workspace);
            var filesConstructor = net.mcreator.workspace.WorkspaceFileManager.class.getDeclaredConstructor(java.io.File.class, net.mcreator.workspace.Workspace.class);
            filesConstructor.setAccessible(true);
            var fileManager = filesConstructor.newInstance(directory.resolve("legacy_test.mcreator").toFile(), workspace);
            // Disable only this temporary test workspace's automatic saves.
            var executorField = net.mcreator.workspace.WorkspaceFileManager.class.getDeclaredField("dataSaveExecutor");
            executorField.setAccessible(true);
            ((java.util.concurrent.ExecutorService)executorField.get(fileManager)).shutdownNow();
            var fileManagerField = net.mcreator.workspace.Workspace.class.getDeclaredField("fileManager");
            fileManagerField.setAccessible(true);
            fileManagerField.set(workspace, fileManager);
            var manager = workspace.getModElementManager();
            var element = new net.mcreator.workspace.elements.ModElement(workspace, "LegacyBook", PluginElementTypes.CUSTOMBOOK);
            var original = new CustomBook(element);
            var zone = CustomBook.BookElement.input();
            zone.inputKey = "terms";
            original.getBookCategories().get(0).pages.get(0).elements.add(zone);
            original.writingPermission = new fr.superbroche.mcreator.custombook.legacy.LogicProcedure("CanWrite", false);
            original.onBookSigned = new net.mcreator.element.parts.Procedure("Signed");
            original.getBookCategories().get(0).pages.get(0).elements.add(CustomBook.BookElement.image("image_test", 80, 60));
            String json = manager.generatableElementToJSON(original);
            Path saved = directory.resolve("LegacyBook.mod.json");
            Files.writeString(saved, json);
            CustomBook reloaded = (CustomBook)manager.fromJSONtoGeneratableElement(Files.readString(saved), element);
            check(reloaded != null && reloaded.getWritingZones().size() == 1, "native workspace reload");
            check(reloaded.writingPermission.getName().equals("CanWrite") && !reloaded.writingPermission.getFixedValue(), "native permission persistence");
            check(reloaded.onBookSigned.getName().equals("Signed"), "native signing trigger persistence");
            check(reloaded.getBookCategories().get(0).pages.get(0).elements.stream()
                    .anyMatch(e -> e.type.equals("IMAGE") && e.mediaName.equals("image_test")), "native image persistence");
        } finally {
            net.mcreator.plugin.PluginLoader.INSTANCE = null;
            try (var files = Files.walk(directory)) {
                for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
    public static final class ResourceOnlyLoader extends net.mcreator.plugin.PluginLoader {
        static java.net.URL icon;
        @Override public java.net.URL getResource(String name) { return icon; }
    }
    public static final class Element { public String getRegistryNameUpper() { return "AUDIT_BOOK"; } }
    private static String render(Configuration config, String file, Map<String, Object> data) throws Exception {
        StringWriter out = new StringWriter();
        config.getTemplate(file).process(data, out);
        return out.toString();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void write(Path file, String value) throws Exception { Files.createDirectories(file.getParent()); Files.writeString(file, value); }
}
