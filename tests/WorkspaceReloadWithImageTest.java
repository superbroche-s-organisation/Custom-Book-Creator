import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.tom.mcreator.custombook.element.types.CustomBook;
import fr.tom.mcreator.custombook.registry.PluginElementTypes;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.Stack;
import net.mcreator.element.GeneratableElement;
import net.mcreator.ui.init.L10N;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.WorkspaceFolderManager;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.ModElementManager;
import net.mcreator.workspace.settings.WorkspaceSettings;

public final class WorkspaceReloadWithImageTest {
    public static void main(String[] args) throws Exception {
        TestWorkspace workspace = new TestWorkspace();

        try {
            initializeTranslations();
            PluginElementTypes.load();
            ModElement modElement = new ModElement(workspace, "ReloadBook", PluginElementTypes.CUSTOMBOOK);
            CustomBook original = new CustomBook(modElement);
            original.categories.clear();
            CustomBook.BookCategory category = new CustomBook.BookCategory("Images");
            CustomBook.BookPage page = new CustomBook.BookPage("Page 1", "Reload-safe page");
            page.elements.add(CustomBook.BookElement.image("restart_test_image", 128, 64));
            category.pages.add(page);
            original.categories.add(category);

            ModElementManager manager = workspace.getModElementManager();
            String savedJson = manager.generatableElementToJSON(original);
            if (!savedJson.contains("restart_test_image")) {
                throw new AssertionError("The imported image reference was not serialized");
            }

            CustomBook loaded = load(manager, savedJson, modElement);
            assertImageSurvived(loaded);

            String restartedJson = manager.generatableElementToJSON(loaded);
            CustomBook loadedAfterSecondRestart = load(manager, restartedJson, modElement);
            assertImageSurvived(loadedAfterSecondRestart);

            JsonObject damagedRoot = JsonParser.parseString(savedJson).getAsJsonObject();
            JsonArray categories = damagedRoot.getAsJsonObject("definition").getAsJsonArray("categories");
            categories.add(JsonNull.INSTANCE);
            JsonArray pages = categories.get(0).getAsJsonObject().getAsJsonArray("pages");
            pages.add(JsonNull.INSTANCE);
            pages.get(0).getAsJsonObject().getAsJsonArray("elements").add(JsonNull.INSTANCE);
            CustomBook recovered = load(manager, damagedRoot.toString(), modElement);
            recovered.getBookCategories();
            for (CustomBook.BookCategory recoveredCategory : recovered.categories) {
                if (recoveredCategory == null || recoveredCategory.pages == null) {
                    throw new AssertionError("A damaged category was not recovered");
                }
                for (CustomBook.BookPage recoveredPage : recoveredCategory.pages) {
                    if (recoveredPage == null || recoveredPage.getBookElements().contains(null)) {
                        throw new AssertionError("A damaged page or element was not recovered");
                    }
                }
            }

            System.out.println("WORKSPACE_RELOAD_WITH_IMAGE_OK");
        }
        finally {
            workspace.getModElementManager().invalidateCache();
            workspace.closeTestResources();
        }
    }

    private static CustomBook load(ModElementManager manager, String json, ModElement modElement) throws Exception {
        Field gsonField = ModElementManager.class.getDeclaredField("gson");
        gsonField.setAccessible(true);
        Gson gson = (Gson)gsonField.get(manager);
        Field stackField = ModElementManager.class.getDeclaredField("modElementsInConversion");
        stackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Stack<ModElement> stack = (Stack<ModElement>)stackField.get(manager);
        GeneratableElement loaded;
        stack.push(modElement);
        try {
            loaded = gson.fromJson(json, GeneratableElement.class);
        }
        finally {
            stack.pop();
        }
        if (!(loaded instanceof CustomBook customBook)) {
            throw new AssertionError("The custom book could not be reloaded");
        }
        return customBook;
    }

    private static void assertImageSurvived(CustomBook book) {
        CustomBook.BookElement image = book.getBookCategories().get(0).pages.get(0).getBookElements().stream()
                .filter(element -> "IMAGE".equals(element.type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The image element disappeared after reload"));
        if (!"restart_test_image".equals(image.mediaName)) {
            throw new AssertionError("The image resource name changed after reload");
        }
    }

    private static final class TestWorkspace extends Workspace {
        private final ModElementManager manager;
        private final File temporaryRoot;
        private final WorkspaceFolderManager folderManager;

        private TestWorkspace() throws Exception {
            super(new WorkspaceSettings("reload_test"));
            getWorkspaceSettings().setWorkspace(this);
            this.temporaryRoot = Files.createTempDirectory("custom-book-reload-test-").toFile();
            Constructor<WorkspaceFolderManager> constructor = WorkspaceFolderManager.class
                    .getDeclaredConstructor(File.class, Workspace.class);
            constructor.setAccessible(true);
            this.folderManager = constructor.newInstance(this.temporaryRoot, this);
            this.manager = new ModElementManager(this);
        }

        @Override
        public ModElementManager getModElementManager() {
            return this.manager;
        }

        @Override
        public WorkspaceFolderManager getFolderManager() {
            return this.folderManager;
        }

        private void closeTestResources() {
            deleteRecursively(this.temporaryRoot);
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static void initializeTranslations() throws Exception {
        ResourceBundle emptyBundle = new ListResourceBundle() {
            @Override
            protected Object[][] getContents() {
                return new Object[0][0];
            }
        };
        for (String fieldName : new String[] { "rb", "rb_en" }) {
            Field field = L10N.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, emptyBundle);
        }
    }
}
