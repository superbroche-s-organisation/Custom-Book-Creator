import com.google.gson.JsonObject;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import java.io.StringWriter;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.ToolProvider;

/** Executes the generated production protocol with deterministic server stand-ins.
 * Real Minecraft/loader compilation is an additional, separate validation step. */
public final class WritingRuntimeTest {
    public static void main(String[] args) throws Exception {
        verifyModel();
        Path root = Files.createTempDirectory("custombook-writing-test-");
        try {
            Configuration config = new Configuration(Configuration.VERSION_2_3_31);
            config.setDirectoryForTemplateLoading(Path.of("compatibility/templates").toFile());
            BeansWrapperBuilder wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_31);
            wrapper.setExposeFields(true);
            config.setObjectWrapper(wrapper.build());
            Map<String, Object> model = new HashMap<>();
            model.put("package", "writingtest");
            model.put("name", "Test");
            model.put("registryname", "test");
            model.put("modid", "test");
            model.put("data", new BookFixture());
            for (String flag : List.of("cbcComponents", "cbcOptionalNbt", "cbcModernProfile"))
                model.put(flag, false);
            for (String suffix : List.of("data", "server", "client")) {
                StringWriter out = new StringWriter();
                config.getTemplate("writing_" + suffix + ".java.ftl").process(model, out);
                String folder = suffix.equals("client") ? "client" : "book";
                String classSuffix = Character.toUpperCase(suffix.charAt(0)) + suffix.substring(1);
                write(root, "writingtest/" + folder + "/TestBookWriting" + classSuffix + ".java", out.toString());
            }
            for (var entry : fixtures().entrySet()) write(root, entry.getKey(), entry.getValue());
            List<String> options = new ArrayList<>(List.of("--release", "17", "-proc:none", "-encoding", "UTF-8",
                    "-cp", System.getProperty("java.class.path"), "-d", root.toString()));
            try (var files = Files.walk(root)) {
                files.filter(p -> p.toString().endsWith(".java")).forEach(p -> options.add(p.toString()));
            }
            int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, options.toArray(String[]::new));
            require(result == 0, "generated writing protocol did not compile");
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { root.toUri().toURL() },
                    WritingRuntimeTest.class.getClassLoader())) {
                loader.loadClass("writingtest.ProtocolChecks").getMethod("run").invoke(null);
            }
            System.out.println("WRITING_RUNTIME_OK");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void verifyModel() {
        CustomBook book = new CustomBook(null);
        var page = book.getBookCategories().get(0).pages.get(0);
        page.elements.clear();
        var first = CustomBook.BookElement.input();
        first.inputKey = "terms";
        page.elements.add(first);
        var second = CustomBook.BookElement.input();
        second.inputKey = "terms";
        page.elements.add(second);
        require(book.getWritingZones().size() == 2 && !first.inputKey.equals(second.inputKey), "duplicate persistent keys");
        String key = second.inputKey;
        book.getWritingZones();
        require(key.equals(second.inputKey), "keys change when reopening");
        book.stackSize = 64;
        require(book.getSafeStackSize() == 1, "writable books must not stack");
        second.inputMode = "invalid";
        second.inputMaxLength = -20;
        second.normalize();
        require(second.inputMode.equals("BOTH") && second.inputMaxLength == 1, "unsafe field limits");
        for (int i = 2; i < 65; i++) page.elements.add(CustomBook.BookElement.input());
        try { book.getWritingZones(); throw new AssertionError("65 zones accepted"); }
        catch (IllegalStateException expected) { require(expected.getMessage().contains("64"), "unhelpful limit message"); }
    }

    public static final class BookFixture {
        public boolean allowSigning = true;
        public Callback writingPermission = new Callback("CanWrite");
        public Callback onBookSigned = new Callback("Signed");
        public List<CustomBook.BookElement> getWritingZones() {
            List<CustomBook.BookElement> zones = new ArrayList<>();
            for (String key : List.of("terms", "signed", "state", "title", "choice")) {
                var field = CustomBook.BookElement.input();
                field.inputKey = key;
                field.label = key;
                field.inputMode = key.equals("choice") ? "PRESET" : "BOTH";
                field.inputChoices = List.of("Accept", "Decline");
                zones.add(field);
            }
            return zones;
        }
        public ElementFixture getModElement() { return new ElementFixture(); }
    }
    public record ElementFixture() { public Object getWorkspace() { return "workspace"; } }
    public record DependencyFixture(String name) { public String getName() { return name; } }
    public record Callback(String name) {
        public boolean getExists() { return true; }
        public String getName() { return name; }
        public List<DependencyFixture> getDependencies(Object workspace) {
            return List.of(new DependencyFixture("entity"), new DependencyFixture("itemstack"));
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void write(Path root, String name, String value) throws Exception {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, value);
    }

    private static Map<String, String> fixtures() {
        Map<String, String> files = new HashMap<>();
        files.put("net/minecraft/nbt/CompoundTag.java", """
            package net.minecraft.nbt;
            public final class CompoundTag {
                private final java.util.Map<String, Object> values = new java.util.HashMap<>();
                public String getString(String key) { Object v = values.get(key); return v instanceof String s ? s : ""; }
                public boolean getBoolean(String key) { return Boolean.TRUE.equals(values.get(key)); }
                public void putString(String key, String value) { values.put(key, value); }
                public void putBoolean(String key, boolean value) { values.put(key, value); }
                public CompoundTag copy() { CompoundTag c = new CompoundTag(); c.values.putAll(values); return c; }
            }
            """);
        files.put("net/minecraft/world/InteractionHand.java", """
            package net.minecraft.world;
            public enum InteractionHand { MAIN_HAND, OFF_HAND }
            """);
        files.put("net/minecraft/world/item/ItemStack.java", """
            package net.minecraft.world.item;
            public final class ItemStack {
                private Object item;
                private net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                public int count = 1;
                public ItemStack(Object item) { this.item = item; }
                public Object getItem() { return item; }
                public int getCount() { return count; }
                public net.minecraft.nbt.CompoundTag getOrCreateTag() { return tag; }
                public void setTag(net.minecraft.nbt.CompoundTag tag) { this.tag = tag; }
                public ItemStack copy() { ItemStack copy = new ItemStack(item); copy.tag = tag.copy(); copy.count = count; return copy; }
            }
            """);
        files.put("net/minecraft/server/level/ServerPlayer.java", """
            package net.minecraft.server.level;
            import net.minecraft.world.item.ItemStack;
            import net.minecraft.world.InteractionHand;
            public final class ServerPlayer {
                public ItemStack main = new ItemStack(new writingtest.item.TestItem());
                public ItemStack off = new ItemStack(new writingtest.item.TestItem());
                public boolean alive = true, allowed = true, throwHook, mutatePermission, replacePermission;
                public int permissionCalls, signatures;
                public String reply;
                public boolean failReply;
                public Inventory inventoryMenu = new Inventory();
                private final java.util.UUID id = java.util.UUID.randomUUID();
                public ItemStack getItemInHand(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? main : off; }
                public boolean isAlive() { return alive; }
                public Object level() { return this; }
                public double getX() { return 0; }
                public double getY() { return 0; }
                public double getZ() { return 0; }
                public java.util.UUID getUUID() { return id; }
                public Profile getGameProfile() { return new Profile(); }
                public Inventory getInventory() { return inventoryMenu; }
                public static class Profile { public String getName() { return "ActualPlayer"; } }
                public static class Inventory { public void setChanged() {} public void broadcastChanges() {} }
            }
            """);
        files.put("writingtest/item/TestItem.java", "package writingtest.item; public final class TestItem {}");
        files.put("net/minecraft/client/Minecraft.java", """
            package net.minecraft.client;
            public final class Minecraft {
                private static final Minecraft INSTANCE = new Minecraft();
                public Object level = new Object(), player = new Object();
                public static Minecraft getInstance() { return INSTANCE; }
            }
            """);
        files.put("writingtest/network/TestBookWritingMessage.java", """
            package writingtest.network;
            public final class TestBookWritingMessage {
                public static String sent;
                public static boolean failSend;
                public static void sendToServer(String json) {
                    if (failSend) throw new IllegalStateException("Disconnected");
                    sent = json;
                }
                public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, String json) {
                    if (player.failReply) throw new IllegalStateException("Disconnected");
                    player.reply = json;
                }
            }
            """);
        files.put("writingtest/procedures/CanWriteProcedure.java", """
            package writingtest.procedures;
            public final class CanWriteProcedure {
                public static boolean execute(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
                    player.permissionCalls++;
                    if (player.replacePermission) player.main = player.main.copy();
                    if (player.mutatePermission) stack.getOrCreateTag().putString(writingtest.book.TestBookWritingData.DATA_KEY, "changed");
                    stack.getOrCreateTag().putString("unrelated_permission_data", "preserved");
                    return player.allowed;
                }
            }
            """);
        files.put("writingtest/procedures/SignedProcedure.java", """
            package writingtest.procedures;
            public final class SignedProcedure {
                public static void execute(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
                    player.signatures++;
                    if (player.throwHook) throw new IllegalStateException("Deliberate test failure");
                }
            }
            """);
        files.put("writingtest/ProtocolChecks.java", """
            package writingtest;
            import com.google.gson.*;
            import java.util.*;
            import net.minecraft.server.level.ServerPlayer;
            import net.minecraft.world.item.ItemStack;
            import writingtest.book.TestBookWritingData;
            import writingtest.book.TestBookWritingServer;
            public final class ProtocolChecks {
                private static int checks;
                static void check(boolean ok, String name) { checks++; if (!ok) throw new AssertionError(name); }
                static JsonObject request(String action, String hand) {
                    JsonObject r = new JsonObject();
                    r.addProperty("action", action);
                    r.addProperty("hand", hand);
                    r.addProperty("request", UUID.randomUUID().toString());
                    return r;
                }
                static JsonObject send(ServerPlayer p, JsonObject message) {
                    p.reply = null;
                    TestBookWritingServer.receive(p, message.toString());
                    return p.reply == null ? new JsonObject() : JsonParser.parseString(p.reply).getAsJsonObject();
                }
                static JsonObject open(ServerPlayer p, String hand) { return send(p, request("open", hand)); }
                static JsonObject edit(JsonObject opened, String action, String hand) {
                    JsonObject r = request(action, hand);
                    r.addProperty("token", opened.get("token").getAsString());
                    r.add("values", opened.getAsJsonObject("state").get("values").deepCopy());
                    if (action.equals("sign")) r.addProperty("title", "Our contract");
                    return r;
                }
                static String stored(ItemStack stack) { return stack.getOrCreateTag().getString(TestBookWritingData.DATA_KEY); }
                static void rejected(ServerPlayer p, JsonObject message, String name) {
                    String before = stored(p.main);
                    JsonObject reply = send(p, message);
                    check(!reply.has("state"), name + " accepted");
                    check(stored(p.main).equals(before), name + " modified data");
                }
                static void invalid(Runnable action, String name) {
                    try { action.run(); throw new AssertionError(name + " accepted"); }
                    catch (IllegalArgumentException expected) { checks++; }
                }
                public static void run() throws Exception {
                    ServerPlayer p = new ServerPlayer();
                    JsonObject first = open(p, "MAIN");
                    check(first.get("writable").getAsBoolean(), "default writing denied");
                    JsonObject save = edit(first, "save", "MAIN");
                    save.getAsJsonObject("values").addProperty("terms", "A contract\\\\nWith two lines 📖");
                    save.getAsJsonObject("values").addProperty("signed", "Text named signed");
                    save.getAsJsonObject("values").addProperty("state", "Text named state");
                    save.getAsJsonObject("values").addProperty("choice", "Accept");
                    check(send(p, save).has("state"), "save failed");
                    check(!stored(p.main).isEmpty() && stored(p.off).isEmpty(), "per-copy persistence");
                    check(p.main.getOrCreateTag().getString("unrelated_permission_data").equals("preserved"), "permission data overwritten");
                    check(p.main.getOrCreateTag().getString(TestBookWritingData.FIELD_PREFIX + "signed").equals("Text named signed"), "metadata collision");
                    JsonObject reopened = open(p, "MAIN");
                    check(reopened.getAsJsonObject("state").getAsJsonObject("values").get("choice").getAsString().equals("Accept"), "value lost on reopen");
                    JsonObject sign = edit(reopened, "sign", "MAIN");
                    sign.addProperty("author", "Forged author");
                    JsonObject signed = send(p, sign).getAsJsonObject("state");
                    check(signed != null && signed.get("signed").getAsBoolean(), "not signed");
                    check(signed.get("author").getAsString().equals("ActualPlayer"), "spoofed author");
                    check(signed.get("authorId").getAsString().equals(p.getUUID().toString()), "wrong signer UUID");
                    check(p.signatures == 1, "signature trigger count");
                    rejected(p, sign, "signature replay");
                    check(p.signatures == 1, "trigger ran twice");
                    JsonObject locked = open(p, "MAIN");
                    check(!locked.get("writable").getAsBoolean(), "signed book is editable");
                    rejected(p, edit(locked, "save", "MAIN"), "signed book editing");
                    ServerPlayer transferred = new ServerPlayer();
                    transferred.main = p.main.copy();
                    check(!open(transferred, "MAIN").get("writable").getAsBoolean(), "copied signature unlocked");

                    for (String attack : List.of("unknown", "missing", "number", "oversize", "preset", "token",
                            "swap", "count", "dead", "denied", "stale", "wronghand", "permission-swap", "permission-mutation")) {
                        ServerPlayer a = new ServerPlayer();
                        JsonObject r = edit(open(a, "MAIN"), "save", "MAIN");
                        JsonObject values = r.getAsJsonObject("values");
                        switch (attack) {
                            case "unknown" -> values.addProperty("unexpected", "x");
                            case "missing" -> values.remove("terms");
                            case "number" -> values.addProperty("terms", 2);
                            case "oversize" -> values.addProperty("terms", "x".repeat(1025));
                            case "preset" -> values.addProperty("choice", "not listed");
                            case "token" -> r.addProperty("token", UUID.randomUUID().toString());
                            case "swap" -> a.main = a.main.copy();
                            case "count" -> a.main.count = 2;
                            case "dead" -> a.alive = false;
                            case "denied" -> a.allowed = false;
                            case "stale" -> a.main.getOrCreateTag().putString(TestBookWritingData.DATA_KEY, "{}");
                            case "wronghand" -> r.addProperty("hand", "OFF");
                            case "permission-swap" -> a.replacePermission = true;
                            case "permission-mutation" -> a.mutatePermission = true;
                        }
                        if (attack.equals("permission-mutation")) {
                            check(!send(a, r).has("state"), "procedure mutation accepted");
                            check(stored(a.main).equals("changed"), "procedure changes overwritten");
                        } else rejected(a, r, attack);
                    }
                    ServerPlayer off = new ServerPlayer();
                    JsonObject offSave = edit(open(off, "OFF"), "save", "OFF");
                    check(send(off, offSave).has("state") && stored(off.main).isEmpty() && !stored(off.off).isEmpty(), "offhand persistence");
                    ServerPlayer badHook = new ServerPlayer();
                    badHook.throwHook = true;
                    JsonObject hookReply = send(badHook, edit(open(badHook, "MAIN"), "sign", "MAIN"));
                    check(hookReply.has("state") && badHook.signatures == 1, "hook error rolled back signature");
                    check(hookReply.get("status").getAsString().contains("procedure failed"), "hook error hidden");
                    ServerPlayer noReply = new ServerPlayer();
                    JsonObject disconnected = edit(open(noReply, "MAIN"), "save", "MAIN");
                    noReply.failReply = true;
                    send(noReply, disconnected);
                    check(!stored(noReply.main).isEmpty(), "disconnect erased committed data");
                    ServerPlayer flood = new ServerPlayer();
                    for (int i = 0; i < 100; i++) open(flood, "MAIN");
                    check(flood.permissionCalls <= 9, "packet flood is not bounded");

                    check(TestBookWritingData.validText("é漢字 📖\\\\n\\\\t", 30), "unicode rejected");
                    check(!TestBookWritingData.validText("\\\\uD800", 10), "unpaired surrogate accepted");
                    check(!TestBookWritingData.validText("§k", 10), "format injection accepted");
                    check(!TestBookWritingData.validText("\\\\u0000", 10), "control char accepted");
                    invalid(() -> TestBookWritingData.parse("{".repeat(8)), "deep JSON");
                    invalid(() -> TestBookWritingData.parse("x".repeat(TestBookWritingData.MAX_WIRE_BYTES + 1)), "large JSON");
                    invalid(() -> TestBookWritingData.parse("[]"), "JSON array");
                    invalid(() -> TestBookWritingData.parse("{" + (char)34 + "x" + (char)34 + ": [1, 2]"), "unbalanced JSON");
                    JsonObject aggregate = TestBookWritingData.encodeValues(TestBookWritingData.defaults());
                    for (String key : List.of("terms", "signed", "state", "title")) aggregate.addProperty(key, "x".repeat(1024));
                    aggregate.addProperty("choice", "Accept");
                    invalid(() -> TestBookWritingData.decodeValues(aggregate), "book total limit");
                    ServerPlayer escapedText = new ServerPlayer();
                    JsonObject maximum = edit(open(escapedText, "MAIN"), "save", "MAIN");
                    for (String key : List.of("terms", "signed", "state", "title"))
                        maximum.getAsJsonObject("values").addProperty(key, String.valueOf((char)0x2028).repeat(1024));
                    check(maximum.toString().length() > 24000, "fixture did not exercise JSON escaping");
                    check(send(escapedText, maximum).has("state"), "valid maximum escaped text could not be saved");
                    JsonObject maxFields = new JsonObject();
                    for (int i = 0; i < 64; i++) maxFields.addProperty("field_" + "x".repeat(24) + String.format("%02d", i),
                            i < 4 ? String.valueOf((char)0x2028).repeat(1024) : "");
                    TestBookWritingData.parse(maxFields.toString());
                    check(maxFields.toString().length() + 512 < TestBookWritingData.MAX_WIRE_BYTES, "64-field wire headroom");
                    clientChecks();
                    System.out.println("WRITING_PROTOCOL_CHECKS " + checks);
                }
                private static void clientChecks() throws Exception {
                    var client = new writingtest.client.TestBookWritingClient(net.minecraft.world.InteractionHand.OFF_HAND);
                    ServerPlayer server = new ServerPlayer();
                    client.load();
                    JsonObject loadRequest = JsonParser.parseString(writingtest.network.TestBookWritingMessage.sent).getAsJsonObject();
                    check(client.pending && !client.writable, "client writable before server confirmation");
                    check(loadRequest.get("hand").getAsString().equals("OFF"), "client lost offhand");
                    writingtest.client.TestBookWritingClient.receive(send(server, loadRequest).toString());
                    check(!client.pending && client.writable, "client failed to load");
                    boolean[] completed = {false};
                    client.save("terms", "Client draft", null, ok -> completed[0] = ok);
                    check(client.pending && !completed[0] && !client.value("terms").equals("Client draft"), "optimistic client save");
                    JsonObject saveRequest = JsonParser.parseString(writingtest.network.TestBookWritingMessage.sent).getAsJsonObject();
                    JsonObject response = send(server, saveRequest);
                    JsonObject stale = response.deepCopy();
                    stale.addProperty("request", UUID.randomUUID().toString());
                    writingtest.client.TestBookWritingClient.receive(stale.toString());
                    check(client.pending, "stale acknowledgement accepted");
                    writingtest.client.TestBookWritingClient.receive(response.toString());
                    check(completed[0] && client.value("terms").equals("Client draft") && !client.writable, "confirmed save failed");
                    check(stored(server.main).isEmpty() && !stored(server.off).isEmpty(), "client edited wrong hand");
                    client.load();
                    writingtest.client.TestBookWritingClient.receive(send(server, JsonParser.parseString(writingtest.network.TestBookWritingMessage.sent).getAsJsonObject()).toString());
                    writingtest.network.TestBookWritingMessage.failSend = true;
                    boolean[] failed = {false};
                    client.save("terms", "Unsent", null, ok -> failed[0] = !ok);
                    check(failed[0] && !client.pending && !client.writable && client.value("terms").equals("Client draft"), "send failure left pending or lost saved value");
                    writingtest.network.TestBookWritingMessage.failSend = false;
                    client.load();
                    var deadline = writingtest.client.TestBookWritingClient.class.getDeclaredField("deadline");
                    deadline.setAccessible(true);
                    deadline.setLong(client, 0);
                    client.tick();
                    check(!client.pending && !client.writable && client.status.contains("Reopen"), "client timeout failed");
                    client.load();
                    writingtest.client.TestBookWritingClient.receive("{");
                    check(!client.pending && !client.writable && client.status.contains("Invalid"), "malformed reply left editable client");
                    net.minecraft.client.Minecraft.getInstance().level = new Object();
                    check(!client.connected(), "session survived world switch");
                }
            }
            """.replace("\\\\", "\\"));
        return files;
    }
}
