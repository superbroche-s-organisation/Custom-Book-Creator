package ${package}.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ${package}.book.${name}BookWritingData;
import ${package}.client.${name}BookWritingClient;

/** A multiline editor using Minecraft's native selection, clipboard, cursor and scrolling controls. */
public final class ${name}BookWritingScreen extends Screen {
    private final ${name}BookScreen parent;
    private final ${name}BookWritingClient session;
    private final ${name}BookWritingData.Field field;
    private MultiLineEditBox editor;
    private EditBox signatureTitle;
    private Button save, choice, mode;
    private boolean choicesMode;
    private int choiceIndex = -1;
    private String draft, signatureDraft = "";
    private boolean initialized;
    private int left, top, boxWidth, boxHeight;

    public ${name}BookWritingScreen(${name}BookScreen parent, ${name}BookWritingClient session, String key) {
        super(Component.literal(key == null ? "Sign book permanently" : "Edit writing zone"));
        this.parent = parent;
        this.session = session;
        this.field = key == null ? null : ${name}BookWritingData.field(key);
        this.draft = field == null ? "" : session.value(field.key());
        this.choicesMode = field != null && "PRESET".equals(field.mode());
    }

    @Override protected void init() {
        if (initialized) capture();
        initialized = true;
        boxWidth = Math.max(100, Math.min(380, width - 24));
        boxHeight = Math.max(48, Math.min(160, height - 148));
        left = (width - boxWidth) / 2;
        top = Math.max(30, (height - (boxHeight + 130)) / 2);
        if (field == null) {
            signatureTitle = new EditBox(font, left, top + 30, boxWidth, 20, Component.literal("Signed title"));
            signatureTitle.setMaxLength(32);
            signatureTitle.setValue(signatureDraft);
            addRenderableWidget(signatureTitle);
            setInitialFocus(signatureTitle);
        } else {
            <#if cbcNewTextBox>
            editor = MultiLineEditBox.builder().setX(left).setY(top + 24)
                    .setPlaceholder(Component.literal("Write here...")).build(font, boxWidth, boxHeight, Component.literal(field.label()));
            <#else>
            editor = new MultiLineEditBox(font, left, top + 24, boxWidth, boxHeight,
                    Component.literal("Write here..."), Component.literal(field.label()));
            </#if>
            editor.setCharacterLimit(field.limit());
            editor.setValue(draft);
            addRenderableWidget(editor);
            setInitialFocus(editor);
            int row = top + boxHeight + 30;
            mode = addRenderableWidget(Button.builder(Component.literal(choicesMode ? "Use free text" : "Use choices"), button -> {
                capture();
                choicesMode = !choicesMode;
                editor.setValue(draft);
                refreshControls();
            }).bounds(left, row, Math.max(90, boxWidth / 2 - 3), 20).build());
            choice = addRenderableWidget(Button.builder(Component.literal("Next choice"), button -> {
                if (field.choices().length == 0) return;
                choiceIndex = (choiceIndex + 1) % field.choices().length;
                draft = field.choices()[choiceIndex];
                editor.setValue(draft);
            }).bounds(left + boxWidth / 2 + 3, row, boxWidth / 2 - 3, 20).build());
        }
        int bottom = field == null ? top + 90 : top + boxHeight + 58;
        save = addRenderableWidget(Button.builder(Component.literal(field == null ? "Sign permanently" : "Save"), button -> submit())
                .bounds(left, bottom, boxWidth / 2 - 3, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(left + boxWidth / 2 + 3, bottom, boxWidth / 2 - 3, 20).build());
        refreshControls();
    }

    private void capture() {
        if (editor != null) draft = editor.getValue();
        if (signatureTitle != null) signatureDraft = signatureTitle.getValue();
    }

    private void refreshControls() {
        boolean canWrite = session.writable && !session.pending;
        if (save != null) save.active = canWrite;
        if (editor != null) {
            editor.active = canWrite && !choicesMode;
            if (!editor.active) editor.setFocused(false);
        }
        if (signatureTitle != null) signatureTitle.setEditable(canWrite);
        if (mode != null) {
            mode.active = canWrite && "BOTH".equals(field.mode());
            mode.setMessage(Component.literal(choicesMode ? "Use free text" : "Use choices"));
        }
        if (choice != null) choice.active = canWrite && choicesMode && field.choices().length > 0;
    }

    private void submit() {
        capture();
        if (field == null) {
            String value = signatureDraft.trim();
            if (value.isEmpty()) { session.status = "Enter a title before signing."; return; }
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                minecraft.setScreen(this);
                if (confirmed) saveNow(value);
            }, Component.literal("Sign this copy permanently?"), Component.literal("All writing zones will be locked. This cannot be undone.")));
        } else saveNow(null);
    }

    private void saveNow(String signedTitle) {
        session.save(field == null ? null : field.key(), draft, signedTitle, success -> {
            if (success && minecraft.screen == this) minecraft.setScreen(parent);
        });
    }

    @Override public void tick() {
        session.tick();
        refreshControls();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xEF201B17);
        graphics.drawCenteredString(font, field == null ? title : Component.literal(field.label()), width / 2, top, 0xFFFFE5B8);
        if (field == null) graphics.drawString(font, "Signature title", left, top + 17, 0xFFFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        int statusY = field == null ? top + 118 : top + boxHeight + 84;
        graphics.drawWordWrap(font, Component.literal(session.status), left, statusY, boxWidth, 0xFFFFE5B8);
    }

    @Override public void onClose() {
        if (session.pending) { session.status = "Waiting for server confirmation..."; return; }
        capture();
        if (field != null && !draft.equals(session.value(field.key())) || field == null && !signatureDraft.isBlank()) {
            minecraft.setScreen(new ConfirmScreen(discard -> minecraft.setScreen(discard ? parent : this),
                    Component.literal("Discard unsaved changes?"), Component.literal("These edits have not been saved.")));
        } else minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}
