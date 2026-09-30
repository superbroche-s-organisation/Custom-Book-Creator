package ${package}.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import ${package}.book.${name}BookWritingData;
import ${package}.book.${name}BookWritingServer;

@EventBusSubscriber(modid = "${modid}"<#if !cbcAutoBus>, bus = EventBusSubscriber.Bus.MOD</#if>)
public record ${name}BookWritingMessage(String json) implements CustomPacketPayload {
    public static final Type<${name}BookWritingMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("${modid}", "${registryname}_writing"));
    public static final StreamCodec<FriendlyByteBuf, ${name}BookWritingMessage> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeUtf(message.json, ${name}BookWritingData.MAX_WIRE_BYTES),
            buffer -> new ${name}BookWritingMessage(buffer.readUtf(${name}BookWritingData.MAX_WIRE_BYTES)));
    @Override public Type<${name}BookWritingMessage> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2.1").playBidirectional(TYPE, CODEC, (message, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) ${name}BookWritingServer.receive(player, message.json);
            else ${package}.client.${name}BookWritingClient.receive(message.json);
        }));
    }

    public static void sendToServer(String json) { PacketDistributor.sendToServer(new ${name}BookWritingMessage(json)); }
    public static void sendToPlayer(ServerPlayer player, String json) { PacketDistributor.sendToPlayer(player, new ${name}BookWritingMessage(json)); }
}
