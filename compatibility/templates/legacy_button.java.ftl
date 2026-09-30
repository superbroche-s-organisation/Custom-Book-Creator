package ${package}.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ${name}BookLegacyButton {
    public static Builder builder(Component label, Button.OnPress action) { return new Builder(label, action); }
    public static final class Builder {
        private final Component label;
        private final Button.OnPress action;
        private int x, y, w, h;
        private Builder(Component label, Button.OnPress action) { this.label = label; this.action = action; }
        public Builder bounds(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h; return this;
        }
        public Button build() { return new Button(x, y, w, h, label, action); }
    }
    private ${name}BookLegacyButton() {}
}
