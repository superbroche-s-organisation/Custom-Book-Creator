package fr.superbroche.mcreator.custombook.legacy;

import java.awt.BorderLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import net.mcreator.blockly.data.Dependency;
import net.mcreator.element.parts.Procedure;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.help.IHelpContext;
import net.mcreator.ui.procedure.AbstractProcedureSelector;
import net.mcreator.ui.procedure.ProcedureSelector;
import net.mcreator.workspace.elements.VariableTypeLoader;

/** Native boolean-returning procedure picker plus its constant fallback value. */
public final class LogicProcedureSelector extends JPanel {
    private final ProcedureSelector selector;
    private final JCheckBox fixed;

    public LogicProcedureSelector(IHelpContext context, MCreator app, String title,
            AbstractProcedureSelector.Side side, JCheckBox fixed, int ignored, Dependency... dependencies) {
        super(new BorderLayout());
        this.fixed = fixed;
        selector = new ProcedureSelector(context, app, title, side, true,
                VariableTypeLoader.INSTANCE.fromName("logic"), dependencies);
        add(selector, BorderLayout.CENTER);
        add(fixed, BorderLayout.SOUTH);
        fixed.setToolTipText("Used only when no permission procedure is selected.");
    }

    public LogicProcedure getSelectedProcedure() {
        Procedure selected = selector.getSelectedProcedure();
        return new LogicProcedure(selected == null ? null : selected.getName(), fixed.isSelected());
    }
    public void setSelectedProcedure(LogicProcedure procedure) {
        selector.setSelectedProcedure(procedure);
        fixed.setSelected(procedure == null || procedure.getFixedValue());
    }
    public void refreshListKeepSelected() { selector.refreshListKeepSelected(); }
}
