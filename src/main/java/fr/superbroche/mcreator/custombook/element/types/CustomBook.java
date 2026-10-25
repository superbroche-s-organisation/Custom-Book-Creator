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
import net.mcreator.element.parts.procedure.LogicProcedure;
import net.mcreator.element.parts.procedure.Procedure;
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
    public boolean allowSigning = true;
    public LogicProcedure writingPermission = new LogicProcedure(null, true);
    public Procedure onBookSigned;
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
        if (hasWritingZones()) return 1;
        return Math.max(1, Math.min(64, this.stackSize));
    }

    public boolean hasWritingZones() {
        return !getWritingZones().isEmpty();
    }

    /** Keys are stable across reordering and are also exposed as item custom-data keys. */
    public List<BookElement> getWritingZones() {
        List<BookElement> zones = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (BookCategory category : getBookCategories()) {
            for (BookPage page : category.pages) {
                for (BookElement element : page.elements) {
                    if (!"INPUT".equals(element.type)) continue;
                    String base = element.inputKey;
                    int suffix = 2;
                    while (!keys.add(element.inputKey)) {
                        element.inputKey = limit(base, 25) + "_" + suffix++;
                    }
                    zones.add(element);
                    if (zones.size() > 64)
                        throw new IllegalStateException("A book can contain at most 64 writing zones. Remove extra zones before saving.");
                }
            }
        }
        return zones;
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

    public boolean hasPageSounds() {
        return getBookCategories().stream().flatMap(category -> category.pages.stream())
                .anyMatch(page -> !page.getPageSound().isEmpty());
    }

    public static class BookPage {
        public String id = UUID.randomUUID().toString();
        public String title;
        public boolean showTitle;
        public TitleDesign titleDesign = new TitleDesign();
        public String pageSound = "";
        public String pageSoundTrigger = "OPEN";
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

        /** Workspace sound event name, never an external file or URL. */
        public String getPageSound() {
            return pageSound != null && pageSound.matches("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*")
                    && !pageSound.contains("..") ? pageSound : "";
        }

        public String getPageSoundTrigger() {
            return "BUTTON".equals(pageSoundTrigger) ? "BUTTON" : "OPEN";
        }

        public TitleDesign getTitleDesign() {
            if (this.titleDesign == null) this.titleDesign = new TitleDesign();
            this.titleDesign.normalize();
            return this.titleDesign;
        }

        public String getTitleContent() {
            String content = this.getTitleDesign().content;
            return content.isBlank() ? (this.title == null ? "Page" : this.title) : content;
        }

        public List<String> getTitleContentChunks() {
            return BookElement.text(this.getTitleContent()).getContentChunks();
        }

        public TitleLayout getTitleLayout() {
            return this.getTitleDesign().layout();
        }

        public void normalizeElements() {
            this.getTitleDesign();
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

    /** Page-relative design coordinates shared by the editor and generated Minecraft screen. */
    public record TitleLayout(int x, int y, int width, int height, int rotation,
                              int handleX, int handleY, int barX, int barY, int barWidth, int barHeight) {}

    public static class TitleDesign {
        public String content = "";
        public String align = "CENTER";
        public String position = "TOP";
        public int offset = 0;
        public int height = 24;
        public boolean barVisible = true;
        public String barColor = "#DDCDAA";
        public int barLength = 228;
        public int barThickness = 1;

        public void normalize() {
            this.content = CustomBook.limit(this.content == null ? "" : this.content, MAX_TEXT_LENGTH);
            if (!"LEFT".equals(this.align) && !"CENTER".equals(this.align) && !"RIGHT".equals(this.align)) this.align = "CENTER";
            if (!"TOP".equals(this.position) && !"BOTTOM".equals(this.position)
                    && !"LEFT".equals(this.position) && !"RIGHT".equals(this.position)) this.position = "TOP";
            this.height = Math.max(10, Math.min(96, this.height));
            boolean vertical = "LEFT".equals(this.position) || "RIGHT".equals(this.position);
            this.offset = Math.max(0, Math.min((vertical ? 256 : 320) - 20 - this.height, this.offset));
            if (!BookElement.validHex(this.barColor)) this.barColor = "#DDCDAA";
            this.barLength = Math.max(1, Math.min(vertical ? 292 : 228, this.barLength));
            this.barThickness = Math.max(1, Math.min(this.height, this.barThickness));
        }

        public TitleLayout layout() {
            this.normalize();
            int x = 14, y = 10 + this.offset, width = 228, boxHeight = this.height, rotation = 0;
            switch (this.position) {
                case "BOTTOM" -> y = 310 - this.offset - this.height;
                case "LEFT" -> { x = 10 + this.offset; y = 14; width = this.height; boxHeight = 292; rotation = -90; }
                case "RIGHT" -> { x = 246 - this.offset - this.height; y = 14; width = this.height; boxHeight = 292; rotation = 90; }
                default -> { }
            }
            boolean vertical = rotation != 0;
            int barX = vertical ? (rotation < 0 ? x + width - this.barThickness : x) : x + (width - this.barLength) / 2;
            int barY = vertical ? y + (boxHeight - this.barLength) / 2 : ("BOTTOM".equals(this.position) ? y : y + boxHeight - this.barThickness);
            int barWidth = vertical ? this.barThickness : this.barLength;
            int barHeight = vertical ? this.barLength : this.barThickness;
            return new TitleLayout(x, y, width, boxHeight, rotation, barX + barWidth / 2, barY + barHeight / 2,
                    barX, barY, barWidth, barHeight);
        }

        public int offsetForHandle(int x, int y, int gridStep) {
            this.normalize();
            boolean vertical = "LEFT".equals(this.position) || "RIGHT".equals(this.position);
            int coordinate = vertical ? x : y;
            if (gridStep > 0) coordinate = (int) Math.round(coordinate / (double) gridStep) * gridStep;
            int offset = switch (this.position) {
                case "BOTTOM" -> 310 - this.height + this.barThickness / 2 - coordinate;
                case "RIGHT" -> 246 - this.height + this.barThickness / 2 - coordinate;
                default -> coordinate - 10 - this.height + this.barThickness - this.barThickness / 2;
            };
            return Math.max(0, Math.min((vertical ? 256 : 320) - 20 - this.height, offset));
        }

        public TitleDesign copy() {
            TitleDesign copy = new TitleDesign();
            copy.content = this.content;
            copy.align = this.align;
            copy.position = this.position;
            copy.offset = this.offset;
            copy.height = this.height;
            copy.barVisible = this.barVisible;
            copy.barColor = this.barColor;
            copy.barLength = this.barLength;
            copy.barThickness = this.barThickness;
            copy.normalize();
            return copy;
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
        public String inputKey = "";
        public String inputMode = "BOTH";
        public int inputMaxLength = 1024;
        public List<String> inputChoices = new ArrayList<>();

        public List<String> getScreenContentChunks() {
            return "INPUT".equals(type) ? List.of(inputKey) : getContentChunks();
        }

        public static BookElement input() {
            BookElement element = new BookElement();
            element.type = "INPUT";
            element.label = "Your text";
            element.height = 96;
            element.normalize();
            return element;
        }

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
                    || this.type.equals("GIF") || this.type.equals("BUTTON") || this.type.equals("INPUT")
                    || this.type.equals("NAV_PREV") || this.type.equals("NAV_NEXT"))) {
                this.type = "TEXT";
            }
            if (this.content == null) {
                this.content = "";
            }
            this.content = CustomBook.limit(this.content, MAX_TEXT_LENGTH);
            if (inputKey == null || !inputKey.matches("[a-z][a-z0-9_]{0,31}")) {
                inputKey = "field_" + UUID.nameUUIDFromBytes(
                        String.valueOf(id).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .toString().replace("-", "").substring(0, 16);
            }
            if (!"FREE".equals(inputMode) && !"PRESET".equals(inputMode) && !"BOTH".equals(inputMode)) inputMode = "BOTH";
            inputMaxLength = Math.max(1, Math.min(1024, inputMaxLength));
            if (inputChoices == null) inputChoices = new ArrayList<>();
            inputChoices = inputChoices.stream().filter(java.util.Objects::nonNull)
                    .map(s -> CustomBook.limit(s.replace("\r", ""), inputMaxLength))
                    .filter(s -> !s.isBlank()).distinct().limit(16).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
            if ("INPUT".equals(type)) {
                content = CustomBook.limit(content.replace("\r", ""), inputMaxLength);
                if ("PRESET".equals(inputMode) && !content.isEmpty() && !inputChoices.contains(content)) content = "";
            }
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
                case "INPUT" -> "Writing zone: " + this.label;
                case "NAV_PREV" -> "Arrow: previous page";
                case "NAV_NEXT" -> "Arrow: next page";
                default -> "Text";
            };
        }
    }
}
