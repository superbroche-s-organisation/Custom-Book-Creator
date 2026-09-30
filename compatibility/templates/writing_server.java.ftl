package ${package}.book;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
<#if cbcComponents>
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
</#if>
import ${package}.item.${name}Item;
import ${package}.network.${name}BookWritingMessage;

/** All calls run on the server thread. No Minecraft client classes are referenced here. */
public final class ${name}BookWritingServer {
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Budget> BUDGETS = new WeakHashMap<>();
    private static final long SESSION_NS = 300_000_000_000L;
    private record Session(ItemStack stack, InteractionHand hand, String token, String before, long expires) {}
    private static final class Budget {
        double tokens = 8;
        long updated = System.nanoTime();
    }

    private static boolean takeRequest(ServerPlayer player) {
        Budget budget = BUDGETS.computeIfAbsent(player, ignored -> new Budget());
        long now = System.nanoTime();
        budget.tokens = Math.min(8, budget.tokens + Math.max(0, now - budget.updated) / 250_000_000.0);
        budget.updated = now;
        if (budget.tokens < 1) return false;
        budget.tokens--;
        return true;
    }

    public static void receive(ServerPlayer player, String wire) {
        if (!takeRequest(player)) return;
        String request = "";
        boolean committed = false;
        try {
            JsonObject message = ${name}BookWritingData.parse(wire);
            request = ${name}BookWritingData.string(message, "request", 36);
            UUID.fromString(request);
            String action = ${name}BookWritingData.string(message, "action", 8);
            String handName = ${name}BookWritingData.string(message, "hand", 4);
            if (!"MAIN".equals(handName) && !"OFF".equals(handName)) return;
            InteractionHand hand = "MAIN".equals(handName) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof ${name}Item) || stack.getCount() != 1 || !player.isAlive()) {
                reply(player, request, null, "", false, "Hold one copy of this book to use its writing zones.");
                return;
            }
            CompoundTag custom = customData(stack);
            String stored = readString(custom, ${name}BookWritingData.DATA_KEY);
            JsonObject state = stored.isEmpty() ? initialState() : ${name}BookWritingData.parse(stored);
            if ("open".equals(action)) {
                ${name}BookWritingData.decodeValues(state.get("values"));
                String token = UUID.randomUUID().toString();
                boolean writable = !isSigned(state) && canWrite(player, stack);
                if (!unchanged(player, hand, stack, stored)) {
                    reply(player, request, null, "", false, "The permission procedure changed the book. Reopen it.");
                    return;
                }
                SESSIONS.put(player, new Session(stack, hand, token, stored, System.nanoTime() + SESSION_NS));
                reply(player, request, state, token, writable, writable ? "" : "Read only");
                return;
            }
            if (!"save".equals(action) && !"sign".equals(action)) return;
            Session session = SESSIONS.get(player);
            String token = ${name}BookWritingData.string(message, "token", 36);
            if (session == null || session.stack != stack || session.hand != hand || !session.token.equals(token)
                    || session.expires < System.nanoTime() || !session.before.equals(stored)) {
                reply(player, request, null, "", false, "The book changed or the session expired. Reopen it.");
                return;
            }
            if (isSigned(state) || !canWrite(player, stack) || ("sign".equals(action) && !${name}BookWritingData.ALLOW_SIGNING)) {
                reply(player, request, null, "", false, "You cannot change or sign this book.");
                return;
            }
            if (!unchanged(player, hand, stack, stored)) {
                SESSIONS.remove(player);
                reply(player, request, null, "", false, "The permission procedure changed the book. Reopen it.");
                return;
            }
            // Preserve unrelated custom data a permission procedure may have updated.
            custom = customData(stack);
            Map<String, String> values = ${name}BookWritingData.decodeValues(message.get("values"));
            JsonObject updated = initialState();
            updated.add("values", ${name}BookWritingData.encodeValues(values));
            boolean sign = "sign".equals(action);
            if (sign) {
                String title = ${name}BookWritingData.string(message, "title", 32).trim();
                if (title.isEmpty() || title.contains("\n")) throw new IllegalArgumentException("Enter a signature title");
                updated.addProperty("signed", true);
                updated.addProperty("title", title);
                updated.addProperty("author", player.getGameProfile().<#if cbcModernProfile>name()<#else>getName()</#if>);
                updated.addProperty("authorId", player.getUUID().toString());
            }
            String json = updated.toString();
            ${name}BookWritingData.parse(json);
            // Persist before the trigger: retries and re-entrant procedures cannot sign twice.
            custom.putString(${name}BookWritingData.DATA_KEY, json);
            for (Map.Entry<String, String> entry : values.entrySet())
                custom.putString(${name}BookWritingData.FIELD_PREFIX + entry.getKey(), entry.getValue());
            custom.putBoolean(${name}BookWritingData.SIGNATURE_PREFIX + "signed", sign);
            if (sign) {
                custom.putString(${name}BookWritingData.SIGNATURE_PREFIX + "author", updated.get("author").getAsString());
                custom.putString(${name}BookWritingData.SIGNATURE_PREFIX + "author_uuid", player.getUUID().toString());
                custom.putString(${name}BookWritingData.SIGNATURE_PREFIX + "title", updated.get("title").getAsString());
            }
            setCustomData(stack, custom);
            committed = true;
            SESSIONS.remove(player);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            String status = sign ? "Book signed. Writing is permanently locked." : "Saved.";
            if (sign) {
                try { onSigned(player, stack); }
                catch (RuntimeException failure) {
                    org.slf4j.LoggerFactory.getLogger("${modid}").error("Book was signed, but its signature procedure failed", failure);
                    status = "Book signed, but the signature procedure failed. See the server log.";
                }
            }
            reply(player, request, updated, "", false, status);
        } catch (RuntimeException failure) {
            if (!request.isEmpty()) {
                try {
                    reply(player, request, null, "", false, committed
                            ? "The book was saved, but confirmation failed. Reopen it to check."
                            : "Invalid book data or permission procedure failure. Reopen the book.");
                } catch (RuntimeException disconnected) {
                    // A disconnected client cannot receive an acknowledgement.
                }
            }
        }
    }

    private static boolean unchanged(ServerPlayer player, InteractionHand hand, ItemStack stack, String stored) {
        return player.isAlive() && player.getItemInHand(hand) == stack && stack.getCount() == 1
                && stored.equals(readString(customData(stack), ${name}BookWritingData.DATA_KEY));
    }

    private static boolean isSigned(JsonObject state) {
        if (!state.has("signed") || !state.get("signed").isJsonPrimitive()
                || !state.getAsJsonPrimitive("signed").isBoolean()) throw new IllegalArgumentException("Invalid signature");
        return state.get("signed").getAsBoolean();
    }

    private static JsonObject initialState() {
        JsonObject state = new JsonObject();
        state.addProperty("signed", false);
        state.addProperty("title", "");
        state.addProperty("author", "");
        state.addProperty("authorId", "");
        state.add("values", ${name}BookWritingData.encodeValues(${name}BookWritingData.defaults()));
        return state;
    }

    private static void reply(ServerPlayer player, String request, JsonObject state, String token, boolean writable, String status) {
        JsonObject response = new JsonObject();
        response.addProperty("request", request);
        response.addProperty("token", token);
        response.addProperty("writable", writable);
        response.addProperty("status", status);
        if (state != null) response.add("state", state);
        ${name}BookWritingMessage.sendToPlayer(player, response.toString());
    }

    private static CompoundTag customData(ItemStack stack) {
        <#if cbcComponents>
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        <#else>
        return stack.getOrCreateTag().copy();
        </#if>
    }

    private static String readString(CompoundTag tag, String key) {
        return tag.<#if cbcOptionalNbt>getStringOr(key, "")<#else>getString(key)</#if>;
    }

    private static void setCustomData(ItemStack stack, CompoundTag tag) {
        <#if cbcComponents>
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        <#else>
        stack.setTag(tag);
        </#if>
    }

    private static boolean canWrite(ServerPlayer entity, ItemStack itemstack) {
        var world = entity.level();
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        <#if data.writingPermission?? && data.writingPermission.getName()?has_content>
        <#assign cbcDependencies=data.writingPermission.getDependencies(data.getModElement().getWorkspace())>
        <#if !data.writingPermission.exists><#stop "Custom Book Creator: the writing permission procedure no longer exists. Select an existing procedure in Writing and signing."></#if>
        <#list cbcDependencies as dependency><#if !["entity", "itemstack", "world", "x", "y", "z"]?seq_contains(dependency.getName())><#stop "Custom Book Creator: unsupported writing permission dependency: " + dependency.getName()></#if></#list>
        return ${package}.procedures.${data.writingPermission.getName()}Procedure.execute(<#list cbcDependencies as dependency>${dependency.getName()}<#sep>, </#sep></#list>);
        <#else>
        return <#if data.writingPermission??>${data.writingPermission.getFixedValue()?c}<#else>true</#if>;
        </#if>
    }

    private static void onSigned(ServerPlayer entity, ItemStack itemstack) {
        var world = entity.level();
        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        <#if data.onBookSigned?? && data.onBookSigned.getName()?has_content>
        <#assign cbcDependencies=data.onBookSigned.getDependencies(data.getModElement().getWorkspace())>
        <#if !data.onBookSigned.exists><#stop "Custom Book Creator: the signature procedure no longer exists. Select an existing procedure in Writing and signing."></#if>
        <#list cbcDependencies as dependency><#if !["entity", "itemstack", "world", "x", "y", "z"]?seq_contains(dependency.getName())><#stop "Custom Book Creator: unsupported signature dependency: " + dependency.getName()></#if></#list>
        ${package}.procedures.${data.onBookSigned.getName()}Procedure.execute(<#list cbcDependencies as dependency>${dependency.getName()}<#sep>, </#sep></#list>);
        </#if>
    }

    private ${name}BookWritingServer() {}
}
