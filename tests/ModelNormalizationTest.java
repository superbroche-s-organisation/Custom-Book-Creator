import fr.tom.mcreator.custombook.element.types.CustomBook;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModelNormalizationTest {
    public static void main(String[] args) {
        verifyLegacyMigration();
        verifyDamagedStructureRepair();
        verifySafeGeneratorValues();
        verifyElementNormalization();
        verifyLegacyTextureNormalization();
        verifyLongUnicodeContent();
        System.out.println("MODEL_NORMALIZATION_OK");
    }

    private static void verifyLegacyMigration() {
        CustomBook book = new CustomBook(null);
        book.pages = new ArrayList<>(Arrays.asList("Legacy first page", null));

        List<CustomBook.BookCategory> categories = book.getBookCategories();
        require(categories.size() == 1, "legacy pages must migrate into one category");
        require(categories.get(0).pages.size() == 2,
                "constructor defaults must not hide serialized legacy pages");
        require("Legacy first page".equals(categories.get(0).pages.get(0).getBookElements().get(0).content),
                "legacy page content was not preserved");
        require("".equals(categories.get(0).pages.get(1).getBookElements().get(0).content),
                "null legacy page content was not repaired");
    }

    private static void verifyDamagedStructureRepair() {
        CustomBook book = new CustomBook(null);
        book.pages.clear();
        book.categories = new ArrayList<>();

        CustomBook.BookCategory first = new CustomBook.BookCategory("A".repeat(300));
        first.id = "duplicate-category";
        CustomBook.BookPage firstPage = new CustomBook.BookPage("P".repeat(300), "");
        firstPage.id = "duplicate-page";
        first.pages.add(firstPage);

        CustomBook.BookCategory second = new CustomBook.BookCategory("Second");
        second.id = "duplicate-category";
        CustomBook.BookPage secondPage = new CustomBook.BookPage("Second page", "");
        secondPage.id = "duplicate-page";
        second.pages.add(null);
        second.pages.add(secondPage);

        book.categories.add(first);
        book.categories.add(null);
        book.categories.add(second);
        List<CustomBook.BookCategory> categories = book.getBookCategories();

        Set<String> categoryIds = new HashSet<>();
        Set<String> pageIds = new HashSet<>();
        for (CustomBook.BookCategory category : categories) {
            require(category != null, "null category survived normalization");
            require(category.name != null && !category.name.isBlank() && category.name.length() <= 256,
                    "category name was not repaired or bounded");
            require(categoryIds.add(category.id), "duplicate category identifier survived normalization");
            require(category.pages != null && !category.pages.isEmpty(), "category has no repaired page");
            for (CustomBook.BookPage page : category.pages) {
                require(page != null, "null page survived normalization");
                require(page.title != null && !page.title.isBlank() && page.title.length() <= 256,
                        "page title was not repaired or bounded");
                require(pageIds.add(page.id), "duplicate page identifier survived normalization");
                require(page.getBookElements().stream().noneMatch(element -> element == null),
                        "null element survived normalization");
            }
        }
    }

    private static void verifySafeGeneratorValues() {
        CustomBook book = new CustomBook(null);
        book.bookTitle = "T".repeat(80);
        book.author = "A".repeat(400);
        book.generation = 99;
        book.rarity = "BROKEN";
        book.stackSize = -10;
        book.enchantability = Integer.MAX_VALUE;

        require(book.getSafeBookTitle().length() == 32, "book title is not generator-safe");
        require(book.getSafeAuthor().length() == 256, "author is not generator-safe");
        require(book.getSafeGeneration() == 3, "generation is not clamped");
        require("COMMON".equals(book.getSafeRarity()), "invalid rarity is not repaired");
        require(book.getSafeStackSize() == 1, "stack size is not clamped");
        require(book.getSafeEnchantability() == 128000, "enchantability is not clamped");
        book.customModelName = "models/book.json:JSON";
        require("models/book".equals(book.getSafeCustomModelName()), "valid custom model name was not normalized");
        book.customModelName = "../outside.obj:OBJ";
        require("normal".equals(book.getSafeCustomModelName()), "unsafe custom model name was not rejected");
        book.renderType = 1;
        require(book.hasNormalModel() && !book.hasCustomJSONModel(), "unsafe JSON model did not fall back to normal");
        book.customModelName = null;
        require("normal".equals(book.getSafeCustomModelName()), "null custom model name was not repaired");
        book.creativeTabs = null;
        require(book.getCreativeTabs().isEmpty(), "null creative tabs were not repaired");
    }

    private static void verifyElementNormalization() {
        CustomBook.BookElement element = new CustomBook.BookElement();
        element.id = "";
        element.type = "UNKNOWN";
        element.x = Integer.MAX_VALUE;
        element.y = Integer.MIN_VALUE;
        element.width = 999;
        element.height = -1;
        element.content = "x".repeat(40000);
        element.align = "DIAGONAL";
        element.mediaName = "../outside";
        element.frames = new ArrayList<>(Arrays.asList("valid_frame", "demo:folder/frame", "../bad", null));
        element.frameDelay = -50;
        element.label = "L".repeat(2000);
        element.buttonStyle = "BROKEN";
        element.buttonBackgroundColor = "red";
        element.buttonBorderColor = null;
        element.buttonTextColor = "#12345G";
        element.buttonImageName = "/absolute/path";
        element.buttonImageType = "ENTITY";
        element.buttonImageMode = "STRETCH";
        element.normalize();

        require(element.id != null && !element.id.isBlank(), "element identifier was not repaired");
        require("TEXT".equals(element.type), "unknown element type was not repaired");
        require(element.width == 256 && element.height == 1 && element.x == 0 && element.y == 0,
                "element geometry was not clamped to the page");
        require(element.content.length() == Short.MAX_VALUE, "text was not bounded");
        require(element.label.length() == 1024, "button label was not bounded");
        require("LEFT".equals(element.align), "text alignment was not repaired");
        require(element.mediaName.isEmpty() && element.buttonImageName.isEmpty(),
                "unsafe resource path survived normalization");
        require(element.frames.equals(List.of("valid_frame", "demo:folder/frame")),
                "invalid GIF frames were not removed");
        require(element.frameDelay == 20, "GIF delay was not clamped");
        require("CLASSIC".equals(element.buttonStyle), "button style was not repaired");
        require("SCREEN".equals(element.buttonImageType), "texture type was not repaired");
        require("NONE".equals(element.buttonImageMode), "image placement was not repaired");
        require("#8A6846".equals(element.buttonBackgroundColor)
                && "#6D5237".equals(element.buttonBorderColor)
                && "#FFEDC5".equals(element.buttonTextColor), "button colors were not repaired");
    }

    private static void verifyLegacyTextureNormalization() {
        CustomBook book = new CustomBook(null);
        book.itemTexture = "";
        book.texturePath = "demo:textures/item/fancy_book.png";
        require("demo:fancy_book".equals(book.getEffectiveItemTexture()),
                "legacy namespaced item texture was not normalized");

        book.itemTexture = "minecraft:item/clock";
        require("minecraft:clock".equals(book.getEffectiveItemTexture()),
                "selected texture path was not normalized");

        book.itemTexture = "../outside";
        book.texturePath = "minecraft:item/written_book";
        require(book.getEffectiveItemTexture().isEmpty(), "unsafe texture path did not fall back to the vanilla book");
    }

    private static void verifyLongUnicodeContent() {
        String value = "漢".repeat(15999) + "📖" + "字".repeat(16000);
        CustomBook.BookElement element = CustomBook.BookElement.text(value);
        element.normalize();
        require(value.equals(element.content), "existing long Unicode content was truncated");
        List<String> chunks = element.getContentChunks();
        require(chunks.size() == 3 && value.equals(String.join("", chunks)), "Java string chunks lost content");
        for (String chunk : chunks) {
            require(chunk.length() <= 16000, "Java string chunk can exceed the constant-pool limit");
            require(!Character.isHighSurrogate(chunk.charAt(chunk.length() - 1)), "chunk split a surrogate pair");
        }
        CustomBook book = new CustomBook(null);
        book.bookTitle = "x".repeat(31) + "📖";
        require(book.getSafeBookTitle().length() == 31, "title truncation split a surrogate pair");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
