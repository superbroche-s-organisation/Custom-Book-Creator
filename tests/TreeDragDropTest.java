import fr.tom.mcreator.custombook.element.types.CustomBook;
import fr.tom.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDropEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import net.mcreator.ui.init.L10N;
import sun.misc.Unsafe;

/** Executes the production Swing transfer handler without creating an MCreator window. */
public final class TreeDragDropTest {
    private static final Unsafe UNSAFE = unsafe();
    private static int assertions;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        initializeTranslations();
        SwingUtilities.invokeAndWait(() -> {
            try {
                reorderPagesAndCategories();
                moveBetweenCategoriesAndRepairLinks();
                moveLastPageKeepsSourceCategoryUsable();
                rejectInvalidDestinationsWithoutLosingData();
                rollBackFailedMoves(false);
                rollBackFailedMoves(true);
                rollBackAfterButtonTargetsChanged();
                repairDeletedAndInvalidButtonTargets();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
        System.out.println("TREE_DRAG_DROP_OK (" + assertions + " dynamic assertions)");
    }

    private static void reorderPagesAndCategories() throws Exception {
        Fixture fixture = new Fixture(3, 2);
        DefaultMutableTreeNode a1 = fixture.page(fixture.a, 0);
        DefaultMutableTreeNode a2 = fixture.page(fixture.a, 1);
        DefaultMutableTreeNode a3 = fixture.page(fixture.a, 2);
        PageSnapshot original = new PageSnapshot(a1);
        require(fixture.handler.getSourceActions(fixture.tree) == TransferHandler.MOVE, "source action must be MOVE");
        require(fixture.move(a1, fixture.a, 3, false), "move first page to category end rejected");
        require(children(fixture.a).equals(List.of(a2, a3, a1)), "same-category append used the wrong index");
        original.assertPreserved(a1);
        require(fixture.move(a1, a2, -1, false), "drop above a page rejected");
        require(children(fixture.a).equals(List.of(a1, a2, a3)), "drop in the upper half must insert before a page");
        require(fixture.move(a1, a2, -1, true), "drop below a page rejected");
        require(children(fixture.a).equals(List.of(a2, a1, a3)), "drop in the lower half must insert after a page");
        require(fixture.move(fixture.a, fixture.root, 2, false), "category append rejected");
        require(children(fixture.root).equals(List.of(fixture.b, fixture.a)), "category order did not change");
        require(fixture.move(fixture.a, fixture.b, -1, false), "drop category before another category rejected");
        require(children(fixture.root).equals(List.of(fixture.a, fixture.b)), "category drop-before used wrong index");
        original.assertPreserved(a1);
        fixture.assertTreeInvariants();
    }

    private static void moveBetweenCategoriesAndRepairLinks() throws Exception {
        Fixture fixture = new Fixture(3, 2);
        DefaultMutableTreeNode moved = fixture.page(fixture.a, 1);
        CustomBook.BookPage movedPage = pageData(moved);
        PageSnapshot original = new PageSnapshot(moved);
        CustomBook.BookElement firstLink = button(pageData(fixture.page(fixture.a, 0)), "a", movedPage.id);
        CustomBook.BookElement secondLink = button(pageData(fixture.page(fixture.b, 0)), "a", movedPage.id);
        String markup = "[page=" + movedPage.id + "]Visit page[/page]";
        pageData(fixture.page(fixture.b, 1)).elements.add(CustomBook.BookElement.text(markup));
        require(fixture.move(moved, fixture.b, -1, false), "cross-category drop rejected");
        require(moved.getParent() == fixture.b, "page did not move into target category");
        require(fixture.b.getChildAt(2) == moved, "on-category drop should append");
        require("b".equals(firstLink.targetCategoryId) && "b".equals(secondLink.targetCategoryId),
                "buttons in all categories must follow the moved page");
        require(movedPage.id.equals(firstLink.targetPageId) && movedPage.id.equals(secondLink.targetPageId),
                "moving a page changed the target page id");
        require(pageData(fixture.page(fixture.b, 1)).elements.stream().anyMatch(element -> markup.equals(element.content)),
                "page-ID rich-text link was changed by a move");
        original.assertPreserved(moved);
        fixture.assertTreeInvariants();
    }

    private static void moveLastPageKeepsSourceCategoryUsable() throws Exception {
        Fixture fixture = new Fixture(1, 1);
        DefaultMutableTreeNode moved = fixture.page(fixture.a, 0);
        PageSnapshot original = new PageSnapshot(moved);
        require(fixture.move(moved, fixture.root, 2, false), "root-level final drop rejected");
        require(moved.getParent() == fixture.b, "last root gap should append to the final category");
        require(fixture.a.getChildCount() == 1, "moving the only page left an empty category");
        CustomBook.BookPage replacement = pageData(fixture.page(fixture.a, 0));
        require(!replacement.id.equals(pageData(moved).id), "replacement page reused the moved page id");
        require(replacement.elements.stream().anyMatch(element -> "NAV_PREV".equals(element.type)),
                "replacement page lacks previous-page navigation");
        require(replacement.elements.stream().anyMatch(element -> "NAV_NEXT".equals(element.type)),
                "replacement page lacks next-page navigation");
        original.assertPreserved(moved);
        fixture.assertTreeInvariants();
    }

    private static void rejectInvalidDestinationsWithoutLosingData() throws Exception {
        Fixture fixture = new Fixture(2, 1);
        DefaultMutableTreeNode moved = fixture.page(fixture.a, 0);
        PageSnapshot original = new PageSnapshot(moved);
        List<DefaultMutableTreeNode> originalOrder = children(fixture.a);
        DefaultMutableTreeNode detached = new DefaultMutableTreeNode(new CustomBook.BookCategory("Detached"));
        require(!fixture.move(moved, detached, -1, false), "detached target must be rejected");
        require(children(fixture.a).equals(originalOrder), "detached target removed the original page");
        require(detached.getChildCount() == 0, "page leaked into a detached category");
        Transferable data = fixture.beginDrag(moved);
        TransferHandler.TransferSupport plain = new TransferHandler.TransferSupport(fixture.tree, data);
        require(!fixture.handler.importData(plain), "clipboard/non-drop import must be rejected");
        fixture.endDrag(data);
        data = fixture.beginDrag(moved);
        TransferHandler.TransferSupport foreign = fixture.support(data, fixture.b, -1, false);
        put(foreign, TransferHandler.TransferSupport.class, "component", new JTree());
        require(!fixture.handler.importData(foreign), "drop belonging to another tree must be rejected");
        fixture.endDrag(data);
        data = fixture.beginDrag(moved);
        TransferHandler.TransferSupport stale = fixture.support(data, fixture.b, -1, false);
        fixture.root.remove(fixture.b);
        require(!fixture.handler.importData(stale), "stale detached drop path must be rejected");
        fixture.root.add(fixture.b);
        fixture.endDrag(data);
        require(children(fixture.a).equals(originalOrder), "invalid drop changed the page order");
        original.assertPreserved(moved);
        fixture.assertTreeInvariants();
    }

    private static void rollBackFailedMoves(boolean failAfterInsertion) throws Exception {
        Fixture fixture = new Fixture(1, 1);
        DefaultMutableTreeNode moved = fixture.page(fixture.a, 0);
        PageSnapshot original = new PageSnapshot(moved);
        CustomBook.BookElement link = button(pageData(fixture.page(fixture.b, 0)), "a", pageData(moved).id);
        List<DefaultMutableTreeNode> originalA = children(fixture.a);
        List<DefaultMutableTreeNode> originalB = children(fixture.b);
        fixture.model.failNextInsertionOf(moved, failAfterInsertion);
        ByteArrayOutputStream errorLog = new ByteArrayOutputStream();
        PrintStream previousError = System.err;
        boolean imported;
        try (PrintStream captured = new PrintStream(errorLog, true, StandardCharsets.UTF_8)) {
            System.setErr(captured);
            imported = fixture.move(moved, fixture.b, -1, false);
        } finally {
            System.setErr(previousError);
        }
        require(!imported, "injected insertion failure must fail the drop");
        require(errorLog.toString(StandardCharsets.UTF_8).contains("rolled back"), "rollback was not logged");
        require(children(fixture.a).equals(originalA), "rollback did not restore the original source order");
        require(children(fixture.b).equals(originalB), "rollback left the page in the destination");
        require("a".equals(link.targetCategoryId) && pageData(moved).id.equals(link.targetPageId),
                "rollback changed a valid button target");
        original.assertPreserved(moved);
        fixture.assertTreeInvariants();
    }

    private static void repairDeletedAndInvalidButtonTargets() throws Exception {
        Fixture fixture = new Fixture(2, 1);
        CustomBook.BookPage owner = pageData(fixture.page(fixture.a, 0));
        String survivingPageId = pageData(fixture.page(fixture.b, 0)).id;
        CustomBook.BookElement wrongCategory = button(owner, "missing-category", survivingPageId);
        CustomBook.BookElement deletedPage = button(owner, "b", "deleted-page");
        CustomBook.BookElement deletedCategory = button(owner, "deleted-category", "");
        CustomBook.BookElement validCategory = button(owner, "b", "");
        CustomBook.BookElement nullTarget = button(owner, null, null);
        invoke(fixture.gui, "repairButtonTargets", new Class<?>[0]);
        require("b".equals(wrongCategory.targetCategoryId) && survivingPageId.equals(wrongCategory.targetPageId),
                "existing page must determine its owning category");
        require("b".equals(deletedPage.targetCategoryId) && deletedPage.targetPageId.isEmpty(),
                "deleted page must fall back to first page of its valid category");
        require("a".equals(deletedCategory.targetCategoryId), "deleted category must fall back to first category");
        require("b".equals(validCategory.targetCategoryId) && validCategory.targetPageId.isEmpty(),
                "valid category-only button was changed");
        require("a".equals(nullTarget.targetCategoryId) && "".equals(nullTarget.targetPageId),
                "null category/page target was not fully repaired");
        fixture.model.removeNodeFromParent(fixture.b);
        invoke(fixture.gui, "syncCategoryPageListsFromTree", new Class<?>[0]);
        invoke(fixture.gui, "repairButtonTargets", new Class<?>[0]);
        require("a".equals(wrongCategory.targetCategoryId) && wrongCategory.targetPageId.isEmpty(),
                "deleting a category left a broken page target");
        fixture.assertTreeInvariants();
    }

    private static void rollBackAfterButtonTargetsChanged() throws Exception {
        Fixture fixture = new Fixture(1, 1);
        DefaultMutableTreeNode moved = fixture.page(fixture.a, 0);
        PageSnapshot original = new PageSnapshot(moved);
        CustomBook.BookElement link = button(pageData(fixture.page(fixture.b, 0)), "a", pageData(moved).id);
        FailingComboBox combo = new FailingComboBox();
        combo.watchedLink = link;
        put(fixture.gui, CustomBookGUI.class, "buttonTarget", combo);
        combo.failNextRefresh = true;
        ByteArrayOutputStream errorLog = new ByteArrayOutputStream();
        PrintStream previousError = System.err;
        boolean imported;
        try (PrintStream captured = new PrintStream(errorLog, true, StandardCharsets.UTF_8)) {
            System.setErr(captured);
            imported = fixture.move(moved, fixture.b, -1, false);
        } finally {
            System.setErr(previousError);
        }
        require(!imported, "inspector-refresh failure must fail the drop");
        require(combo.sawMovedTarget, "injected exception did not occur after button-target updates");
        require(moved.getParent() == fixture.a && fixture.a.getChildCount() == 1 && fixture.b.getChildCount() == 1,
                "late rollback lost the source page or retained the replacement page");
        require("a".equals(link.targetCategoryId) && pageData(moved).id.equals(link.targetPageId),
                "late rollback did not restore the moved-page button target");
        original.assertPreserved(moved);
        fixture.assertTreeInvariants();
    }

    private static CustomBook.BookElement button(CustomBook.BookPage page, String categoryId, String pageId) {
        CustomBook.BookElement element = CustomBook.BookElement.button("Navigate", categoryId);
        element.targetCategoryId = categoryId;
        element.targetPageId = pageId;
        page.elements.add(element);
        return element;
    }

    private static List<DefaultMutableTreeNode> children(DefaultMutableTreeNode parent) {
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) children.add((DefaultMutableTreeNode) parent.getChildAt(i));
        return children;
    }

    private static CustomBook.BookPage pageData(DefaultMutableTreeNode node) {
        return (CustomBook.BookPage) node.getUserObject();
    }

    private static final class Fixture {
        final CustomBookGUI gui = (CustomBookGUI) UNSAFE.allocateInstance(CustomBookGUI.class);
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Book");
        final DefaultMutableTreeNode a = category("a");
        final DefaultMutableTreeNode b = category("b");
        final FailingTreeModel model = new FailingTreeModel(root);
        final JTree tree = new JTree(model);
        final TransferHandler handler;

        Fixture(int pagesA, int pagesB) throws Exception {
            root.add(a);
            root.add(b);
            addPages(a, pagesA);
            addPages(b, pagesB);
            tree.setRootVisible(false);
            tree.setRowHeight(24);
            tree.setSize(400, 800);
            put(gui, CustomBookGUI.class, "rootNode", root);
            put(gui, CustomBookGUI.class, "treeModel", model);
            put(gui, CustomBookGUI.class, "bookTree", tree);
            put(gui, CustomBookGUI.class, "buttonTarget", new JComboBox<>());
            put(gui, CustomBookGUI.class, "buttonPageTarget", new JComboBox<>());
            Class<?> handlerType = Class.forName(CustomBookGUI.class.getName() + "$BookTreeTransferHandler");
            Constructor<?> constructor = handlerType.getDeclaredConstructor(CustomBookGUI.class);
            constructor.setAccessible(true);
            handler = (TransferHandler) constructor.newInstance(gui);
            tree.setTransferHandler(handler);
            model.reload();
            expandAll();
            invoke(gui, "syncCategoryPageListsFromTree", new Class<?>[0]);
        }

        DefaultMutableTreeNode page(DefaultMutableTreeNode category, int index) {
            return (DefaultMutableTreeNode) category.getChildAt(index);
        }

        private static DefaultMutableTreeNode category(String id) {
            CustomBook.BookCategory data = new CustomBook.BookCategory(id.toUpperCase());
            data.id = id;
            return new DefaultMutableTreeNode(data);
        }

        private static void addPages(DefaultMutableTreeNode category, int count) {
            String categoryId = ((CustomBook.BookCategory) category.getUserObject()).id;
            for (int i = 0; i < count; i++) {
                String id = categoryId + (i + 1);
                CustomBook.BookPage page = new CustomBook.BookPage("Title " + id, "[b]Text " + id + "[/b] \uD83D\uDE00");
                page.id = id;
                page.elements.add(CustomBook.BookElement.image("image_" + id, 90, 70));
                page.elements.add(CustomBook.BookElement.gif(List.of("frame_" + id + "_0", "frame_" + id + "_1"), 80, 60, 40));
                category.add(new DefaultMutableTreeNode(page));
            }
        }

        void expandAll() {
            for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
            tree.doLayout();
        }

        Transferable beginDrag(DefaultMutableTreeNode source) throws Exception {
            expandAll();
            tree.setSelectionPath(new TreePath(source.getPath()));
            Transferable transferable = (Transferable) invoke(handler, "createTransferable",
                    new Class<?>[] { JComponent.class }, tree);
            require(transferable != null, "production createTransferable rejected a valid source");
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            require(flavors.length == 1 && transferable.getTransferData(flavors[0]) == source,
                    "drag transferable must carry the original node");
            return transferable;
        }

        void endDrag(Transferable transferable) throws Exception {
            invoke(handler, "exportDone", new Class<?>[] { JComponent.class, Transferable.class, int.class },
                    tree, transferable, TransferHandler.MOVE);
        }

        TransferHandler.TransferSupport support(Transferable data, DefaultMutableTreeNode target, int index,
                boolean lowerHalf) throws Exception {
            TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(tree, data);
            FakeDropEvent event = (FakeDropEvent) UNSAFE.allocateInstance(FakeDropEvent.class);
            event.data = data;
            JTree.DropLocation location = (JTree.DropLocation) UNSAFE.allocateInstance(JTree.DropLocation.class);
            TreePath path = new TreePath(target.getPath());
            Rectangle bounds = tree.getPathBounds(path);
            Point point = bounds == null ? new Point() : new Point(bounds.x + 8,
                    bounds.y + (lowerHalf ? bounds.height * 3 / 4 : bounds.height / 4));
            put(location, JTree.DropLocation.class, "path", path);
            putInt(location, JTree.DropLocation.class, "index", index);
            put(location, TransferHandler.DropLocation.class, "dropPoint", point);
            putBoolean(support, TransferHandler.TransferSupport.class, "isDrop", true);
            put(support, TransferHandler.TransferSupport.class, "source", event);
            put(support, TransferHandler.TransferSupport.class, "dropLocation", location);
            return support;
        }

        boolean move(DefaultMutableTreeNode source, DefaultMutableTreeNode target, int index, boolean lowerHalf)
                throws Exception {
            Transferable data = beginDrag(source);
            try {
                return handler.importData(support(data, target, index, lowerHalf));
            } finally {
                endDrag(data);
            }
        }

        void assertTreeInvariants() {
            Set<String> categoryIds = new HashSet<>();
            Set<String> pageIds = new HashSet<>();
            Set<DefaultMutableTreeNode> pages = new HashSet<>();
            for (DefaultMutableTreeNode categoryNode : children(root)) {
                CustomBook.BookCategory category = (CustomBook.BookCategory) categoryNode.getUserObject();
                require(categoryIds.add(category.id), "duplicate category id");
                require(categoryNode.getChildCount() > 0, "empty category");
                require(category.pages.size() == categoryNode.getChildCount(), "category model order differs from Swing tree");
                for (int index = 0; index < categoryNode.getChildCount(); index++) {
                    DefaultMutableTreeNode pageNode = page(categoryNode, index);
                    require(pages.add(pageNode), "page node duplicated");
                    require(pageIds.add(pageData(pageNode).id), "page id duplicated");
                    require(category.pages.get(index) == pageData(pageNode), "model copied/lost a page during a move");
                    require(pageNode.getParent() == categoryNode, "page parent is invalid");
                }
            }
        }
    }

    private static final class PageSnapshot {
        private final CustomBook.BookPage page;
        private final String id;
        private final String title;
        private final String content;
        private final List<CustomBook.BookElement> elements;
        private final List<String> mediaNames;

        PageSnapshot(DefaultMutableTreeNode node) {
            page = pageData(node);
            id = page.id;
            title = page.title;
            content = page.content;
            elements = List.copyOf(page.elements);
            mediaNames = page.elements.stream().map(element -> element.mediaName + ":" + element.frames).toList();
        }

        void assertPreserved(DefaultMutableTreeNode node) {
            require(pageData(node) == page, "page object identity changed");
            require(page.id.equals(id) && page.title.equals(title) && page.content.equals(content), "page data changed");
            require(page.elements.equals(elements), "visual elements were copied, reordered, or lost");
            require(page.elements.stream().map(element -> element.mediaName + ":" + element.frames).toList().equals(mediaNames),
                    "image/GIF references changed");
        }
    }

    private static final class FailingTreeModel extends DefaultTreeModel {
        private MutableTreeNode failingChild;
        private boolean afterInsertion;

        FailingTreeModel(DefaultMutableTreeNode root) { super(root); }

        void failNextInsertionOf(MutableTreeNode child, boolean after) {
            failingChild = child;
            afterInsertion = after;
        }

        @Override
        public void insertNodeInto(MutableTreeNode child, MutableTreeNode parent, int index) {
            if (child == failingChild) {
                failingChild = null;
                if (afterInsertion) super.insertNodeInto(child, parent, index);
                throw new IllegalStateException("Injected " + (afterInsertion ? "post-insert" : "pre-insert") + " failure");
            }
            super.insertNodeInto(child, parent, index);
        }
    }

    private static final class FailingComboBox extends JComboBox<Object> {
        boolean failNextRefresh;
        boolean sawMovedTarget;
        CustomBook.BookElement watchedLink;

        @Override
        public void removeAllItems() {
            if (failNextRefresh) {
                failNextRefresh = false;
                sawMovedTarget = watchedLink != null && "b".equals(watchedLink.targetCategoryId);
                throw new IllegalStateException("Injected inspector refresh failure after target repair");
            }
            super.removeAllItems();
        }
    }

    /** Its constructor is deliberately skipped; only the safe event-query overrides are used. */
    private static final class FakeDropEvent extends DropTargetDropEvent {
        private Transferable data;
        private FakeDropEvent() { super(null, new Point(), TransferHandler.MOVE, TransferHandler.MOVE); }
        @Override public DataFlavor[] getCurrentDataFlavors() { return data.getTransferDataFlavors(); }
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return data.isDataFlavorSupported(flavor); }
        @Override public int getSourceActions() { return TransferHandler.MOVE; }
        @Override public int getDropAction() { return TransferHandler.MOVE; }
        @Override public Transferable getTransferable() { return data; }
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception cause) throw cause;
            if (exception.getCause() instanceof Error cause) throw cause;
            throw exception;
        }
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    private static long offset(Class<?> type, String name) throws Exception {
        return UNSAFE.objectFieldOffset(type.getDeclaredField(name));
    }

    private static void put(Object target, Class<?> type, String name, Object value) throws Exception {
        UNSAFE.putObject(target, offset(type, name), value);
    }

    private static void putInt(Object target, Class<?> type, String name, int value) throws Exception {
        UNSAFE.putInt(target, offset(type, name), value);
    }

    private static void putBoolean(Object target, Class<?> type, String name, boolean value) throws Exception {
        UNSAFE.putBoolean(target, offset(type, name), value);
    }

    private static void initializeTranslations() throws Exception {
        ResourceBundle bundle;
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/lang/texts.properties"), StandardCharsets.UTF_8)) {
            bundle = new PropertyResourceBundle(reader);
        }
        for (String name : List.of("rb", "rb_en")) {
            Field field = L10N.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, bundle);
        }
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
