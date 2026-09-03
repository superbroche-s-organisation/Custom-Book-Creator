import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.tools.ToolProvider;

/** Executes the actual dependency-free screen logic extracted from both generator templates. */
public final class ScreenRuntimeLogicTest {
    public static void main(String[] args) throws Exception {
        for (String generator : new String[] { "neoforge-1.21.1", "neoforge-26.1.2" }) {
            Path template = Path.of("src", "main", "resources", generator, "templates", "custombook", "screen.java.ftl");
            String source = Files.readString(template, StandardCharsets.UTF_8);
            verify(source, generator);
            System.out.println("SCREEN_RUNTIME_LOGIC_OK " + generator);
        }
    }

    private static void verify(String source, String generator) throws Exception {
        String pattern = source.lines().filter(line -> line.contains("private static final Pattern TAG_PATTERN"))
                .findFirst().orElseThrow();
        String graphicsType = generator.equals("neoforge-1.21.1") ? "GuiGraphics" : "GuiGraphicsExtractor";
        String categories = between(source, "    private void renderCategories(", "    private void renderElement(")
                .replace(graphicsType, "Graphics");
        String texture = between(source, "    private static String textureResource(", "    private void renderResourceImage(")
                .replace("${modid}", "audit_mod");
        String navigationCheck = between(source, "    private boolean canAdvance(", "    private void renderRichTextBlock(");
        String glyphs = between(source, "    private List<Glyph> buildGlyphs(", "    private Component styledComponent(");
        String parser = between(source, "    private static List<RichToken> parse(", "    private Page currentPage(");
        String navigation = between(source, "    private Page currentPage(", "    /**\n     * External links");
        String records = between(source, "    private record Category(", "    private static final class Line");
        String state = source.substring(source.indexOf("    private static final class State"), source.lastIndexOf('}'));

        String fixture = """
                import java.util.*;
                import java.util.regex.*;
                public class ScreenLogicFixture {
                    private static final int DEFAULT_SIZE = 12;
                    private static final int CATEGORY_ROWS = 11;
                    private static Category[] CATEGORIES;
                    private static boolean HIDE_NEXT_ARROW_AT_CATEGORY_END;
                    private int categoryIndex, pageIndex, categoryListPage;
                    private float uiScale = 1f;
                    private final List<ClickRegion> clickRegions = new ArrayList<>();
                    private static final class Graphics {}
                    private record Component(String text) {
                        static Component literal(String text) { return new Component(text); }
                        static Component empty() { return literal(""); }
                    }
                    private void fillD(Graphics graphics, int a, int b, int c, int d, int color) {}
                    private void drawTextD(Graphics graphics, Component text, int x, int y, int color) {}
                    private void drawCenteredD(Graphics graphics, Component text, int x, int y, int color) {}
                    private void addClickRegion(int x, int y, int w, int h, String type, String target) {
                        clickRegions.add(new ClickRegion(x, y, w, h, type, target));
                    }
                    private Component styledComponent(String raw, State state) { return Component.literal(raw); }
                    private int glyphScreenAdvance(Component text, float scale) { return 6; }
                    private int glyphScreenHeight(float scale) { return 9; }
                """ + pattern + categories + texture + navigationCheck + glyphs + parser + navigation + records + state + """
                    private static void check(boolean condition, String message) {
                        if (!condition) throw new AssertionError(message);
                    }
                    private static void textureIs(String name, String type, String expected) {
                        check(Objects.equals(textureResource(name, type), expected), "Texture: " + name + " / " + type);
                    }
                    private static Category category(String id, String... pages) {
                        return new Category(id, id, Arrays.stream(pages)
                            .map(page -> new Page(page, page, true, new VisualElement[0])).toArray(Page[]::new));
                    }
                    private boolean clickable(String type, String target) {
                        return clickRegions.stream().anyMatch(region -> region.type.equals(type) && region.target.equals(target));
                    }
                    public static void run() {
                        textureIs("image", "SCREEN", "audit_mod:textures/screens/image.png");
                        textureIs("image.png", "GUI", "audit_mod:textures/screens/image.png");
                        textureIs("textures/screens/image.png", "SCREEN", "audit_mod:textures/screens/image.png");
                        textureIs("screens/image.png", "SCREEN", "audit_mod:textures/screens/image.png");
                        textureIs("minecraft:apple", "ITEM", "minecraft:textures/item/apple.png");
                        textureIs("minecraft:item/apple.png", "SCREEN", "minecraft:textures/item/apple.png");
                        textureIs("minecraft:textures/block/stone.png", "BLOCK", "minecraft:textures/block/stone.png");
                        textureIs(null, "SCREEN", null);
                        textureIs("", "SCREEN", null);
                        textureIs("minecraft:", "ITEM", null);
                        textureIs(":image", "SCREEN", null);
                        textureIs("bad:name:again", "SCREEN", null);
                        textureIs("../image", "SCREEN", null);
                        textureIs("Bad Image", "SCREEN", null);
                        textureIs("/absolute/image", "SCREEN", null);

                        List<RichToken> tokens = parse("[b]A[/i]B[/b]C");
                        check(tokens.size() == 3, "Unmatched closing tag changed token count");
                        check(tokens.get(0).state.bold && tokens.get(1).state.bold && !tokens.get(2).state.bold,
                            "Unmatched closing tag must not close bold");
                        tokens = parse("[b][i]A[/b]B[/i]C");
                        check(tokens.get(0).state.bold && tokens.get(0).state.italic, "Nested opening tags lost");
                        check(!tokens.get(1).state.bold && !tokens.get(1).state.italic && !tokens.get(2).state.italic,
                            "Matching outer close must close its nested styles");
                        tokens = parse("[url=https://example.com][color=#123456]A[/color]B[/url]C");
                        check(tokens.get(0).state.color == 0x123456 && tokens.get(1).state.color == null,
                            "Color restoration failed");
                        check("url".equals(tokens.get(1).state.actionType) && tokens.get(2).state.actionType == null,
                            "Link scope restoration failed");

                        ScreenLogicFixture screen = new ScreenLogicFixture();
                        String rocket = new String(Character.toChars(0x1F680));
                        List<Glyph> unicode = screen.buildGlyphs("A" + rocket + "B" + (char) 13 + (char) 10 + "C");
                        check(unicode.size() == 5, "Supplementary character split into surrogate glyphs");
                        check(unicode.get(1).raw.equals(rocket) && unicode.get(1).raw.length() == 2,
                            "Unicode code point did not remain intact");
                        check(unicode.get(3).newline, "CRLF handling changed");

                        CATEGORIES = new Category[25];
                        for (int i = 0; i < CATEGORIES.length; i++) CATEGORIES[i] = category("c" + i, "p" + i);
                        screen.renderCategories(new Graphics(), 13, 47, 114);
                        check(screen.clickable("category", "c10") && !screen.clickable("category", "c11"), "First category page bounds");
                        check(screen.clickable("category_list", "next"), "Missing next category-list control");
                        screen.changeCategoryListPage(1);
                        screen.clickRegions.clear();
                        screen.renderCategories(new Graphics(), 13, 47, 114);
                        check(screen.clickable("category", "c11") && screen.clickable("category", "c21"), "Middle categories inaccessible");
                        screen.changeCategoryListPage(1);
                        screen.clickRegions.clear();
                        screen.renderCategories(new Graphics(), 13, 47, 114);
                        check(screen.clickable("category", "c24") && !screen.clickable("category_list", "next"), "Last categories inaccessible");
                        screen.selectCategoryById("c0");
                        check(screen.categoryListPage == 0, "Selected category not revealed");
                        screen.selectPageById("p24");
                        check(screen.categoryListPage == 2 && screen.categoryIndex == 24, "Linked category not revealed");

                        CATEGORIES = new Category[] { category("a", "a1", "a2"), category("empty"), category("b", "b1") };
                        screen.categoryIndex = 0;
                        screen.pageIndex = 1;
                        HIDE_NEXT_ARROW_AT_CATEGORY_END = false;
                        screen.advancePage(1);
                        check(screen.categoryIndex == 2 && screen.pageIndex == 0, "Cross-category next navigation failed");
                        screen.advancePage(-1);
                        check(screen.categoryIndex == 0 && screen.pageIndex == 1, "Cross-category previous navigation failed");
                        HIDE_NEXT_ARROW_AT_CATEGORY_END = true;
                        check(!screen.canAdvance(1), "Category boundary restriction ignored");
                        screen.advancePage(1);
                        check(screen.categoryIndex == 0 && screen.pageIndex == 1, "Hidden arrow action still navigates");
                        CATEGORIES = new Category[0];
                        check(!screen.canAdvance(1) && screen.currentPage() == null, "Empty book navigation failed");
                    }
                }
                """;

        Path temporary = Files.createTempDirectory("custom-book-screen-logic-");
        try {
            Path javaFile = temporary.resolve("ScreenLogicFixture.java");
            Files.writeString(javaFile, fixture, StandardCharsets.UTF_8);
            int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "--release", "21", "-encoding", "UTF-8", "-d", temporary.toString(), javaFile.toString());
            if (result != 0) throw new AssertionError(generator + ": extracted screen logic does not compile");
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { temporary.toUri().toURL() })) {
                loader.loadClass("ScreenLogicFixture").getMethod("run").invoke(null);
            }
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        if (from < 0 || to < 0) throw new AssertionError("Missing screen logic section: " + start);
        return source.substring(from, to);
    }
}
