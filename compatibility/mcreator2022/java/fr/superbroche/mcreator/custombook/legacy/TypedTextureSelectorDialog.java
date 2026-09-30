package fr.superbroche.mcreator.custombook.legacy;

import java.util.List;
import net.mcreator.io.ResourcePointer;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.dialogs.TextureSelectorDialog;
import net.mcreator.ui.workspace.resources.TextureType;

/** Uses the texture browser supplied by MCreator 2022.2. */
public final class TypedTextureSelectorDialog extends TextureSelectorDialog {
    final MCreator app;
    final TextureType textureType;

    public TypedTextureSelectorDialog(MCreator app, TextureType type) {
        super(textures(app, type), app);
        this.app = app;
        this.textureType = type;
        setModal(true);
    }

    public void refreshTextures() {
        list.setListData(textures(app, textureType).toArray(ResourcePointer[]::new));
    }

    private static List<ResourcePointer> textures(MCreator app, TextureType type) {
        return app.getWorkspace().getFolderManager().getTexturesList(type).stream()
                .map(ResourcePointer::new).toList();
    }
}
