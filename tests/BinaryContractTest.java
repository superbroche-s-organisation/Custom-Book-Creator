import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

public final class BinaryContractTest {
    public static void main(String[] args) throws Exception {
        Class<?> guiClass = Class.forName("fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI", false,
                Thread.currentThread().getContextClassLoader());
        Class<?> generatableElement = Class.forName("net.mcreator.element.GeneratableElement", false,
                Thread.currentThread().getContextClassLoader());

        try (InputStream stream = guiClass.getClassLoader().getResourceAsStream("plugin.json")) {
            if (stream == null) {
                throw new AssertionError("The packaged plugin manifest is missing");
            }
            JsonObject manifest = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (!"Superbroche".equals(manifest.getAsJsonObject("info").get("author").getAsString())
                    || !"custom_book_creator".equals(manifest.get("id").getAsString())) {
                throw new AssertionError("The packaged author or stable plugin identifier is incorrect");
            }
            String entryPoint = manifest.get("javaplugin").getAsString();
            if (!"fr.superbroche.mcreator.custombook.CustomBookPlugin".equals(entryPoint)) {
                throw new AssertionError("The packaged entry point uses an unexpected namespace");
            }
            Class.forName(entryPoint, false, guiClass.getClassLoader());
        }

        Method bridge = null;
        for (Method method : guiClass.getDeclaredMethods()) {
            if (method.getName().equals("getElementFromGUI")
                    && method.getParameterCount() == 0
                    && method.getReturnType().equals(generatableElement)) {
                bridge = method;
                break;
            }
        }

        if (bridge == null) {
            throw new AssertionError("Missing getElementFromGUI(): GeneratableElement bridge");
        }
        if (!bridge.isBridge() || !bridge.isSynthetic() || Modifier.isAbstract(bridge.getModifiers())) {
            throw new AssertionError("The save bridge exists but has invalid JVM attributes");
        }

        System.out.println("BINARY_CONTRACT_OK " + bridge);
    }
}
