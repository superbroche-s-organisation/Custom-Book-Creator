import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.Comparator;
import javax.tools.ToolProvider;

/** Runs the generated keyboard/close and translation methods with a small client fixture. */
public final class PageInteractionTest {
    public static void main(String[] args) throws Exception {
        Path resources = Path.of(System.getProperty("custombook.pluginRoot", "src/main/resources"));
        try (var profiles = Files.list(resources)) {
            for (Path profile : profiles.filter(p -> Files.isRegularFile(p.resolve("custombook.definition.yaml"))).toList()) {
                Configuration config = new Configuration(Configuration.VERSION_2_3_31);
                config.setDirectoryForTemplateLoading(profile.resolve("templates/custombook").toFile());
                config.setDefaultEncoding("UTF-8");
                BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_31);
                wrapper.setExposeFields(true); config.setObjectWrapper(wrapper.build());
                var model = TemplateRenderTest.baseModel(); model.put("data", TemplateRenderTest.representativeBook());
                StringWriter writer = new StringWriter(); config.getTemplate("screen.java.ftl").process(model, writer);
                verify(writer.toString());
                System.out.println("PAGE_INTERACTION_OK " + profile.getFileName());
            }
        }
    }
    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new AssertionError("Missing generated method: " + signature);
        int first = source.indexOf('{', start), depth = 1, end = first + 1;
        while (depth > 0) { char c = source.charAt(end++); if (c == '{') depth++; if (c == '}') depth--; }
        return source.substring(start, end);
    }
    private static void verify(String source) throws Exception {
        boolean modern = source.contains("keyPressed(net.minecraft.client.input.KeyEvent");
        String methods = method(source, "@Override public boolean keyPressed(")
                + method(source, "@Override public void onClose()")
                + method(source, "private void cancelInlineWriting()")
                + method(source, "private void navigateToCategory(")
                + method(source, "private boolean closeCategoryMenuOnEscape(")
                + method(source, "private static String translated(");
        methods = methods.replace("net.minecraft.client.input.KeyEvent", "KeyEvent")
                .replace("net.minecraft.client.gui.screens.ConfirmScreen", "ConfirmScreen")
                .replace("net.minecraft.locale.Language", "Language");
        String keyCall = modern ? "keyPressed(new KeyEvent(key))" : "keyPressed(key, 0, 0)";
        String fixture = """
            import java.util.*;
            import java.util.function.Consumer;
            class Parent {
                int closed, delegated;
                public boolean keyPressed(int key, int scan, int mods) { delegated++; if (key==256) onClose(); return true; }
                public boolean keyPressed(InteractionFixture.KeyEvent event) { return keyPressed(event.key(),0,0); }
                public void onClose() { closed++; }
            }
            public class InteractionFixture extends Parent {
                record KeyEvent(int key) {}
                static final class InventoryKey {
                    int bound = 69;
                    boolean matches(int key, int scan) { return key==bound; }
                    boolean matches(KeyEvent event) { return event.key()==bound; }
                }
                static final class Options { InventoryKey keyInventory = new InventoryKey(); }
                static final class Client {
                    Options options = new Options(); Client gui = this; Object screen;
                    void setScreen(Object screen) { this.screen=screen; }
                }
                record Field(String key) {}
                static final class Editor { String value=""; String getValue() { return value; } }
                static final class Writing { boolean pending; String value(String key) { return "saved"; } }
                record ConfirmScreen(Consumer<Boolean> reply, Object title, Object message) {}
                static final class Component { static String literal(String s) { return s; } }
                static final class Language {
                    static Language active = new Language(); Map<String,String> values=new HashMap<>();
                    static Language getInstance() { return active; }
                    String getOrDefault(String key, String fallback) { return values.getOrDefault(key,fallback); }
                }
                Client minecraft=new Client(); Field inlineField; Editor inlineEditor;
                Writing writing=new Writing(); String inlineDraft=""; int ended;
                boolean categoryMenuOpen; String selectedCategory;
                void selectCategoryById(String id) { selectedCategory=id; }
                void updatePageSound() {}
                void endInlineWriting() { inlineField=null;inlineEditor=null;ended++; }
                void start(String text) { inlineField=new Field("field"); inlineEditor=new Editor(); inlineEditor.value=text; }
                %s
                boolean press(int key) { return %s; }
                static void check(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
                public static void run() {
                    InteractionFixture s=new InteractionFixture();
                    check(s.press(69) && s.closed==1,"Inventory key does not close book");
                    s.minecraft.options.keyInventory.bound=82;
                    s.press(69);check(s.closed==1,"Hard-coded E ignores inventory remapping");
                    s.press(82);check(s.closed==2,"Remapped inventory key does not close");
                    s.start("saved");s.press(69);s.press(82);
                    check(s.closed==2 && s.inlineField!=null,"Inventory key closes while typing");
                    s.writing.pending=true;s.press(256);
                    check(s.inlineField!=null && s.closed==2,"Escape cancels a pending server write");
                    s.writing.pending=false;s.press(256);
                    check(s.inlineField==null && s.ended==1 && s.closed==2,"Escape must finish clean editing before closing book");
                    s.start("draft");s.press(256);
                    check(s.minecraft.screen instanceof ConfirmScreen && s.inlineDraft.equals("draft"),"Unsaved changes lack confirmation");
                    ((ConfirmScreen)s.minecraft.screen).reply.accept(false);
                    check(s.inlineField!=null && s.minecraft.screen==s,"Keep editing lost draft or book");
                    s.press(256);((ConfirmScreen)s.minecraft.screen).reply.accept(true);
                    check(s.inlineField==null && s.ended==2,"Discard did not end inline editing");
                    s.press(256);check(s.closed==3,"Escape cannot close book after editing");
                    s.categoryMenuOpen=true;s.press(256);check(!s.categoryMenuOpen && s.closed==3,"Escape closed book through dropdown");
                    s.start("draft");s.writing.pending=true;s.navigateToCategory("other");
                    check(s.selectedCategory==null,"Category changed during server write");
                    s.writing.pending=false;s.navigateToCategory("other");
                    ((ConfirmScreen)s.minecraft.screen).reply.accept(false);
                    check(s.selectedCategory==null && s.inlineField!=null,"Category change discarded draft without approval");
                    s.navigateToCategory("other");((ConfirmScreen)s.minecraft.screen).reply.accept(true);
                    check(s.selectedCategory.equals("other") && s.inlineField==null,"Confirmed category navigation failed");
                    check(translated("page.one","[b]Source[/b]").equals("[b]Source[/b]"),"Missing translation loses fallback markup");
                    String key="custombook.audit_mod.audit_book.page.one";
                    Language.active.values.put(key,"[b]Bonjour[/b]");
                    check(translated("page.one","Source").equals("[b]Bonjour[/b]"),"Translation/markup not resolved");
                    Language.active=new Language();Language.active.values.put(key,"Hello");
                    check(translated("page.one","Source").equals("Hello"),"Minecraft language switch uses stale text");
                    check(translated("","Source").equals("Source") && translated(null,"Source").equals("Source"),"Empty translation keys fail");
                }
            }
            """.formatted(methods,keyCall);
        Path dir=Files.createTempDirectory("custombook-interaction-");
        try {
            Path java=dir.resolve("InteractionFixture.java");Files.writeString(java,fixture);
            int result=ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","21","-d",dir.toString(),java.toString());
            if(result!=0)throw new AssertionError("Interaction fixture compilation failed");
            try(var loader=new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})) {
                loader.loadClass("InteractionFixture").getMethod("run").invoke(null);
            }
        } finally {
            try(var paths=Files.walk(dir)) { for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path); }
        }
    }
}
