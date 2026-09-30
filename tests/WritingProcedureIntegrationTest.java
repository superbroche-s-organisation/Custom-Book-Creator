import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.registry.PluginElementTypes;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.mcreator.blockly.data.Dependency;
import net.mcreator.element.GeneratableElement;
import net.mcreator.element.parts.procedure.LogicProcedure;
import net.mcreator.element.parts.procedure.Procedure;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.WorkspaceFolderManager;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.settings.WorkspaceSettings;

/** Exercises the actual MCreator procedure-reference resolver and its dependency order. */
public final class WritingProcedureIntegrationTest {
    public static void main(String[] args) throws Exception {
        var translations = new ListResourceBundle() {
            @Override protected Object[][] getContents() { return new Object[0][0]; }
        };
        for (String name : List.of("rb", "rb_en")) {
            var field = net.mcreator.ui.init.L10N.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, translations);
        }
        PluginElementTypes.load();
        Path directory = Files.createTempDirectory("custombook-procedure-test-");
        try {
            TestWorkspace workspace = new TestWorkspace(directory);
            List<Dependency> dependencies = List.of(
                new Dependency("world", "world"), new Dependency("z", "number"),
                new Dependency("entity", "entity"), new Dependency("x", "number"),
                new Dependency("itemstack", "itemstack"), new Dependency("y", "number"));
            workspace.references.put("CanWrite", new ProcedureElement(workspace, "CanWrite", dependencies));
            workspace.references.put("Signed", new ProcedureElement(workspace, "Signed", dependencies));
            CustomBook book = new CustomBook(new ModElement(workspace, "Book", PluginElementTypes.CUSTOMBOOK));
            book.writingPermission = new LogicProcedure("CanWrite", false);
            book.onBookSigned = new Procedure("Signed");
            book.getBookCategories().get(0).pages.get(0).elements.add(CustomBook.BookElement.input());
            Configuration config = new Configuration(Configuration.VERSION_2_3_31);
            config.setDirectoryForTemplateLoading(Path.of("compatibility/templates").toFile());
            config.setLogTemplateExceptions(false);
            BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_31);
            wrapper.setExposeFields(true);
            config.setObjectWrapper(wrapper.build());
            Map<String, Object> model = new HashMap<>();
            model.put("data", book);
            model.put("package", "com.example.audit");
            model.put("name", "Audit");
            model.put("modid", "audit");
            for (String flag : List.of("cbcComponents", "cbcOptionalNbt", "cbcModernProfile")) model.put(flag, false);
            StringWriter output = new StringWriter();
            config.getTemplate("writing_server.java.ftl").process(model, output);
            String result = output.toString();
            check(result.contains("CanWriteProcedure.execute(world, z, entity, x, itemstack, y)"), "permission dependency order");
            check(result.contains("SignedProcedure.execute(world, z, entity, x, itemstack, y)"), "signature dependency order");
            check(book.writingPermission.exists && book.onBookSigned.exists, "native procedure reference not resolved");
            book.writingPermission = new LogicProcedure(null, false);
            book.onBookSigned = null;
            output = new StringWriter();
            config.getTemplate("writing_server.java.ftl").process(model, output);
            check(output.toString().contains("return false;"), "constant read-only permission");
            check(!output.toString().contains(".procedures."), "unconfigured procedure invocation");
            book.writingPermission = new LogicProcedure("MissingPermission", false);
            expectFailure(config, model, "permission procedure no longer exists");
            book.writingPermission = new LogicProcedure(null, false);
            book.onBookSigned = new Procedure("MissingSignature");
            expectFailure(config, model, "signature procedure no longer exists");
            workspace.references.put("BadDependency", new ProcedureElement(workspace, "BadDependency", List.of(new Dependency("direction", "direction"))));
            book.onBookSigned = new Procedure("BadDependency");
            expectFailure(config, model, "unsupported signature dependency: direction");
            System.out.println("WRITING_NATIVE_PROCEDURE_INTEGRATION_OK");
        } finally {
            try (var files = Files.walk(directory)) {
                for (var file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void expectFailure(Configuration config, Map<String, Object> model, String message) throws Exception {
        try {
            config.getTemplate("writing_server.java.ftl").process(model, new StringWriter());
            throw new AssertionError("Unsafe procedure reference was accepted");
        } catch (freemarker.template.TemplateException expected) {
            check(expected.getMessage().contains(message), "Unhelpful procedure validation message");
        }
    }
    private static final class TestWorkspace extends Workspace {
        final Map<String, ModElement> references = new HashMap<>();
        final WorkspaceFolderManager folders;
        TestWorkspace(Path root) throws Exception {
            super(new WorkspaceSettings("procedure_test"));
            getWorkspaceSettings().setWorkspace(this);
            var constructor = WorkspaceFolderManager.class.getDeclaredConstructor(java.io.File.class, Workspace.class);
            constructor.setAccessible(true);
            folders = constructor.newInstance(root.toFile(), this);
        }
        @Override public WorkspaceFolderManager getFolderManager() { return folders; }
        @Override public ModElement getModElementByName(String name) { return references.get(name); }
    }
    private static final class ProcedureElement extends ModElement {
        private final net.mcreator.element.types.Procedure procedure;
        ProcedureElement(Workspace workspace, String name, List<Dependency> dependencies) {
            super(workspace, name, PluginElementTypes.CUSTOMBOOK);
            procedure = new net.mcreator.element.types.Procedure(this) {
                @Override public List<Dependency> getDependencies() { return dependencies; }
            };
        }
        @Override public GeneratableElement getGeneratableElement() { return procedure; }
    }
}
