import com.google.gson.Gson;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.tools.ToolProvider;

public final class PageSoundTest {
    public static void main(String[] args) throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/lang/texts.properties"))) {
            var bundle = new PropertyResourceBundle(reader);
            for (String name : List.of("rb", "rb_en")) {
                Field field = net.mcreator.ui.init.L10N.class.getDeclaredField(name);
                field.setAccessible(true); field.set(null, bundle);
            }
        }
        SwingUtilities.invokeAndWait(() -> {
            try { modelAndEditor(); } catch (Exception failure) { throw new AssertionError(failure); }
        });
        Path resources = Path.of(System.getProperty("custombook.pluginRoot", "src/main/resources"));
        try (var profiles = Files.list(resources)) {
            for (Path profile : profiles.filter(p -> Files.isRegularFile(p.resolve("custombook.definition.yaml"))).toList()) {
                runtime(profile.resolve("templates/custombook/screen.java.ftl"));
            }
        }
        System.out.println("PAGE_SOUND_OK model, editor, persistence, playback lifecycle");
    }
    private static void modelAndEditor() throws Exception {
        CustomBook book = new CustomBook(null);
        check(!book.hasPageSounds(), "Old silent book changed");
        var page = book.getBookCategories().get(0).pages.get(0);
        page.pageSound = "chapter.intro"; page.pageSoundTrigger = "BUTTON";
        check(book.hasPageSounds(), "Legacy sound not detected/migrated");
        var first = page.getBookElements().stream().filter(e -> "SOUND".equals(e.type)).findFirst().orElseThrow();
        check(page.pageSound.isEmpty() && first.getSoundTrigger().equals("BUTTON"), "Migration lost mode or did not clear legacy setting");
        for (int i = 0; i < 4; i++) page.normalizeElements();
        check(page.elements.stream().filter(e -> "SOUND".equals(e.type)).count() == 1, "Migration duplicated a sound");
        for (String bad : List.of("../file", "https://example.com", "Bad", "bad sound")) {
            var invalid = CustomBook.BookElement.sound(bad, "BAD");
            check(invalid.getSoundName().isEmpty() && invalid.getSoundTrigger().equals("OPEN"), "Invalid sound accepted");
        }
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        var gui = (CustomBookGUI)((sun.misc.Unsafe)unsafe.get(null)).allocateInstance(CustomBookGUI.class);
        JComboBox<String> sound = new JComboBox<>(), trigger = new JComboBox<>(new String[]{"Open", "Button"});
        set(gui,"pageSound",sound); set(gui,"pageSoundTrigger",trigger); set(gui,"soundLabel",new JTextField());
        set(gui,"activePage",page); set(gui,"activeElement",first);
        var canvasType = Class.forName(CustomBookGUI.class.getName()+"$PagePreviewCanvas");
        var constructor = canvasType.getDeclaredConstructor(CustomBookGUI.class); constructor.setAccessible(true);
        set(gui,"previewCanvas",constructor.newInstance(gui));
        call(gui,"createSoundCard");
        check(sound.getSelectedItem().equals("chapter.intro") && trigger.getSelectedIndex()==1, "Inspector lost old sound");
        trigger.setSelectedIndex(0); check(first.getSoundTrigger().equals("OPEN"), "Mode change not saved");
        var second = CustomBook.BookElement.sound("chapter.second","BUTTON"); page.elements.add(second);
        set(gui,"activeElement",second); call(gui,"refreshPageSoundChoices");
        check(sound.getSelectedItem().equals("chapter.second") && trigger.getSelectedIndex()==1, "Element selection not restored");
        sound.addItem("chapter.changed"); sound.setSelectedItem("chapter.changed");
        check(second.mediaName.equals("chapter.changed") && first.mediaName.equals("chapter.intro"), "Sound settings leak between elements");
        Method copy = CustomBookGUI.class.getDeclaredMethod("copyCategory",CustomBook.BookCategory.class); copy.setAccessible(true);
        var category = (CustomBook.BookCategory)copy.invoke(null,book.getBookCategories().get(0));
        var restored = new Gson().fromJson(new Gson().toJson(category),CustomBook.BookCategory.class);
        var reloaded = restored.pages.get(0).getBookElements().stream().filter(e -> e.id.equals(second.id)).findFirst().orElseThrow();
        check(reloaded.mediaName.equals("chapter.changed") && reloaded.getSoundTrigger().equals("BUTTON"), "Copy/reload lost sound");
        sound.setSelectedItem(""); check(second.getSoundName().isEmpty() && !reloaded.getSoundName().isEmpty(), "Independent no-sound failed");
        set(gui,"activeElement",null); call(gui,"refreshPageSoundChoices"); check(!sound.isEnabled(), "No selection leaves inspector enabled");
    }
    private static void runtime(Path template) throws Exception {
        String source = Files.readString(template);
        int start = source.indexOf("    private Page soundPage;");
                String methods = source.substring(start, source.indexOf("    private void renderSoundElement", start));
        int removed = source.indexOf("    @Override public void removed()", start);
        methods += source.substring(removed, source.indexOf("    </#if>", removed));
        methods = methods.replace("net.minecraft.client.resources.sounds.", "")
                .replace("net.minecraft.sounds.SoundSource.RECORDS", "\"RECORDS\"")
                .replace("net.minecraft.util.RandomSource.create()", "new Object()")
                .replace("${modid}", "audit");
        String fixture = """
            import java.util.*;
            class Parent { public void removed() {} }
            public class AudioFixture extends Parent {
                record Page(VisualElement[] elements) {}
                record VisualElement(String type, String mediaName, String soundTrigger, String translationKey) {}
                interface SoundInstance { enum Attenuation { NONE } }
                record ResourceLocation(String namespace, String name) {
                    static ResourceLocation fromNamespaceAndPath(String n, String p) { return new ResourceLocation(n,p); }
                }
                record Identifier(String namespace, String name) {
                    static Identifier fromNamespaceAndPath(String n, String p) { return new Identifier(n,p); }
                }
                static final class SimpleSoundInstance implements SoundInstance {
                    final String source;
                    SimpleSoundInstance(Object... args) { source = args.length > 1 ? (String) args[1] : "UNRELATED"; }
                }
                static final class Manager {
                    int plays, stops;
                    Set<SoundInstance> active = new HashSet<>();
                    void play(SoundInstance s) {
                        if (!"RECORDS".equals(((SimpleSoundInstance)s).source))
                            throw new AssertionError("Book sound does not use the Jukebox/Note Blocks mixer channel");
                        active.add(s); plays++;
                    }
                    void stop(SoundInstance s) { active.remove(s); stops++; }
                    boolean isActive(SoundInstance s) { return active.contains(s); }
                }
                static final class Client { Manager manager = new Manager(); Manager getSoundManager() { return manager; } }
                private Client minecraft = new Client();
                private Page page;
                private Page currentPage() { return page; }
                %s
                static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
                public static void run() {
                    AudioFixture screen = new AudioFixture();
                    Manager manager = screen.minecraft.manager;
                    SoundInstance unrelated = new SimpleSoundInstance(); manager.active.add(unrelated);
                    var auto = new VisualElement("SOUND","intro","OPEN","a");
                    var auto2 = new VisualElement("SOUND","ambient","OPEN","a2");
                    var button = new VisualElement("SOUND","manual","BUTTON","b");
                    var button2 = new VisualElement("SOUND","manual2","BUTTON","b2");
                    screen.page = new Page(new VisualElement[]{auto,auto2,button,button2}); screen.updatePageSound();
                    for(int i=0;i<100;i++)screen.updatePageSound();
                    check(manager.plays==2 && manager.stops==0,"Auto/button modes or repeated render start failed");
                    screen.togglePageSound("b");screen.togglePageSound("b2");check(manager.plays==4,"Independent buttons did not play");
                    screen.togglePageSound("b");check(manager.stops==1 && screen.isPageSoundPlaying(button2),"One button stopped another track");
                    screen.togglePageSound("a");check(manager.plays==4,"Automatic sound activated as a button");
                    manager.active.remove(screen.pageSounds.get(button2));screen.updatePageSound();check(manager.plays==4,"Finished track restarts itself");
                    screen.togglePageSound("b2");check(manager.plays==5,"Finished button track cannot replay");
                    screen.page=new Page(new VisualElement[0]);screen.updatePageSound();check(manager.active.equals(Set.of(unrelated)),"Page change leaks or stops unrelated audio");
                    screen.page=new Page(new VisualElement[]{auto});screen.updatePageSound();check(manager.plays==6,"Returning page did not play");
                    screen.removed();check(manager.active.equals(Set.of(unrelated)),"Close leaked audio");
                    screen.updatePageSound();check(manager.plays==7,"Reopening did not reset playback");
                    screen.page=null;screen.updatePageSound();check(manager.active.equals(Set.of(unrelated)),"Empty category leaked audio");
                }
            }
            """.formatted(methods);
        Path directory = Files.createTempDirectory("custombook-audio-test-");
        try {
            Path java = directory.resolve("AudioFixture.java"); Files.writeString(java, fixture);
            int result = ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","21","-d",directory.toString(),java.toString());
            check(result == 0, "Audio fixture compilation failed");
            try (var loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()})) {
                loader.loadClass("AudioFixture").getMethod("run").invoke(null);
            }
        } finally {
            try(var paths = Files.walk(directory)) {
                for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
    private static void set(Object object,String name,Object value)throws Exception {
        Field f=CustomBookGUI.class.getDeclaredField(name); f.setAccessible(true); f.set(object,value);
    }
    private static void call(Object object,String name)throws Exception {
        Method m=CustomBookGUI.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);
    }
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
}
