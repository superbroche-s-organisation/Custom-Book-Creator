package ${package}.network;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import ${package}.${JavaModName};
import ${package}.book.${name}BookWritingData;
import ${package}.book.${name}BookWritingServer;

@Mod.EventBusSubscriber(modid = "${modid}", bus = Mod.EventBusSubscriber.Bus.MOD)
public record ${name}BookWritingMessage(String json) {
    public static void encode(${name}BookWritingMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.json, ${name}BookWritingData.MAX_WIRE_BYTES);
    }
    public static ${name}BookWritingMessage decode(FriendlyByteBuf buffer) {
        return new ${name}BookWritingMessage(buffer.readUtf(${name}BookWritingData.MAX_WIRE_BYTES));
    }
    public static void handle(${name}BookWritingMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) ${name}BookWritingServer.receive(context.getSender(), message.json);
            else ${package}.client.${name}BookWritingClient.receive(message.json);
        });
        context.setPacketHandled(true);
    }
    @SubscribeEvent public static void register(FMLCommonSetupEvent event) {
        ${JavaModName}.addNetworkMessage(${name}BookWritingMessage.class, ${name}BookWritingMessage::encode,
                ${name}BookWritingMessage::decode, ${name}BookWritingMessage::handle);
    }
    public static void sendToServer(String json) { ${JavaModName}.PACKET_HANDLER.sendToServer(new ${name}BookWritingMessage(json)); }
    public static void sendToPlayer(ServerPlayer player, String json) {
        ${JavaModName}.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ${name}BookWritingMessage(json));
    }
}
