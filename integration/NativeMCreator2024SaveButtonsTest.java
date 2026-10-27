import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.*;
import javax.swing.*;
import net.mcreator.ui.*;
import net.mcreator.ui.init.L10N;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;

/** Tests real 2024.4 Save listeners and real optional procedure controls without opening a desktop window. */
public final class NativeMCreator2024SaveButtonsTest {
    public static CustomBook verify(Workspace workspace, ModElement element) throws Exception {
        if (net.mcreator.ui.laf.themes.Theme.current() == null) {
            net.mcreator.ui.laf.themes.ThemeManager.loadThemes();
            net.mcreator.ui.laf.themes.ThemeManager.applySelectedTheme();
        }
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        var allocator = (sun.misc.Unsafe) field.get(null);
        TestMCreator app = (TestMCreator) allocator.allocateInstance(TestMCreator.class);
        Field workspaceField = MCreatorFrame.class.getDeclaredField("workspace");
        workspaceField.setAccessible(true);
        workspaceField.set(app, workspace);
        CustomBook[] converted = new CustomBook[1];
        SwingUtilities.invokeAndWait(() -> {
            for (String label : new String[]{"elementgui.save_mod_element", "elementgui.save_keep_open"}) {
                SaveProbe gui = new SaveProbe(app, element);
                if (!gui.validateAllPages().validateIsErrorFree())
                    throw new AssertionError("Default book failed native all-page validation");
                JButton save = findButton(gui, L10N.t(label));
                if (save == null) throw new AssertionError("Native Save button missing: " + label);
                try {
                    save.doClick(0);
                    throw new AssertionError("Save did not reach GUI-to-model conversion: " + label);
                } catch (SaveReached expected) {
                    if (expected.book == null || expected.book.getBookCategories().isEmpty())
                        throw new AssertionError("GUI conversion did not produce a book");
                    converted[0] = expected.book;
                    System.out.println("NATIVE_SAVE_BUTTON_OK " + workspace.getWorkspaceSettings().getCurrentGenerator() + " " + label);
                }
            }
        });
        return converted[0];
    }

    private static JButton findButton(Container parent, String label) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && label.equals(button.getText())) return button;
            if (component instanceof Container child) {
                JButton button = findButton(child, label);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static final class SaveReached extends RuntimeException {
        final CustomBook book;
        SaveReached(CustomBook book) { this.book = book; }
    }

    private static final class SaveProbe extends CustomBookGUI {
        SaveProbe(MCreator app, ModElement element) { super(app, element, false); }
        @Override protected void initGUI() {
            addPage("Visuals", new JPanel());
            addPage("Properties", new JPanel());
            try {
                Method interaction = CustomBookGUI.class.getDeclaredMethod("createInteractionPage");
                interaction.setAccessible(true);
                addPage("Interaction", (JComponent) interaction.invoke(this));
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            addPage("Book editor", new JPanel());
        }
        @Override protected boolean allowCodePreview() { return false; }
        @Override public void reloadDataLists() {}
        // The parent test stores, generates and reloads this converted model;
        // stop before native analytics, Gradle and desktop-tab updates.
        @Override public CustomBook getElementFromGUI() { throw new SaveReached(super.getElementFromGUI()); }
    }

    private static final class TestMCreator extends MCreator {
        private TestMCreator() { super(null, null, false); }
        @Override protected MainMenuBar createMenuBar() { return null; }
        @Override protected MainToolBar createToolBar() { return null; }
        @Override protected JPanel createWorkspaceTabContent() { return null; }
        @Override protected void reloadWorkspaceTabContentsImpl() {}
    }
}
