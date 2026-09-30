package fr.superbroche.mcreator.custombook.legacy;

import javax.swing.ImageIcon;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.Workspace;

/** Plugin-owned bridge; never shadows a class in MCreator's namespace. */
public final class TextureHolder {
    private String texture;
    private transient Workspace workspace;

    public TextureHolder(Workspace workspace, String texture) {
        this.workspace = workspace;
        this.texture = texture == null ? "" : texture;
    }

    public String getRawTextureName() { return texture == null ? "" : texture; }
    public boolean isEmpty() { return getRawTextureName().isBlank(); }
    public ImageIcon getImageIcon(TextureType type) {
        if (workspace == null || isEmpty()) return null;
        return workspace.getFolderManager().getTextureImageIcon(texture, type);
    }
}
