package ${package}.client;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import ${package}.network.${name}BookWritingMessage;
import ${package}.book.${name}BookWritingData;

public final class ${name}BookWritingFabricClient {
    public static void register() {
        <#if cbcComponents>
        ClientPlayNetworking.registerGlobalReceiver(${name}BookWritingMessage.TYPE, (message, context) ->
                context.client().execute(() -> ${name}BookWritingClient.receive(message.json())));
        <#else>
        ClientPlayNetworking.registerGlobalReceiver(${name}BookWritingMessage.ID, (client, handler, buffer, sender) -> {
            String json = buffer.readUtf(${name}BookWritingData.MAX_WIRE_BYTES);
            client.execute(() -> ${name}BookWritingClient.receive(json));
        });
        </#if>
    }
    public static void send(String json) {
        <#if cbcComponents>
        ClientPlayNetworking.send(new ${name}BookWritingMessage(json));
        <#else>
        net.minecraft.network.FriendlyByteBuf buffer = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buffer.writeUtf(json, ${name}BookWritingData.MAX_WIRE_BYTES);
        ClientPlayNetworking.send(${name}BookWritingMessage.ID, buffer);
        </#if>
    }
}
