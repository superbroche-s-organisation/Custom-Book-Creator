package ${package}.book;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared by client and server; the server never trusts client field definitions. */
public final class ${name}BookWritingData {
    // 4,096 characters can require six JSON bytes each (for example U+2028).
    // Leave room for 64 field keys and the envelope, below legacy's 32,767-byte payload cap.
    public static final int MAX_WIRE_BYTES = 30000;
    public static final int MAX_TOTAL_CHARS = 4096;
    public static final String DATA_KEY = "cbc_${registryname}_state";
    public static final String FIELD_PREFIX = "cbc_${registryname}_field_";
    public static final String SIGNATURE_PREFIX = "cbc_${registryname}_signature_";
    public static final boolean ALLOW_SIGNING = ${data.allowSigning?c};
    public static final Field[] FIELDS = {
        <#list data.getWritingZones() as field>
        new Field("${field.inputKey?j_string}", "${field.label?j_string}", "${field.inputMode}", ${field.inputMaxLength?c},
                "${field.content?j_string}", new String[] {<#list field.inputChoices as choice>"${choice?j_string}"<#sep>, </#sep></#list>})<#sep>,</#sep>
        </#list>
    };

    public record Field(String key, String label, String mode, int limit, String initial, String[] choices) {
        public boolean accepts(String value) {
            if (!validText(value, limit)) return false;
            if (!"PRESET".equals(mode) || value.isEmpty()) return true;
            for (String choice : choices) if (choice.equals(value)) return true;
            return false;
        }
    }

    public static Field field(String key) {
        for (Field field : FIELDS) if (field.key.equals(key)) return field;
        return null;
    }

    public static Map<String, String> defaults() {
        Map<String, String> values = new LinkedHashMap<>();
        int remaining = MAX_TOTAL_CHARS;
        for (Field field : FIELDS) {
            String value = field.accepts(field.initial) && field.initial.length() <= remaining ? field.initial : "";
            values.put(field.key, value);
            remaining -= value.length();
        }
        return values;
    }

    public static JsonObject encodeValues(Map<String, String> values) {
        JsonObject result = new JsonObject();
        values.forEach(result::addProperty);
        return result;
    }

    /** Missing/unknown fields, non-string values, oversize text and invalid preset choices are rejected. */
    public static Map<String, String> decodeValues(JsonElement input) {
        if (FIELDS.length > 64 || input == null || !input.isJsonObject()) throw new IllegalArgumentException("Invalid fields");
        JsonObject object = input.getAsJsonObject();
        if (object.size() != FIELDS.length) throw new IllegalArgumentException("Field count changed; reopen the book");
        Map<String, String> result = new LinkedHashMap<>();
        int total = 0;
        for (Field field : FIELDS) {
            String value = string(object, field.key, 1024);
            if (!field.accepts(value) || (total += value.length()) > MAX_TOTAL_CHARS)
                throw new IllegalArgumentException("Invalid or oversized text");
            result.put(field.key, value);
        }
        return result;
    }

    public static boolean validText(String text, int max) {
        if (text == null || text.length() > max) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((Character.isISOControl(c) && c != '\n' && c != '\t') || c == '\u00a7') return false;
            if (Character.isHighSurrogate(c)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) return false;
        }
        return true;
    }

    public static String string(JsonObject object, String key, int max) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Missing text field");
        String text = value.getAsString();
        if (!validText(text, max)) throw new IllegalArgumentException("Invalid text field");
        return text;
    }

    public static JsonObject parse(String json) {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_WIRE_BYTES)
            throw new IllegalArgumentException("Message too large");
        // Bound nesting before Gson allocates or recurses through untrusted input.
        int depth = 0;
        boolean quoted = false, escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') { if (++depth > 5) throw new IllegalArgumentException("Message too deep"); }
            else if (c == '}' || c == ']') { if (--depth < 0) throw new IllegalArgumentException("Invalid message"); }
        }
        if (depth != 0 || quoted) throw new IllegalArgumentException("Invalid message");
        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) throw new IllegalArgumentException("Expected an object");
        return element.getAsJsonObject();
    }

    private ${name}BookWritingData() {}
}
