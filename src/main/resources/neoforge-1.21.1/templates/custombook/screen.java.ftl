package ${package}.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public class ${name}BookScreen extends Screen {

    private static final int PAGE_W = 256;
    private static final int PAGE_H = 320;
    private static final int SIDEBAR_W = 140;
    private static final int GAP_W = 14;
    private static final int PAD = 5;
    private static final int CONTENT_W = SIDEBAR_W + GAP_W + PAGE_W;
    private static final int DESIGN_W = CONTENT_W + PAD * 2;
    private static final int DESIGN_H = PAGE_H + PAD * 2;
    private static final int DEFAULT_SIZE = 12;
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[(/?)(b|i|u|s|obf|size|color|url|page)(?:=([^\\]]+))?\\]", Pattern.CASE_INSENSITIVE);

    private static final Category[] CATEGORIES = new Category[] {
        <#list data.getBookCategories() as category>
        new Category("${category.id?j_string}", "${category.name?j_string}", new Page[] {
            <#list category.pages as page>
            new Page("${page.id?j_string}", "${page.title?j_string}", ${page.showTitle?c}, new VisualElement[] {
                <#list page.getBookElements() as element>
                new VisualElement(
                    "${element.type?j_string}", ${element.x}, ${element.y}, ${element.width}, ${element.height},
                    "${element.content?j_string}", "${element.align?j_string}", "${element.mediaName?j_string}",
                    new String[] {<#list element.frames as frame>"${frame?j_string}"<#sep>, </#sep></#list>}, ${element.frameDelay},
                    "${element.label?j_string}", "${element.targetCategoryId?j_string}", "${element.targetPageId?j_string}",
                    "${element.buttonStyle?j_string}", "${element.buttonBackgroundColor?j_string}", "${element.buttonBorderColor?j_string}",
                    "${element.buttonTextColor?j_string}", "${element.buttonImageName?j_string}", "${element.buttonImageType?j_string}", "${element.buttonImageMode?j_string}"
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
    private float uiScale = 1f;
    private int originX = 0;
    private int originY = 0;

    public ${name}BookScreen() {
        super(Component.literal("${data.bookTitle?j_string}"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Vanilla 1.21.1 applies the in-game menu blur from Screen#renderBackground.
     * This book is an overlay, so keep only a translucent darkening layer.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0100D0B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
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

        drawCenteredD(graphics, Component.literal("${data.bookTitle?j_string}"), startX + SIDEBAR_W / 2, sideY + 16, 0xFFFFE5B8);
        renderCategories(graphics, startX + 13, sideY + 42, SIDEBAR_W - 26);

        Page page = currentPage();
        if (page == null) {
            drawCenteredD(graphics, Component.literal("No pages"), pageX + PAGE_W / 2, pageY + 32, 0xFF4B3929);
            return;
        }

        if (page.showTitle) {
            drawCenteredD(graphics, Component.literal(page.title), pageX + PAGE_W / 2, pageY + 17, 0xFF38291E);
            fillD(graphics, pageX + 14, pageY + 33, pageX + PAGE_W - 14, pageY + 34, 0xFFDDCDAA);
        }

        for (VisualElement element : page.elements) renderElement(graphics, element, pageX, pageY);
        renderNavigation(graphics, pageX, pageY);
    }

    /**
     * The page is never scaled as one big texture/matrix. Every coordinate is transformed
     * individually to an integer GUI coordinate. This avoids bilinear-looking blur at GUI
     * scales 3-5 while still fitting the entire book on screen.
     */
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

    private void fillD(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(sx(x1), sy(y1), sx(x2), sy(y2), color);
    }

    private float crispTextScale(float requested) {
        double guiScale = Math.max(1.0, Minecraft.getInstance().getWindow().getGuiScale());
        int physicalScale = Math.max(1, (int) Math.round(Math.max(0.05f, requested) * guiScale));
        return (float) (physicalScale / guiScale);
    }

    private void drawTextD(GuiGraphics graphics, Component text, int x, int y, int color) {
        float scale = crispTextScale(uiScale);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(sx(x), sy(y), 0);
        pose.scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, color, false);
        pose.popPose();
    }

    private void drawCenteredD(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        float scale = crispTextScale(uiScale);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(sx(centerX), sy(y), 0);
        pose.scale(scale, scale, 1f);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, false);
        pose.popPose();
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

    private void enableDesignScissor(GuiGraphics graphics, int x, int y, int w, int h) {
        int x1 = sx(x);
        int y1 = sy(y);
        int x2 = sx(x + w);
        int y2 = sy(y + h);
        graphics.enableScissor(x1, y1, Math.max(x1 + 1, x2), Math.max(y1 + 1, y2));
    }

    private void renderCategories(GuiGraphics graphics, int x, int y, int w) {
        int cy = y;
        for (int i = 0; i < CATEGORIES.length; i++) {
            Category category = CATEGORIES[i];
            boolean selected = i == categoryIndex;
            fillD(graphics, x - 4, cy - 4, x + w + 4, cy + 15, selected ? 0xFF5C4A3A : 0x00101010);
            drawTextD(graphics, Component.literal(category.name), x, cy, selected ? 0xFFFFE8B8 : 0xFFD7C4A5);
            addClickRegion(x - 4, cy - 4, w + 8, 19, "category", category.id);
            cy += 23;
            if (cy > y + PAGE_H - 72) break;
        }
    }

    private void renderElement(GuiGraphics graphics, VisualElement element, int pageX, int pageY) {
        int x = pageX + element.x;
        int y = pageY + element.y;
        int w = Math.max(1, element.width);
        int h = Math.max(1, element.height);
        switch (element.type) {
            case "TEXT" -> {
                enableDesignScissor(graphics, x, y, w, h);
                renderRichTextBlock(graphics, element.content, x, y, w, h, element.align);
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

    private void renderButton(GuiGraphics graphics, VisualElement element, int x, int y, int w, int h) {
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

        Component label = Component.literal(element.label == null ? "Button" : element.label);
        int available = Math.max(1, textRight - textLeft);
        float labelScale = crispTextScale(uiScale);
        int logicalLabelWidth = Math.max(1, Math.round(font.width(label) * labelScale / Math.max(0.05f, uiScale)));
        int center = textLeft + available / 2;
        if (logicalLabelWidth > available) center = textLeft + logicalLabelWidth / 2;
        drawCenteredD(graphics, label, center, y + Math.max(2, (h - 9) / 2), textColor);
    }

    private void drawButtonBorder(GuiGraphics graphics, int x, int y, int w, int h, int color, int thickness) {
        for (int i = 0; i < thickness; i++) {
            fillD(graphics, x + i, y + i, x + w - i, y + i + 1, color);
            fillD(graphics, x + i, y + h - i - 1, x + w - i, y + h - i, color);
            fillD(graphics, x + i, y + i, x + i + 1, y + h - i, color);
            fillD(graphics, x + w - i - 1, y + i, x + w - i, y + h - i, color);
        }
    }

    private static int parseHexColor(String value, int fallback) {
        try {
            if (value != null && value.matches("#[0-9a-fA-F]{6}")) return 0xFF000000 | Integer.parseInt(value.substring(1), 16);
        } catch (Exception ignored) { }
        return fallback;
    }

    private void renderImage(GuiGraphics graphics, String name, int x, int y, int w, int h) {
        if (name == null || name.isBlank()) return;
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("${modid}", "textures/screens/" + name + ".png");
        int rx = sx(x);
        int ry = sy(y);
        int rw = Math.max(1, sx(x + w) - rx);
        int rh = Math.max(1, sy(y + h) - ry);
        graphics.blit(texture, rx, ry, 0, 0, rw, rh, rw, rh);
    }

    private String currentFrame(VisualElement element) {
        if (element.frames.length == 0) return "";
        long frame = System.currentTimeMillis() / Math.max(20, element.frameDelay);
        return element.frames[(int) (frame % element.frames.length)];
    }


    private void renderResourceImage(GuiGraphics graphics, String name, String typeName, int x, int y, int w, int h) {
        if (name == null || name.isBlank()) return;
        String namespace = "${modid}";
        String pathName = name;
        int colon = name.indexOf(':');
        if (colon > 0) {
            namespace = name.substring(0, colon);
            pathName = name.substring(colon + 1);
        }
        String folder = switch (typeName == null ? "SCREEN" : typeName) {
            case "ITEM" -> "item";
            case "BLOCK" -> "block";
            default -> "screens";
        };
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(namespace, "textures/" + folder + "/" + pathName + ".png");
        int rx = sx(x);
        int ry = sy(y);
        int rw = Math.max(1, sx(x + w) - rx);
        int rh = Math.max(1, sy(y + h) - ry);
        graphics.blit(texture, rx, ry, 0, 0, rw, rh, rw, rh);
    }

    private void renderNavigation(GuiGraphics graphics, int pageX, int pageY) {
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
            "TRANSPARENT", "#FFF4D6", "#6D5237", "#6A5842", "", "SCREEN", "NONE");
    }

    private void renderNavigationArrow(GuiGraphics graphics, VisualElement element, int x, int y, int w, int h, boolean right) {
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
    private void paintNavigationGlyphD(GuiGraphics graphics, int x, int y, int w, int h, boolean right, String style, int color) {
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

    private void paintChevronStrokeScreen(GuiGraphics graphics, int cx, int cy, int radius, int thickness, boolean right, int color, int offset) {
        for (int i = 0; i <= radius; i++) {
            int px = (right ? cx - radius / 2 + i : cx + radius / 2 - i) + offset;
            graphics.fill(px, cy - radius + i, px + thickness, cy - radius + i + thickness, color);
            graphics.fill(px, cy + radius - i, px + thickness, cy + radius - i + thickness, color);
        }
    }

    private boolean canAdvance(int delta) {
        if (CATEGORIES.length == 0) return false;
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

    private void renderRichTextBlock(GuiGraphics graphics, String markup, int x, int y, int width, int height, String align) {
        // Wrapping uses logical book coordinates, while glyph placement uses a
        // screen-pixel pen. This avoids cumulative rounding when uiScale < 1:
        // two neighboring glyphs can never collapse onto the same GUI pixels.
        List<Glyph> glyphs = buildGlyphs(markup == null ? "" : markup);
        List<Line> lines = wrapGlyphs(glyphs, Math.max(1, width));

        int cy = y;
        for (Line current : lines) {
            if (cy >= y + height) break;
            int offset = 0;
            if ("CENTER".equals(align)) offset = Math.max(0, (width - current.width) / 2);
            else if ("RIGHT".equals(align)) offset = Math.max(0, width - current.width);

            int screenCx = sx(x + offset);
            int screenCy = sy(cy);
            for (Glyph glyph : current.glyphs) {
                if (cy + glyph.height > y + height) break;
                float screenScale = crispTextScale(glyph.scale * uiScale);
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(screenCx, screenCy, 0);
                pose.scale(screenScale, screenScale, 1f);
                graphics.drawString(font, glyph.component, 0, 0, 0xFF38291E, false);
                pose.popPose();

                int screenAdvance = glyphScreenAdvance(glyph.component, glyph.scale);
                int screenHeight = glyphScreenHeight(glyph.scale);
                if (glyph.actionType != null && !glyph.raw.isBlank()) {
                    addScreenClickRegion(screenCx, screenCy, screenAdvance, screenHeight, glyph.actionType, glyph.actionTarget);
                }
                screenCx += screenAdvance;
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
        int i = 0;
        while (i < glyphs.size()) {
            Glyph glyph = glyphs.get(i);
            if (glyph.newline) {
                lines.add(current);
                current = new Line();
                i++;
                continue;
            }
            if (glyph.raw.isBlank()) {
                // Do not carry leading spaces to a freshly wrapped line.
                if (!current.glyphs.isEmpty()) {
                    if (current.width + glyph.width > maxWidth) {
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
                lines.add(current);
                current = new Line();
            }
            if (wordWidth <= maxWidth) {
                for (Glyph part : word) current.add(part);
            } else {
                for (Glyph part : word) {
                    if (!current.glyphs.isEmpty() && current.width + part.width > maxWidth) {
                        lines.add(current);
                        current = new Line();
                    }
                    current.add(part);
                }
            }
            i = j;
        }
        lines.add(current);
        return lines;
    }

    private List<Glyph> buildGlyphs(String markup) {
        List<Glyph> result = new ArrayList<>();
        List<RichToken> tokens = parse(markup);
        for (RichToken token : tokens) {
            State state = token.state;
            float designScale = Math.max(0.05f, state.size / 9.0f);
            for (int i = 0; i < token.text.length(); i++) {
                char ch = token.text.charAt(i);
                if (ch == '\r') continue;
                if (ch == '\n') {
                    result.add(new Glyph("", Component.empty(), 1f, 0, 10, true, null, null));
                    continue;
                }
                String raw = String.valueOf(ch);
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
                result.add(new Glyph(raw, component, designScale, gw, gh, false, state.actionType, state.actionTarget));
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
                if (!stack.isEmpty()) state = stack.pop().previous;
            } else {
                stack.push(new StateFrame(tag, state.copy()));
                switch (tag) {
                    case "b" -> state.bold = true;
                    case "i" -> state.italic = true;
                    case "u" -> state.underline = true;
                    case "s" -> state.strike = true;
                    case "obf" -> state.obfuscated = true;
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
                    pageIndex = p;
                    return;
                }
            }
        }
    }

    private void advancePage(int delta) {
        if (CATEGORIES.length == 0) return;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = clickRegions.size() - 1; i >= 0; i--) {
                ClickRegion region = clickRegions.get(i);
                if (!region.contains(mouseX, mouseY)) continue;
                switch (region.type) {
                    case "category" -> selectCategoryById(region.target);
                    case "nav" -> advancePage("next".equals(region.target) ? 1 : -1);
                    case "page" -> selectPageById(region.target);
                    case "url" -> {
                        if (region.target.startsWith("https://") || region.target.startsWith("http://")) {
                            openExternalLinkWithWarning(region.target);
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record Category(String id, String name, Page[] pages) {}
    private record Page(String id, String title, boolean showTitle, VisualElement[] elements) {}
    private record VisualElement(String type, int x, int y, int width, int height, String content, String align,
                                 String mediaName, String[] frames, int frameDelay, String label, String targetCategoryId, String targetPageId,
                                 String buttonStyle, String buttonBackgroundColor, String buttonBorderColor, String buttonTextColor,
                                 String buttonImageName, String buttonImageType, String buttonImageMode) {}
    private record StateFrame(String tag, State previous) {}
    private record RichToken(String text, State state) {}
    private record Glyph(String raw, Component component, float scale, int width, int height, boolean newline,
                         String actionType, String actionTarget) {
        static Glyph newLineGlyph() { return new Glyph("", Component.empty(), 1f, 0, 10, true, null, null); }
    }
    private record ClickRegion(int x, int y, int width, int height, String type, String target) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }

    private static final class Line {
        final List<Glyph> glyphs = new ArrayList<>();
        int width;
        int height = 0;
        void add(Glyph glyph) {
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
            return copy;
        }
    }
}
