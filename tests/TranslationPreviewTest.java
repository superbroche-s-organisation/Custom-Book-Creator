import fr.superbroche.mcreator.custombook.ui.*;
import java.awt.Color;
import javax.swing.*;
import javax.swing.text.StyleConstants;

public final class TranslationPreviewTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var factory = BookTranslations.class.getDeclaredMethod("translationEditor");factory.setAccessible(true);
                var editor=(VisualRichTextEditor)factory.invoke(null);
                editor.setMarkup("[color=#080808][b]Dark source[/b][/color] and plain", "LEFT");
                check(editor.getText().equals("Dark source and plain"),"Markup became visible");
                white(editor);
                check(editor.getMarkup().contains("[color=#080808]"),"Preview changed the stored source color");
                editor.selectAll();editor.applyStyle("color","#000000",false);white(editor);
                editor.setEditable(false);white(editor);
                editor.setEditable(true);editor.setEnabled(false);white(editor);editor.setEnabled(true);
                check(editor.getDisabledTextColor().equals(Color.WHITE),"Disabled source text is dark");
                editor.setMarkup("", "LEFT");editor.replaceSelection("New translation");white(editor);
                check(!editor.getMarkup().contains("[color="),"View-only white leaked into saved translations");
                editor.selectAll();editor.toggleStyle("i");editor.undo();white(editor);editor.redo();white(editor);
            } catch(Exception failure) { throw new AssertionError(failure); }
        });
        System.out.println("TRANSLATION_PREVIEW_OK white source, editing, explicit colors, read-only, disabled, undo/redo");
    }
    private static void white(VisualRichTextEditor editor) {
        for(int i=0;i<editor.getDocument().getLength();i++)
            check(Color.WHITE.equals(StyleConstants.getForeground(editor.getStyledDocument().getCharacterElement(i).getAttributes())),"Non-white translation text at "+i);
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
