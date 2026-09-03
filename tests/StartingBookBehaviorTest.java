import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;

/** Executes the generated login/clone handlers against an isolated, observable game API fixture. */
public final class StartingBookBehaviorTest {
    private static final String MARKER = "audit_mod:received_starting_book_audit_book";

    public static void main(String[] args) throws Exception {
        for (String generator : List.of("neoforge-1.21.1", "neoforge-26.1.2")) {
            verify(generator);
        }
        System.out.println("STARTING_BOOK_BEHAVIOR_OK");
    }

    private static void verify(String generator) throws Exception {
        Path temporary = Files.createTempDirectory("custombook-starting-behavior-");
        try {
            Path source = temporary.resolve("src");
            Path classes = Files.createDirectories(temporary.resolve("classes"));
            write(source, "audit/event/AuditStartingBookEvents.java", render(generator));
            write(source, "net/minecraft/nbt/CompoundTag.java", """
                    package net.minecraft.nbt;
                    public class CompoundTag {
                        private final java.util.Map<String, Boolean> values = new java.util.HashMap<>();
                        public boolean getBoolean(String key) { return values.getOrDefault(key, false); }
                        public boolean getBooleanOr(String key, boolean fallback) { return values.getOrDefault(key, fallback); }
                        public void putBoolean(String key, boolean value) { values.put(key, value); }
                    }
                    """);
            write(source, "net/minecraft/world/item/ItemStack.java", """
                    package net.minecraft.world.item;
                    public class ItemStack { public ItemStack(Object item) {} }
                    """);
            write(source, "net/minecraft/world/entity/player/Player.java", """
                    package net.minecraft.world.entity.player;
                    public class Player {
                        private final net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                        public net.minecraft.nbt.CompoundTag getPersistentData() { return data; }
                    }
                    """);
            write(source, "net/minecraft/server/level/ServerPlayer.java", """
                    package net.minecraft.server.level;
                    public class ServerPlayer extends net.minecraft.world.entity.player.Player {
                        public boolean accept = true;
                        public boolean failAdd;
                        public boolean failDrop;
                        public int addCalls;
                        public int drops;
                        public Inventory getInventory() { return new Inventory(); }
                        public Object drop(net.minecraft.world.item.ItemStack item, boolean random) {
                            if (failDrop) throw new IllegalStateException("injected drop failure");
                            drops++;
                            return item;
                        }
                        public final class Inventory {
                            public boolean add(net.minecraft.world.item.ItemStack item) {
                                if (failAdd) throw new IllegalStateException("injected inventory failure");
                                addCalls++;
                                return accept;
                            }
                        }
                    }
                    """);
            write(source, "net/neoforged/bus/api/SubscribeEvent.java", """
                    package net.neoforged.bus.api;
                    public @interface SubscribeEvent {}
                    """);
            write(source, "net/neoforged/fml/common/EventBusSubscriber.java", """
                    package net.neoforged.fml.common;
                    public @interface EventBusSubscriber { String modid(); }
                    """);
            write(source, "net/neoforged/neoforge/event/entity/player/PlayerEvent.java", """
                    package net.neoforged.neoforge.event.entity.player;
                    import net.minecraft.world.entity.player.Player;
                    public class PlayerEvent {
                        private final Player player;
                        public PlayerEvent(Player player) { this.player = player; }
                        public Player getEntity() { return player; }
                        public static class PlayerLoggedInEvent extends PlayerEvent {
                            public PlayerLoggedInEvent(Player player) { super(player); }
                        }
                        public static class Clone extends PlayerEvent {
                            private final Player original;
                            public Clone(Player player, Player original) { super(player); this.original = original; }
                            public Player getOriginal() { return original; }
                        }
                    }
                    """);
            write(source, "audit/init/AuditModItems.java", """
                    package audit.init;
                    public class AuditModItems {
                        public static final Holder AUDIT_BOOK = new Holder();
                        public static class Holder { public Object get() { return new Object(); } }
                    }
                    """);

            var compiler = ToolProvider.getSystemJavaCompiler();
            require(compiler != null, "a JDK is required for the generated-handler test");
            try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
                    var paths = Files.walk(source)) {
                var units = manager.getJavaFileObjectsFromPaths(paths.filter(path -> path.toString().endsWith(".java")).toList());
                require(compiler.getTask(null, manager, null,
                        List.of("--release", "21", "-proc:none", "-d", classes.toString()), null, units).call(),
                        generator + ": generated behavior fixture did not compile");
            }

            try (var loader = new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                Class<?> playerType = loader.loadClass("net.minecraft.world.entity.player.Player");
                Class<?> serverType = loader.loadClass("net.minecraft.server.level.ServerPlayer");
                Class<?> loginType = loader.loadClass("net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent");
                Class<?> cloneType = loader.loadClass("net.neoforged.neoforge.event.entity.player.PlayerEvent$Clone");
                Class<?> handler = loader.loadClass("audit.event.AuditStartingBookEvents");
                var login = handler.getMethod("onPlayerLoggedIn", loginType);
                var clone = handler.getMethod("onPlayerClone", cloneType);

                Object client = playerType.getConstructor().newInstance();
                login.invoke(null, loginType.getConstructor(playerType).newInstance(client));
                require(!marked(client), generator + ": client-side player received a marker");

                Object player = serverType.getConstructor().newInstance();
                Object event = loginType.getConstructor(playerType).newInstance(player);
                login.invoke(null, event);
                login.invoke(null, event);
                require(count(player, "addCalls") == 1 && count(player, "drops") == 0 && marked(player),
                        generator + ": repeated login did not grant exactly once");

                Object full = serverType.getConstructor().newInstance();
                serverType.getField("accept").setBoolean(full, false);
                Object fullEvent = loginType.getConstructor(playerType).newInstance(full);
                login.invoke(null, fullEvent);
                login.invoke(null, fullEvent);
                require(count(full, "addCalls") == 1 && count(full, "drops") == 1 && marked(full),
                        generator + ": full inventory lost or duplicated the starting book");

                Object failing = serverType.getConstructor().newInstance();
                serverType.getField("failAdd").setBoolean(failing, true);
                Object failingEvent = loginType.getConstructor(playerType).newInstance(failing);
                expectFailure(() -> login.invoke(null, failingEvent));
                require(!marked(failing), generator + ": failed grant incorrectly persisted a received marker");
                serverType.getField("failAdd").setBoolean(failing, false);
                login.invoke(null, failingEvent);
                require(marked(failing) && count(failing, "addCalls") == 1,
                        generator + ": failed grant could not be retried");

                Object failedDrop = serverType.getConstructor().newInstance();
                serverType.getField("accept").setBoolean(failedDrop, false);
                serverType.getField("failDrop").setBoolean(failedDrop, true);
                expectFailure(() -> login.invoke(null, loginType.getConstructor(playerType).newInstance(failedDrop)));
                require(!marked(failedDrop), generator + ": failed drop incorrectly persisted a received marker");

                Object respawn = serverType.getConstructor().newInstance();
                clone.invoke(null, cloneType.getConstructor(playerType, playerType).newInstance(respawn, player));
                login.invoke(null, loginType.getConstructor(playerType).newInstance(respawn));
                require(marked(respawn) && count(respawn, "addCalls") == 0,
                        generator + ": player clone lost the once-only marker");

                Object unmarkedClone = serverType.getConstructor().newInstance();
                clone.invoke(null, cloneType.getConstructor(playerType, playerType).newInstance(unmarkedClone, client));
                require(!marked(unmarkedClone), generator + ": clone invented a received marker");
            }
            System.out.println("STARTING_BOOK_BEHAVIOR_OK " + generator);
        } finally {
            try (var paths = Files.walk(temporary)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static boolean marked(Object player) throws Exception {
        Object tag = player.getClass().getMethod("getPersistentData").invoke(player);
        return (Boolean) tag.getClass().getMethod("getBoolean", String.class).invoke(tag, MARKER);
    }

    private static int count(Object player, String field) throws Exception {
        return player.getClass().getField(field).getInt(player);
    }

    private static void expectFailure(CheckedAction action) throws Exception {
        try { action.run(); throw new AssertionError("injected failure was not raised"); }
        catch (InvocationTargetException expected) {
            require(expected.getCause() instanceof IllegalStateException, "unexpected handler failure: " + expected.getCause());
        }
    }

    private static String render(String generator) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_34);
        configuration.setDirectoryForTemplateLoading(Path.of("src", "main", "resources", generator, "templates", "custombook").toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_34);
        wrapper.setExposeFields(true);
        configuration.setObjectWrapper(wrapper.build());
        StringWriter output = new StringWriter();
        configuration.getTemplate("starting_book_events.java.ftl").process(Map.of(
                "package", "audit", "JavaModName", "AuditMod", "modid", "audit_mod", "name", "Audit",
                "registryname", "audit_book", "data", new Data()), output);
        return output.toString();
    }

    private static void write(Path root, String relative, String content) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    public static final class Data { public Element getModElement() { return new Element(); } }
    public static final class Element { public String getRegistryNameUpper() { return "AUDIT_BOOK"; } }
}
