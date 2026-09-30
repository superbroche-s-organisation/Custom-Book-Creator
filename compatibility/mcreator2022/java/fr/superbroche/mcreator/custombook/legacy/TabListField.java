package fr.superbroche.mcreator.custombook.legacy;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JPanel;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.minecraft.ElementUtil;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.ComboBoxUtil;
import net.mcreator.ui.minecraft.DataListComboBox;

/** Forge 1.17.1 assigns an item to one creative tab. */
public final class TabListField extends JPanel {
    private final MCreator app;
    private final DataListComboBox selector;

    public TabListField(MCreator app) {
        super(new BorderLayout());
        this.app = app;
        selector = new DataListComboBox(app);
        add(selector, BorderLayout.CENTER);
        refreshListKeepSelected();
    }

    public void refreshListKeepSelected() {
        ComboBoxUtil.updateComboBoxContents(selector, ElementUtil.loadAllTabs(app.getWorkspace()));
    }
    public void setListElements(List<TabEntry> tabs) {
        refreshListKeepSelected();
        if (tabs != null && !tabs.isEmpty()) selector.setSelectedItem(tabs.get(0));
    }
    public List<TabEntry> getListElements() {
        return selector.getSelectedItem() == null ? List.of()
                : List.of(new TabEntry(app.getWorkspace(), selector.getSelectedItem()));
    }
}
