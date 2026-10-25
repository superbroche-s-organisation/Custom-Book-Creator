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
        check(!book.hasPageSounds(), "Old books must remain silent");
        var first = book.getBookCategories().get(0).pages.get(0);
        for (String bad : new String[]{"", "../file", "https://example.com", "bad sound", "Bad", "quote\""}) {
            first.pageSound = bad;
            check(first.getPageSound().isEmpty(), "Invalid sound accepted: " + bad);
        }
        first.pageSound = "chapter.intro";
        check(book.hasPageSounds(), "Sound not detected");
        Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
        CustomBookGUI gui = (CustomBookGUI)((sun.misc.Unsafe)unsafe.get(null)).allocateInstance(CustomBookGUI.class);
        JComboBox<String> sound = new JComboBox<>(), trigger = new JComboBox<>(new String[]{"Open", "Button"});
        set(gui, "pageSound", sound); set(gui, "pageSoundTrigger", trigger); set(gui, "activePage", first);
        call(gui, "createPageSoundPanel");
        check(sound.getSelectedItem().equals("chapter.intro"), "Existing sound reference lost");
        trigger.setSelectedIndex(1);
        check(first.getPageSoundTrigger().equals("BUTTON"), "Button mode not saved");
        var second = new CustomBook.BookPage("Second", "");
        set(gui, "activePage", second); call(gui, "refreshPageSoundChoices");
        sound.addItem("chapter.second"); sound.setSelectedItem("chapter.second");
        check(second.pageSound.equals("chapter.second") && second.getPageSoundTrigger().equals("OPEN"), "New page controls");
        check(first.pageSound.equals("chapter.intro") && first.getPageSoundTrigger().equals("BUTTON"), "Page settings leaked");
        set(gui, "activePage", first); call(gui, "refreshPageSoundChoices");
        check(trigger.getSelectedIndex() == 1, "Trigger not restored on page switch");
        Method copy = CustomBookGUI.class.getDeclaredMethod("copyCategory", CustomBook.BookCategory.class); copy.setAccessible(true);
        var category = (CustomBook.BookCategory)copy.invoke(null, book.getBookCategories().get(0));
        var reloaded = new Gson().fromJson(new Gson().toJson(category), CustomBook.BookCategory.class).pages.get(0);
        check(reloaded.pageSound.equals("chapter.intro") && reloaded.getPageSoundTrigger().equals("BUTTON"), "Copy/reload lost sound");
        sound.setSelectedItem("");
        check(first.pageSound.isEmpty() && !trigger.isEnabled(), "No sound not applied");
        check(!reloaded.pageSound.isEmpty(), "Copy aliases original");
        set(gui, "activePage", null); call(gui, "refreshPageSoundChoices");
        check(!sound.isEnabled() && !trigger.isEnabled(), "Category selection leaves sound controls enabled");
    }
    private static void runtime(Path template) throws Exception {
        String source = Files.readString(template);
        int start = source.indexOf("    private Page soundPage;");
        String methods = source.substring(start, source.indexOf("    </#if>", start));
        methods = methods.replace("net.minecraft.client.resources.sounds.", "")
                .replace("net.minecraft.sounds.SoundSource.MUSIC", "\"MUSIC\"")
                .replace("net.minecraft.util.RandomSource.create()", "new Object()")
                .replace("${modid}", "audit");
        String fixture = """
            import java.util.*;
            class Parent { public void removed() {} }
            public class AudioFixture extends Parent {
                record Page(String sound, String soundTrigger) {}
                interface SoundInstance { enum Attenuation { NONE } }
                record ResourceLocation(String namespace, String name) {
                    static ResourceLocation fromNamespaceAndPath(String n, String p) { return new ResourceLocation(n,p); }
                }
                record Identifier(String namespace, String name) {
                    static Identifier fromNamespaceAndPath(String n, String p) { return new Identifier(n,p); }
                }
                static final class SimpleSoundInstance implements SoundInstance { SimpleSoundInstance(Object... args) {} }
                static final class Manager {
                    int plays, stops;
                    Set<SoundInstance> active = new HashSet<>();
                    void play(SoundInstance s) { active.add(s); plays++; }
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
                    SoundInstance unrelated = new SimpleSoundInstance();
                    manager.active.add(unrelated);
                    screen.page = new Page("", "OPEN"); screen.updatePageSound();
                    check(manager.plays == 0, "Silent page played");
                    screen.page = new Page("intro", "OPEN"); screen.updatePageSound();
                    for (int i=0;i<100;i++) screen.updatePageSound();
                    check(manager.plays == 1 && manager.stops == 0, "Rendering/resizing retriggers sound");
                    screen.page = new Page("manual", "BUTTON"); screen.updatePageSound();
                    check(manager.plays == 1 && manager.stops == 1, "Button mode autoplays or old track leaks");
                    screen.togglePageSound(); check(manager.plays == 2, "Button does not play");
                    screen.togglePageSound(); check(manager.stops == 2, "Button does not stop");
                    screen.togglePageSound(); manager.active.remove(screen.pageSoundInstance);
                    screen.updatePageSound(); check(manager.plays == 3, "Finished track restarts itself");
                    screen.togglePageSound(); check(manager.plays == 4, "Finished track cannot replay");
                    screen.page = new Page("intro", "OPEN"); screen.updatePageSound();
                    check(manager.plays == 5, "New page did not start");
                    screen.removed(); check(manager.active.equals(Set.of(unrelated)), "Close leaked audio or stopped other sounds");
                    screen.updatePageSound(); check(manager.plays == 6, "Reopening did not reset playback");
                    screen.page = null; screen.updatePageSound();
                    check(manager.active.equals(Set.of(unrelated)), "Empty category leaked audio");
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
