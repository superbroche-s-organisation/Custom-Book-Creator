import fr.superbroche.mcreator.custombook.ui.RichTextTools;
import fr.superbroche.mcreator.custombook.ui.VisualRichTextEditor;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

public final class RichTextEditingTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        String content = "[color=#3333FF][b][u]Ore Names:[/u][/b][/color]\n"
                + "[color=#3333FF]Amethyst Cluster, Arkansas Antimony, Arsenopyrite, Asbestos, "
                + "Black Hawaiian Salt, Brazilian Citrine Block, Chalcanthite, Chalcopyrite, "
                + "Cinnabar, Cobalt, Coloradoite,\nCopper Mineral, Copper Mineral Green[/color]";
        RichTextTools.Edit edit = new RichTextTools.Edit(content, 0, content.length());
        String plain = visible(content);
        for (int size : new int[] {12, 12, 12, 11, 11, 11, 10, 10, 10, 9, 9, 9}) {
            edit = RichTextTools.format(edit.text(), edit.selectionStart(), edit.selectionEnd(), "size", "" + size, false);
            check(visible(edit.text()).equals(plain), "Repeated spinner updates changed user content");
            check(RichTextTools.parse(edit.text()).stream().allMatch(atom -> ("" + size).equals(atom.styles().get("size"))),
                    "Selected text did not receive the requested size");
            check(count(edit.text(), "[size=") == 1, "Repeated spinner updates stacked size tags");
        }
        check(edit.text().contains("[b][u]Ore Names:"), "Independent bold/underline formatting was lost");
        String nested = "[size=12][size=11][size=10][color=#123456]A😀B[/color][/size][/size][/size]";
        edit = RichTextTools.format(nested, 0, nested.length(), "size", "9", false);
        check(count(edit.text(), "[size=") == 1 && visible(edit.text()).equals("A😀B"), "Legacy nested sizes not normalized");

        content = "[size=18]Before SELECT After[/size]";
        int start = content.indexOf("SELECT");
        edit = RichTextTools.format(content, start, start + 6, "size", "9", false);
        List<RichTextTools.Atom> atoms = RichTextTools.parse(edit.text());
        check(atoms.subList(0, 7).stream().allMatch(a -> "18".equals(a.styles().get("size"))), "Prefix size changed");
        check(atoms.subList(7, 13).stream().allMatch(a -> "9".equals(a.styles().get("size"))), "Selected size not replaced");
        check(atoms.subList(13, atoms.size()).stream().allMatch(a -> "18".equals(a.styles().get("size"))), "Suffix size changed");
        for (int i = 0; i < 50; i++) {
            String previous = edit.text();
            edit = RichTextTools.format(edit.text(), edit.selectionStart(), edit.selectionEnd(), "size", "9", false);
            check(edit.text().equals(previous), "Repeated change is not idempotent");
        }

        content = "A heading\nA first paragraph\nAnother paragraph";
        edit = RichTextTools.format(content, 2, 5, "align", "CENTER", true);
        atoms = RichTextTools.parse(edit.text());
        check(atoms.subList(0, 9).stream().allMatch(a -> "CENTER".equals(a.styles().get("align"))), "Heading was not centered as a paragraph");
        check(atoms.subList(10, atoms.size()).stream().anyMatch(a -> !a.styles().containsKey("align")),
                "Alignment leaked into body");
        String html = RichTextTools.previewHtml(edit.text(), "LEFT");
        check(html.contains("text-align:center") && html.contains("text-align:left"), "Preview lacks multiple alignments");
        start = edit.text().indexOf("first");
        edit = RichTextTools.format(edit.text(), start, start, "align", "RIGHT", true);
        check(RichTextTools.previewHtml(edit.text(), "LEFT").contains("text-align:right"), "Caret paragraph alignment failed");

        content = "[b][tr=book.example.heading][/b]\n[tr=book.example.body]";
        edit = RichTextTools.format(content, 0, content.length(), "size", "14", false);
        check(count(edit.text(), "[tr=") == 2 && edit.text().contains("[tr=book.example.heading]"), "Translation tokens split or lost");
        check(!RichTextTools.validKey("key]broken") && RichTextTools.validKey("book.example.title"), "Translation key validation failed");

        SwingUtilities.invokeAndWait(() -> {
            try {
                JTextPane pane = new JTextPane();
                pane.setText("[b]Title[/b]\r\nLine two\r\nLine three 😀\r\nTail");
                String documentText = pane.getDocument().getText(0, pane.getDocument().getLength());
                int from = documentText.indexOf("Line two");
                pane.select(from, documentText.indexOf("Tail"));
                String expected = visible(documentText);
                for (int size = 12; size >= 6; size--) {
                    RichTextTools.Edit update = RichTextTools.format(
                            pane.getDocument().getText(0, pane.getDocument().getLength()),
                            pane.getSelectionStart(), pane.getSelectionEnd(), "size", "" + size, false);
                    pane.setText(update.text());
                    pane.select(update.selectionStart(), update.selectionEnd());
                    check(visible(pane.getDocument().getText(0, pane.getDocument().getLength())).equals(expected),
                            "CRLF document offsets corrupted multiple lines");
                }
            } catch (Exception failure) { throw new AssertionError(failure); }
        });
        verifyLiveEditor();
        System.out.println("RICH_TEXT_EDITING_OK assertions=" + assertions);
    }
    private static void verifyLiveEditor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                CustomBookGUI gui = (CustomBookGUI)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(CustomBookGUI.class);
                VisualRichTextEditor pane = new VisualRichTextEditor();
                JSpinner size = new JSpinner(new SpinnerNumberModel(12, 6, 48, 1));
                JComboBox<String> alignment = new JComboBox<>(new String[] {"LEFT","CENTER","RIGHT"});
                put(gui, "textEditor", pane); put(gui, "fontSize", size); put(gui, "textAlignment", alignment);
                Class<?> canvasClass = Class.forName(CustomBookGUI.class.getName() + "$PagePreviewCanvas");
                var constructor = canvasClass.getDeclaredConstructor(CustomBookGUI.class);
                constructor.setAccessible(true);
                put(gui, "previewCanvas", constructor.newInstance(gui));
                Method apply = method("applyLiveFontSize");
                Method controls = method("updateTextFormattingControls");
                method("bindFontSize").invoke(gui);
                pane.addCaretListener(event -> call(controls, gui));
                Method sync = method("syncVisualText");
                pane.setOnChange(() -> call(sync, gui));
                pane.setMarkup("[color=#3333FF][b]Ore Names:[/b][/color]\r\nLine two\r\nCopper Mineral Green", "LEFT");
                CustomBook.BookElement element = CustomBook.BookElement.text(pane.getMarkup());
                put(gui, "activeElement", element);
                String expected = visible(pane.getDocument().getText(0, pane.getDocument().getLength()));
                pane.select(0, pane.getDocument().getLength());
                for (int value : new int[] {11,10,9,8,9,10,11,12,9}) {
                    size.setValue(value);
                    String actual = pane.getDocument().getText(0, pane.getDocument().getLength());
                    check(visible(actual).equals(expected), "Real editor/spinner callbacks damaged multiline text");
                    check(!actual.contains("[size=") && count(pane.getMarkup(), "[size=") <= 1,
                            "Real editor/spinner callbacks displayed or stacked size tags");
                    check(element.content.equals(pane.getMarkup()), "Actual model was not synchronized");
                    check(RichTextTools.parse(pane.getMarkup()).stream().allMatch(atom ->
                            Integer.toString(value).equals(atom.styles().getOrDefault("size", "12"))),
                            "Spinner did not apply the requested size " + value);
                }
                String before = element.content;
                pane.select(0, 0);
                check(element.content.equals(before), "Moving the caret changed the saved text");
                check(((Number)size.getValue()).intValue() == 9, "Selected size was not displayed: control="
                        + size.getValue() + " styles=" + pane.getSelectionStyles() + " markup=" + pane.getMarkup());
                put(gui, "loadingElement", true);
                pane.setMarkup("[size=18]Heading[/size]\n[size=9]Body[/size]", "LEFT");
                put(gui, "loadingElement", false);
                int body = pane.getDocument().getText(0, pane.getDocument().getLength()).indexOf("Body");
                pane.select(body, body + 4);
                check(((Number)size.getValue()).intValue() == 9, "Body style was not read independently");
                pane.select(0, 7);
                check(((Number)size.getValue()).intValue() == 18, "Heading style was not read independently");
            } catch (Exception failure) { throw new AssertionError(failure); }
        });
    }
    private static Method method(String name, Class<?>... types) throws Exception {
        Method method = CustomBookGUI.class.getDeclaredMethod(name, types); method.setAccessible(true); return method;
    }
    private static void put(Object target, String name, Object value) throws Exception {
        Field field = CustomBookGUI.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static void call(Method method, Object target) {
        try { method.invoke(target); } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static String visible(String source) {
        return RichTextTools.parse(source).stream().map(RichTextTools.Atom::display).reduce("", String::concat);
    }
    private static int count(String text, String needle) { return (text.length() - text.replace(needle, "").length()) / needle.length(); }
    private static void check(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
}
