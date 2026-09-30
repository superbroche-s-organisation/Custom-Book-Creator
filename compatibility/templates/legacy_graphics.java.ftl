package ${package}.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Small plugin-owned bridge to the pre-GuiGraphics rendering API. */
public final class ${name}BookLegacyGraphics {
    private final PoseStack pose;
    public ${name}BookLegacyGraphics(PoseStack pose) { this.pose = pose; }
    public PoseStack pose() { return pose; }
    public void fill(int x1, int y1, int x2, int y2, int color) { GuiComponent.fill(pose, x1, y1, x2, y2, color); }
    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color); else font.draw(pose, text, x, y, color);
    }
    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) font.drawShadow(pose, text, x, y, color); else font.draw(pose, text, x, y, color);
    }
    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        font.draw(pose, text, x - font.width(text) / 2, y, color);
    }
    public void drawWordWrap(Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, Math.max(1, width))) {
            font.draw(pose, line, x, y, color);
            y += font.lineHeight + 1;
        }
    }
    public void enableScissor(int x1, int y1, int x2, int y2) {
        var window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int left = (int)Math.floor(x1 * scale), right = (int)Math.ceil(x2 * scale);
        int top = (int)Math.floor(y1 * scale), bottom = (int)Math.ceil(y2 * scale);
        RenderSystem.enableScissor(left, window.getHeight() - bottom, Math.max(0, right - left), Math.max(0, bottom - top));
    }
    public void disableScissor() { RenderSystem.disableScissor(); }
    public void blit(ResourceLocation texture, int x, int y, int u, int v, int w, int h, int tw, int th) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        GuiComponent.blit(pose, x, y, (float)u, (float)v, w, h, tw, th);
        RenderSystem.disableBlend();
    }
}
