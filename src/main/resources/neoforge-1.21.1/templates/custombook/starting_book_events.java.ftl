package ${package}.event;

import ${package}.init.${JavaModName}Items;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "${modid}")
public final class ${name}StartingBookEvents {
    private static final String RECEIVED_TAG = "${modid}:received_starting_book_${registryname}";

    private ${name}StartingBookEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean(RECEIVED_TAG)) {
            return;
        }

        persistentData.putBoolean(RECEIVED_TAG, true);
        ItemStack book = new ItemStack(${JavaModName}Items.${data.getModElement().getRegistryNameUpper()}.get());
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(RECEIVED_TAG)) {
            event.getEntity().getPersistentData().putBoolean(RECEIVED_TAG, true);
        }
    }
}
