import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.*;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.awt.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;

public final class BookEditorActionsTest {
    private static CustomBookGUI gui;
    private static DefaultMutableTreeNode root;
    public static void main(String[] args) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/lang/texts.properties"))) {
            var bundle = new PropertyResourceBundle(reader);
            for (String name : java.util.List.of("rb", "rb_en")) {
                Field field = net.mcreator.ui.init.L10N.class.getDeclaredField(name);
                field.setAccessible(true); field.set(null,bundle);
            }
        }
        step(BookEditorActionsTest::create);
        int original = page().elements.size();
        step(() -> call("addTextElement"));
        check(page().elements.size() == original + 1, "Add did not change model");
        step(() -> ((JButton)get("undoBook")).doClick());
        check(page().elements.size() == original, "Undo button did not remove added element");
        step(() -> ((JButton)get("redoBook")).doClick());
        check(page().elements.size() == original + 1, "Redo button did not restore added element");
        String id = ((CustomBook.BookElement)get("activeElement")).id;
        step(() -> {
            ((JSpinner)get("elementX")).setValue(45);
            call("geometryChangedFromEditor", new Class[]{JSpinner.class,String.class}, get("elementX"), "X");
        });
        check(element(id).x == 45, "Geometry edit failed");
        step(() -> ((JButton)get("undoBook")).doClick());
        check(element(id).x != 45, "Movement undo failed");
        step(() -> ((JButton)get("redoBook")).doClick());
        check(element(id).x == 45, "Movement redo failed");
        step(() -> call("deleteActiveElement"));
        check(elementOrNull(id) == null, "Delete failed");
        step(() -> ((JButton)get("undoBook")).doClick());
        check(element(id).x == 45, "Deleted element not restored with original geometry");
        step(() -> call("addTextElement"));
        check(!((JButton)get("redoBook")).isEnabled(), "New edit kept stale redo branch");

        CustomBook book = new CustomBook(null);
        book.categories = java.util.List.of((CustomBook.BookCategory)((DefaultMutableTreeNode)root.getChildAt(0)).getUserObject());
        Map<String,String> keys = BookTranslations.entries(book,"audit","book");
        String key = "custombook.audit.book.element." + id;
        check(keys.containsKey(key), "Text translation key missing");
        String pageKey = "custombook.audit.book.page." + page().id;
        check(keys.containsKey(pageKey), "Page translation key missing");
        var richKeys = BookTranslations.richTextKeys(book,"audit","book");
        check(richKeys.contains(key) && richKeys.contains(pageKey), "Text/title translations lost rich formatting");
        check(!richKeys.contains("custombook.audit.book.title"), "Plain book title translation exposes unsupported markup");
        page().title = "Renamed"; Collections.reverse(page().elements);
        check(BookTranslations.entries(book,"audit","book").keySet().equals(keys.keySet()), "Reorder/rename changed translation keys");
        BookEditHistory history = new BookEditHistory();
        for(int i=0;i<100;i++) history.push(new BookEditHistory.State(""+i,null,null,false));
        int count=0; while(history.canUndo()) { history.undo();count++; }
        check(count == 63, "History is not bounded");
        System.out.println("BOOK_EDITOR_ACTIONS_OK add/delete/move, toolbar undo/redo, branching, stable translation keys");
    }
    private static void create() throws Exception {
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        gui=(CustomBookGUI)((sun.misc.Unsafe)unsafe.get(null)).allocateInstance(CustomBookGUI.class);
        for(Field field:CustomBookGUI.class.getDeclaredFields()) {
            if(Modifier.isStatic(field.getModifiers())) continue;
            Object value=null;Class<?> type=field.getType();
            if(type==JSpinner.class)value=new JSpinner(new SpinnerNumberModel(12,-10000,128000,1));
            else if(type==JComboBox.class)value=new JComboBox<>(new String[]{"LEFT","CENTER","RIGHT","BOTH","FREE","PRESET"});
            else if(type==JCheckBox.class)value=new JCheckBox();
            else if(type==JButton.class)value=new JButton();
            else if(type==JTextField.class)value=new JTextField();
            else if(type==JTextArea.class)value=new JTextArea();
            else if(type==JPanel.class)value=new JPanel();
            else if(type==JLabel.class)value=new JLabel();
            if(value!=null){field.setAccessible(true);field.set(gui,value);}
        }
        set("textEditor",new VisualRichTextEditor());
        set("bookHistory",new BookEditHistory());set("historyJson",new com.google.gson.Gson());
        CardLayout cards=new CardLayout();JPanel panel=new JPanel(cards);
        for(String card:java.util.List.of("NONE","TEXT","MEDIA","BUTTON","INPUT","NAV"))panel.add(new JPanel(),card);
        set("inspectorCardLayout",cards);set("inspectorCards",panel);
        root=new DefaultMutableTreeNode("Book");
        CustomBook.BookCategory category=new CustomBook.BookCategory("General");
        CustomBook.BookPage page=new CustomBook.BookPage("First","Body"); category.pages.add(page);
        DefaultMutableTreeNode categoryNode=new DefaultMutableTreeNode(category),pageNode=new DefaultMutableTreeNode(page);
        categoryNode.add(pageNode);root.add(categoryNode);set("rootNode",root);
        DefaultTreeModel model=new DefaultTreeModel(root);JTree tree=new JTree(model);
        set("treeModel",model);set("bookTree",tree);
        Class<?> canvasType=Class.forName(CustomBookGUI.class.getName()+"$PagePreviewCanvas");
        var constructor=canvasType.getDeclaredConstructor(CustomBookGUI.class);constructor.setAccessible(true);
        set("previewCanvas",constructor.newInstance(gui));
        tree.addTreeSelectionListener(event->{try{call("onTreeSelectionChanged");}catch(Exception e){throw new AssertionError(e);}});
        tree.setSelectionPath(new TreePath(pageNode.getPath()));
        call("createElementToolbar");
        call("createPreviewPanel");
        call("resetBookHistory");
    }
    private static CustomBook.BookPage page()throws Exception{return (CustomBook.BookPage)get("activePage");}
    private static CustomBook.BookElement elementOrNull(String id)throws Exception{return page().elements.stream().filter(e->e.id.equals(id)).findFirst().orElse(null);}
    private static CustomBook.BookElement element(String id)throws Exception{return Objects.requireNonNull(elementOrNull(id),"Missing element");}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object get(String name)throws Exception{Field f=CustomBookGUI.class.getDeclaredField(name);f.setAccessible(true);return f.get(gui);}
    private static void set(String name,Object value)throws Exception{Field f=CustomBookGUI.class.getDeclaredField(name);f.setAccessible(true);f.set(gui,value);}
    private static Object call(String name)throws Exception{return call(name,new Class[0]);}
    private static Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=CustomBookGUI.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(gui,args);}
    private interface Checked {void run()throws Exception;}
    private static void step(Checked action)throws Exception {
        SwingUtilities.invokeAndWait(()->{try{action.run();}catch(Exception e){throw new AssertionError(e);}});
        SwingUtilities.invokeAndWait(()->{});
    }
}
