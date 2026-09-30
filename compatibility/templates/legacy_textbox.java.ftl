package ${package}.client.gui;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import ${package}.book.${name}BookWritingData;

/** Multiline editor for 1.17/1.18, using vanilla's text-selection and clipboard helper. */
public final class ${name}BookLegacyTextBox extends AbstractWidget {
    private final Font font;
    private final TextFieldHelper input;
    private String value = "";
    private int limit = 1024, scroll;
    private final Component placeholder;
    private record Line(int start, int end) {}

    public ${name}BookLegacyTextBox(Font font, int x, int y, int width, int height, Component placeholder, Component label) {
        super(x, y, width, height, label);
        this.font = font;
        this.placeholder = placeholder;
        Minecraft minecraft = Minecraft.getInstance();
        input = new TextFieldHelper(() -> value, text -> value = text,
                TextFieldHelper.createClipboardGetter(minecraft), TextFieldHelper.createClipboardSetter(minecraft),
                text -> ${name}BookWritingData.validText(text, limit));
    }
    public void setCharacterLimit(int limit) { this.limit = Math.max(1, Math.min(1024, limit)); }
    public String getValue() { return value; }
    public void setValue(String value) {
        this.value = value == null ? "" : value;
        if (this.value.length() > limit) {
            int end = limit;
            if (Character.isHighSurrogate(this.value.charAt(end - 1))) end--;
            this.value = this.value.substring(0, end);
        }
        input.setCursorToEnd();
        ensureCursorVisible();
    }
    @Override public void setFocused(boolean focused) { super.setFocused(focused); }

    private List<Line> lines() {
        List<Line> result = new ArrayList<>();
        int start = 0;
        while (start < value.length()) {
            int newline = value.indexOf('\n', start);
            if (newline < 0) newline = value.length();
            if (newline == start) { result.add(new Line(start, start)); start++; continue; }
            String remaining = value.substring(start, newline);
            int length = font.getSplitter().plainIndexAtWidth(remaining, Math.max(1, width - 12), Style.EMPTY);
            if (length <= 0) length = Character.charCount(value.codePointAt(start));
            int end = Math.min(newline, start + length);
            result.add(new Line(start, end));
            start = end == newline && newline < value.length() ? end + 1 : end;
        }
        if (result.isEmpty() || value.endsWith("\n")) result.add(new Line(value.length(), value.length()));
        return result;
    }
    private int rowAt(List<Line> rows, int position) {
        int row = 0;
        while (row + 1 < rows.size() && rows.get(row + 1).start <= position) row++;
        return row;
    }
    private int rowHeight() { return font.lineHeight + 2; }
    private int visibleRows() { return Math.max(1, (height - 8) / rowHeight()); }
    private void ensureCursorVisible() {
        List<Line> rows = lines();
        int cursor = rowAt(rows, input.getCursorPos());
        if (cursor < scroll) scroll = cursor;
        if (cursor >= scroll + visibleRows()) scroll = cursor - visibleRows() + 1;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visibleRows())));
    }
    private int indexAt(Line line, int pixelX) {
        String text = value.substring(line.start, line.end);
        return line.start + font.getSplitter().plainIndexAtWidth(text, Math.max(0, pixelX), Style.EMPTY);
    }
    private void moveVertically(int delta) {
        List<Line> rows = lines();
        int current = rowAt(rows, input.getCursorPos());
        Line line = rows.get(current);
        int column = font.width(value.substring(line.start, Math.min(line.end, input.getCursorPos())));
        Line target = rows.get(Math.max(0, Math.min(rows.size() - 1, current + delta)));
        input.setCursorPos(indexAt(target, column), Screen.hasShiftDown());
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!active || !isFocused()) return false;
        boolean handled = true;
        if (key == 257 || key == 335) input.insertText("\n");
        else if (key == 265) moveVertically(-1);
        else if (key == 264) moveVertically(1);
        else if (key == 266) moveVertically(-visibleRows());
        else if (key == 267) moveVertically(visibleRows());
        else if ((key == 268 || key == 269) && !Screen.hasControlDown()) {
            List<Line> rows = lines();
            Line row = rows.get(rowAt(rows, input.getCursorPos()));
            input.setCursorPos(key == 268 ? row.start : row.end, Screen.hasShiftDown());
        } else handled = input.keyPressed(key);
        if (handled) ensureCursorVisible();
        return handled;
    }
    @Override public boolean charTyped(char character, int modifiers) {
        if (!active || !isFocused()) return false;
        boolean handled = input.charTyped(character);
        if (handled) ensureCursorVisible();
        return handled;
    }
    private void clickAt(double mouseX, double mouseY, boolean selecting) {
        List<Line> rows = lines();
        int row = Math.max(0, Math.min(rows.size() - 1, scroll + (int)Math.floor((mouseY - y - 4) / rowHeight())));
        input.setCursorPos(indexAt(rows.get(row), (int)mouseX - x - 5), selecting);
        ensureCursorVisible();
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = active && visible && button == 0 && isMouseOver(mouseX, mouseY);
        setFocused(inside);
        if (inside) clickAt(mouseX, mouseY, Screen.hasShiftDown());
        return inside;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!active || !isFocused() || button != 0) return false;
        clickAt(mouseX, mouseY, true);
        return true;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return false;
        scroll = Math.max(0, Math.min(Math.max(0, lines().size() - visibleRows()), scroll - (int)Math.signum(amount) * 3));
        return true;
    }
    @Override public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        var graphics = new ${name}BookLegacyGraphics(pose);
        graphics.fill(x, y, x + width, y + height, isFocused() ? 0xFFE0CBA2 : 0xFF8A7755);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF211E18);
        graphics.enableScissor(x + 3, y + 3, x + width - 3, y + height - 3);
        try {
            List<Line> rows = lines();
            int lo = Math.min(input.getCursorPos(), input.getSelectionPos());
            int hi = Math.max(input.getCursorPos(), input.getSelectionPos());
            int cursorRow = rowAt(rows, input.getCursorPos());
            for (int i = scroll; i < Math.min(rows.size(), scroll + visibleRows()); i++) {
                Line row = rows.get(i);
                int lineY = y + 4 + (i - scroll) * rowHeight();
                if (lo < row.end && hi > row.start) {
                    int a = font.width(value.substring(row.start, Math.max(row.start, lo)));
                    int end = Math.min(row.end, hi);
                    int z = font.width(value.substring(row.start, end));
                    graphics.fill(x + 5 + a, lineY - 1, x + 5 + Math.max(a + 2, z), lineY + rowHeight() - 1, 0xFF345A87);
                }
                font.draw(pose, value.substring(row.start, row.end), x + 5, lineY, 0xFFF4E9CD);
                if (active && isFocused() && i == cursorRow && (System.currentTimeMillis() / 500) % 2 == 0) {
                    int cx = x + 5 + font.width(value.substring(row.start, Math.min(row.end, input.getCursorPos())));
                    graphics.fill(cx, lineY, cx + 1, lineY + font.lineHeight, 0xFFFFFFFF);
                }
            }
            if (value.isEmpty() && !isFocused()) font.draw(pose, placeholder, x + 5, y + 4, 0xFF999999);
        } finally { graphics.disableScissor(); }
    }
    @Override public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        output.add(NarratedElementType.HINT, new TextComponent(value));
    }
}
