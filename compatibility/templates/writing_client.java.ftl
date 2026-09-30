package ${package}.client;

import com.google.gson.JsonObject;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ${package}.book.${name}BookWritingData;
import ${package}.network.${name}BookWritingMessage;

public final class ${name}BookWritingClient {
    private static WeakReference<${name}BookWritingClient> active = new WeakReference<>(null);
    private final Object level = Minecraft.getInstance().level;
    private final InteractionHand hand;
    private Map<String, String> values = ${name}BookWritingData.defaults();
    private String request = "", token = "";
    private long deadline;
    private Consumer<Boolean> completion;
    public boolean pending, writable, signed;
    public String status = "Loading...", author = "", title = "";

    public ${name}BookWritingClient(InteractionHand hand) {
        this.hand = hand;
        active = new WeakReference<>(this);
    }

    public String value(String key) { return values.getOrDefault(key, ""); }
    public boolean connected() { return level != null && Minecraft.getInstance().level == level && Minecraft.getInstance().player != null; }

    public void tick() {
        if (pending && System.nanoTime() > deadline) {
            pending = false;
            writable = false;
            token = "";
            status = "Server did not confirm. Reopen the book to check whether it was saved.";
            Consumer<Boolean> callback = completion;
            completion = null;
            if (callback != null) callback.accept(false);
        }
    }

    public void load() {
        if (pending || !connected()) return;
        writable = false;
        send("open", null, null, null);
    }

    public void save(String key, String text, String signatureTitle, Consumer<Boolean> callback) {
        if (pending || !writable || !connected()) { callback.accept(false); return; }
        Map<String, String> edited = new LinkedHashMap<>(values);
        if (key != null) edited.put(key, text);
        try { ${name}BookWritingData.decodeValues(${name}BookWritingData.encodeValues(edited)); }
        catch (RuntimeException failure) {
            status = "Invalid text, choice or character limit (4,096 characters per book).";
            callback.accept(false);
            return;
        }
        send(signatureTitle == null ? "save" : "sign", edited, signatureTitle, callback);
    }

    private void send(String action, Map<String, String> edited, String signatureTitle, Consumer<Boolean> callback) {
        request = UUID.randomUUID().toString();
        JsonObject message = new JsonObject();
        message.addProperty("action", action);
        message.addProperty("request", request);
        message.addProperty("hand", hand == InteractionHand.MAIN_HAND ? "MAIN" : "OFF");
        if (edited != null) {
            message.addProperty("token", token);
            message.add("values", ${name}BookWritingData.encodeValues(edited));
        }
        if (signatureTitle != null) message.addProperty("title", signatureTitle);
        String json = message.toString();
        try {
            ${name}BookWritingData.parse(json);
            completion = callback;
            pending = true;
            deadline = System.nanoTime() + 15_000_000_000L;
            status = "open".equals(action) ? "Loading..." : "Waiting for the server...";
            ${name}BookWritingMessage.sendToServer(json);
        } catch (RuntimeException failure) {
            pending = false;
            writable = false;
            token = "";
            completion = null;
            status = "Could not send to the server. Reopen the book to check.";
            if (callback != null) callback.accept(false);
        }
    }

    public static void receive(String json) {
        ${name}BookWritingClient client = active.get();
        if (client == null || !client.pending || !client.connected()) return;
        try {
            JsonObject message = ${name}BookWritingData.parse(json);
            if (!client.request.equals(${name}BookWritingData.string(message, "request", 36))) return;
            boolean success = message.has("state");
            if (success) {
                JsonObject state = message.getAsJsonObject("state");
                Map<String, String> snapshot = ${name}BookWritingData.decodeValues(state.get("values"));
                client.signed = state.get("signed").getAsBoolean();
                client.author = ${name}BookWritingData.string(state, "author", 64);
                client.title = ${name}BookWritingData.string(state, "title", 32);
                client.values = snapshot;
            }
            client.status = ${name}BookWritingData.string(message, "status", 256);
            client.token = ${name}BookWritingData.string(message, "token", 36);
            client.writable = success && !client.signed && !client.token.isEmpty() && message.get("writable").getAsBoolean();
            client.pending = false;
            Consumer<Boolean> callback = client.completion;
            client.completion = null;
            if (callback != null) callback.accept(success);
        } catch (RuntimeException failure) {
            client.pending = false;
            client.writable = false;
            client.status = "Invalid server reply. Reopen the book.";
            Consumer<Boolean> callback = client.completion;
            client.completion = null;
            if (callback != null) callback.accept(false);
        }
    }
}
