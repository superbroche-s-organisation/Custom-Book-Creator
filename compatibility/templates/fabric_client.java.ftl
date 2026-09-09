package ${package}.client;

import ${package}.item.${name}Item;
import ${package}.client.gui.${name}BookScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

@Environment(EnvType.CLIENT)
public final class ${name}BookClientEvents {
    private ${name}BookClientEvents() {}

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new ${name}BookScreen());
    }

    private static boolean openHeldBook(Player player, Level world, InteractionHand hand, ${name}Item item) {
        if (!world.isClientSide() || player.isSpectator() || player.getItemInHand(hand).getItem() != item
                || Minecraft.getInstance().screen != null) return false;
        open();
        return true;
    }

    public static void register(${name}Item item) {
        UseItemCallback.EVENT.register((player, world, hand) ->
                openHeldBook(player, world, hand, item)
                        ? InteractionResultHolder.success(player.getItemInHand(hand))
                        : InteractionResultHolder.pass(player.getItemInHand(hand)));
        UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                openHeldBook(player, world, hand, item) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                openHeldBook(player, world, hand, item) ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }
}
