package fr.superbroche.mcreator.custombook.element.types;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.mcreator.element.GeneratableElement;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.parts.TextureHolder;
import net.mcreator.element.types.interfaces.IItem;
import net.mcreator.element.types.interfaces.IItemWithModel;
import net.mcreator.element.types.interfaces.IItemWithTexture;
import net.mcreator.element.types.interfaces.ITabContainedElement;
import net.mcreator.minecraft.MCItem;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.references.TextureReference;
import net.mcreator.workspace.resources.Model;
import net.mcreator.workspace.resources.TexturedModel;

public class CustomBook
extends GeneratableElement
implements IItem,
ITabContainedElement,
IItemWithModel,
IItemWithTexture {
    private static final int MAX_STRUCTURAL_LABEL_LENGTH = 256;
    private static final int MAX_BUTTON_LABEL_LENGTH = 1024;
    private static final int MAX_TEXT_LENGTH = Short.MAX_VALUE;
    private static final int MAX_JAVA_STRING_CHUNK_LENGTH = 16000;
    public String name = "Custom Book";
    public String bookTitle = "Custom Book";
    public String author = "Unknown";
    public int generation = 0;
    public String rarity = "COMMON";
    public boolean glow = false;
    public int stackSize = 1;
    public int enchantability = 0;
    public boolean immuneToFire = false;
    public boolean isPiglinCurrency = false;
    public boolean destroyAnyBlock = false;
    public boolean startingBook = false;
    public boolean hideNextArrowAtCategoryEnd = false;
    public int renderType = 0;
    public String customModelName = "Normal";
    @TextureReference(value=TextureType.ITEM)
    public TextureHolder texture = null;
    public String itemTexture = "";
    public String texturePath = "minecraft:item/written_book";
    public List<BookCategory> categories = new ArrayList<BookCategory>();
    public List<String> pages;
    public String creativeTab;
    public List<TabEntry> creativeTabs;

    private CustomBook() {
        this(null);
    }

    public CustomBook(ModElement modElement) {
        super(modElement);
        BookCategory bookCategory = new BookCategory("General");
        bookCategory.pages.add(new BookPage("Page 1", ""));
        this.categories.add(bookCategory);
        this.pages = new ArrayList<String>();
        this.creativeTab = "TOOLS";
        this.creativeTabs = new ArrayList<TabEntry>();
    }

    public List<BookCategory> getBookCategories() {
        if (this.categories == null) {
            this.categories = new ArrayList<BookCategory>();
        }
        if (this.categories.isEmpty() || this.shouldMigrateLegacyPages()) {
            this.categories.clear();
            BookCategory bookCategory = new BookCategory("General");
            if (this.pages != null && !this.pages.isEmpty()) {
                int n = 1;
                for (String string : this.pages) {
                    bookCategory.pages.add(new BookPage("Page " + n++, string == null ? "" : string));
                }
            }
            if (bookCategory.pages.isEmpty()) {
                bookCategory.pages.add(new BookPage("Page 1", ""));
            }
            this.categories.add(bookCategory);
        }
        CustomBook.normalizeStructure(this.categories);
        return this.categories;
    }

    private boolean shouldMigrateLegacyPages() {
        if (this.pages == null || this.pages.isEmpty() || this.categories.size() != 1) {
            return false;
        }
        BookCategory category = this.categories.get(0);
        if (category == null || category.pages == null || category.pages.size() != 1) {
            return false;
        }
        BookPage page = category.pages.get(0);
        if (page == null) {
            return true;
        }
        boolean defaultTitle = page.title == null || page.title.isBlank() || "Page 1".equals(page.title);
        boolean emptyLegacyContent = page.content == null || page.content.isBlank();
        boolean emptyElements = page.elements == null || page.elements.isEmpty()
                || page.elements.stream().allMatch(element -> element == null
                        || "TEXT".equals(element.type) && (element.content == null || element.content.isBlank()));
        return defaultTitle && emptyLegacyContent && emptyElements;
    }

    private static void normalizeStructure(List<BookCategory> list) {
        Set<String> categoryIds = new HashSet<String>();
        Set<String> pageIds = new HashSet<String>();
        for (int categoryIndex = 0; categoryIndex < list.size(); ++categoryIndex) {
            BookCategory bookCategory = list.get(categoryIndex);
            if (bookCategory == null) {
                bookCategory = new BookCategory("Category");
                list.set(categoryIndex, bookCategory);
            }
            if (bookCategory.id == null || bookCategory.id.isBlank() || !categoryIds.add(bookCategory.id)) {
                bookCategory.id = UUID.randomUUID().toString();
                categoryIds.add(bookCategory.id);
            }
            if (bookCategory.name == null || bookCategory.name.isBlank()) {
                bookCategory.name = "Category";
            }
            bookCategory.name = limit(bookCategory.name, MAX_STRUCTURAL_LABEL_LENGTH);
            if (bookCategory.pages == null) {
                bookCategory.pages = new ArrayList<BookPage>();
            }
            if (bookCategory.pages.isEmpty()) {
                bookCategory.pages.add(new BookPage("Page 1", ""));
            }
            for (int pageIndex = 0; pageIndex < bookCategory.pages.size(); ++pageIndex) {
                BookPage bookPage = bookCategory.pages.get(pageIndex);
                if (bookPage == null) {
                    bookPage = new BookPage("Page", "");
                    bookCategory.pages.set(pageIndex, bookPage);
                }
                if (bookPage.id == null || bookPage.id.isBlank() || !pageIds.add(bookPage.id)) {
                    bookPage.id = UUID.randomUUID().toString();
                    pageIds.add(bookPage.id);
                }
                if (bookPage.title == null || bookPage.title.isBlank()) {
                    bookPage.title = "Page";
                }
                bookPage.title = limit(bookPage.title, MAX_STRUCTURAL_LABEL_LENGTH);
                if (bookPage.content == null) {
                    bookPage.content = "";
                }
                bookPage.content = limit(bookPage.content, MAX_TEXT_LENGTH);
                bookPage.normalizeElements();
            }
        }
    }

    public String getEffectiveItemTexture() {
        if (this.itemTexture != null && !this.itemTexture.isBlank()) {
            String normalizedTexture = normalizeItemTextureReference(this.itemTexture);
            if (!normalizedTexture.isBlank()) {
                return normalizedTexture;
            }
        }
        if (this.texturePath != null && !this.texturePath.isBlank()) {
            String normalizedTexture = normalizeItemTextureReference(this.texturePath);
            if (!"minecraft:written_book".equals(normalizedTexture)) {
                return normalizedTexture;
            }
        }
        return "";
    }

    private static String normalizeItemTextureReference(String value) {
        String path = value == null ? "" : value.trim().replace('\\', '/');
        int namespaceSeparator = path.indexOf(':');
        if (namespaceSeparator != path.lastIndexOf(':')) {
            return "";
        }
        String namespace = namespaceSeparator > 0 ? path.substring(0, namespaceSeparator) : "";
        String resourcePath = namespaceSeparator > 0 ? path.substring(namespaceSeparator + 1) : path;
        if (resourcePath.startsWith("textures/")) {
            resourcePath = resourcePath.substring("textures/".length());
        }
        if (resourcePath.startsWith("item/")) {
            resourcePath = resourcePath.substring("item/".length());
        }
        if (resourcePath.endsWith(".png")) {
            resourcePath = resourcePath.substring(0, resourcePath.length() - 4);
        }
        if ((!namespace.isBlank() && !namespace.matches("[a-z0-9_.-]+"))
                || !resourcePath.matches("[a-z0-9_./-]+") || resourcePath.contains("..")
                || resourcePath.startsWith("/") || resourcePath.endsWith("/")) {
            return "";
        }
        return namespace.isBlank() ? resourcePath : namespace + ":" + resourcePath;
    }

    public String getSafeBookTitle() {
        return this.bookTitle == null || this.bookTitle.isBlank() ? "Custom Book" : limit(this.bookTitle, 32);
    }

    public String getSafeAuthor() {
        return this.author == null || this.author.isBlank() ? "Unknown" : limit(this.author, 256);
    }

    public int getSafeGeneration() {
        return Math.max(0, Math.min(3, this.generation));
    }

    public String getSafeRarity() {
        return switch (this.rarity == null ? "COMMON" : this.rarity) {
            case "UNCOMMON", "RARE", "EPIC" -> this.rarity;
            default -> "COMMON";
        };
    }

    public int getSafeStackSize() {
        return Math.max(1, Math.min(64, this.stackSize));
    }

    public int getSafeEnchantability() {
        return Math.max(0, Math.min(128000, this.enchantability));
    }

    public String getSafeCustomModelName() {
        if (this.customModelName == null || this.customModelName.isBlank()) {
            return "normal";
        }
        String modelName = this.customModelName.trim().replace('\\', '/');
        int separator = modelName.indexOf(':');
        String path = separator >= 0 ? modelName.substring(0, separator) : modelName;
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        } else if (path.endsWith(".obj")) {
            path = path.substring(0, path.length() - 4);
        }
        return path.matches("[a-zA-Z0-9_./-]+") && !path.contains("..")
                && !path.startsWith("/") && !path.endsWith("/") ? path : "normal";
    }

    private static String limit(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        int end = maximumLength;
        if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))
                && Character.isLowSurrogate(value.charAt(end))) {
            --end;
        }
        return value.substring(0, end);
    }

    public TextureHolder getTexture() {
        if (this.texture != null && !this.texture.isEmpty()) {
            return this.texture;
        }
        if (this.getModElement() == null || this.getModElement().getWorkspace() == null) {
            return this.texture;
        }
        String string = this.getEffectiveItemTexture();
        return new TextureHolder(this.getModElement().getWorkspace(), string.isBlank() ? "minecraft:written_book" : string);
    }

    public Model getItemModel() {
        if (this.getModElement() == null || this.getModElement().getWorkspace() == null) {
            return null;
        }
        try {
            Model.Type modelType = CustomBook.decodeModelType(this.renderType);
            boolean useNormalModel = modelType == Model.Type.BUILTIN || !this.hasValidCustomModelName();
            return Model.getModelByParams(this.getModElement().getWorkspace(),
                    useNormalModel ? "Normal" : this.customModelName,
                    useNormalModel ? Model.Type.BUILTIN : modelType);
        }
        catch (RuntimeException runtimeException) {
            try {
                return Model.getModelByParams((Workspace)this.getModElement().getWorkspace(), (String)"Normal", (Model.Type)Model.Type.BUILTIN);
            }
            catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    public Map<String, TextureHolder> getTextureMap() {
        TexturedModel texturedModel;
        Model model = this.getItemModel();
        if (model instanceof TexturedModel && (texturedModel = (TexturedModel)model).getTextureMapping() != null) {
            Map<String, TextureHolder> textureMap = texturedModel.getTextureMapping().getTextureMap();
            if (textureMap != null && !textureMap.isEmpty()) {
                HashMap<String, TextureHolder> safeTextureMap = new HashMap<String, TextureHolder>();
                for (Map.Entry<String, TextureHolder> entry : textureMap.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                        safeTextureMap.put(entry.getKey(), entry.getValue());
                    }
                }
                return safeTextureMap;
            }
        }
        return new HashMap<String, TextureHolder>();
    }

    public boolean hasNormalModel() {
        return CustomBook.decodeModelType(this.renderType) == Model.Type.BUILTIN || !this.hasValidCustomModelName();
    }

    public boolean hasCustomJSONModel() {
        return CustomBook.decodeModelType(this.renderType) == Model.Type.JSON && this.hasValidCustomModelName();
    }

    public boolean hasCustomOBJModel() {
        return CustomBook.decodeModelType(this.renderType) == Model.Type.OBJ && this.hasValidCustomModelName();
    }

    private boolean hasValidCustomModelName() {
        return this.customModelName != null && !this.customModelName.isBlank()
                && !"normal".equalsIgnoreCase(this.getSafeCustomModelName());
    }

    public boolean hasCustomJAVAModel() {
        return false;
    }

    public static int encodeModelType(Model.Type type) {
        return switch (type) {
            case Model.Type.JSON -> 1;
            case Model.Type.OBJ -> 2;
            default -> 0;
        };
    }

    public static Model.Type decodeModelType(int n) {
        return switch (n) {
            case 1 -> Model.Type.JSON;
            case 2 -> Model.Type.OBJ;
            default -> Model.Type.BUILTIN;
        };
    }

    public BufferedImage generateModElementPicture() {
        BufferedImage bufferedImage = new BufferedImage(32, 32, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(new Color(78, 45, 24));
        graphics2D.fillRoundRect(4, 3, 24, 26, 5, 5);
        graphics2D.setColor(new Color(196, 48, 48));
        graphics2D.fillRoundRect(5, 4, 22, 24, 4, 4);
        graphics2D.setColor(new Color(238, 224, 184));
        graphics2D.fillRoundRect(8, 6, 16, 20, 2, 2);
        graphics2D.setColor(new Color(103, 59, 30));
        graphics2D.fillRect(8, 6, 2, 20);
        graphics2D.setColor(new Color(112, 98, 76));
        graphics2D.drawLine(12, 11, 21, 11);
        graphics2D.drawLine(12, 15, 21, 15);
        graphics2D.drawLine(12, 19, 19, 19);
        graphics2D.dispose();
        return bufferedImage;
    }

    public List<MCItem> providedMCItems() {
        if (this.getModElement() == null) {
            return List.of();
        }
        return List.of(new MCItem.Custom(this.getModElement(), null, "item"));
    }

    public List<TabEntry> getCreativeTabs() {
        if (this.creativeTabs == null) {
            this.creativeTabs = new ArrayList<TabEntry>();
        }
        this.creativeTabs.removeIf(tabEntry -> tabEntry == null);
        return this.creativeTabs;
    }

    public List<MCItem> getCreativeTabItems() {
        return this.providedMCItems();
    }

    public static class BookCategory {
        public String id = UUID.randomUUID().toString();
        public String name;
        public List<BookPage> pages;

        public BookCategory() {
            this("Category");
        }

        public BookCategory(String string) {
            this.name = string;
            this.pages = new ArrayList<BookPage>();
        }

        public String toString() {
            return this.name == null ? "Category" : this.name;
        }
    }

    public static class BookPage {
        public String id = UUID.randomUUID().toString();
        public String title;
        public boolean showTitle;
        public String content;
        public List<BookElement> elements;

        public BookPage() {
            this("Page", "");
        }

        public BookPage(String string, String string2) {
            this.title = string;
            this.showTitle = true;
            this.content = string2 == null ? "" : string2;
            this.elements = new ArrayList<BookElement>();
            this.elements.add(BookElement.text(this.content));
        }

        public List<BookElement> getBookElements() {
            this.normalizeElements();
            return this.elements;
        }

        public void normalizeElements() {
            if (this.elements == null) {
                this.elements = new ArrayList<BookElement>();
            }
            this.elements.removeIf(bookElement -> bookElement == null);
            String string = this.content == null ? "" : this.content;
            BookElement bookElement = null;
            boolean bl = false;
            for (BookElement bookElement2 : this.elements) {
                if (bookElement2 == null) continue;
                bookElement2.normalize();
                if (!"TEXT".equals(bookElement2.type)) continue;
                if (bookElement == null) {
                    bookElement = bookElement2;
                }
                if (bookElement2.content == null || bookElement2.content.isBlank()) continue;
                bl = true;
            }
            if (this.elements.isEmpty()) {
                this.elements.add(BookElement.text(string));
            } else if (!string.isBlank() && !bl) {
                if (bookElement != null) {
                    bookElement.content = string;
                } else {
                    this.elements.add(0, BookElement.text(string));
                }
            }
            for (BookElement bookElement2 : this.elements) {
                if (bookElement2 == null) continue;
                bookElement2.normalize();
            }
        }

        public String toString() {
            return this.title == null ? "Page" : this.title;
        }
    }

    public static class BookElement {
        public String id = UUID.randomUUID().toString();
        public String type = "TEXT";
        public int x = 18;
        public int y = 48;
        public int width = 220;
        public int height = 220;
        public String content = "";
        public String align = "LEFT";
        public String mediaName = "";
        public List<String> frames = new ArrayList<String>();
        public int frameDelay = 100;
        public String label = "Button";
        public String targetCategoryId = "";
        public String targetPageId = "";
        public String buttonStyle = "CLASSIC";
        public String buttonBackgroundColor = "#8A6846";
        public String buttonBorderColor = "#6D5237";
        public String buttonTextColor = "#FFEDC5";
        public String buttonImageName = "";
        public String buttonImageType = "SCREEN";
        public String buttonImageMode = "NONE";
        public boolean resizeBySides = true;
        public boolean resizeByCorners = true;

        public static BookElement text(String string) {
            BookElement bookElement = new BookElement();
            bookElement.type = "TEXT";
            bookElement.content = string == null ? "" : string;
            return bookElement;
        }

        public static BookElement image(String string, int n, int n2) {
            BookElement bookElement = new BookElement();
            bookElement.type = "IMAGE";
            bookElement.x = 58;
            bookElement.y = 70;
            bookElement.width = n;
            bookElement.height = n2;
            bookElement.mediaName = string == null ? "" : string;
            return bookElement;
        }

        public static BookElement gif(List<String> list, int n, int n2, int n3) {
            BookElement bookElement = new BookElement();
            bookElement.type = "GIF";
            bookElement.x = 58;
            bookElement.y = 70;
            bookElement.width = n2;
            bookElement.height = n3;
            bookElement.frames = list == null ? new ArrayList<String>() : new ArrayList<String>(list);
            bookElement.frameDelay = Math.max(20, n);
            return bookElement;
        }

        public static BookElement button(String string, String string2) {
            BookElement bookElement = new BookElement();
            bookElement.type = "BUTTON";
            bookElement.x = 68;
            bookElement.y = 250;
            bookElement.width = 120;
            bookElement.height = 22;
            bookElement.label = string == null ? "Button" : string;
            bookElement.targetCategoryId = string2 == null ? "" : string2;
            return bookElement;
        }

        /** Java class files cap a single UTF-8 constant at 65,535 bytes. */
        public List<String> getContentChunks() {
            String value = this.content == null ? "" : this.content;
            if (value.isEmpty()) {
                return List.of("");
            }
            List<String> chunks = new ArrayList<String>();
            for (int start = 0; start < value.length();) {
                int end = Math.min(value.length(), start + MAX_JAVA_STRING_CHUNK_LENGTH);
                if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1))
                        && Character.isLowSurrogate(value.charAt(end))) {
                    --end;
                }
                chunks.add(value.substring(start, end));
                start = end;
            }
            return chunks;
        }

        public void normalize() {
            if (this.id == null || this.id.isBlank()) {
                this.id = UUID.randomUUID().toString();
            }
            if (this.type == null || !(this.type.equals("TEXT") || this.type.equals("IMAGE")
                    || this.type.equals("GIF") || this.type.equals("BUTTON")
                    || this.type.equals("NAV_PREV") || this.type.equals("NAV_NEXT"))) {
                this.type = "TEXT";
            }
            if (this.content == null) {
                this.content = "";
            }
            this.content = CustomBook.limit(this.content, MAX_TEXT_LENGTH);
            if (this.align == null || !this.align.equals("LEFT") && !this.align.equals("CENTER") && !this.align.equals("RIGHT")) {
                this.align = "LEFT";
            }
            if (this.mediaName == null) {
                this.mediaName = "";
            }
            if (this.frames == null) {
                this.frames = new ArrayList<String>();
            }
            this.frames.removeIf(frame -> !BookElement.validResourceReference(frame));
            if (this.label == null) {
                this.label = "Button";
            }
            this.label = CustomBook.limit(this.label, MAX_BUTTON_LABEL_LENGTH);
            if (this.targetCategoryId == null) {
                this.targetCategoryId = "";
            }
            if (this.targetPageId == null) {
                this.targetPageId = "";
            }
            if (!(this.buttonStyle != null && (this.buttonStyle.equals("CLASSIC") || this.buttonStyle.equals("FLAT") || this.buttonStyle.equals("OUTLINE") || this.buttonStyle.equals("TRANSPARENT") || this.buttonStyle.equals("IMAGE")))) {
                this.buttonStyle = "CLASSIC";
            }
            if (!BookElement.validHex(this.buttonBackgroundColor)) {
                this.buttonBackgroundColor = "#8A6846";
            }
            if (!BookElement.validHex(this.buttonBorderColor)) {
                this.buttonBorderColor = "#6D5237";
            }
            if (!BookElement.validHex(this.buttonTextColor)) {
                this.buttonTextColor = "#FFEDC5";
            }
            if (this.buttonImageName == null) {
                this.buttonImageName = "";
            }
            if (!this.buttonImageName.isBlank() && !BookElement.validResourceReference(this.buttonImageName)) {
                this.buttonImageName = "";
            }
            if (!this.mediaName.isBlank() && !BookElement.validResourceReference(this.mediaName)) {
                this.mediaName = "";
            }
            if (this.buttonImageType == null || !this.buttonImageType.equals("SCREEN") && !this.buttonImageType.equals("ITEM") && !this.buttonImageType.equals("BLOCK")) {
                this.buttonImageType = "SCREEN";
            }
            if (this.buttonImageMode == null || !this.buttonImageMode.equals("NONE") && !this.buttonImageMode.equals("ICON_LEFT") && !this.buttonImageMode.equals("BACKGROUND")) {
                this.buttonImageMode = "NONE";
            }
            this.width = Math.max(1, Math.min(256, this.width));
            this.height = Math.max(1, Math.min(320, this.height));
            this.x = Math.max(0, Math.min(256 - this.width, this.x));
            this.y = Math.max(0, Math.min(320 - this.height, this.y));
            this.frameDelay = Math.max(20, this.frameDelay);
        }

        private static boolean validHex(String string) {
            return string != null && string.matches("#[0-9a-fA-F]{6}");
        }

        private static boolean validResourceReference(String value) {
            return value != null && !value.isBlank()
                    && value.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_./-]+")
                    && !value.contains("..") && !value.startsWith("/") && !value.endsWith("/");
        }

        public String toString() {
            return switch (this.type == null ? "TEXT" : this.type) {
                case "IMAGE" -> "Image";
                case "GIF" -> "GIF";
                case "BUTTON" -> "Button: " + this.label;
                case "NAV_PREV" -> "Arrow: previous page";
                case "NAV_NEXT" -> "Arrow: next page";
                default -> "Text";
            };
        }
    }
}
