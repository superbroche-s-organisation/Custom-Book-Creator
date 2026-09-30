import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.tools.ToolProvider;

/** Executes the actual Fabric ledger template with observable join, storage and inventory fixtures. */
public final class FabricStartingBookTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("custombook.pluginRoot", "src/main/resources"));
        try (var profiles = Files.list(root)) {
            for (Path profile : profiles.filter(p -> p.getFileName().toString().startsWith("fabric-")).sorted().toList()) {
                verify(profile.getFileName().toString().substring(7));
            }
        }
    }
    private static void verify(String version) throws Exception {
        boolean modern = version.startsWith("26.");
        int patch = modern ? 6 : version.equals("1.20") ? 0 : Integer.parseInt(version.substring(5));
        boolean components = patch >= 5;
        String lookup = components ? ", net.minecraft.core.HolderLookup.Provider registries" : "";
        Path temp = Files.createTempDirectory("custombook-fabric-starting-test-");
        try {
            Path source = temp.resolve("src"), classes = Files.createDirectories(temp.resolve("classes"));
            Path template = Path.of(System.getProperty("custombook.pluginRoot", "src/main/resources"),
                    "fabric-" + version, "templates/custombook/starting_book_events.java.ftl");
            String generated = Files.readString(template).replace("${package}", "audit").replace("${name}", "Audit")
                    .replace("${modid}", "audit_mod").replace("${registryname}", "book");
            write(source, "audit/event/AuditStartingBookEvents.java", generated);
            write(source, "net/minecraft/nbt/CompoundTag.java", """
                    package net.minecraft.nbt;
                    public class CompoundTag {
                        private final java.util.Map<String,Object> data = new java.util.HashMap<>();
                        public CompoundTag getCompound(String key) { return (CompoundTag)data.getOrDefault(key, new CompoundTag()); }
                        public void put(String key, CompoundTag value) { data.put(key,value); }
                        public boolean getBoolean(String key) { return Boolean.TRUE.equals(data.get(key)); }
                        public void putBoolean(String key, boolean value) { data.put(key,value); }
                        public java.util.Set<String> getAllKeys() { return data.keySet(); }
                    }
                    """);
            write(source, "net/minecraft/core/HolderLookup.java", "package net.minecraft.core; public class HolderLookup { public interface Provider {} }");
            write(source, "net/minecraft/util/datafix/DataFixTypes.java", "package net.minecraft.util.datafix; public enum DataFixTypes { LEVEL }");
            write(source, "net/minecraft/world/item/Item.java", "package net.minecraft.world.item; public class Item {}");
            write(source, "net/minecraft/world/item/ItemStack.java", "package net.minecraft.world.item; public record ItemStack(Item item) {}");
            String loaderType = components ? "java.util.function.BiFunction<CompoundTag, net.minecraft.core.HolderLookup.Provider,T>"
                    : "java.util.function.Function<CompoundTag,T>";
            write(source, "net/minecraft/world/level/saveddata/SavedData.java", """
                    package net.minecraft.world.level.saveddata;
                    import net.minecraft.nbt.CompoundTag;
                    public abstract class SavedData {
                        public boolean dirty;
                        public void setDirty() { dirty = true; }
                        public abstract CompoundTag save(CompoundTag tag%s);
                        public record Factory<T extends SavedData>(java.util.function.Supplier<T> constructor,
                            %s loader, net.minecraft.util.datafix.DataFixTypes type) {}
                    }
                    """.formatted(lookup, loaderType));
            String loadCall = components ? "factory.loader().apply(saved, null)" : "factory.loader().apply(saved)";
            String storageGet = patch <= 1 ? """
                    @SuppressWarnings("unchecked")
                    public <T extends SavedData> T computeIfAbsent(java.util.function.Function<CompoundTag,T> load,
                            java.util.function.Supplier<T> create, String key) {
                        if (current == null) current = saved == null ? create.get() : load.apply(saved);
                        return (T)current;
                    }
                    """ : """
                    @SuppressWarnings("unchecked")
                    public <T extends SavedData> T computeIfAbsent(SavedData.Factory<T> factory, String key) {
                        if (current == null) current = saved == null ? factory.constructor().get() : %s;
                        return (T)current;
                    }
                    """.formatted(loadCall);
            write(source, "net/minecraft/world/level/storage/DimensionDataStorage.java", """
                    package net.minecraft.world.level.storage;
                    import net.minecraft.nbt.CompoundTag;
                    import net.minecraft.world.level.saveddata.SavedData;
                    public class DimensionDataStorage {
                        public SavedData current;
                        public CompoundTag saved;
                        %s
                        public void restart() { saved = current.save(new CompoundTag()%s); current = null; }
                    }
                    """.formatted(storageGet, components ? ", null" : ""));
            write(source, "net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.java", """
                    package net.fabricmc.fabric.api.networking.v1;
                    public final class ServerPlayConnectionEvents {
                        public static final JoinEvent JOIN = new JoinEvent();
                        public interface Join { void onJoin(Handler handler, Object sender, Server server); }
                        public static final class JoinEvent {
                            public int registrations;
                            public Join callback;
                            public void register(Join listener) { registrations++; callback = listener; }
                        }
                        public record Handler(Player player) { }
                        public static final class Server {
                            public final World world = new World();
                            public World overworld() { return world; }
                        }
                        public static final class World {
                            public final net.minecraft.world.level.storage.DimensionDataStorage storage = new net.minecraft.world.level.storage.DimensionDataStorage();
                            public net.minecraft.world.level.storage.DimensionDataStorage getDataStorage() { return storage; }
                        }
                        public static final class Player {
                            public final java.util.UUID id;
                            public boolean accepts = true, dropSucceeds = true, failAdd;
                            public int added, dropped;
                            public Player(java.util.UUID id) { this.id = id; }
                            public java.util.UUID getUUID() { return id; }
                            public Inventory getInventory() { return new Inventory(); }
                            public Object drop(net.minecraft.world.item.ItemStack stack, boolean random) {
                                if (!dropSucceeds) return null;
                                dropped++; return stack;
                            }
                            public final class Inventory {
                                public boolean add(net.minecraft.world.item.ItemStack stack) {
                                    if (failAdd) throw new IllegalStateException("Injected inventory error");
                                    if (accepts) added++;
                                    return accepts;
                                }
                            }
                        }
                    }
                    """);
            // Handler.player is a public field in the real server API.
            Path callbacks = source.resolve("net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.java");
            Files.writeString(callbacks, Files.readString(callbacks).replace("public record Handler(Player player) { }",
                    "public static final class Handler { public final Player player; public Handler(Player value) {player=value;} }"));
            write(source, "audit/Behavior.java", """
                    package audit;
                    import audit.event.AuditStartingBookEvents;
                    import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
                    import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.*;
                    public final class Behavior {
                        private static void check(boolean okay) { if (!okay) throw new AssertionError("Fabric starting-book regression"); }
                        private static void join(Player player, Server server) { ServerPlayConnectionEvents.JOIN.callback.onJoin(new Handler(player), null, server); }
                        public static void run() {
                            var item = new net.minecraft.world.item.Item();
                            AuditStartingBookEvents.register(item); AuditStartingBookEvents.register(item);
                            check(ServerPlayConnectionEvents.JOIN.registrations == 1);
                            Server world = new Server();
                            Player original = new Player(java.util.UUID.randomUUID());
                            join(original, world); join(original, world);
                            check(original.added == 1 && world.world.storage.current.dirty);
                            Player respawn = new Player(original.id);
                            join(respawn, world); check(respawn.added == 0);
                            world.world.storage.restart();
                            Player reconnect = new Player(original.id);
                            join(reconnect, world); check(reconnect.added == 0);
                            Player full = new Player(java.util.UUID.randomUUID()); full.accepts = false;
                            join(full, world); join(full, world); check(full.dropped == 1);
                            Player retry = new Player(java.util.UUID.randomUUID()); retry.accepts = false; retry.dropSucceeds = false;
                            join(retry, world); retry.dropSucceeds = true;
                            join(retry, world); join(retry, world); check(retry.dropped == 1);
                            Player failure = new Player(java.util.UUID.randomUUID()); failure.failAdd = true;
                            try { join(failure, world); throw new AssertionError("Failure was swallowed"); } catch (IllegalStateException expected) {}
                            failure.failAdd = false; join(failure, world); check(failure.added == 1);
                            Server anotherWorld = new Server();
                            join(original, anotherWorld); check(original.added == 2);
                        }
                    }
                    """);
            if (modern) {
                write(source, "net/minecraft/resources/Identifier.java", "package net.minecraft.resources; public record Identifier(String namespace, String path) { public static Identifier fromNamespaceAndPath(String n, String p) { return new Identifier(n, p); } }");
                write(source, "com/mojang/serialization/Codec.java", """
                    package com.mojang.serialization;
                    import java.util.*;
                    import java.util.function.Function;
                    public record Codec<T>(Function<Object,T> decode, Function<T,Object> encode) {
                        public static final Codec<String> STRING = new Codec<>(v -> (String)v, v -> v);
                        public Codec<List<T>> listOf() {
                            return new Codec<>(v -> ((List<?>)v).stream().map(decode).toList(), v -> v.stream().map(encode).toList());
                        }
                        public <R> Codec<R> xmap(Function<T,R> from, Function<R,T> to) {
                            return new Codec<>(v -> from.apply(decode.apply(v)), v -> encode.apply(to.apply(v)));
                        }
                    }
                    """);
                write(source, "net/minecraft/world/level/saveddata/SavedData.java", "package net.minecraft.world.level.saveddata; public abstract class SavedData { public boolean dirty; public void setDirty() { dirty = true; } }");
                write(source, "net/minecraft/world/level/saveddata/SavedDataType.java", """
                    package net.minecraft.world.level.saveddata;
                    public record SavedDataType<T extends SavedData>(net.minecraft.resources.Identifier id, java.util.function.Supplier<T> create,
                        com.mojang.serialization.Codec<T> codec, net.minecraft.util.datafix.DataFixTypes type) {}
                    """);
                write(source, "net/minecraft/world/level/storage/DimensionDataStorage.java", """
                    package net.minecraft.world.level.storage;
                    import net.minecraft.world.level.saveddata.*;
                    public class DimensionDataStorage {
                        public SavedData current;
                        private Object saved;
                        private com.mojang.serialization.Codec codec;
                        public <T extends SavedData> T computeIfAbsent(SavedDataType<T> type) {
                            codec = type.codec();
                            if (current == null) current = saved == null ? type.create().get() : type.codec().decode().apply(saved);
                            return (T)current;
                        }
                        public void restart() { saved = codec.encode().apply(current); current = null; }
                    }
                    """);
            }
            var compiler = ToolProvider.getSystemJavaCompiler();
            try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8); var paths = Files.walk(source)) {
                var units = manager.getJavaFileObjectsFromPaths(paths.filter(p -> p.toString().endsWith(".java")).toList());
                if (!compiler.getTask(null, manager, null, List.of("--release", components ? "21" : "17", "-d", classes.toString()),
                        null, units).call()) throw new AssertionError("Fabric fixture compile: " + version);
            }
            try (var loader = new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                loader.loadClass("audit.Behavior").getMethod("run").invoke(null);
            }
            System.out.println("FABRIC_STARTING_BOOK_OK " + version);
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    private static void write(Path root, String name, String text) throws Exception {
        Path file = root.resolve(name); Files.createDirectories(file.getParent()); Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
