package fr.superbroche.mcreator.custombook.legacy;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import net.mcreator.io.ResourcePointer;

public final class TextureSelectionButton extends JButton {
    private TextureHolder selectedTexture;
    private final TypedTextureSelectorDialog dialog;
    private final List<ActionListener> listeners = new ArrayList<>();
    private final int side;

    public TextureSelectionButton(TypedTextureSelectorDialog dialog, int side) {
        this.dialog = dialog;
        this.side = side;
        setPreferredSize(new Dimension(side, side));
        dialog.naprej.addActionListener(event -> {
            ResourcePointer selected = dialog.list.getSelectedValue();
            if (selected == null || !(selected.identifier instanceof File file)) return;
            String name = file.getName().replaceFirst("(?i)\\.png$", "");
            setTexture(new TextureHolder(dialog.app.getWorkspace(), name));
            dialog.setVisible(false);
            ActionEvent changed = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "texture");
            for (ActionListener listener : List.copyOf(listeners)) listener.actionPerformed(changed);
        });
        addActionListener(event -> {
            dialog.refreshTextures();
            dialog.setLocationRelativeTo(dialog.app);
            dialog.setVisible(true);
        });
    }

    public void addTextureSelectedListener(ActionListener listener) { listeners.add(listener); }
    public TextureHolder getTextureHolder() { return selectedTexture; }
    public boolean hasTexture() { return selectedTexture != null && !selectedTexture.isEmpty(); }
    public void setTexture(TextureHolder texture) {
        selectedTexture = texture;
        ImageIcon icon = hasTexture() ? texture.getImageIcon(dialog.textureType) : null;
        setIcon(icon == null ? null : new ImageIcon(icon.getImage()
                .getScaledInstance(Math.max(16, side - 12), Math.max(16, side - 12), java.awt.Image.SCALE_FAST)));
        setToolTipText(hasTexture() ? texture.getRawTextureName() : null);
    }
}
