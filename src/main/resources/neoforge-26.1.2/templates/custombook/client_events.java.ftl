package ${package}.client;

import ${package}.client.gui.${name}BookScreen;
import ${package}.item.${name}Item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = "${modid}", value = Dist.CLIENT)
public final class ${name}BookClientEvents {
    private ${name}BookClientEvents() {
    }

    /**
     * Open before vanilla block/entity interaction consumes the right click.
     * This makes guide books behave consistently no matter what the player is looking at.
     */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        ItemStack stack = minecraft.player.getItemInHand(event.getHand());
        if (stack.getItem() instanceof ${name}Item) {
            minecraft.setScreen(new ${name}BookScreen());
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }
}
