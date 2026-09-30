package ${package}.event;

import java.util.HashSet;
import java.util.Set;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.util.datafix.DataFixTypes;

/** One persistent overworld ledger per book type; respawns and dimension changes cannot reset it. */
public final class ${name}StartingBookEvents extends SavedData {
    private static final SavedDataType<${name}StartingBookEvents> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("${modid}", "received_starting_book_${registryname}"),
            ${name}StartingBookEvents::new,
            Codec.STRING.listOf().xmap(${name}StartingBookEvents::load, data -> java.util.List.copyOf(data.received)),
            DataFixTypes.LEVEL);
    private final Set<String> received = new HashSet<>();
    private static boolean registered;

    private static ${name}StartingBookEvents load(java.util.List<String> players) {
        var data = new ${name}StartingBookEvents();
        data.received.addAll(players);
        return data;
    }
    public static synchronized void register(Item item) {
        if (registered) return;
        registered = true;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            var data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
            String key = player.getUUID().toString();
            if (data.received.contains(key)) return;
            ItemStack book = new ItemStack(item);
            if (!player.getInventory().add(book) && player.drop(book, false) == null) return;
            data.received.add(key);
            data.setDirty();
        });
    }
}
