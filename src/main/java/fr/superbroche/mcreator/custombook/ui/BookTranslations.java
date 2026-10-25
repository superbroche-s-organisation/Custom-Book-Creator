package fr.superbroche.mcreator.custombook.ui;

import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/** Translations use MCreator's normal language resources; player-written values remain untouched. */
public final class BookTranslations {
    private BookTranslations() {}
    public static Map<String, String> entries(CustomBook book, String modid, String registryName) {
        String prefix = "custombook." + modid + "." + registryName + ".";
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(prefix + "title", book.getSafeBookTitle());
        for (var category : book.getBookCategories()) {
            entries.put(prefix + "category." + category.id, category.name);
            for (var page : category.pages) {
                entries.put(prefix + "page." + page.id, page.getTitleContent());
                for (var element : page.getBookElements()) {
                    if ("TEXT".equals(element.type)) entries.put(prefix + "element." + element.id, element.content);
                    if ("BUTTON".equals(element.type) || "INPUT".equals(element.type))
                        entries.put(prefix + "element." + element.id, element.label);
                }
            }
        }
        return entries;
    }

    /** Only page titles and body text use the rich-text renderer in game. */
    public static Set<String> richTextKeys(CustomBook book, String modid, String registryName) {
        String prefix = "custombook." + modid + "." + registryName + ".";
        Set<String> result = new HashSet<>();
        for (var category : book.getBookCategories()) for (var page : category.pages) {
            result.add(prefix + "page." + page.id);
            for (var element : page.getBookElements()) if ("TEXT".equals(element.type))
                result.add(prefix + "element." + element.id);
        }
        return result;
    }

    public static void show(Component parent, CustomBook book) {
        var workspace = book.getModElement().getWorkspace();
        var sources = entries(book, workspace.getWorkspaceSettings().getModID(), book.getModElement().getRegistryName());
        var richKeys = richTextKeys(book, workspace.getWorkspaceSettings().getModID(), book.getModElement().getRegistryName());
        java.util.List<String> keys = new ArrayList<>(sources.keySet());
        Map<String, Map<String, String>> drafts = new LinkedHashMap<>();
        Set<String> locales = new TreeSet<>(workspace.getLanguageMap().keySet());
        locales.addAll(java.util.List.of("en_us", "fr_fr", "de_de", "es_es", "it_it", "pt_br", "ja_jp", "zh_cn"));
        JComboBox<String> language = new JComboBox<>(locales.toArray(String[]::new));
        language.setEditable(true);
        language.setSelectedItem("en_us");
        JList<String> list = new JList<>(keys.toArray(String[]::new));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> owner, Object value, int index, boolean selected, boolean focused) {
                String plain = RichTextTools.parse(sources.get(String.valueOf(value))).stream()
                        .map(RichTextTools.Atom::display).collect(java.util.stream.Collectors.joining()).replace('\n', ' ');
                return super.getListCellRendererComponent(owner, plain.isBlank() ? "(empty text)" : plain, index, selected, focused);
            }
        });
        VisualRichTextEditor editor = new VisualRichTextEditor();
        VisualRichTextEditor original = new VisualRichTextEditor();
        original.setEditable(false);
        JTextField keyField = new JTextField();
        keyField.setEditable(false);
        String[] active = {null, null};
        JToolBar toolbar = new JToolBar(); toolbar.setFloatable(false);
        Runnable capture = () -> {
            if (active[0] != null && active[1] != null) drafts.get(active[0]).put(active[1],
                    richKeys.contains(active[1]) ? editor.getMarkup() : editor.getText());
        };
        Runnable load = () -> {
            capture.run();
            String locale = String.valueOf(language.getSelectedItem()).trim().toLowerCase(Locale.ROOT);
            String key = list.getSelectedValue();
            active[0] = locale; active[1] = key;
            drafts.computeIfAbsent(locale, ignored -> {
                Map<String, String> copy = new LinkedHashMap<>();
                var values = workspace.getLanguageMap().get(locale);
                for (String entry : keys) copy.put(entry, values == null ? "" : values.getOrDefault(entry, ""));
                return copy;
            });
            editor.setMarkup(key == null ? "" : drafts.get(locale).getOrDefault(key, ""), "LEFT");
            original.setMarkup(key == null ? "" : sources.get(key), "LEFT");
            editor.setEnabled(key != null);
            for (Component control : toolbar.getComponents()) control.setEnabled(key != null && richKeys.contains(key));
            keyField.setText(key == null ? "" : key);
        };
        language.addActionListener(event -> load.run());
        list.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) load.run(); });
        for (String style : java.util.List.of("b", "i", "u", "s")) {
            JButton button = new JButton(style.toUpperCase(Locale.ROOT)); button.setFocusable(false);
            button.addActionListener(event -> editor.toggleStyle(style)); toolbar.add(button);
        }
        JSpinner size = new JSpinner(new SpinnerNumberModel(12, 6, 48, 1));
        size.addChangeListener(event -> editor.applyStyle("size", size.getValue().toString(), false));
        toolbar.add(size);
        JButton copy = new JButton("Copy source");
        copy.addActionListener(event -> { String key = list.getSelectedValue(); if (key != null) editor.setMarkup(sources.get(key), "LEFT"); });
        JPanel tools = new JPanel(new BorderLayout());
        tools.add(toolbar, BorderLayout.CENTER); tools.add(copy, BorderLayout.EAST);
        JPanel right = new JPanel(new BorderLayout(5, 5));
        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(keyField, BorderLayout.NORTH); top.add(new JScrollPane(original), BorderLayout.CENTER);
        top.setPreferredSize(new Dimension(420, 100)); top.add(tools, BorderLayout.SOUTH);
        right.add(top, BorderLayout.NORTH); right.add(new JScrollPane(editor), BorderLayout.CENTER);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel header = new JPanel(new BorderLayout(6, 6));
        header.add(new JLabel("Minecraft language code:"), BorderLayout.WEST); header.add(language, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(list), right);
        split.setDividerLocation(220); panel.add(split, BorderLayout.CENTER);
        panel.add(new JLabel("<html>Translate the selected text on the right. Leave empty to use the source.<br>"
                + "Keys can also be edited in MCreator's Localization tab. Save the book and regenerate code.<br>"
                + "Player-written text, preset values and signatures are never translated.</html>"), BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(740, 460));
        list.setSelectedIndex(0);
        if (JOptionPane.showConfirmDialog(parent, panel, "Book translations", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        capture.run();
        for (var entry : drafts.entrySet()) {
            if (!entry.getKey().matches("[a-z]{2,3}_[a-z0-9]{2,4}")) {
                JOptionPane.showMessageDialog(parent, "Invalid language code: " + entry.getKey() + ". Use a code such as en_us or fr_fr.");
                return;
            }
        }
        for (var entry : drafts.entrySet()) {
            var values = new LinkedHashMap<String, String>();
            var existing = workspace.getLanguageMap().get(entry.getKey());
            if (existing != null) values.putAll(existing);
            entry.getValue().forEach((key, text) -> { if (text.isBlank()) values.remove(key); else values.put(key, text); });
            workspace.updateLanguage(entry.getKey(), values);
        }
        workspace.markDirty();
    }
}
