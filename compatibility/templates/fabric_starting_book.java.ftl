package ${package}.event;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.util.datafix.DataFixTypes;

/** A world-scoped ledger survives death, dimension changes, reconnects and server restarts. */
public final class ${name}StartingBookEvents extends SavedData {
    private static final String DATA_ID = "${modid}_starting_book_${registryname}";
    private final Set<String> recipients = new HashSet<>();
    private static boolean registered;

    public static void register(Item item) {
        if (registered) return;
        registered = true;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            var storage = server.overworld().getDataStorage();
            ${name}StartingBookEvents ledger = @@LEDGER_GET@@;
            String playerId = player.getUUID().toString();
            if (ledger.recipients.contains(playerId)) return;
            ItemStack book = new ItemStack(item);
            if (!player.getInventory().add(book) && player.drop(book, false) == null) return;
            ledger.recipients.add(playerId);
            ledger.setDirty();
        });
    }

    private static ${name}StartingBookEvents load(CompoundTag tag@@LOOKUP_PARAMETER@@) {
        ${name}StartingBookEvents ledger = new ${name}StartingBookEvents();
        CompoundTag entries = tag.getCompound("recipients");
        for (String id : entries.getAllKeys()) {
            if (entries.getBoolean(id)) ledger.recipients.add(id);
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag@@LOOKUP_PARAMETER@@) {
        CompoundTag entries = new CompoundTag();
        for (String id : recipients) entries.putBoolean(id, true);
        tag.put("recipients", entries);
        return tag;
    }
}
