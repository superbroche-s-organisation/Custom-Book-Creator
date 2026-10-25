    <#if data.hasWritingZones()>
    private ${package}.book.${name}BookWritingData.Field inlineField;
    private VisualElement inlineElement;
    private net.minecraft.client.gui.components.MultiLineEditBox inlineEditor;
    private net.minecraft.client.gui.components.Button inlineSave, inlineCancel, inlineChoice, inlineMode;
    private String inlineDraft = "";
    private boolean inlineChoices;
    private int inlineChoiceIndex = -1;

    private void beginInlineWriting(String key) {
        if (writing.pending || !writing.writable || inlineField != null) return;
        var field = ${package}.book.${name}BookWritingData.field(key);
        Page page = currentPage();
        if (field == null || page == null) return;
        for (VisualElement element : page.elements) {
            if ("INPUT".equals(element.type) && key.equals(element.content)) {
                inlineField = field;
                inlineElement = element;
                inlineDraft = writing.value(key);
                inlineChoices = "PRESET".equals(field.mode());
                inlineChoiceIndex = -1;
                createInlineWidgets();
                break;
            }
        }
    }

    private void createInlineWidgets() {
        if (inlineField == null) return;
        updateLayout();
        int x = sx(PAD + SIDEBAR_W + GAP_W + inlineElement.x);
        int y = sy(PAD + inlineElement.y);
        int w = Math.max(16, sd(inlineElement.width)), h = Math.max(16, sd(inlineElement.height));
        <#if cbcNewTextBox>
        inlineEditor = net.minecraft.client.gui.components.MultiLineEditBox.builder().setX(x).setY(y)
                .setPlaceholder(Component.literal("")).build(font, w, h, Component.literal(inlineField.label()));
        <#else>
        inlineEditor = new net.minecraft.client.gui.components.MultiLineEditBox(font, x, y, w, h,
                Component.literal(""), Component.literal(inlineField.label()));
        </#if>
        inlineEditor.setCharacterLimit(inlineField.limit());
        inlineEditor.setValue(inlineDraft);
        addRenderableWidget(inlineEditor);
        setFocused(inlineEditor);
        inlineEditor.setFocused(true);
        // Controls live below the right-hand page, outside the category sidebar.
        int left = sx(PAD + SIDEBAR_W + GAP_W), right = sx(PAD + SIDEBAR_W + GAP_W + PAGE_W / 2 + 2);
        int buttonWidth = Math.max(10, sd(PAGE_W / 2 - 2)), buttonHeight = Math.max(12, sd(19));
        inlineMode = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(""), button -> {
            inlineDraft = inlineEditor.getValue();
            inlineChoices = !inlineChoices;
            refreshInlineWidgets();
        }).bounds(left, sy(PAD + PAGE_H + 5), buttonWidth, buttonHeight).build());
        inlineChoice = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Next choice"), button -> {
            if (inlineField.choices().length == 0) return;
            inlineChoiceIndex = (inlineChoiceIndex + 1) % inlineField.choices().length;
            inlineDraft = inlineField.choices()[inlineChoiceIndex];
            inlineEditor.setValue(inlineDraft);
        }).bounds(right, sy(PAD + PAGE_H + 5), buttonWidth, buttonHeight).build());
        inlineSave = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Save"), button -> {
            inlineDraft = inlineEditor.getValue();
            writing.save(inlineField.key(), inlineDraft, null, success -> {
                if (success) { endInlineWriting(); writing.load(); }
                refreshInlineWidgets();
            });
            refreshInlineWidgets();
        }).bounds(left, sy(PAD + PAGE_H + 27), buttonWidth, buttonHeight).build());
        inlineCancel = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Cancel"), button -> cancelInlineWriting())
                .bounds(right, sy(PAD + PAGE_H + 27), buttonWidth, buttonHeight).build());
        refreshInlineWidgets();
    }

    private void refreshInlineWidgets() {
        if (inlineEditor == null) return;
        boolean enabled = writing.writable && !writing.pending;
        inlineEditor.active = enabled && !inlineChoices;
        if (!inlineEditor.active) inlineEditor.setFocused(false);
        inlineSave.active = enabled;
        inlineCancel.active = !writing.pending;
        inlineMode.active = enabled && "BOTH".equals(inlineField.mode());
        inlineMode.setMessage(Component.literal(inlineChoices ? "Free text" : "Choices"));
        inlineChoice.active = enabled && inlineChoices && inlineField.choices().length > 0;
    }

    private void endInlineWriting() {
        clearWidgets();
        setFocused(null);
        inlineField = null;
        inlineElement = null;
        inlineEditor = null;
        inlineSave = inlineCancel = inlineChoice = inlineMode = null;
        inlineDraft = "";
    }

    private void cancelInlineWriting() {
        if (writing.pending) return;
        if (inlineEditor != null && !inlineEditor.getValue().equals(writing.value(inlineField.key()))) {
            inlineDraft = inlineEditor.getValue();
            minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(discard -> {
                if (discard) endInlineWriting();
                minecraft.setScreen(this);
            }, Component.literal("Discard unsaved changes?"), Component.literal("These edits have not been saved.")));
        } else endInlineWriting();
    }

    private void navigateToCategory(String id) {
        if (writing.pending) return;
        Runnable navigate = () -> {
            if (inlineField != null) endInlineWriting();
            selectCategoryById(id);
            <#if data.hasPageSounds()>updatePageSound();</#if>
        };
        if (inlineField != null && inlineEditor != null && !inlineEditor.getValue().equals(writing.value(inlineField.key()))) {
            inlineDraft = inlineEditor.getValue();
            minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(discard -> {
                if (discard) navigate.run();
                minecraft.setScreen(this);
            }, Component.literal("Discard unsaved changes?"), Component.literal("These edits have not been saved.")));
        } else navigate.run();
    }

    private void renderInlineWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshInlineWidgets();
        if (inlineEditor == null) return;
        inlineEditor.render(graphics, mouseX, mouseY, partialTick);
        inlineSave.render(graphics, mouseX, mouseY, partialTick);
        inlineCancel.render(graphics, mouseX, mouseY, partialTick);
        inlineMode.render(graphics, mouseX, mouseY, partialTick);
        inlineChoice.render(graphics, mouseX, mouseY, partialTick);
        drawButtonBorder(graphics, PAD + SIDEBAR_W + GAP_W + inlineElement.x, PAD + inlineElement.y,
                inlineElement.width, inlineElement.height, 0xFF6D5237, 1);
    }
    </#if>

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        <#if data.hasWritingZones()>
        // While a writing zone is open, inventory keys are text, including E.
        if (inlineField != null) return super.keyPressed(key, scanCode, modifiers);
        </#if>
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() {
        <#if data.hasWritingZones()>
        if (inlineField != null) { cancelInlineWriting(); return; }
        </#if>
        super.onClose();
    }
