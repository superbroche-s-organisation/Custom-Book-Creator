package ${package}.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joml.Matrix3x2fStack;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class ${name}BookScreen extends Screen {

    private static final int PAGE_W = 256;
    private static final int PAGE_H = 320;
    private static final int SIDEBAR_W = 140;
    private static final int GAP_W = 14;
    private static final int PAD = 5;
    private static final int CONTENT_W = SIDEBAR_W + GAP_W + PAGE_W;
    private static final int DESIGN_W = CONTENT_W + PAD * 2;
    private static final int DESIGN_H = PAGE_H + PAD * 2 + <#if data.hasWritingZones()>50<#else>0</#if>;
    private static final int DEFAULT_SIZE = 12;
    private static final int CATEGORY_ROWS = 11;
    private static final boolean HIDE_NEXT_ARROW_AT_CATEGORY_END = ${data.hideNextArrowAtCategoryEnd?c};
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[(/?)(b|i|u|s|obf|size|color|url|page|align)(?:=([^\\]]+))?\\]", Pattern.CASE_INSENSITIVE);

    private static final Category[] CATEGORIES = new Category[] {
        <#list data.getBookCategories() as category>
        new Category("${category.id?j_string}", "${category.name?j_string}", new Page[] {
            <#list category.pages as page>
            <#assign title = page.getTitleDesign()>
            <#assign layout = page.getTitleLayout()>
            new Page("${page.id?j_string}", "${page.title?j_string}", ${page.showTitle?c},
                "${page.getPageSound()?j_string}", "${page.getPageSoundTrigger()?j_string}",
                new Title(String.join("", new String[] {<#list page.getTitleContentChunks() as chunk>"${chunk?j_string}"<#sep>, </#sep></#list>}),
                    "${title.align?j_string}", ${layout.x()}, ${layout.y()}, ${layout.width()}, ${layout.height()}, ${layout.rotation()},
                    ${title.barVisible?c}, "${title.barColor?j_string}", ${layout.barX()}, ${layout.barY()}, ${layout.barWidth()}, ${layout.barHeight()}),
                new VisualElement[] {
                <#list page.getBookElements() as element>
                new VisualElement(
                    "${element.type?j_string}", ${element.x}, ${element.y}, ${element.width}, ${element.height},
                    String.join("", new String[] {<#list element.getScreenContentChunks() as chunk>"${chunk?j_string}"<#sep>, </#sep></#list>}), "${element.align?j_string}", "${element.mediaName?j_string}",
                    new String[] {<#list element.frames as frame>"${frame?j_string}"<#sep>, </#sep></#list>}, ${element.frameDelay},
                    "${element.label?j_string}", "${element.targetCategoryId?j_string}", "${element.targetPageId?j_string}",
                    "${element.buttonStyle?j_string}", "${element.buttonBackgroundColor?j_string}", "${element.buttonBorderColor?j_string}",
                    "${element.buttonTextColor?j_string}", "${element.buttonImageName?j_string}", "${element.buttonImageType?j_string}", "${element.buttonImageMode?j_string}",
                    "element.${element.id?j_string}", "${element.getSoundTrigger()?j_string}"
                )<#sep>,</#sep>
                </#list>
            })<#sep>,</#sep>
            </#list>
        })<#sep>,</#sep>
        </#list>
    };

    private final List<ClickRegion> clickRegions = new ArrayList<>();
    private int categoryIndex = 0;
    private int pageIndex = 0;
    private int categoryMenuScroll = 0;
    private float uiScale = 1f;
    private int originX = 0;
    private int originY = 0;

    public ${name}BookScreen() {
        this(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    public ${name}BookScreen(net.minecraft.world.InteractionHand hand) {
        super(Component.literal("${data.getSafeBookTitle()?j_string}"));
        <#if data.hasWritingZones()>
        this.writing = new ${package}.client.${name}BookWritingClient(hand);
        </#if>
    }

    @Override protected void init() {
        <#if data.hasWritingZones()>
        if (inlineEditor != null) inlineDraft = inlineEditor.getValue();
        if (inlineField == null) writing.load(); else createInlineWidgets();
        </#if>
        <#if data.hasPageSounds()>updatePageSound();</#if>
    }
    @Override public void tick() {
        <#if data.hasWritingZones()>writing.tick();</#if>
        <#if data.hasPageSounds()>updatePageSound();</#if>
    }

    <#if data.hasPageSounds()>
    private Page soundPage;
    private final java.util.Map<VisualElement, net.minecraft.client.resources.sounds.SoundInstance> pageSounds = new java.util.HashMap<>();

    private void updatePageSound() {
        Page page = currentPage();
        if (soundPage == page) return;
        stopPageSound(); soundPage = page;
        if (page != null) for (VisualElement element : page.elements)
            if ("SOUND".equals(element.type) && "OPEN".equals(element.soundTrigger)) playPageSound(element);
    }

    private void playPageSound(VisualElement element) {
        stopPageSound(element);
        if (element.mediaName.isEmpty() || minecraft == null) return;
        var instance = new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                Identifier.fromNamespaceAndPath("${modid}", element.mediaName), net.minecraft.sounds.SoundSource.RECORDS,
                1f, 1f, net.minecraft.util.RandomSource.create(), false, 0,
                net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        pageSounds.put(element, instance);
        minecraft.getSoundManager().play(instance);
    }

    private boolean isPageSoundPlaying(VisualElement element) {
        var instance = pageSounds.get(element);
        return instance != null && minecraft != null && minecraft.getSoundManager().isActive(instance);
    }

    private void stopPageSound(VisualElement element) {
        var instance = pageSounds.remove(element);
        if (instance != null && minecraft != null) minecraft.getSoundManager().stop(instance);
    }

    private void stopPageSound() {
        if (minecraft != null) for (var instance : pageSounds.values()) minecraft.getSoundManager().stop(instance);
        pageSounds.clear();
    }

    private void togglePageSound(String key) {
        Page page = currentPage();
        if (page == null) return;
        for (VisualElement element : page.elements) if ("SOUND".equals(element.type)
                && "BUTTON".equals(element.soundTrigger) && key.equals(element.translationKey)) {
            if (isPageSoundPlaying(element)) stopPageSound(element); else playPageSound(element);
            return;
        }
    }

    private void renderSoundElement(GuiGraphicsExtractor graphics, VisualElement element, int x, int y, int w, int h) {
        if (!"BUTTON".equals(element.soundTrigger) || element.mediaName.isEmpty()) return;
        fillD(graphics, x, y, x + w, y + h, parseHexColor(element.buttonBackgroundColor, 0xFF8A6846));
        drawButtonBorder(graphics, x, y, w, h, parseHexColor(element.buttonBorderColor, 0xFF6D5237), 1);
        enableDesignScissor(graphics, x + 2, y + 1, Math.max(1, w - 4), Math.max(1, h - 2));
        String label = (isPageSoundPlaying(element) ? "Stop: " : "Play: ") + translated(element.translationKey, element.label);
        drawCenteredD(graphics, Component.literal(label), x + w / 2, y + Math.max(2, (h - 9) / 2),
                parseHexColor(element.buttonTextColor, 0xFFFFEDC5));
        graphics.disableScissor();
        addClickRegion(x, y, w, h, "page_sound", element.translationKey);
    }

    @Override public void removed() {
        stopPageSound(); soundPage = null; super.removed();
    }
    </#if>


    <#if data.hasWritingZones()>
    private final ${package}.client.${name}BookWritingClient writing;

    private void renderWritingZone(GuiGraphicsExtractor graphics, VisualElement element, int x, int y, int w, int h) {
        fillD(graphics, x, y, x + w, y + h, 0xFFE9DEBF);
        drawButtonBorder(graphics, x, y, w, h, 0xFF6D5237, 1);
        enableDesignScissor(graphics, x + 3, y + 3, Math.max(1, w - 6), Math.max(1, h - 6));
        String text = writing.value(element.content);
        if (text.isEmpty()) text = writing.writable ? "Click to write: " + translated(element.translationKey, element.label)
                : translated(element.translationKey, element.label);
        int cy = y + 4;
        int wrapWidth = Math.max(1, Math.round(sd(Math.max(1, w - 8)) / crispTextScale(uiScale)));
        for (var line : font.split(Component.literal(text), wrapWidth)) {
            if (cy + 9 > y + h) break;
            StringBuilder plain = new StringBuilder();
            line.accept((index, style, codePoint) -> { plain.appendCodePoint(codePoint); return true; });
            drawTextD(graphics, Component.literal(plain.toString()), x + 4, cy, 0xFF38291E);
            cy += Math.max(10, Math.round(10 * crispTextScale(uiScale) / uiScale));
        }
        graphics.disableScissor();
        addClickRegion(x, y, w, h, "writing", element.content);
    }
    </#if>

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    <#include "page_interaction.java.ftl">

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // No call to the default Screen render state: this screen owns all its visual strata.
        // In particular, this prevents a menu/background post-process from softening the book.
        clickRegions.clear();
        updateLayout();

        int startX = PAD;
        int pageX = PAD + SIDEBAR_W + GAP_W;
        int pageY = PAD;
        int sideY = PAD;

        fillD(graphics, startX, sideY, startX + SIDEBAR_W, sideY + PAGE_H, 0xEE28231F);
        fillD(graphics, startX + 5, sideY + 5, startX + SIDEBAR_W - 5, sideY + PAGE_H - 5, 0xFF342D27);
        fillD(graphics, pageX - 3, pageY - 3, pageX + PAGE_W + 3, pageY + PAGE_H + 3, 0xFF6C5033);
        fillD(graphics, pageX, pageY, pageX + PAGE_W, pageY + PAGE_H, 0xFFFFF4D6);

        drawCenteredD(graphics, Component.literal(translated("title", "${data.getSafeBookTitle()?j_string}")), startX + SIDEBAR_W / 2, sideY + 16, 0xFFFFE5B8);
        renderCategories(graphics, startX + 13, sideY + 42, SIDEBAR_W - 26);
        <#if data.hasWritingZones()>
        String writingStatus = writing.signed ? "Signed: " + writing.author : writing.pending ? "Loading..."
                : writing.writable && ${package}.book.${name}BookWritingData.ALLOW_SIGNING ? "Sign book..." : "Read only";
        enableDesignScissor(graphics, startX + 7, sideY + PAGE_H - 22, SIDEBAR_W - 14, 20);
        drawCenteredD(graphics, Component.literal(writingStatus), startX + SIDEBAR_W / 2, sideY + PAGE_H - 18, 0xFFFFE5B8);
        graphics.disableScissor();
        if (writing.writable && ${package}.book.${name}BookWritingData.ALLOW_SIGNING)
            addClickRegion(startX + 7, sideY + PAGE_H - 22, SIDEBAR_W - 14, 18, "sign", "");
        if (inlineField != null) {
            enableDesignScissor(graphics, startX + 5, sideY + 25, SIDEBAR_W - 10, 15);
            drawTextD(graphics, Component.literal(writing.status), startX + 7, sideY + 28, 0xFFFFE5B8);
            graphics.disableScissor();
        }
        </#if>

        Page page = currentPage();
        <#if data.hasPageSounds()>
        updatePageSound();
        </#if>
        if (page == null) {
            drawCenteredD(graphics, Component.literal("No pages"), pageX + PAGE_W / 2, pageY + 32, 0xFF4B3929);
            return;
        }

        if (page.showTitle) renderTitle(graphics, page.titleDesign, pageX, pageY);

        for (VisualElement element : page.elements) renderElement(graphics, element, pageX, pageY);
        renderNavigation(graphics, pageX, pageY);
        <#if data.hasWritingZones()>renderInlineWidgets(graphics, mouseX, mouseY, partialTick);</#if>
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately no blurBeforeThisStratum(): only dim the world behind the book.
        graphics.fill(0, 0, width, height, 0xB0100D0B);
    }

    private void updateLayout() {
        float availableW = Math.max(1f, width - 6f);
        float availableH = Math.max(1f, height - 6f);
        uiScale = Math.max(0.05f, Math.min(1f, Math.min(availableW / DESIGN_W, availableH / DESIGN_H)));
        originX = Math.max(3, Math.round((width - DESIGN_W * uiScale) / 2f));
        originY = Math.max(3, Math.round((height - DESIGN_H * uiScale) / 2f));
    }

    private int sx(int designX) { return originX + Math.round(designX * uiScale); }
    private int sy(int designY) { return originY + Math.round(designY * uiScale); }
    private int sd(int designSize) { return Math.max(1, Math.round(designSize * uiScale)); }

    private void fillD(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(sx(x1), sy(y1), sx(x2), sy(y2), color);
    }

    private float crispTextScale(float requested) {
        double guiScale = Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
        int physicalScale = Math.max(1, (int) Math.round(Math.max(0.05f, requested) * guiScale));
        return (float) (physicalScale / guiScale);
    }

    private void drawTextD(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        float scale = crispTextScale(uiScale);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(sx(x), sy(y));
        pose.scale(scale, scale);
        graphics.text(font, text, 0, 0, color, false);
        pose.popMatrix();
    }

    private void drawCenteredD(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color) {
        float scale = crispTextScale(uiScale);
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(sx(centerX), sy(y));
        pose.scale(scale, scale);
        graphics.text(font, text, -font.width(text) / 2, 0, color, false);
        pose.popMatrix();
    }

    private void addClickRegion(int x, int y, int w, int h, String type, String target) {
        int rx = sx(x);
        int ry = sy(y);
        int rw = Math.max(1, sx(x + w) - rx);
        int rh = Math.max(1, sy(y + h) - ry);
        clickRegions.add(new ClickRegion(rx, ry, rw, rh, type, target));
    }

    private void addScreenClickRegion(int x, int y, int w, int h, String type, String target) {
        clickRegions.add(new ClickRegion(x, y, Math.max(1, w), Math.max(1, h), type, target));
    }

    private void enableDesignScissor(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int x1 = sx(x);
        int y1 = sy(y);
        int x2 = sx(x + w);
        int y2 = sy(y + h);
        graphics.enableScissor(x1, y1, Math.max(x1 + 1, x2), Math.max(y1 + 1, y2));
    }

    private void renderCategories(GuiGraphicsExtractor graphics, int x, int y, int w) {
        if (CATEGORIES.length == 0) {
            drawTextD(graphics, Component.literal("No categories"), x, y, 0xFFFFE8B8);
            return;
        }
        scrollCategories(0);
        int rows = Math.min(CATEGORY_ROWS, CATEGORIES.length);
        fillD(graphics, x - 4, y - 4, x + w + 4, y - 4 + rows * 23, 0xFF342D27);
        for (int row = 0; row < rows; row++) {
            int index = categoryMenuScroll + row;
            Category category = CATEGORIES[index];
            int cy = y + row * 23;
            if (index == categoryIndex) fillD(graphics, x - 3, cy - 4, x + w - 5, cy + 15, 0xFF5C4A3A);
            enableDesignScissor(graphics, x, cy - 2, Math.max(1, w - 8), 18);
            drawTextD(graphics, Component.literal(translated("category." + category.id, category.name)), x, cy, 0xFFFFE8B8);
            graphics.disableScissor();
            addClickRegion(x - 4, cy - 4, w + 8, 23, "category", category.id);
        }
        if (CATEGORIES.length > rows && rows > 0) {
            int track = rows * 23;
            int thumb = Math.max(6, track * rows / CATEGORIES.length);
            int offset = (track - thumb) * categoryMenuScroll / Math.max(1, CATEGORIES.length - rows);
            fillD(graphics, x + w, y - 4, x + w + 3, y - 4 + track, 0xFF5C4A3A);
            fillD(graphics, x + w, y - 4 + offset, x + w + 3, y - 4 + offset + thumb, 0xFFD7C4A5);
        }
    }

    private void scrollCategories(int delta) {
        categoryMenuScroll = Math.max(0, Math.min(categoryMenuScroll + delta, Math.max(0, CATEGORIES.length - CATEGORY_ROWS)));
    }

    private boolean categoryMenuContains(double mouseX, double mouseY) {
        return mouseX >= sx(PAD + 9) && mouseX < sx(PAD + SIDEBAR_W - 9)
                && mouseY >= sy(PAD + 38) && mouseY < sy(PAD + 38 + Math.min(CATEGORY_ROWS, CATEGORIES.length) * 23);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (categoryMenuContains(mouseX, mouseY)) {
            scrollCategories(deltaY > 0 ? -1 : deltaY < 0 ? 1 : 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
    private void renderTitle(GuiGraphicsExtractor graphics, Title title, int pageX, int pageY) {
        int x = pageX + title.x;
        int y = pageY + title.y;
        enableDesignScissor(graphics, x, y, title.width, title.height);
        boolean rotated = title.rotation != 0;
        renderRichTextBlock(graphics, translated("page." + currentPage().id, title.content), x, y, rotated ? title.height : title.width,
                rotated ? title.width : title.height, title.align, title.rotation);
        graphics.disableScissor();
        if (title.barVisible) fillD(graphics, pageX + title.barX, pageY + title.barY,
                pageX + title.barX + title.barWidth, pageY + title.barY + title.barHeight,
                parseHexColor(title.barColor, 0xFFDDCDAA));
    }

    private void renderElement(GuiGraphicsExtractor graphics, VisualElement element, int pageX, int pageY) {
        int x = pageX + element.x;
        int y = pageY + element.y;
        int w = Math.max(1, element.width);
        int h = Math.max(1, element.height);
        switch (element.type) {
            <#if data.hasWritingZones()>
            case "INPUT" -> renderWritingZone(graphics, element, x, y, w, h);
            </#if>
            <#if data.hasPageSounds()>case "SOUND" -> renderSoundElement(graphics, element, x, y, w, h);</#if>
            case "TEXT" -> {
                enableDesignScissor(graphics, x, y, w, h);
                renderRichTextBlock(graphics, translated(element.translationKey, element.content), x, y, w, h, element.align);
                graphics.disableScissor();
            }
            case "IMAGE" -> renderImage(graphics, element.mediaName, x, y, w, h);
            case "GIF" -> renderImage(graphics, currentFrame(element), x, y, w, h);
            case "BUTTON" -> {
                renderButton(graphics, element, x, y, w, h);
                addClickRegion(x, y, w, h, element.targetPageId.isBlank() ? "category" : "page",
                    element.targetPageId.isBlank() ? element.targetCategoryId : element.targetPageId);
            }
            case "NAV_PREV" -> {
                if (canAdvance(-1)) {
                    renderNavigationArrow(graphics, element, x, y, w, h, false);
                    addClickRegion(x, y, w, h, "nav", "prev");
                }
            }
            case "NAV_NEXT" -> {
                if (canAdvance(1)) {
                    renderNavigationArrow(graphics, element, x, y, w, h, true);
                    addClickRegion(x, y, w, h, "nav", "next");
                }
            }
        }
    }

    private void renderButton(GuiGraphicsExtractor graphics, VisualElement element, int x, int y, int w, int h) {
        int bg = parseHexColor(element.buttonBackgroundColor, 0xFF8A6846);
        int border = parseHexColor(element.buttonBorderColor, 0xFF6D5237);
        int textColor = parseHexColor(element.buttonTextColor, 0xFFFFEDC5);
        boolean hasImage = element.buttonImageName != null && !element.buttonImageName.isBlank();
        boolean backgroundImage = hasImage && ("BACKGROUND".equals(element.buttonImageMode) || "IMAGE".equals(element.buttonStyle));

        if (backgroundImage) renderResourceImage(graphics, element.buttonImageName, element.buttonImageType, x, y, w, h);

        switch (element.buttonStyle) {
            case "FLAT" -> {
                if (!backgroundImage) fillD(graphics, x, y, x + w, y + h, bg);
                drawButtonBorder(graphics, x, y, w, h, border, 1);
            }
            case "OUTLINE" -> drawButtonBorder(graphics, x, y, w, h, border, 2);
            case "TRANSPARENT" -> { }
            case "IMAGE" -> {
                if (!backgroundImage) fillD(graphics, x, y, x + w, y + h, bg);
                drawButtonBorder(graphics, x, y, w, h, border, 1);
            }
            default -> {
                if (!backgroundImage) {
                    fillD(graphics, x, y, x + w, y + h, border);
                    if (w > 4 && h > 4) fillD(graphics, x + 2, y + 2, x + w - 2, y + h - 2, bg);
                } else {
                    drawButtonBorder(graphics, x, y, w, h, border, 1);
                }
            }
        }

        int textLeft = x + 4;
        int textRight = x + w - 4;
        if (hasImage && "ICON_LEFT".equals(element.buttonImageMode)) {
            int icon = Math.max(4, Math.min(h - 6, Math.min(24, w / 3)));
            int iy = y + Math.max(2, (h - icon) / 2);
            renderResourceImage(graphics, element.buttonImageName, element.buttonImageType, x + 4, iy, icon, icon);
            textLeft += icon + 4;
        }

        Component label = Component.literal(translated(element.translationKey, element.label == null ? "Button" : element.label));
        int available = Math.max(1, textRight - textLeft);
        float labelScale = crispTextScale(uiScale);
        int logicalLabelWidth = Math.max(1, Math.round(font.width(label) * labelScale / Math.max(0.05f, uiScale)));
        int center = textLeft + available / 2;
        if (logicalLabelWidth > available) center = textLeft + logicalLabelWidth / 2;
        drawCenteredD(graphics, label, center, y + Math.max(2, (h - 9) / 2), textColor);
    }

    private void drawButtonBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color, int thickness) {
        int left = sx(x), top = sy(y);
        int right = Math.max(left + 1, sx(x + w)), bottom = Math.max(top + 1, sy(y + h));
        int edge = Math.min(Math.max(1, sd(thickness)), Math.min(right - left, bottom - top));
        graphics.fill(left, top, right, top + edge, color);
        graphics.fill(left, bottom - edge, right, bottom, color);
        graphics.fill(left, top, left + edge, bottom, color);
        graphics.fill(right - edge, top, right, bottom, color);
    }
    private static int parseHexColor(String value, int fallback) {
        try {
            if (value != null && value.matches("#[0-9a-fA-F]{6}")) return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
        } catch (Exception ignored) { }
        return fallback;
    }

    private void renderImage(GuiGraphicsExtractor graphics, String name, int x, int y, int w, int h) {
        renderResourceImage(graphics, name, "SCREEN", x, y, w, h);
    }

    private String currentFrame(VisualElement element) {
        if (element.frames.length == 0) return "";
        long frame = System.currentTimeMillis() / Math.max(20, element.frameDelay);
        return element.frames[(int) (frame % element.frames.length)];
    }


    private static String textureResource(String name, String typeName) {
        if (name == null || name.isBlank()) return null;
        String namespace = "${modid}";
        String pathName = name.trim().replace('\\', '/');
        int colon = pathName.indexOf(':');
        if (colon >= 0) {
            namespace = pathName.substring(0, colon);
            pathName = pathName.substring(colon + 1);
        }
        if (pathName.isBlank() || pathName.startsWith("/")) return null;
        String folder = switch (typeName == null ? "SCREEN" : typeName) {
            case "ITEM" -> "item";
            case "BLOCK" -> "block";
            default -> "screens";
        };
        if (!pathName.startsWith("textures/")) {
            if (pathName.startsWith("item/") || pathName.startsWith("block/") || pathName.startsWith("screens/")) {
                pathName = "textures/" + pathName;
            } else {
                pathName = "textures/" + folder + "/" + pathName;
            }
        }
        if (!pathName.endsWith(".png")) pathName += ".png";
        if (!namespace.matches("[a-z0-9_.-]+") || !pathName.matches("[a-z0-9/._-]+")) return null;
        for (String segment : pathName.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) return null;
        }
        return namespace + ":" + pathName;
    }

    private void renderResourceImage(GuiGraphicsExtractor graphics, String name, String typeName, int x, int y, int w, int h) {
        String resource = textureResource(name, typeName);
        if (resource == null) return;
        int colon = resource.indexOf(':');
        Identifier texture = Identifier.fromNamespaceAndPath(resource.substring(0, colon), resource.substring(colon + 1));
        int rx = sx(x);
        int ry = sy(y);
        int rw = Math.max(1, sx(x + w) - rx);
        int rh = Math.max(1, sy(y + h) - ry);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, rx, ry, 0, 0, rw, rh, rw, rh);
    }

    private void renderNavigation(GuiGraphicsExtractor graphics, int pageX, int pageY) {
        Page page = currentPage();
        if (page != null) {
            // Compatibility fallback for books saved before v2.5.0. Once the element is
            // re-saved in MCreator, NAV_PREV / NAV_NEXT become normal configurable elements.
            if (canAdvance(-1) && !hasNavigationElement(page, "NAV_PREV")) {
                VisualElement fallback = defaultNavigationElement(false);
                renderNavigationArrow(graphics, fallback, pageX + fallback.x, pageY + fallback.y, fallback.width, fallback.height, false);
                addClickRegion(pageX + fallback.x, pageY + fallback.y, fallback.width, fallback.height, "nav", "prev");
            }
            if (canAdvance(1) && !hasNavigationElement(page, "NAV_NEXT")) {
                VisualElement fallback = defaultNavigationElement(true);
                renderNavigationArrow(graphics, fallback, pageX + fallback.x, pageY + fallback.y, fallback.width, fallback.height, true);
                addClickRegion(pageX + fallback.x, pageY + fallback.y, fallback.width, fallback.height, "nav", "next");
            }
        }

        drawCenteredD(graphics, Component.literal((pageIndex + 1) + " / " + CATEGORIES[categoryIndex].pages.length),
            pageX + PAGE_W / 2, pageY + PAGE_H - 14, 0xFF806B52);
    }

    private boolean hasNavigationElement(Page page, String type) {
        for (VisualElement element : page.elements) if (type.equals(element.type)) return true;
        return false;
    }

    private VisualElement defaultNavigationElement(boolean next) {
        return new VisualElement(next ? "NAV_NEXT" : "NAV_PREV", next ? 226 : 12, 278, 18, 18,
            "", "LEFT", "", new String[0], 100, "", "", "",
            "TRANSPARENT", "#FFF4D6", "#6D5237", "#6A5842", "", "SCREEN", "NONE", "", "OPEN");
    }

    private void renderNavigationArrow(GuiGraphicsExtractor graphics, VisualElement element, int x, int y, int w, int h, boolean right) {
        int bg = parseHexColor(element.buttonBackgroundColor, 0xFFFFF4D6);
        int border = parseHexColor(element.buttonBorderColor, 0xFF6D5237);
        int icon = parseHexColor(element.buttonTextColor, 0xFF6A5842);
        boolean hasImage = element.buttonImageName != null && !element.buttonImageName.isBlank();

        if ("IMAGE".equals(element.buttonStyle) && hasImage) {
            renderResourceImage(graphics, element.buttonImageName, element.buttonImageType, x, y, w, h);
            return;
        }

        switch (element.buttonStyle) {
            case "CLASSIC" -> {
                fillD(graphics, x, y, x + w, y + h, border);
                if (w > 4 && h > 4) fillD(graphics, x + 2, y + 2, x + w - 2, y + h - 2, bg);
            }
            case "FLAT" -> fillD(graphics, x, y, x + w, y + h, bg);
            case "OUTLINE" -> drawButtonBorder(graphics, x, y, w, h, border, 1);
            default -> { }
        }

        String glyphStyle = switch (element.align) {
            case "CENTER" -> "TRIANGLE";
            case "RIGHT" -> "DOUBLE";
            default -> "CHEVRON";
        };
        paintNavigationGlyphD(graphics, x, y, w, h, right, glyphStyle, icon);
    }

    /**
     * Navigation glyphs are rasterized directly in final GUI pixels.
     * This keeps the arrow proportions identical when the window becomes small:
     * the logical rectangle is converted once, then the glyph is drawn inside
     * that exact screen-space rectangle.
     */
    private void paintNavigationGlyphD(GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean right, String style, int color) {
        int rx = sx(x);
        int ry = sy(y);
        int rw = Math.max(3, sx(x + w) - rx);
        int rh = Math.max(3, sy(y + h) - ry);
        int cx = rx + rw / 2;
        int cy = ry + rh / 2;
        int radius = Math.max(2, Math.min(rw, rh) / 3);
        int thickness = Math.max(1, Math.min(rw, rh) / 9);

        if ("TRIANGLE".equals(style)) {
            int diameter = radius * 2;
            for (int step = 0; step <= diameter; step++) {
                int px = right ? cx - radius + step : cx + radius - step;
                int half = Math.max(0, radius - step / 2);
                graphics.fill(px, cy - half, px + thickness, cy + half + 1, color);
            }
            return;
        }

        paintChevronStrokeScreen(graphics, cx, cy, radius, thickness, right, color, 0);
        if ("DOUBLE".equals(style)) {
            int offset = Math.max(2, radius / 2 + 1);
            paintChevronStrokeScreen(graphics, cx, cy, radius, thickness, right, color, right ? -offset : offset);
        }
    }

    private void paintChevronStrokeScreen(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int thickness, boolean right, int color, int offset) {
        for (int i = 0; i <= radius; i++) {
            int px = (right ? cx - radius / 2 + i : cx + radius / 2 - i) + offset;
            graphics.fill(px, cy - radius + i, px + thickness, cy - radius + i + thickness, color);
            graphics.fill(px, cy + radius - i, px + thickness, cy + radius - i + thickness, color);
        }
    }

    private boolean canAdvance(int delta) {
        if (CATEGORIES.length == 0) return false;
        if (delta > 0 && HIDE_NEXT_ARROW_AT_CATEGORY_END
                && categoryIndex >= 0 && categoryIndex < CATEGORIES.length
                && pageIndex >= CATEGORIES[categoryIndex].pages.length - 1) return false;
        int c = categoryIndex;
        int p = pageIndex + delta;
        if (delta > 0) {
            while (c < CATEGORIES.length && p >= CATEGORIES[c].pages.length) { c++; p = 0; }
            return c < CATEGORIES.length;
        } else {
            while (c >= 0 && p < 0) { c--; if (c >= 0) p = CATEGORIES[c].pages.length - 1; }
            return c >= 0;
        }
    }

    private void renderRichTextBlock(GuiGraphicsExtractor graphics, String markup, int x, int y, int width, int height, String align) {
        renderRichTextBlock(graphics, markup, x, y, width, height, align, 0);
    }

    private void renderRichTextBlock(GuiGraphicsExtractor graphics, String markup, int x, int y, int width, int height, String align, int rotation) {
        // Wrapping uses logical book coordinates, while glyph placement uses a
        // screen-pixel pen. This avoids cumulative rounding when uiScale < 1:
        // two neighboring glyphs can never collapse onto the same GUI pixels.
        List<Glyph> glyphs = buildGlyphs(markup == null ? "" : markup);
        List<Line> lines = wrapGlyphs(glyphs, Math.max(1, width));

        int cy = y;
        for (Line current : lines) {
            if (cy >= y + height) break;
            int offset = 0;
            String lineAlignment = current.align == null ? align : current.align;
            if ("CENTER".equals(lineAlignment)) offset = Math.max(0, (width - current.width) / 2);
            else if ("RIGHT".equals(lineAlignment)) offset = Math.max(0, width - current.width);

            int screenCx = sx(x + offset);
            int screenCy = sy(cy);
            if (rotation < 0) {
                screenCx = sx(x + cy - y);
                screenCy = sy(y + width - offset);
            } else if (rotation > 0) {
                screenCx = sx(x + height - (cy - y));
                screenCy = sy(y + offset);
            }
            for (Glyph glyph : current.glyphs) {
                if (cy + glyph.height > y + height) break;
                float screenScale = crispTextScale(glyph.scale * uiScale);
                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(screenCx, screenCy);
                if (rotation != 0) pose.rotate((float) Math.toRadians(rotation));
                pose.scale(screenScale, screenScale);
                graphics.text(font, glyph.component, 0, 0, 0xFF38291E, false);
                pose.popMatrix();

                int screenAdvance = glyphScreenAdvance(glyph.component, glyph.scale);
                int screenHeight = glyphScreenHeight(glyph.scale);
                if (glyph.actionType != null && !glyph.raw.isBlank()) {
                    if (rotation < 0) addScreenClickRegion(screenCx, screenCy - screenAdvance, screenHeight, screenAdvance, glyph.actionType, glyph.actionTarget);
                    else if (rotation > 0) addScreenClickRegion(screenCx - screenHeight, screenCy, screenHeight, screenAdvance, glyph.actionType, glyph.actionTarget);
                    else addScreenClickRegion(screenCx, screenCy, screenAdvance, screenHeight, glyph.actionType, glyph.actionTarget);
                }
                if (rotation < 0) screenCy -= screenAdvance;
                else if (rotation > 0) screenCy += screenAdvance;
                else screenCx += screenAdvance;
            }
            cy += Math.max(10, current.height);
        }
    }

    private int glyphScreenAdvance(Component component, float designScale) {
        float screenScale = crispTextScale(designScale * uiScale);
        int advance = Math.max(1, (int) Math.ceil(font.width(component) * screenScale));
        // A one-pixel breathing space at reduced layout scales prevents glyph
        // edge pixels from touching after raster quantization.
        if (uiScale < 0.999f) advance += 1;
        return advance;
    }

    private int glyphScreenHeight(float designScale) {
        float screenScale = crispTextScale(designScale * uiScale);
        return Math.max(1, (int) Math.ceil(9 * screenScale) + (uiScale < 0.999f ? 1 : 0));
    }

    private List<Line> wrapGlyphs(List<Glyph> glyphs, int maxWidth) {
        List<Line> lines = new ArrayList<>();
        Line current = new Line();
        String paragraphAlign = null;
        boolean paragraphStart = true;
        int i = 0;
        while (i < glyphs.size()) {
            Glyph glyph = glyphs.get(i);
            if (paragraphStart && !glyph.newline) { paragraphAlign = glyph.align; paragraphStart = false; }
            if (glyph.newline) {
                current.align = paragraphAlign;
                lines.add(current);
                current = new Line();
                paragraphStart = true;
                paragraphAlign = null;
                i++;
                continue;
            }
            if (glyph.raw.isBlank()) {
                // Do not carry leading spaces to a freshly wrapped line.
                if (!current.glyphs.isEmpty()) {
                    if (current.width + glyph.width > maxWidth) {
                        current.align = paragraphAlign;
                lines.add(current);
                        current = new Line();
                    } else {
                        current.add(glyph);
                    }
                }
                i++;
                continue;
            }

            // Gather one word. It is placed as a unit whenever it fits; very long
            // words are hard-wrapped character by character so text can never escape
            // the configured text-zone width.
            List<Glyph> word = new ArrayList<>();
            int wordWidth = 0;
            int j = i;
            while (j < glyphs.size()) {
                Glyph next = glyphs.get(j);
                if (next.newline || next.raw.isBlank()) break;
                word.add(next);
                wordWidth += next.width;
                j++;
            }
            if (!current.glyphs.isEmpty() && current.width + wordWidth > maxWidth) {
                current.align = paragraphAlign;
                lines.add(current);
                current = new Line();
            }
            if (wordWidth <= maxWidth) {
                for (Glyph part : word) current.add(part);
            } else {
                for (Glyph part : word) {
                    if (!current.glyphs.isEmpty() && current.width + part.width > maxWidth) {
                        current.align = paragraphAlign;
                lines.add(current);
                        current = new Line();
                    }
                    current.add(part);
                }
            }
            i = j;
        }
        current.align = paragraphAlign;
                lines.add(current);
        return lines;
    }

    private List<Glyph> buildGlyphs(String markup) {
        List<Glyph> result = new ArrayList<>();
        List<RichToken> tokens = parse(markup);
        for (RichToken token : tokens) {
            State state = token.state;
            float designScale = Math.max(0.05f, state.size / 9.0f);
            for (int i = 0; i < token.text.length();) {
                int ch = token.text.codePointAt(i);
                i += Character.charCount(ch);
                if (ch == '\r') continue;
                if (ch == '\n') {
                    result.add(new Glyph("", Component.empty(), 1f, 0, 10, true, null, null, null));
                    continue;
                }
                String raw = new String(Character.toChars(ch));
                Component component = styledComponent(raw, state);

                // crispTextScale() may quantize the requested scale to a whole
                // physical-pixel multiple. In a small window that quantized scale
                // can be slightly larger than designScale * uiScale. Compute the
                // logical advance from the *actual* render scale so the next glyph
                // can never start before the previous one has finished.
                int screenAdvance = glyphScreenAdvance(component, designScale);
                int screenHeight = glyphScreenHeight(designScale);
                int gw = Math.max(1, (int) Math.ceil(screenAdvance / Math.max(0.05f, uiScale)));
                int gh = Math.max(1, (int) Math.ceil(screenHeight / Math.max(0.05f, uiScale)));
                result.add(new Glyph(raw, component, designScale, gw, gh, false, state.actionType, state.actionTarget, state.align));
            }
        }
        return result;
    }

    private Component styledComponent(String text, State state) {
        Style style = Style.EMPTY
            .withBold(state.bold)
            .withItalic(state.italic)
            .withUnderlined(state.underline || state.actionType != null)
            .withStrikethrough(state.strike)
            .withObfuscated(state.obfuscated);
        if (state.color != null) style = style.withColor(state.color);
        else if (state.actionType != null) style = style.withColor(0x2F62C9);
        return Component.literal(text).withStyle(style);
    }

    private static List<RichToken> parse(String markup) {
        List<RichToken> tokens = new ArrayList<>();
        Deque<StateFrame> stack = new ArrayDeque<>();
        State state = new State();
        Matcher matcher = TAG_PATTERN.matcher(markup);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) tokens.add(new RichToken(markup.substring(last, matcher.start()), state.copy()));
            boolean closing = !matcher.group(1).isEmpty();
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String arg = matcher.group(3);
            if (closing) {
                boolean hasMatchingOpening = false;
                for (StateFrame frame : stack) {
                    if (frame.tag.equals(tag)) { hasMatchingOpening = true; break; }
                }
                if (hasMatchingOpening) {
                    while (!stack.isEmpty()) {
                        StateFrame frame = stack.pop();
                        state = frame.previous;
                        if (frame.tag.equals(tag)) break;
                    }
                }
            } else {
                stack.push(new StateFrame(tag, state.copy()));
                switch (tag) {
                    case "b" -> state.bold = true;
                    case "i" -> state.italic = true;
                    case "u" -> state.underline = true;
                    case "s" -> state.strike = true;
                    case "obf" -> state.obfuscated = true;
                    case "align" -> state.align = arg != null && List.of("LEFT", "CENTER", "RIGHT").contains(arg.toUpperCase(Locale.ROOT)) ? arg.toUpperCase(Locale.ROOT) : "LEFT";
                    case "size" -> state.size = safeInt(arg, DEFAULT_SIZE, 6, 48);
                    case "color" -> state.color = parseColor(arg);
                    case "url" -> { state.actionType = "url"; state.actionTarget = arg == null ? "" : arg; }
                    case "page" -> { state.actionType = "page"; state.actionTarget = arg == null ? "" : arg; }
                }
            }
            last = matcher.end();
        }
        if (last < markup.length()) tokens.add(new RichToken(markup.substring(last), state.copy()));
        return tokens;
    }

    private static Integer parseColor(String color) {
        try {
            if (color != null && color.matches("#[0-9a-fA-F]{6}")) return Integer.parseInt(color.substring(1), 16);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int safeInt(String value, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(value))); }
        catch (Exception ignored) { return fallback; }
    }

    private Page currentPage() {
        if (CATEGORIES.length == 0) return null;
        categoryIndex = Math.max(0, Math.min(categoryIndex, CATEGORIES.length - 1));
        Category category = CATEGORIES[categoryIndex];
        if (category.pages.length == 0) return null;
        pageIndex = Math.max(0, Math.min(pageIndex, category.pages.length - 1));
        return category.pages[pageIndex];
    }

    private void selectCategoryById(String id) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].id.equals(id)) {
                categoryIndex = i;
                categoryMenuScroll = i; scrollCategories(0);
                pageIndex = 0;
                return;
            }
        }
    }

    private void selectPageById(String id) {
        for (int c = 0; c < CATEGORIES.length; c++) {
            for (int p = 0; p < CATEGORIES[c].pages.length; p++) {
                if (CATEGORIES[c].pages[p].id.equals(id)) {
                    categoryIndex = c;
                    categoryMenuScroll = c; scrollCategories(0);
                    pageIndex = p;
                    return;
                }
            }
        }
    }

    private void advancePage(int delta) {
        if (!canAdvance(delta)) return;
        int c = categoryIndex;
        int p = pageIndex + delta;
        if (delta > 0) {
            while (c < CATEGORIES.length && p >= CATEGORIES[c].pages.length) {
                c++;
                p = 0;
            }
            if (c >= CATEGORIES.length) return;
        } else {
            while (c >= 0 && p < 0) {
                c--;
                if (c >= 0) p = CATEGORIES[c].pages.length - 1;
            }
            if (c < 0) return;
        }
        categoryIndex = c;
        categoryMenuScroll = c; scrollCategories(0);
        pageIndex = p;
    }

    /**
     * External links always require explicit confirmation. The link is marked as
     * untrusted so Minecraft displays its standard security warning and the URL
     * before the browser is opened.
     */
    private void openExternalLinkWithWarning(String url) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) Util.getPlatform().openUri(url);
            minecraft.setScreen(this);
        }, url, false));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {

        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0) {
            <#if data.hasWritingZones()>if (inlineField != null && mouseX >= sx(PAD + SIDEBAR_W + GAP_W)) return super.mouseClicked(event, doubleClick);</#if>
            for (int i = clickRegions.size() - 1; i >= 0; i--) {
                ClickRegion region = clickRegions.get(i);
                if (!region.contains(mouseX, mouseY)) continue;
                switch (region.type) {
                    <#if data.hasWritingZones()>
                    case "writing" -> beginInlineWriting(region.target);
                    case "sign" -> { if (inlineField == null && !writing.pending) Minecraft.getInstance().setScreen(new ${name}BookWritingScreen(this, writing, null)); }
                    </#if>
                    <#if data.hasPageSounds()>case "page_sound" -> togglePageSound(region.target);</#if>
                    case "category" -> { <#if data.hasWritingZones()>navigateToCategory(region.target);<#else>selectCategoryById(region.target); <#if data.hasPageSounds()>updatePageSound();</#if></#if> }
                    case "nav" -> { advancePage("next".equals(region.target) ? 1 : -1); <#if data.hasPageSounds()>updatePageSound();</#if> }
                    case "page" -> { selectPageById(region.target); <#if data.hasPageSounds()>updatePageSound();</#if> }
                    case "url" -> {
                        if (region.target.startsWith("https://") || region.target.startsWith("http://")) {
                            openExternalLinkWithWarning(region.target);
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private record Category(String id, String name, Page[] pages) {}
    private static String translated(String suffix, String fallback) {
        if (suffix == null || suffix.isEmpty()) return fallback;
        String key = "custombook.${modid}.${registryname}." + suffix;
        return net.minecraft.locale.Language.getInstance().getOrDefault(key, fallback);
    }
    private record Page(String id, String title, boolean showTitle, String sound, String soundTrigger, Title titleDesign, VisualElement[] elements) {}
    private record Title(String content, String align, int x, int y, int width, int height, int rotation,
                         boolean barVisible, String barColor, int barX, int barY, int barWidth, int barHeight) {}
    private record VisualElement(String type, int x, int y, int width, int height, String content, String align,
                                 String mediaName, String[] frames, int frameDelay, String label, String targetCategoryId, String targetPageId,
                                 String buttonStyle, String buttonBackgroundColor, String buttonBorderColor, String buttonTextColor,
                                 String buttonImageName, String buttonImageType, String buttonImageMode, String translationKey, String soundTrigger) {}
    private record StateFrame(String tag, State previous) {}
    private record RichToken(String text, State state) {}
    private record Glyph(String raw, Component component, float scale, int width, int height, boolean newline,
                         String actionType, String actionTarget, String align) {
        static Glyph newLineGlyph() { return new Glyph("", Component.empty(), 1f, 0, 10, true, null, null, null); }
    }
    private record ClickRegion(int x, int y, int width, int height, String type, String target) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }

    private static final class Line {
        final List<Glyph> glyphs = new ArrayList<>();
        int width;
        int height = 0;
        String align;
        void add(Glyph glyph) {
            if (glyphs.isEmpty()) align = glyph.align;
            glyphs.add(glyph);
            width += glyph.width;
            height = Math.max(height, glyph.height);
        }
    }

    private static final class State {
        boolean bold;
        boolean italic;
        boolean underline;
        boolean strike;
        boolean obfuscated;
        int size = DEFAULT_SIZE;
        Integer color;
        String actionType;
        String actionTarget;
        String align;
        State copy() {
            State copy = new State();
            copy.bold = bold;
            copy.italic = italic;
            copy.underline = underline;
            copy.strike = strike;
            copy.obfuscated = obfuscated;
            copy.size = size;
            copy.color = color;
            copy.actionType = actionType;
            copy.actionTarget = actionTarget;
            copy.align = align;
            return copy;
        }
    }
}
