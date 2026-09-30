package ${package}.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
<#if cbcComponents>
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
</#if>
import ${package}.book.${name}BookWritingData;
import ${package}.book.${name}BookWritingServer;

public record ${name}BookWritingMessage(String json)<#if cbcComponents> implements CustomPacketPayload</#if> {
    <#if cbcComponents>
    public static final Type<${name}BookWritingMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("${modid}", "${registryname}_writing"));
    public static final StreamCodec<FriendlyByteBuf, ${name}BookWritingMessage> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeUtf(message.json, ${name}BookWritingData.MAX_WIRE_BYTES),
            buffer -> new ${name}BookWritingMessage(buffer.readUtf(${name}BookWritingData.MAX_WIRE_BYTES)));
    @Override public Type<${name}BookWritingMessage> type() { return TYPE; }
    <#else>
    public static final ResourceLocation ID = new ResourceLocation("${modid}", "${registryname}_writing");
    </#if>
    private static boolean registered;
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        <#if cbcComponents>
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (message, context) ->
                context.server().execute(() -> ${name}BookWritingServer.receive(context.player(), message.json)));
        <#else>
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buffer, sender) -> {
            String json = buffer.readUtf(${name}BookWritingData.MAX_WIRE_BYTES);
            server.execute(() -> ${name}BookWritingServer.receive(player, json));
        });
        </#if>
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT)
            ${package}.client.${name}BookWritingFabricClient.register();
    }
    public static void sendToServer(String json) {
        ${package}.client.${name}BookWritingFabricClient.send(json);
    }
    public static void sendToPlayer(ServerPlayer player, String json) {
        <#if cbcComponents>
        ServerPlayNetworking.send(player, new ${name}BookWritingMessage(json));
        <#else>
        FriendlyByteBuf buffer = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buffer.writeUtf(json, ${name}BookWritingData.MAX_WIRE_BYTES);
        ServerPlayNetworking.send(player, ID, buffer);
        </#if>
    }
}
