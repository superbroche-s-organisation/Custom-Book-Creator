import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class BinaryContractTest {
    public static void main(String[] args) throws Exception {
        Class<?> guiClass = Class.forName("fr.tom.mcreator.custombook.ui.modgui.CustomBookGUI", false,
                Thread.currentThread().getContextClassLoader());
        Class<?> generatableElement = Class.forName("net.mcreator.element.GeneratableElement", false,
                Thread.currentThread().getContextClassLoader());

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
