import fr.superbroche.mcreator.custombook.ui.RichTextTools;
import fr.superbroche.mcreator.custombook.ui.VisualRichTextEditor;
import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.text.StyleConstants;

/** Tests the actual Swing document, editing callbacks and clipboard, not an editor mock. */
public final class VisualRichTextEditorTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Exception failure) { throw new AssertionError(failure); }
        });
        System.out.println("VISUAL_RICH_TEXT_EDITOR_OK assertions=" + assertions);
    }

    private static void run() throws Exception {
        VisualRichTextEditor editor = new VisualRichTextEditor();
        AtomicInteger callbacks = new AtomicInteger();
        editor.setOnChange(callbacks::incrementAndGet);
        String original = "[align=CENTER][size=18][color=#6666FF][b][i][u][s][obf]Heading 😀[/obf][/s][/u][/i][/b][/color][/size][/align]\n"
                + "[url=https://example.com]Link[/url] and [page=Introduction]Page[/page]\nBody";
        editor.setMarkup(original, "LEFT");
        check(callbacks.get() == 0, "Loading emitted a change callback");
        check(!editor.canUndo(), "Loading polluted undo history");
        check(text(editor).equals("Heading 😀\nLink and Page\nBody"), "Formatting markup remains visible");
        var first = editor.getStyledDocument().getCharacterElement(0).getAttributes();
        check(StyleConstants.isBold(first) && StyleConstants.isItalic(first), "Bold and italic are not displayed");
        check(StyleConstants.isUnderline(first) && StyleConstants.isStrikeThrough(first), "Underline and strike are not displayed");
        check(StyleConstants.getFontSize(first) == 18, "Font size is not displayed");
        check(StyleConstants.getForeground(first).equals(new Color(0x6666FF)), "Font color is not displayed");
        check(StyleConstants.getAlignment(editor.getStyledDocument().getParagraphElement(0).getAttributes()) == StyleConstants.ALIGN_CENTER,
                "Centered heading is not displayed centered");
        String saved = editor.getMarkup();
        check(saved.contains("[obf]") && saved.contains("[url=https://example.com]") && saved.contains("[page=Introduction]"),
                "Obfuscation or links were lost on save");
        editor.setMarkup(saved, "LEFT");
        check(editor.getMarkup().equals(saved), "Load/save is not stable");

        String plain = text(editor);
        editor.select(0, plain.length());
        for (int size : new int[] {12,12,11,11,10,10,9,9,8,9,10,9}) {
            editor.applyStyle("size", Integer.toString(size), false);
            check(text(editor).equals(plain), "Size changes corrupted visible content");
            check(count(editor.getMarkup(), "[size=") <= 3, "Repeated size changes added duplicate nesting");
            check(editor.getSelectionStart() == 0 && editor.getSelectionEnd() == plain.length(), "Size changes lost selection");
            check(RichTextTools.parse(editor.getMarkup()).stream().allMatch(atom -> Integer.toString(size).equals(atom.styles().get("size"))),
                    "Requested size was not applied to the entire selection");
        }
        check(callbacks.get() > 0, "Style changes were not synchronously reported");

        editor.setMarkup("Before SELECT After", "LEFT");
        editor.select(7, 13);
        editor.applyStyle("size", "9", false);
        editor.toggleStyle("b");
        List<RichTextTools.Atom> atoms = RichTextTools.parse(editor.getMarkup());
        check(atoms.subList(0,7).stream().noneMatch(a -> a.styles().containsKey("b")), "Selection formatting changed prefix");
        check(atoms.subList(7,13).stream().allMatch(a -> a.styles().containsKey("b") && "9".equals(a.styles().get("size"))), "Selected style missing");
        check(atoms.subList(13,atoms.size()).stream().noneMatch(a -> a.styles().containsKey("size")), "Selection formatting changed suffix");
        editor.toggleStyle("b");
        check(!editor.getMarkup().contains("[b]"), "Bold could not be turned off");
        editor.undo();
        check(editor.getMarkup().contains("[b]"), "Undo did not restore formatting");
        editor.redo();
        check(!editor.getMarkup().contains("[b]"), "Redo did not remove formatting");
        editor.undo(); editor.undo(); editor.undo();
        check(editor.getMarkup().equals("Before SELECT After"), "A formatting action required more than one undo");
        check(!editor.canUndo(), "Undo actions introduced empty undo edits");

        editor.setMarkup("Heading\nBody\nTail", "LEFT");
        editor.select(1, 3);
        editor.applyStyle("align", "CENTER", true);
        check(editor.getSelectionStart() == 1 && editor.getSelectionEnd() == 3, "Paragraph alignment moved selection");
        atoms = RichTextTools.parse(editor.getMarkup());
        check(atoms.get(0).styles().get("align").equals("CENTER"), "Heading alignment missing");
        check(!atoms.get(8).styles().containsKey("align"), "Heading alignment leaked into body");
        editor.select(9, 9);
        editor.applyStyle("align", "RIGHT", true);
        check(StyleConstants.getAlignment(editor.getStyledDocument().getParagraphElement(9).getAttributes()) == StyleConstants.ALIGN_RIGHT,
                "Caret paragraph alignment was not changed");
        check(editor.getSelectionStyles().get("align").equals("RIGHT"), "Selection controls do not expose alignment");
        editor.setMarkup("Heading\nBody", "LEFT");
        editor.select(0, 8);
        editor.applyStyle("align", "CENTER", true);
        check(StyleConstants.getAlignment(editor.getStyledDocument().getParagraphElement(8).getAttributes()) == StyleConstants.ALIGN_LEFT,
                "Selecting a heading including its newline changed the following paragraph");

        editor.setMarkup("Existing", "LEFT");
        editor.setCaretPosition(editor.getDocument().getLength());
        editor.applyStyle("color", "#AA33DD", false);
        editor.applyStyle("size", "20", false);
        editor.toggleStyle("i");
        check(editor.getMarkup().equals("Existing"), "Caret formatting changed existing content");
        editor.replaceSelection(" next 😀");
        atoms = RichTextTools.parse(editor.getMarkup());
        check(atoms.subList(8, atoms.size()).stream().allMatch(a -> "20".equals(a.styles().get("size"))
                && "#AA33DD".equals(a.styles().get("color")) && a.styles().containsKey("i")), "Typing attributes were not applied");
        check(text(editor).equals("Existing next 😀"), "Unicode typing corrupted content");
        editor.undo();
        check(text(editor).equals("Existing"), "Typing undo failed");
        editor.redo();
        check(text(editor).equals("Existing next 😀"), "Typing redo failed");

        editor.setMarkup("[tr=book.old.translation]", "LEFT");
        editor.setMarkup("A [size=9]B[/size]", "LEFT");
        editor.setCaretPosition(0);
        editor.applyStyle("size", "20", false);
        check(editor.getSelectionStyles().get("size").equals("20"), "Typing size not exposed at caret");
        editor.setCaretPosition(3);
        check(editor.getSelectionStyles().get("size").equals("9"), "Typing size leaked after moving caret");
        editor.setCaretPosition(0);
        check(editor.getSelectionStyles().get("size").equals("12"), "Old typing size revived on returning to caret");
        editor.setMarkup("[tr=book.old.translation]", "LEFT");
        check(text(editor).equals("[tr=book.old.translation]"), "Removed localization syntax is still interpreted");
        check(editor.getMarkup().equals("[tr=book.old.translation]"), "Removed localization syntax was lost");

        editor.setMarkup("[b]Clipboard[/b]\n[color=#123456]Second 😀[/color]", "LEFT");
        editor.selectAll();
        Clipboard clipboard = new Clipboard("headless rich text regression");
        editor.getTransferHandler().exportToClipboard(editor, clipboard, TransferHandler.COPY);
        check(clipboard.getData(DataFlavor.stringFlavor).equals("Clipboard\nSecond 😀"), "Plain clipboard exposes hidden tags");
        VisualRichTextEditor target = new VisualRichTextEditor();
        check(target.getTransferHandler().importData(target, clipboard.getContents(null)), "Rich clipboard paste was rejected");
        check(text(target).equals(text(editor)) && target.getMarkup().contains("[b]") && target.getMarkup().contains("[color=#123456]"),
                "Rich clipboard did not preserve formatting");
        target.undo();
        check(text(target).isEmpty(), "Rich paste is not a single undo action");
        target.redo();
        check(text(target).equals("Clipboard\nSecond 😀"), "Rich paste redo failed");
        target.setDefaultStyle(16, "#38291E");
        target.setMarkup("", "CENTER");
        check(target.getTransferHandler().importData(target, clipboard.getContents(null)), "Body-to-title formatted paste failed");
        check(StyleConstants.getFontSize(target.getStyledDocument().getCharacterElement(0).getAttributes()) == 12,
                "Rich paste changed the source's default font size to the destination default");
        check(StyleConstants.getForeground(target.getStyledDocument().getCharacterElement(0).getAttributes()).equals(new Color(0x241B13)),
                "Rich paste changed the source's default text color");
        target.setDefaultStyle(12, "#241B13");
        target.setMarkup("Plain ", "LEFT");
        target.setCaretPosition(6);
        check(target.getTransferHandler().importData(target, new StringSelection("paste\r\nsecond line")), "Plain paste was rejected");
        check(text(target).equals("Plain paste\nsecond line"), "CRLF paste was not normalized");

        target.setMarkup("", "LEFT");
        String html = "<html><head><title>Ignore</title></head><body><p style='text-align:center'><b>Heading</b></p>"
                + "<p><span style='font-size:18px;color:#112233'>Text</span> <i>italic</i></p></body></html>";
        DataFlavor htmlFlavor = new DataFlavor("text/html;class=java.lang.String");
        Transferable htmlData = new Transferable() {
            public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{ htmlFlavor }; }
            public boolean isDataFlavorSupported(DataFlavor flavor) { return htmlFlavor.equals(flavor); }
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
                return html;
            }
        };
        check(target.getTransferHandler().importData(target, htmlData), "HTML paste was rejected");
        check(text(target).equals("Heading\nText italic"), "HTML text import retained tags or hidden head content: " + text(target));
        check(target.getMarkup().contains("[b]") && target.getMarkup().contains("[i]") && target.getMarkup().contains("[size=18]")
                && target.getMarkup().contains("[color=#112233]"), "HTML paste lost supported formatting");

        target.setDefaultStyle(16, "#38291E");
        int priorCallbacks = callbacks.get();
        editor.setMarkup("Another element", "RIGHT");
        check(callbacks.get() == priorCallbacks, "Switching elements emitted stale callbacks");
        check(!editor.canUndo() && !editor.canRedo(), "Undo history crossed element boundary");
        target.setMarkup("Title", "CENTER");
        check(target.getMarkup().equals("Title"), "Implicit title defaults were serialized as explicit body styles");
        check(StyleConstants.getFontSize(target.getStyledDocument().getCharacterElement(0).getAttributes()) == 16, "Title default font not used");
        check(target.getSelectionStyles().get("color").equals("#38291E"), "Title default color not exposed");

        editor.setMarkup("x".repeat(VisualRichTextEditor.MAX_MARKUP_LENGTH), "LEFT");
        editor.setCaretPosition(editor.getDocument().getLength());
        editor.replaceSelection("overflow");
        check(text(editor).length() == VisualRichTextEditor.MAX_MARKUP_LENGTH, "Length guard silently truncated or accepted overflow");
        editor.selectAll();
        String before = editor.getMarkup();
        boolean rejected = false;
        try { editor.applyStyle("size", "18", false); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && editor.getMarkup().equals(before), "Formatting length rejection changed existing content");
        check(editor.getSelectionStart() == 0 && editor.getSelectionEnd() == before.length(), "Length rejection lost selection");
    }
    private static String text(VisualRichTextEditor editor) throws Exception { return editor.getDocument().getText(0, editor.getDocument().getLength()); }
    private static int count(String text, String needle) { return (text.length() - text.replace(needle, "").length()) / needle.length(); }
    private static void check(boolean condition, String message) { assertions++; if (!condition) throw new AssertionError(message); }
}
