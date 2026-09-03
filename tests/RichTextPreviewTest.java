import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import sun.misc.Unsafe;

public final class RichTextPreviewTest {
    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        CustomBookGUI gui = (CustomBookGUI) ((Unsafe) unsafeField.get(null)).allocateInstance(CustomBookGUI.class);
        Method markup = CustomBookGUI.class.getDeclaredMethod("markupToHtml", String.class);
        markup.setAccessible(true);
        require("".equals(markup.invoke(gui, (Object) null)), "null text should have an empty preview");
        require("<b><i>A</i></b>BC".equals(markup.invoke(gui, "[b][i]A[/b]B[/i]C")),
                "crossed formatting closures differ from the runtime parser");
        require("<b>AB</b>C".equals(markup.invoke(gui, "[b]A[/i]B[/b]C")),
                "unmatched close must not end another style");
        require("<b><b>A</b>B</b>".equals(markup.invoke(gui, "[b][b]A[/b]B")),
                "nested repeated tags must close in stack order");
        String emoji = "\uD83D\uDE00";
        String html = (String) markup.invoke(gui, "A" + emoji + "B");
        require(html.contains(emoji), "preview inserted a line-break opportunity inside a surrogate pair");
        require(!html.contains("\uD83D&#8203;\uDE00"), "preview split a supplementary Unicode character");
        require(html.replace("&#8203;", "").equals("A" + emoji + "B"), "preview changed Unicode text");
        String escaped = (String) markup.invoke(gui, "<&\"\n>");
        require(escaped.contains("&lt;") && escaped.contains("&amp;") && escaped.contains("&quot;")
                && escaped.contains("<br>") && escaped.contains("&gt;"), "HTML escaping was broken");
        Method limit = CustomBookGUI.class.getDeclaredMethod("limit", String.class, int.class);
        limit.setAccessible(true);
        require("A".equals(limit.invoke(null, "A" + emoji + "B", 2)), "text truncation split a surrogate pair");
        require(("A" + emoji).equals(limit.invoke(null, "A" + emoji + "B", 3)), "valid Unicode boundary was truncated");
        Field maximum = CustomBookGUI.class.getDeclaredField("MAX_TEXT_LENGTH");
        maximum.setAccessible(true);
        require(maximum.getInt(null) == Short.MAX_VALUE, "legacy maximum text length must remain supported");
        System.out.println("RICH_TEXT_PREVIEW_OK (11 dynamic assertions)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
