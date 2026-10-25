package fr.superbroche.mcreator.custombook.ui;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;

/** Visual text editing; markup is only an on-disk/game format, never the editing surface. */
public final class VisualRichTextEditor extends JTextPane {
    public static final int MAX_MARKUP_LENGTH = 32767;
    private static final String ATTRIBUTE = "custombook.style.";
    private static final List<String> STYLES = List.of("size", "color", "b", "i", "u", "s", "obf", "url", "page");
    private static final List<String> TOGGLES = List.of("b", "i", "u", "s", "obf");
    private static final DataFlavor MARKUP_FLAVOR = new DataFlavor("application/x-custombook-markup;class=java.lang.String", "Custom Book formatted text");
    private static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html;class=java.lang.String", "HTML text");
    private final UndoManager undo = new UndoManager();
    private CompoundEdit compound;
    private int compoundEdits;
    private int transactionDepth;
    private boolean silent;
    private boolean lengthChecked;
    private boolean changed;
    private int defaultSize = 12;
    private Color defaultColor = new Color(0x241B13);
    private Color displayColor;
    private String defaultAlignment = "LEFT";
    private Runnable onChange = () -> {};
    private int typingStylePosition = -1;
    private AttributeSet typingStyle;

    public VisualRichTextEditor() {
        super(new DefaultStyledDocument());
        ((AbstractDocument)getDocument()).setDocumentFilter(new LengthFilter());
        getDocument().addUndoableEditListener(event -> {
            if (silent) return;
            if (compound != null) { compound.addEdit(event.getEdit()); compoundEdits++; }
            else undo.addEdit(event.getEdit());
        });
        getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { documentChanged(); }
            public void removeUpdate(DocumentEvent event) { documentChanged(); }
            public void changedUpdate(DocumentEvent event) { documentChanged(); }
        });
        setTransferHandler(new RichTransferHandler());
        addCaretListener(event -> {
            if (event.getDot() != typingStylePosition || event.getMark() != event.getDot()) {
                typingStylePosition = -1;
                typingStyle = null;
            }
        });
        bind(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, "custombook-undo", this::undo);
        bind(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK, "custombook-redo", this::redo);
        bind(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, "custombook-redo-shift", this::redo);
        setMarkup("", "LEFT");
    }

    private void bind(int key, int modifiers, String name, Runnable action) {
        getInputMap().put(KeyStroke.getKeyStroke(key, modifiers), name);
        getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    public void setOnChange(Runnable action) { onChange = action == null ? () -> {} : action; }

    /** View-only color override; original color tags remain in the stored document styles. */
    public void setDisplayColor(Color color) {
        displayColor = color;
        if (color != null) {
            setForeground(color); setCaretColor(color); setSelectedTextColor(color); setDisabledTextColor(color);
        }
        boolean previous = silent;
        silent = true;
        try {
            StyledDocument document = getStyledDocument();
            for (int start = 0; start < document.getLength();) {
                Element run = document.getCharacterElement(start);
                int end = Math.min(document.getLength(), run.getEndOffset());
                document.setCharacterAttributes(start, end - start, attributes(styles(run.getAttributes())), true);
                start = end;
            }
            getInputAttributes().addAttributes(attributes(styles(getInputAttributes())));
        } finally { silent = previous; }
        repaint();
    }

    /** Sets defaults for the next document loaded by setMarkup; does not modify the current document. */
    public void setDefaultStyle(int size, Color color) {
        defaultSize = Math.max(6, Math.min(48, size));
        defaultColor = color == null ? new Color(0x241B13) : color;
    }

    public void setDefaultStyle(int size, String color) {
        setDefaultStyle(size, decodeColor(color, new Color(0x241B13)));
    }

    /** Loading an element is deliberately silent and clears undo history from the previous element. */
    public void setMarkup(String markup, String alignment) {
        typingStylePosition = -1;
        typingStyle = null;
        String source = normalizeLines(markup == null ? "" : markup);
        if (source.length() > MAX_MARKUP_LENGTH) throw new IllegalArgumentException("Text exceeds the book text limit");
        defaultAlignment = normalizedAlignment(alignment);
        silent = true;
        try {
            StyledDocument document = getStyledDocument();
            document.remove(0, document.getLength());
            insertMarkup(document, 0, source);
            if (document.getLength() == 0) setParagraph(document, 0, 0, defaultAlignment, false);
            setCaretPosition(0);
            MutableAttributeSet input = getInputAttributes();
            input.removeAttributes(input);
            input.addAttributes(document.getLength() == 0 ? attributes(Map.of())
                    : document.getCharacterElement(0).getAttributes());
            undo.discardAllEdits();
            changed = false;
        } catch (BadLocationException impossible) { throw new IllegalStateException(impossible); }
        finally { silent = false; }
    }

    public String getMarkup() { return serialize(getStyledDocument(), 0, getDocument().getLength()); }

    /** Returns the first selected character's style, or the current typing style at an empty selection. */
    public Map<String, String> getSelectionStyles() {
        int position = getSelectionStart();
        boolean caretOnly = getSelectionStart() == getSelectionEnd();
        // External caret listeners may run before StyledEditorKit refreshes input attributes.
        // Read the actual adjacent character unless a toolbar explicitly set a typing style.
        AttributeSet attrs = caretOnly && typingStylePosition == position && typingStyle != null ? typingStyle
                : getStyledDocument().getCharacterElement(caretOnly ? Math.max(0, position - 1) : position).getAttributes();
        Map<String, String> result = styles(attrs);
        result.putIfAbsent("size", Integer.toString(defaultSize));
        result.putIfAbsent("color", colorHex(defaultColor));
        result.put("align", paragraphAlignment(getStyledDocument().getParagraphElement(position).getAttributes()));
        return result;
    }

    public void toggleStyle(String key) {
        if (!TOGGLES.contains(key)) throw new IllegalArgumentException("Not a toggle style: " + key);
        boolean enabled = true;
        int from = getSelectionStart(), to = getSelectionEnd();
        if (from == to) enabled = getSelectionStyles().containsKey(key);
        else {
            for (int i = from; i < to; i++) {
                if (!styles(getStyledDocument().getCharacterElement(i).getAttributes()).containsKey(key)) {
                    enabled = false;
                    break;
                }
            }
        }
        applyStyle(key, enabled ? null : "", false);
    }

    /** A null value clears a character style. An empty selection affects future typing only. */
    public void applyStyle(String key, String value, boolean paragraphs) {
        if (!STYLES.contains(key) && !"align".equals(key)) throw new IllegalArgumentException("Unknown style: " + key);
        int from = getSelectionStart(), to = getSelectionEnd();
        if ("align".equals(key) || paragraphs) {
            if (!"align".equals(key)) throw new IllegalArgumentException("Only alignment is a paragraph style");
            String alignment = normalizedAlignment(value);
            StyledDocument candidate = copyDocument();
            setParagraph(candidate, from, to - from, alignment, true);
            checkLength(candidate);
            transaction(() -> setParagraph(getStyledDocument(), from, to - from, alignment, true));
        } else if (from == to) {
            Map<String, String> current = getSelectionStyles();
            updateStyle(current, key, value);
            setCharacterAttributes(attributes(current), true);
            typingStylePosition = from;
            typingStyle = getInputAttributes().copyAttributes();
        } else {
            typingStylePosition = -1;
            typingStyle = null;
            StyledDocument candidate = copyDocument();
            styleCharacters(candidate, from, to, key, value);
            checkLength(candidate);
            transaction(() -> styleCharacters(getStyledDocument(), from, to, key, value));
        }
        select(from, to);
    }

    @Override public void replaceSelection(String text) {
        if (undo == null) { super.replaceSelection(text); return; }
        transaction(() -> super.replaceSelection(normalizeLines(text == null ? "" : text)));
    }

    /** Inserts formatted clipboard data as visible text, preserving surrounding formatting. */
    public void replaceMarkupSelection(String markup) {
        String source = normalizeLines(markup == null ? "" : markup);
        int start = getSelectionStart(), end = getSelectionEnd();
        StyledDocument candidate = copyDocument();
        try {
            candidate.remove(start, end - start);
            int count = insertMarkup(candidate, start, source);
            checkLength(candidate);
            transaction(() -> {
                boolean wasChecked = lengthChecked;
                // Length was checked against the entire final document, not intermediate fragments.
                try {
                    lengthChecked = true;
                    getStyledDocument().remove(start, end - start);
                    insertMarkup(getStyledDocument(), start, source);
                } catch (BadLocationException impossible) { throw new IllegalStateException(impossible); }
                finally { lengthChecked = wasChecked; }
                changed = true;
                setCaretPosition(start + count);
            });
        } catch (BadLocationException impossible) { throw new IllegalStateException(impossible); }
    }

    public void undo() { if (undo.canUndo()) transaction(undo::undo); }
    public void redo() { if (undo.canRedo()) transaction(undo::redo); }
    public boolean canUndo() { return undo.canUndo(); }
    public boolean canRedo() { return undo.canRedo(); }

    private void transaction(Runnable action) {
        boolean outer = transactionDepth++ == 0;
        if (outer) { compound = new CompoundEdit(); compoundEdits = 0; }
        try { action.run(); }
        finally {
            transactionDepth--;
            if (outer) {
                compound.end();
                if (compoundEdits > 0) undo.addEdit(compound);
                compound = null;
                if (changed && !silent) { changed = false; onChange.run(); }
            }
        }
    }

    private void documentChanged() {
        if (silent) return;
        if (transactionDepth > 0) changed = true;
        else onChange.run();
    }

    private int insertMarkup(StyledDocument document, int offset, String markup) throws BadLocationException {
        List<RichTextTools.Atom> atoms = RichTextTools.parse(markup);
        int position = offset;
        boolean paragraphStart = true;
        for (int i = 0; i < atoms.size();) {
            RichTextTools.Atom atom = atoms.get(i);
            if (paragraphStart && (position == document.getParagraphElement(position).getStartOffset()
                    || atom.styles().containsKey("align"))) {
                String alignment = atom.styles().getOrDefault("align", defaultAlignment);
                setParagraph(document, position, 0, alignment, atom.styles().containsKey("align"));
            }
            int end = i + 1;
            StringBuilder run = new StringBuilder(atom.display());
            if (!"\n".equals(atom.display())) {
                while (end < atoms.size() && !"\n".equals(atoms.get(end).display())
                        && atoms.get(end).styles().equals(atom.styles())) run.append(atoms.get(end++).display());
            }
            document.insertString(position, run.toString(), attributes(atom.styles()));
            position += run.length();
            paragraphStart = "\n".equals(atom.display());
            i = end;
        }
        return position - offset;
    }

    private void styleCharacters(StyledDocument document, int from, int to, String key, String value) {
        for (int position = from; position < to;) {
            Element run = document.getCharacterElement(position);
            int end = Math.min(to, run.getEndOffset());
            Map<String, String> current = styles(run.getAttributes());
            updateStyle(current, key, value);
            document.setCharacterAttributes(position, end - position, attributes(current), true);
            position = end;
        }
    }

    private void updateStyle(Map<String, String> current, String key, String value) {
        if (value == null) { current.remove(key); return; }
        if ("size".equals(key)) {
            try { value = Integer.toString(Math.max(6, Math.min(48, Integer.parseInt(value)))); }
            catch (NumberFormatException ignored) { value = Integer.toString(defaultSize); }
        } else if ("color".equals(key)) value = colorHex(decodeColor(value, defaultColor));
        else if (("url".equals(key) || "page".equals(key)) && (value.contains("[") || value.contains("]")))
            throw new IllegalArgumentException("Links cannot contain markup delimiters");
        current.put(key, TOGGLES.contains(key) ? "" : value);
    }

    private SimpleAttributeSet attributes(Map<String, String> styles) {
        SimpleAttributeSet result = new SimpleAttributeSet();
        for (String key : STYLES) if (styles.containsKey(key)) result.addAttribute(ATTRIBUTE + key, styles.get(key));
        StyleConstants.setFontFamily(result, styles.containsKey("obf") ? "Monospaced" : getFont().getFamily());
        int size = defaultSize;
        try { size = Integer.parseInt(styles.getOrDefault("size", Integer.toString(defaultSize))); }
        catch (NumberFormatException ignored) { }
        StyleConstants.setFontSize(result, size);
        StyleConstants.setForeground(result, displayColor == null ? decodeColor(styles.get("color"), defaultColor) : displayColor);
        StyleConstants.setBold(result, styles.containsKey("b"));
        StyleConstants.setItalic(result, styles.containsKey("i"));
        StyleConstants.setUnderline(result, styles.containsKey("u") || styles.containsKey("url") || styles.containsKey("page"));
        StyleConstants.setStrikeThrough(result, styles.containsKey("s"));
        if (styles.containsKey("obf")) StyleConstants.setBackground(result, new Color(displayColor == null ? 0xDDDDDD : 0x555555));
        return result;
    }

    private Map<String, String> styles(AttributeSet attrs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : STYLES) {
            Object value = attrs.getAttribute(ATTRIBUTE + key);
            if (value != null) result.put(key, value.toString());
        }
        int size = StyleConstants.getFontSize(attrs);
        if (attrs.isDefined(StyleConstants.FontSize) && size != defaultSize) result.put("size", Integer.toString(size));
        if (attrs.isDefined(StyleConstants.Foreground) && !StyleConstants.getForeground(attrs).equals(defaultColor))
            result.put("color", colorHex(StyleConstants.getForeground(attrs)));
        nativeToggle(result, "b", StyleConstants.isBold(attrs));
        nativeToggle(result, "i", StyleConstants.isItalic(attrs));
        nativeToggle(result, "s", StyleConstants.isStrikeThrough(attrs));
        boolean link = result.containsKey("url") || result.containsKey("page");
        nativeToggle(result, "u", StyleConstants.isUnderline(attrs) && (!link || result.containsKey("u")));
        return result;
    }

    private static void nativeToggle(Map<String, String> map, String key, boolean enabled) {
        if (enabled) map.put(key, ""); else map.remove(key);
    }

    private void setParagraph(StyledDocument document, int from, int length, String alignment, boolean explicit) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, alignmentConstant(alignment));
        attributes.addAttribute(ATTRIBUTE + "align", explicit ? alignment : "");
        document.setParagraphAttributes(from, length, attributes, true);
    }

    private String paragraphAlignment(AttributeSet attrs) {
        return switch (StyleConstants.getAlignment(attrs)) {
            case StyleConstants.ALIGN_CENTER -> "CENTER";
            case StyleConstants.ALIGN_RIGHT -> "RIGHT";
            default -> "LEFT";
        };
    }

    private String serialize(StyledDocument document, int start, int end) {
        return serialize(document, start, end, false);
    }

    private String serialize(StyledDocument document, int start, int end, boolean clipboardDefaults) {
        try {
            String text = document.getText(start, end - start);
            List<RichTextTools.Atom> atoms = new ArrayList<>();
            for (int index = 0; index < text.length();) {
                int next = index + Character.charCount(text.codePointAt(index));
                Map<String, String> style = styles(document.getCharacterElement(start + index).getAttributes());
                if (clipboardDefaults) {
                    style.putIfAbsent("size", Integer.toString(defaultSize));
                    style.putIfAbsent("color", colorHex(defaultColor));
                }
                AttributeSet paragraph = document.getParagraphElement(start + index).getAttributes();
                String alignment = paragraphAlignment(paragraph);
                Object explicit = paragraph.getAttribute(ATTRIBUTE + "align");
                if (clipboardDefaults || (explicit != null && !explicit.toString().isEmpty()) || !alignment.equals(defaultAlignment))
                    style.put("align", alignment);
                String character = text.substring(index, next);
                atoms.add(new RichTextTools.Atom(character, character, style, index, next));
                index = next;
            }
            return RichTextTools.serializeMarkup(atoms);
        } catch (BadLocationException impossible) { throw new IllegalStateException(impossible); }
    }

    private StyledDocument copyDocument() {
        DefaultStyledDocument copy = new DefaultStyledDocument();
        StyledDocument current = getStyledDocument();
        try {
            for (int position = 0; position < current.getLength();) {
                Element run = current.getCharacterElement(position);
                int end = Math.min(current.getLength(), run.getEndOffset());
                copy.insertString(position, current.getText(position, end - position), run.getAttributes());
                position = end;
            }
            for (int position = 0; position <= current.getLength();) {
                Element paragraph = current.getParagraphElement(position);
                int end = Math.min(current.getLength(), paragraph.getEndOffset());
                copy.setParagraphAttributes(position, end - position, paragraph.getAttributes(), true);
                if (end >= current.getLength()) break;
                position = end;
            }
            return copy;
        } catch (BadLocationException impossible) { throw new IllegalStateException(impossible); }
    }

    private void checkLength(StyledDocument document) {
        if (serialize(document, 0, document.getLength()).length() > MAX_MARKUP_LENGTH)
            throw new IllegalArgumentException("Formatting would exceed the book text limit");
    }

    private final class LengthFilter extends DocumentFilter {
        @Override public void insertString(FilterBypass bypass, int offset, String string, AttributeSet attrs) throws BadLocationException {
            replace(bypass, offset, 0, string, attrs);
        }
        @Override public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String normalized = normalizeLines(text == null ? "" : text);
            if (!silent && !lengthChecked) {
                StyledDocument candidate = copyDocument();
                candidate.remove(offset, length);
                candidate.insertString(offset, normalized, attrs);
                try { checkLength(candidate); }
                catch (IllegalArgumentException tooLong) { Toolkit.getDefaultToolkit().beep(); return; }
            }
            bypass.replace(offset, length, normalized, attrs);
        }
    }

    private final class RichTransferHandler extends TransferHandler {
        @Override public int getSourceActions(JComponent component) { return COPY_OR_MOVE; }
        @Override protected Transferable createTransferable(JComponent component) {
            int from = getSelectionStart(), to = getSelectionEnd();
            String markup = serialize(getStyledDocument(), from, to, true);
            String plain = getSelectedText();
            return new Transferable() {
                public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] {MARKUP_FLAVOR, DataFlavor.stringFlavor}; }
                public boolean isDataFlavorSupported(DataFlavor flavor) { return flavor.equals(MARKUP_FLAVOR) || flavor.equals(DataFlavor.stringFlavor); }
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (flavor.equals(MARKUP_FLAVOR)) return markup;
                    if (flavor.equals(DataFlavor.stringFlavor)) return plain == null ? "" : plain;
                    throw new UnsupportedFlavorException(flavor);
                }
            };
        }
        @Override protected void exportDone(JComponent component, Transferable data, int action) {
            if (action == MOVE) replaceSelection("");
        }
        @Override public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(MARKUP_FLAVOR) || support.isDataFlavorSupported(HTML_FLAVOR)
                    || support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }
        @Override public boolean importData(TransferSupport support) { return importData((JComponent)support.getComponent(), support.getTransferable()); }
        @Override public boolean importData(JComponent component, Transferable data) {
            if (!isEditable() || !isEnabled()) return false;
            try {
                if (data.isDataFlavorSupported(MARKUP_FLAVOR)) replaceMarkupSelection((String)data.getTransferData(MARKUP_FLAVOR));
                else if (data.isDataFlavorSupported(HTML_FLAVOR)) replaceMarkupSelection(htmlMarkup((String)data.getTransferData(HTML_FLAVOR)));
                else if (data.isDataFlavorSupported(DataFlavor.stringFlavor)) replaceSelection((String)data.getTransferData(DataFlavor.stringFlavor));
                else return false;
                return true;
            } catch (UnsupportedFlavorException | IOException | IllegalArgumentException failure) {
                Toolkit.getDefaultToolkit().beep();
                return false;
            }
        }
    }

    /** Import common HTML text styles, without loading images or executing external content. */
    private String htmlMarkup(String html) throws IOException {
        List<RichTextTools.Atom> atoms = new ArrayList<>();
        Deque<Map<String, String>> stack = new ArrayDeque<>();
        Map<String, String> current = new LinkedHashMap<>();
        new ParserDelegator().parse(new StringReader(html), new HTMLEditorKit.ParserCallback() {
            private int ignored;
            private boolean block(HTML.Tag tag) { return List.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li").contains(tag.toString()); }
            private void newline() {
                if (!atoms.isEmpty() && !"\n".equals(atoms.get(atoms.size() - 1).source())) atoms.add(new RichTextTools.Atom("\n", "\n", Map.copyOf(current), 0, 1));
            }
            @Override public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                if (tag == HTML.Tag.HEAD || tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE) ignored++;
                stack.push(new LinkedHashMap<>(current));
                if (block(tag)) newline();
                String name = tag.toString();
                if (List.of("b", "strong").contains(name)) current.put("b", "");
                if (List.of("i", "em").contains(name)) current.put("i", "");
                if (name.equals("u")) current.put("u", "");
                if (List.of("s", "strike", "del").contains(name)) current.put("s", "");
                if (name.matches("h[1-6]")) { current.put("b", ""); current.put("size", "" + (28 - 2 * Integer.parseInt(name.substring(1)))); }
                Object color = attrs.getAttribute(HTML.Attribute.COLOR);
                if (color != null) current.put("color", colorHex(decodeColor(color.toString(), defaultColor)));
                Object align = attrs.getAttribute(HTML.Attribute.ALIGN);
                if (align != null) current.put("align", normalizedAlignment(align.toString()));
                Object url = attrs.getAttribute(HTML.Attribute.HREF);
                if (url != null && !url.toString().contains("[") && !url.toString().contains("]")) current.put("url", url.toString());
                Object style = attrs.getAttribute(HTML.Attribute.STYLE);
                if (style != null) cssStyle(current, style.toString());
            }
            @Override public void handleEndTag(HTML.Tag tag, int position) {
                if (block(tag)) newline();
                if (!stack.isEmpty()) { current.clear(); current.putAll(stack.pop()); }
                if (tag == HTML.Tag.HEAD || tag == HTML.Tag.SCRIPT || tag == HTML.Tag.STYLE) ignored = Math.max(0, ignored - 1);
            }
            @Override public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrs, int position) {
                if (tag == HTML.Tag.BR && ignored == 0) atoms.add(new RichTextTools.Atom("\n", "\n", Map.copyOf(current), 0, 1));
            }
            @Override public void handleText(char[] data, int position) {
                if (ignored == 0) { String text = new String(data); atoms.add(new RichTextTools.Atom(text, text, Map.copyOf(current), 0, text.length())); }
            }
        }, true);
        if (!atoms.isEmpty() && "\n".equals(atoms.get(atoms.size() - 1).source())) atoms.remove(atoms.size() - 1);
        return RichTextTools.serializeMarkup(atoms);
    }

    private void cssStyle(Map<String, String> style, String css) {
        for (String declaration : css.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2) continue;
            String key = parts[0].trim().toLowerCase(Locale.ROOT), value = parts[1].trim().toLowerCase(Locale.ROOT);
            switch (key) {
                case "font-weight" -> { if (value.equals("bold") || value.matches("[6-9]00")) style.put("b", ""); }
                case "font-style" -> { if (value.equals("italic")) style.put("i", ""); }
                case "font-size" -> { if (value.matches("[0-9]+(?:px|pt)?")) updateStyle(style, "size", value.replaceAll("[^0-9]", "")); }
                case "color" -> { if (value.matches("#[0-9a-f]{6}")) updateStyle(style, "color", value); }
                case "text-align" -> style.put("align", normalizedAlignment(value));
                case "text-decoration" -> { if (value.contains("underline")) style.put("u", ""); if (value.contains("line-through")) style.put("s", ""); }
                default -> { }
            }
        }
    }

    private static int alignmentConstant(String value) {
        return switch (normalizedAlignment(value)) {
            case "CENTER" -> StyleConstants.ALIGN_CENTER;
            case "RIGHT" -> StyleConstants.ALIGN_RIGHT;
            default -> StyleConstants.ALIGN_LEFT;
        };
    }
    private static String normalizedAlignment(String value) {
        return "CENTER".equalsIgnoreCase(value) ? "CENTER" : "RIGHT".equalsIgnoreCase(value) ? "RIGHT" : "LEFT";
    }
    private static Color decodeColor(String value, Color fallback) {
        try { return value != null && value.matches("#[0-9a-fA-F]{6}") ? Color.decode(value) : fallback; }
        catch (NumberFormatException invalid) { return fallback; }
    }
    private static String colorHex(Color color) { return String.format(Locale.ROOT, "#%06X", color.getRGB() & 0xFFFFFF); }
    private static String normalizeLines(String text) { return text.replace("\r\n", "\n").replace('\r', '\n'); }
}
