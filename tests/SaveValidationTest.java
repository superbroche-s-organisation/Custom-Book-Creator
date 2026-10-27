import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.lang.reflect.*;
import javax.swing.SwingUtilities;
import net.mcreator.ui.procedure.LogicProcedureSelector;
import net.mcreator.ui.procedure.ProcedureSelector;

/** Exercises the native pre-save validation used by MCreator 2024.4. */
public final class SaveValidationTest {
    public static void main(String[] args) throws Exception {
        Method validatePage;
        try {
            validatePage = CustomBookGUI.class.getDeclaredMethod("validatePage", int.class);
        } catch (NoSuchMethodException modernMCreator) {
            System.out.println("SAVE_VALIDATION_OK native validation adapter not needed");
            return;
        }
        validatePage.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe allocator = (sun.misc.Unsafe) field.get(null);
                CustomBookGUI gui = (CustomBookGUI) allocator.allocateInstance(CustomBookGUI.class);
                // Native optional selectors have no validator by default and
                // getValidationStatus() returns null in MCreator 2024.4.
                LogicProcedureSelector permission = (LogicProcedureSelector) allocator.allocateInstance(LogicProcedureSelector.class);
                ProcedureSelector signed = (ProcedureSelector) allocator.allocateInstance(ProcedureSelector.class);
                long contextOffset = allocator.objectFieldOffset(java.awt.Component.class.getDeclaredField("appContext"));
                Object context = allocator.getObject(new javax.swing.JPanel(), contextOffset);
                allocator.putObject(permission, contextOffset, context);
                allocator.putObject(signed, contextOffset, context);
                set(gui, "writingPermission", permission);
                set(gui, "onBookSigned", signed);
                check(permission.getValidator() == null && signed.getValidator() == null,
                        "Default procedure selectors unexpectedly have validators");
                for (int page = 0; page < 4; page++)
                    check(valid(gui, validatePage, page), "Unconfigured optional selector blocks page " + page);

                Class<?> validator = Class.forName("net.mcreator.ui.validation.Validator");
                Class<?> result = Class.forName("net.mcreator.ui.validation.Validator$ValidationResult");
                Object passed = result.getField("PASSED").get(null);
                Object passValidator = Proxy.newProxyInstance(validator.getClassLoader(), new Class<?>[]{validator},
                        (proxy, method, values) -> passed);
                Method setValidator = permission.getClass().getMethod("setValidator", validator);
                setValidator.invoke(permission, passValidator);
                check(valid(gui, validatePage, 2), "Valid permission cannot save beside an unvalidated signed hook");

                Class<?> type = Class.forName("net.mcreator.ui.validation.Validator$ValidationResultType");
                Object error = result.getConstructor(type, String.class).newInstance(type.getField("ERROR").get(null), "Invalid procedure");
                Object failValidator = Proxy.newProxyInstance(validator.getClassLoader(), new Class<?>[]{validator},
                        (proxy, method, values) -> error);
                setValidator.invoke(permission, failValidator);
                check(!valid(gui, validatePage, 2), "Configured validation errors must still block saving");
                setValidator.invoke(permission, new Object[]{null});
                signed.getClass().getMethod("setValidator", validator).invoke(signed, failValidator);
                check(!valid(gui, validatePage, 2), "Signed-hook validation errors must still block saving");
                set(gui, "writingPermission", null);
                set(gui, "onBookSigned", null);
                check(valid(gui, validatePage, 2), "Absent optional selectors must be accepted");
            } catch (Throwable failure) {
                throw new AssertionError("MCreator pre-save validation failed", failure);
            }
        });
        System.out.println("SAVE_VALIDATION_OK optional defaults and explicit procedure errors");
    }

    private static boolean valid(CustomBookGUI gui, Method method, int page) throws Exception {
        Object result = method.invoke(gui, page);
        return (boolean) result.getClass().getMethod("validateIsErrorFree").invoke(result);
    }

    private static void set(Object gui, String name, Object value) throws Exception {
        Field field = CustomBookGUI.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(gui, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
