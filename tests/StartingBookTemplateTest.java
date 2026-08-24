import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StartingBookTemplateTest {
    public static void main(String[] args) throws Exception {
        for (String generator : new String[] { "neoforge-1.21.1", "neoforge-26.1.2" }) {
            Path templateDirectory = Path.of("src", "main", "resources", generator, "templates", "custombook");

            Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
            configuration.setDirectoryForTemplateLoading(templateDirectory.toFile());
            configuration.setDefaultEncoding("UTF-8");
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            Map<String, Object> model = new HashMap<>();
            model.put("package", "com.example.guide");
            model.put("JavaModName", "GuideMod");
            model.put("modid", "guide_mod");
            model.put("name", "GuideBook");
            model.put("registryname", "guide_book");
            model.put("data", new Data(new ModElement("GUIDE_BOOK")));

            Template template = configuration.getTemplate("starting_book_events.java.ftl");
            StringWriter output = new StringWriter();
            template.process(model, output);
            String generated = output.toString();

            require(generated.contains("class GuideBookStartingBookEvents"), generator + ": wrong class name");
            require(generated.contains("GuideModItems.GUIDE_BOOK.get()"), generator + ": wrong item reference");
            require(generated.contains("guide_mod:received_starting_book_guide_book"), generator + ": wrong persistence tag");
            require(generated.contains("PlayerLoggedInEvent"), generator + ": missing login handler");
            require(generated.contains("PlayerEvent.Clone"), generator + ": missing clone handler");
            require(!generated.contains("${"), generator + ": unresolved FreeMarker expression");

            System.out.println("STARTING_BOOK_TEMPLATE_OK " + generator);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public record Data(ModElement modElement) {
        public ModElement getModElement() {
            return modElement;
        }
    }

    public record ModElement(String registryNameUpper) {
        public String getRegistryNameUpper() {
            return registryNameUpper;
        }
    }
}
