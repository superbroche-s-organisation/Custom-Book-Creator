import com.google.gson.JsonParser;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TemplateRenderTest {
    public static void main(String[] args) throws Exception {
        Path outputRoot = args.length == 0 ? null : Path.of(args[0]);
        for (String generator : List.of("neoforge-1.21.1", "neoforge-26.1.2")) {
            verifyGenerator(generator, outputRoot);
        }
        System.out.println("TEMPLATE_RENDER_OK");
    }

    private static void verifyGenerator(String generator, Path outputRoot) throws Exception {
        Path templateRoot = Path.of("src", "main", "resources", generator, "templates", "custombook");
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setDirectoryForTemplateLoading(templateRoot.toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_34);
        wrapper.setExposeFields(true);
        configuration.setObjectWrapper(wrapper.build());

        CustomBook book = representativeBook();
        Map<String, Object> model = baseModel();
        model.put("data", book);

        String item = render(configuration, "item.java.ftl", model);
        String screen = render(configuration, "screen.java.ftl", model);
        String clientEvents = render(configuration, "client_events.java.ftl", model);
        requireResolved(generator, item);
        requireResolved(generator, screen);
        requireResolved(generator, clientEvents);
        require(item.contains("Rarity.EPIC") && item.contains("isFoil") && item.contains("isPiglinCurrency")
                        && item.contains("isCorrectToolForDrops"),
                generator + ": optional item features did not render");
        require(screen.contains("case \"TEXT\"") && screen.contains("case \"IMAGE\"")
                        && screen.contains("case \"GIF\"") && screen.contains("case \"BUTTON\"")
                        && screen.contains("case \"NAV_NEXT\""),
                generator + ": not every visual element type rendered");
        require(screen.contains("String.join(\"\", new String[] {"),
                generator + ": long text is not split into safe Java constants");

        Map<String, Object> startingModel = baseModel();
        startingModel.put("data", new StartingData(new StartingModElement("AUDIT_BOOK")));
        String startingEvents = render(configuration, "starting_book_events.java.ftl", startingModel);
        requireResolved(generator, startingEvents);
        require(startingEvents.contains("AuditModItems.AUDIT_BOOK.get()"),
                generator + ": starting-book item reference is wrong");

        String normalModel = render(configuration, "item_model.json.ftl", model);
        JsonParser.parseString(normalModel);
        book.customModelName = "audit_model:JSON";
        String customJsonModel = render(configuration, "item_model_custom_json.json.ftl", model);
        String customObjModel = render(configuration, "item_model_custom_obj.json.ftl", model);
        JsonParser.parseString(customJsonModel);
        JsonParser.parseString(customObjModel);
        require(customJsonModel.contains("\"particle\": \"minecraft:item/clock\""),
                generator + ": custom JSON model has no safe particle fallback");
        require(customObjModel.contains("\"particle\": \"minecraft:item/clock\""),
                generator + ": custom OBJ model has no safe particle fallback");
        book.customModelName = null;
        book.renderType = 1;
        require(book.hasNormalModel() && !book.hasCustomJSONModel(),
                generator + ": a missing custom-model name must fall back to the normal item model");
        JsonParser.parseString(render(configuration, "item_model_custom_json.json.ftl", model));
        JsonParser.parseString(render(configuration, "item_model_custom_obj.json.ftl", model));
        if ("neoforge-26.1.2".equals(generator)) {
            JsonParser.parseString(render(configuration, "client_item.json.ftl", model));
        }

        if (outputRoot != null) {
            Path packageRoot = outputRoot.resolve(generator).resolve(Path.of("com", "example", "audit"));
            write(packageRoot.resolve(Path.of("item", "AuditItem.java")), item);
            write(packageRoot.resolve(Path.of("client", "gui", "AuditBookScreen.java")), screen);
            write(packageRoot.resolve(Path.of("client", "AuditBookClientEvents.java")), clientEvents);
            write(packageRoot.resolve(Path.of("event", "AuditStartingBookEvents.java")), startingEvents);
        }
        System.out.println("TEMPLATE_RENDER_OK " + generator);
    }

    private static CustomBook representativeBook() {
        CustomBook book = new CustomBook(null);
        book.bookTitle = "Audit \\\"Book\\\"";
        book.author = "Automated test";
        book.generation = 2;
        book.rarity = "EPIC";
        book.stackSize = 7;
        book.enchantability = 12;
        book.immuneToFire = true;
        book.isPiglinCurrency = true;
        book.destroyAnyBlock = true;
        book.startingBook = true;
        book.glow = true;
        book.hideNextArrowAtCategoryEnd = true;
        book.itemTexture = "minecraft:item/clock";
        book.categories.clear();

        CustomBook.BookCategory first = new CustomBook.BookCategory("Welcome & basics");
        CustomBook.BookPage firstPage = new CustomBook.BookPage("First \\\"page\\\"", "");
        firstPage.elements.clear();
        firstPage.elements.add(CustomBook.BookElement.text(
                "[b]Bold[/b] [i]italic[/i] [u]underlined[/u] [s]struck[/s] é漢字 📖\\n[url=https://example.com]Link[/url]"));
        // More than 65,535 modified-UTF-8 bytes: compiling this fixture catches
        // constant-pool regressions that an ASCII-only sample cannot detect.
        firstPage.elements.add(CustomBook.BookElement.text("漢".repeat(Short.MAX_VALUE)));
        firstPage.elements.add(CustomBook.BookElement.image("audit_image", 96, 64));
        firstPage.elements.add(CustomBook.BookElement.gif(List.of("audit_gif_0", "audit_gif_1"), 80, 64, 64));
        CustomBook.BookElement previous = navigation(false);
        CustomBook.BookElement next = navigation(true);
        firstPage.elements.add(previous);
        firstPage.elements.add(next);
        first.pages.add(firstPage);

        CustomBook.BookCategory second = new CustomBook.BookCategory("Advanced");
        CustomBook.BookPage secondPage = new CustomBook.BookPage("Destination", "Target page");
        CustomBook.BookElement button = CustomBook.BookElement.button("Open destination", second.id);
        button.targetPageId = secondPage.id;
        button.buttonStyle = "OUTLINE";
        button.buttonImageName = "minecraft:arrow";
        button.buttonImageType = "ITEM";
        button.buttonImageMode = "ICON_LEFT";
        firstPage.elements.add(button);
        secondPage.elements.add(navigation(false));
        secondPage.elements.add(navigation(true));
        second.pages.add(secondPage);

        book.categories.add(first);
        book.categories.add(second);
        book.pages = new ArrayList<>();
        book.getBookCategories();
        return book;
    }

    private static CustomBook.BookElement navigation(boolean next) {
        CustomBook.BookElement element = new CustomBook.BookElement();
        element.type = next ? "NAV_NEXT" : "NAV_PREV";
        element.x = next ? 226 : 12;
        element.y = 278;
        element.width = 18;
        element.height = 18;
        element.buttonStyle = next ? "IMAGE" : "TRANSPARENT";
        element.buttonImageName = next ? "minecraft:arrow" : "";
        element.buttonImageType = "ITEM";
        return element;
    }

    private static Map<String, Object> baseModel() {
        HashMap<String, Object> model = new HashMap<>();
        model.put("package", "com.example.audit");
        model.put("JavaModName", "AuditMod");
        model.put("modid", "audit_mod");
        model.put("name", "Audit");
        model.put("registryname", "audit_book");
        return model;
    }

    private static String render(Configuration configuration, String templateName, Map<String, Object> model)
            throws Exception {
        Template template = configuration.getTemplate(templateName);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void requireResolved(String generator, String generated) {
        require(!generated.contains("${") && !generated.contains("<#"),
                generator + ": generated source contains an unresolved FreeMarker expression");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public record StartingData(StartingModElement modElement) {
        public StartingModElement getModElement() {
            return modElement;
        }
    }

    public record StartingModElement(String registryNameUpper) {
        public String getRegistryNameUpper() {
            return registryNameUpper;
        }
    }
}
