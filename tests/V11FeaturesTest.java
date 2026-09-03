import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public final class V11FeaturesTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");

    public static void main(String[] args) throws Exception {
        verifyVersion();
        verifyEditorModelAndDragDrop();
        verifyGeneratedNavigation();
        verifyLocalization();
        System.out.println("V11_FEATURES_OK");
    }

    private static void verifyVersion() throws Exception {
        String pluginJson = read(RESOURCES.resolve("plugin.json"));
        require(pluginJson.contains("\"version\": \"1.1.2-hotfix\""),
                "plugin.json does not report version 1.1.2-hotfix");
    }

    private static void verifyEditorModelAndDragDrop() throws Exception {
        Path javaRoot = Path.of("src", "main", "java", "fr", "tom", "mcreator", "custombook");
        String model = read(javaRoot.resolve(Path.of("element", "types", "CustomBook.java")));
        String gui = read(javaRoot.resolve(Path.of("ui", "modgui", "CustomBookGUI.java")));

        require(model.contains("boolean hideNextArrowAtCategoryEnd = false"), "missing saved category-boundary property");
        require(gui.contains("hideNextArrowAtCategoryEnd.setSelected(customBook.hideNextArrowAtCategoryEnd)"),
                "category-boundary property is not loaded into the editor");
        require(gui.contains("customBook.hideNextArrowAtCategoryEnd = this.hideNextArrowAtCategoryEnd.isSelected()"),
                "category-boundary property is not saved from the editor");
        require(gui.contains("bookTree.setDragEnabled(true)"), "book tree drag support is disabled");
        require(gui.contains("setDropMode(DropMode.ON_OR_INSERT)"), "book tree drop mode is missing");
        require(gui.contains("setTransferHandler(new BookTreeTransferHandler())"), "book tree transfer handler is missing");
        require(gui.contains("this.draggedNode = node"),
                "the dragged node is not retained while Swing evaluates possible drop locations");
        require(gui.contains("DataFlavor.javaJVMLocalObjectMimeType"),
                "tree nodes must use an in-process data flavor for reliable Windows drag-and-drop");
        require(gui.contains("support.setDropAction(MOVE)"), "drop action is not explicitly accepted as a move");
        require(!gui.contains("support.getTransferable().getTransferData(this.nodeFlavor)"),
                "drop validation must not request transferable data while Windows is still negotiating the drag");
        require(gui.contains("treeModel.removeNodeFromParent(source)"), "drag-and-drop does not remove the original node");
        require(gui.contains("treeModel.insertNodeInto(source, destination.parent, insertionIndex)"),
                "drag-and-drop must reinsert the same node so its identifier and content stay intact");
        require(gui.contains("updateMovedPageButtonTargets(movedPage.id, destinationCategory.id)"),
                "internal page-button targets are not updated after cross-category moves");
        require(gui.contains("target == CustomBookGUI.this.rootNode"),
                "page drops between categories are not handled");
        require(gui.contains("this.isLowerHalf(location, target) ? 1 : 0"),
                "drop placement does not distinguish before and after a visible row");
    }

    private static void verifyGeneratedNavigation() throws Exception {
        for (String generator : List.of("neoforge-1.21.1", "neoforge-26.1.2")) {
            String template = read(RESOURCES.resolve(Path.of(generator, "templates", "custombook", "screen.java.ftl")));
            require(template.contains("HIDE_NEXT_ARROW_AT_CATEGORY_END = ${data.hideNextArrowAtCategoryEnd?c}"),
                    generator + ": category-boundary option is not generated");
            require(template.contains("delta > 0 && HIDE_NEXT_ARROW_AT_CATEGORY_END"),
                    generator + ": next-page availability ignores the category boundary");
            require(template.contains("if (!canAdvance(delta)) return;"),
                    generator + ": next-page action can bypass its availability check");
        }
    }

    private static void verifyLocalization() throws Exception {
        Path langDirectory = RESOURCES.resolve("lang");
        List<Path> catalogs;
        try (var stream = Files.list(langDirectory)) {
            catalogs = stream.filter(path -> path.getFileName().toString().matches("texts(?:_[A-Za-z_]+)?\\.properties"))
                    .sorted().toList();
        }
        require(catalogs.size() == 30, "expected 30 MCreator locale catalogs, found " + catalogs.size());

        String[] keys = {
                "custombook.field.category_navigation",
                "custombook.option.stop_at_category_end",
                "custombook.tooltip.drag_reorder"
        };
        for (Path catalog : catalogs) {
            Properties properties = new Properties();
            try (Reader reader = Files.newBufferedReader(catalog, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            for (String key : keys) {
                require(properties.getProperty(key) != null && !properties.getProperty(key).isBlank(),
                        catalog.getFileName() + ": missing or empty " + key);
            }
        }
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
