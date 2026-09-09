import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.VisualRichTextEditor;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import com.google.gson.Gson;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.*;

public final class TitleEditorIntegrationTest {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/lang/texts.properties"))) {
            var bundle = new java.util.PropertyResourceBundle(reader);
            for (String name : new String[]{"rb","rb_en"}) {
                Field field = net.mcreator.ui.init.L10N.class.getDeclaredField(name);
                field.setAccessible(true); field.set(null,bundle);
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Exception failure) { throw new AssertionError(failure); }
        });
        System.out.println("TITLE_EDITOR_INTEGRATION_OK assertions=" + assertions);
    }

    private static void run() throws Exception {
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        CustomBookGUI gui = (CustomBookGUI)((sun.misc.Unsafe)unsafe.get(null)).allocateInstance(CustomBookGUI.class);
        VisualRichTextEditor editor = new VisualRichTextEditor();
        set(gui, "textEditor", editor);
        set(gui, "fontSize", spinner(12,6,48));
        set(gui, "textAlignment", new JComboBox<>(new String[]{"LEFT","CENTER","RIGHT"}));
        for (String field : new String[]{"elementX","elementY","elementW","elementH"}) set(gui,field,spinner(1,0,320));
        set(gui, "pageTitle", new JTextField());
        set(gui, "selectionInfo", new JLabel());
        set(gui, "titlePosition", new JComboBox<>(new String[]{"Top","Bottom","Left","Right"}));
        set(gui, "titleOffset", spinner(0,0,290)); set(gui, "titleHeight", spinner(24,10,96));
        set(gui, "titleBarLength", spinner(228,1,292)); set(gui, "titleBarThickness", spinner(1,1,96));
        set(gui, "titleBarColor", new JButton()); set(gui, "titleBarVisible", new JCheckBox());
        set(gui, "titleLayoutPanel", new JPanel(new GridBagLayout()));
        JCheckBox snap = new JCheckBox(); snap.setSelected(true);
        set(gui, "snapToGrid", snap); set(gui, "showGrid", new JCheckBox()); set(gui, "gridSize", spinner(8,2,32));
        CardLayout cards = new CardLayout(); JPanel panel = new JPanel(cards);
        panel.add(new JPanel(), "NONE"); panel.add(new JPanel(), "TEXT");
        set(gui, "inspectorCardLayout", cards); set(gui, "inspectorCards", panel);
        CustomBook.BookPage page = new CustomBook.BookPage("Title", "Body");
        page.getTitleDesign().content = "[size=18][b]Heading[/b][/size]";
        set(gui, "activePage", page);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(page);
        DefaultTreeModel treeModel = new DefaultTreeModel(node); JTree tree = new JTree(treeModel);
        tree.setSelectionPath(new TreePath(node)); set(gui,"treeModel",treeModel); set(gui,"bookTree",tree);
        Class<?> canvasType = Class.forName(CustomBookGUI.class.getName() + "$PagePreviewCanvas");
        var constructor = canvasType.getDeclaredConstructor(CustomBookGUI.class); constructor.setAccessible(true);
        JPanel canvas = (JPanel)constructor.newInstance(gui);
        set(gui,"previewCanvas",canvas); canvas.setSize(536,664);
        invoke(canvas,"setPage",new Class[]{CustomBook.BookPage.class},page);
        invoke(gui,"createTitleLayoutPanel");
        editor.setOnChange(() -> unchecked(gui,"syncVisualText"));
        editor.addCaretListener(event -> unchecked(gui,"updateTextFormattingControls"));
        invoke(gui,"selectPageTitle");
        check(editor.getText().equals("Heading"),"Title markup is visible");
        check((boolean)get(gui,"editingTitle"),"Title editing mode not selected");
        editor.selectAll();
        invoke(gui,"applyTextStyle",new Class[]{String.class,String.class,boolean.class},"color","#6666FF",false);
        check(page.getTitleContent().contains("[color=#6666FF]"),"Title style was not saved");
        check(page.title.equals("Heading"),"Tree label contains hidden markup");
        check(page.elements.get(0).content.equals("Body"),"Title edit changed body");
        invoke(gui,"selectElement",new Class[]{CustomBook.BookElement.class},page.elements.get(0));
        check(editor.getText().equals("Body"),"Switching to body left title content");
        check(!(boolean)get(gui,"editingTitle"),"Body still writes title");
        editor.selectAll(); editor.toggleStyle("i");
        check(page.elements.get(0).content.contains("[i]"),"Body style not synchronized");
        check(page.getTitleContent().contains("Heading"),"Body change damaged title");
        invoke(gui,"selectPageTitle");
        for (String position : new String[]{"TOP","BOTTOM","LEFT","RIGHT"}) {
            page.getTitleDesign().position=position; page.getTitleDesign().offset=32;
            invoke(gui,"syncTitleControls");
            CustomBook.TitleLayout layout=page.getTitleLayout();
            MouseEvent press=mouse(canvas,MouseEvent.MOUSE_PRESSED,layout.handleX(),layout.handleY(),MouseEvent.BUTTON1);
            for (var listener:canvas.getMouseListeners()) listener.mousePressed(press);
            int x=layout.handleX(),y=layout.handleY();
            if(position.equals("LEFT")) x+=27;
            else if(position.equals("RIGHT")) x-=27;
            else if(position.equals("TOP")) y+=27;
            else y-=27;
            MouseEvent drag=mouse(canvas,MouseEvent.MOUSE_DRAGGED,x,y,MouseEvent.NOBUTTON);
            for(var listener:canvas.getMouseMotionListeners())listener.mouseDragged(drag);
            CustomBook.TitleLayout moved=page.getTitleLayout();
            int coordinate=position.equals("TOP")||position.equals("BOTTOM")?moved.handleY():moved.handleX();
            check(coordinate%8==0,"Title handle did not snap for "+position);
            check(page.getTitleDesign().offset>32,"Title did not move inward for "+position);
            check(((Number)((JSpinner)get(gui,"titleOffset")).getValue()).intValue()==page.getTitleDesign().offset,
                    "Drag did not synchronize offset control");
            for(var listener:canvas.getMouseListeners())listener.mouseReleased(mouse(canvas,MouseEvent.MOUSE_RELEASED,x,y,MouseEvent.BUTTON1));
        }
        snap.setSelected(false);
        CustomBook.TitleLayout before=page.getTitleLayout();
        invoke(canvas,"moveTitleHandle",new Class[]{int.class,int.class},before.handleX()-3,before.handleY());
        check(page.getTitleLayout().handleX()==before.handleX()-3,"Grid-disabled movement is not pixel precise");
        ((JCheckBox)get(gui,"titleBarVisible")).doClick();
        check(!page.getTitleDesign().barVisible,"Bar visibility not synchronized");
        CustomBook.BookCategory category=new CustomBook.BookCategory("Category"); category.pages.add(page);
        Method copy=method(CustomBookGUI.class,"copyCategory",CustomBook.BookCategory.class);
        CustomBook.BookCategory copied=(CustomBook.BookCategory)copy.invoke(null,category);
        CustomBook.BookPage copiedPage=copied.pages.get(0);
        check(copiedPage.titleDesign!=page.titleDesign,"Copied title design aliases source");
        check(copiedPage.getTitleContent().equals(page.getTitleContent()),"Copy lost rich title");
        String saved=new Gson().toJson(copied);
        CustomBook.BookCategory reloaded=new Gson().fromJson(saved,CustomBook.BookCategory.class);
        check(reloaded.pages.get(0).getTitleLayout().equals(copiedPage.getTitleLayout()),"Reload lost title geometry");
        check(reloaded.pages.get(0).getTitleContent().equals(copiedPage.getTitleContent()),"Reload lost title styles");
        Path output=Path.of("build","gui-validation"); Files.createDirectories(output);
        BufferedImage image=new BufferedImage(536,664,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics(); canvas.paint(graphics); graphics.dispose();
        ImageIO.write(image,"png",output.resolve("title-preview.png").toFile());
        editor.setMarkup("[size=22][color=#6666FF][b]Visual heading[/b][/color][/size]\n[align=LEFT]Body with [i]italic[/i] and [u]underline[/u].[/align]","CENTER");
        editor.setSize(600,240);
        image=new BufferedImage(600,240,BufferedImage.TYPE_INT_ARGB);
        graphics=image.createGraphics(); graphics.setColor(new Color(0xFFF4D6));graphics.fillRect(0,0,600,240); editor.paint(graphics);graphics.dispose();
        ImageIO.write(image,"png",output.resolve("visual-editor.png").toFile());
    }
    private static MouseEvent mouse(JPanel canvas,int id,int x,int y,int button) {
        return new MouseEvent(canvas,id,System.currentTimeMillis(),id==MouseEvent.MOUSE_DRAGGED?MouseEvent.BUTTON1_DOWN_MASK:0,
                12+x*2,12+y*2,1,false,button);
    }
    private static JSpinner spinner(int value,int min,int max){return new JSpinner(new SpinnerNumberModel(value,min,max,1));}
    private static void set(Object object,String field,Object value)throws Exception{Field f=CustomBookGUI.class.getDeclaredField(field);f.setAccessible(true);f.set(object,value);}
    private static Object get(Object object,String field)throws Exception{Field f=CustomBookGUI.class.getDeclaredField(field);f.setAccessible(true);return f.get(object);}
    private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static Object invoke(Object object,String name,Class<?>[] types,Object...args)throws Exception{return method(object.getClass(),name,types).invoke(object,args);}
    private static Object invoke(Object object,String name)throws Exception{return invoke(object,name,new Class[0]);}
    private static void unchecked(Object object,String name){try{invoke(object,name);}catch(Exception failure){throw new AssertionError(failure);}}
    private static void check(boolean result,String message){assertions++;if(!result)throw new AssertionError(message);}
}
