package fr.superbroche.mcreator.custombook.ui.modgui;

import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DropMode;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.parts.TextureHolder;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.SearchableComboBox;
import net.mcreator.ui.dialogs.TypedTextureSelectorDialog;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.help.IHelpContext;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.laf.renderer.ModelComboBoxRenderer;
import net.mcreator.ui.minecraft.TabListField;
import net.mcreator.ui.minecraft.TextureSelectionButton;
import net.mcreator.ui.modgui.ModElementGUI;
import net.mcreator.ui.workspace.resources.TextureType;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.resources.Model;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CustomBookGUI
extends ModElementGUI<CustomBook> {
    private static final int MAX_TITLE_LENGTH = 32;
    private static final int MAX_TEXT_LENGTH = Short.MAX_VALUE;
    private static final int MAX_MEDIA_DIMENSION = 8192;
    private static final int MAX_GIF_FRAMES = 500;
    private static final long MAX_DECODED_GIF_PIXELS = 64_000_000L;
    private final MCreator app;
    private final JTextField displayName = new JTextField("Custom Book", 32);
    private final JTextField bookTitle = new JTextField("Custom Book", 32);
    private final JTextField author = new JTextField("Unknown", 32);
    private final JSpinner generation = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
    private final JComboBox<String> rarity = new JComboBox<String>(new String[]{"COMMON", "UNCOMMON", "RARE", "EPIC"});
    private final JSpinner stackSize = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
    private final JSpinner enchantability = new JSpinner(new SpinnerNumberModel(0, 0, 128000, 1));
    private final JCheckBox immuneToFire = new JCheckBox(CustomBookGUI.tr("option.enable"));
    private final JCheckBox piglinCurrency = new JCheckBox(CustomBookGUI.tr("option.enable"));
    private final JCheckBox destroyAnyBlock = new JCheckBox(CustomBookGUI.tr("option.enable"));
    private final JCheckBox startingBook = new JCheckBox(CustomBookGUI.tr("option.first_spawn_only"));
    private final JCheckBox hideNextArrowAtCategoryEnd = new JCheckBox(CustomBookGUI.tr("option.stop_at_category_end"));
    private final JCheckBox glow = new JCheckBox(CustomBookGUI.tr("option.enable"));
    private TabListField creativeTabsField;
    private TypedTextureSelectorDialog textureSelectorDialog;
    private TextureSelectionButton itemTexture;
    private final SearchableComboBox<Model> itemModel = new SearchableComboBox();
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(CustomBookGUI.tr("tree.book"));
    private final DefaultTreeModel treeModel = new DefaultTreeModel(this.rootNode);
    private final JTree bookTree = new JTree(this.treeModel);
    private final JTextField pageTitle = new JTextField();
    private final JCheckBox pageTitleVisible = new JCheckBox(CustomBookGUI.tr("option.show_title"), true);
    private final JTextPane textEditor = new JTextPane();
    private final JSpinner fontSize = new JSpinner(new SpinnerNumberModel(12, 6, 48, 1));
    private final JComboBox<String> textAlignment = new JComboBox<String>(new String[]{"LEFT", "CENTER", "RIGHT"});
    private final JSpinner elementX = new JSpinner(new SpinnerNumberModel(18, 0, 255, 1));
    private final JSpinner elementY = new JSpinner(new SpinnerNumberModel(48, 0, 319, 1));
    private final JSpinner elementW = new JSpinner(new SpinnerNumberModel(220, 1, 256, 1));
    private final JSpinner elementH = new JSpinner(new SpinnerNumberModel(220, 1, 320, 1));
    private final JCheckBox snapToGrid = new JCheckBox(CustomBookGUI.tr("option.snap_to_grid"));
    private final JCheckBox showGrid = new JCheckBox(CustomBookGUI.tr("option.show_grid"), true);
    private final JSpinner gridSize = new JSpinner(new SpinnerNumberModel(8, 2, 32, 1));
    private final JTextField buttonLabel = new JTextField(CustomBookGUI.tr("default.button"));
    private final JComboBox<CategoryRef> buttonTarget = new JComboBox();
    private final JComboBox<PageRef> buttonPageTarget = new JComboBox();
    private final JComboBox<String> buttonStyle = new JComboBox<String>(new String[]{"CLASSIC", "FLAT", "OUTLINE", "TRANSPARENT"});
    private final JComboBox<String> buttonImageMode = new JComboBox<String>(new String[]{"NONE", "ICON_LEFT", "BACKGROUND"});
    private final JButton buttonBackgroundColor = CustomBookGUI.colorSwatch("#8A6846", CustomBookGUI.tr("field.background_color"));
    private final JButton buttonBorderColor = CustomBookGUI.colorSwatch("#6D5237", CustomBookGUI.tr("field.border_color"));
    private final JButton buttonTextColor = CustomBookGUI.colorSwatch("#FFEDC5", CustomBookGUI.tr("field.text_color"));
    private final JComboBox<String> buttonImageType = new JComboBox<String>(new String[]{"GUI", "ITEM", "BLOCK"});
    private final CardLayout buttonImagePickerLayout = new CardLayout();
    private final JPanel buttonImagePicker = new JPanel(this.buttonImagePickerLayout);
    private TextureSelectionButton buttonImageScreenTexture;
    private TextureSelectionButton buttonImageItemTexture;
    private TextureSelectionButton buttonImageBlockTexture;
    private final JComboBox<String> navContainerStyle = new JComboBox<String>(new String[]{"TRANSPARENT", "OUTLINE", "FLAT", "CLASSIC", "IMAGE"});
    private final JComboBox<String> navGlyphStyle = new JComboBox<String>(new String[]{"CHEVRON", "TRIANGLE", "DOUBLE"});
    private final JButton navBackgroundColor = CustomBookGUI.colorSwatch("#FFF4D6", CustomBookGUI.tr("field.arrow_background_color"));
    private final JButton navBorderColor = CustomBookGUI.colorSwatch("#6D5237", CustomBookGUI.tr("field.arrow_border_color"));
    private final JButton navIconColor = CustomBookGUI.colorSwatch("#6A5842", CustomBookGUI.tr("field.arrow_symbol_color"));
    private final JComboBox<String> navImageType = new JComboBox<String>(new String[]{"GUI", "ITEM", "BLOCK"});
    private final CardLayout navImagePickerLayout = new CardLayout();
    private final JPanel navImagePicker = new JPanel(this.navImagePickerLayout);
    private TextureSelectionButton navImageScreenTexture;
    private TextureSelectionButton navImageItemTexture;
    private TextureSelectionButton navImageBlockTexture;
    private final JPanel navImageOptionsPanel = new JPanel(new GridBagLayout());
    private final JLabel navDirectionInfo = new JLabel(CustomBookGUI.tr("navigation.title"));
    private final JCheckBox resizeBySides = new JCheckBox(CustomBookGUI.tr("option.resize_sides"), true);
    private final JCheckBox resizeByCorners = new JCheckBox(CustomBookGUI.tr("option.resize_corners"), true);
    private final JLabel mediaInfo = new JLabel(" ");
    private final JLabel selectionInfo = new JLabel(CustomBookGUI.tr("message.no_selection"));
    private final CardLayout inspectorCardLayout = new CardLayout();
    private final JPanel inspectorCards = new JPanel(this.inspectorCardLayout);
    private final PagePreviewCanvas previewCanvas = new PagePreviewCanvas();
    private CustomBook.BookPage activePage;
    private CustomBook.BookElement activeElement;
    private boolean loadingPage;
    private boolean loadingElement;

    private static String tr(String key, Object... arguments) {
        return L10N.t("custombook." + key, arguments);
    }

    public CustomBookGUI(MCreator mCreator, ModElement modElement, boolean bl) {
        super(mCreator, modElement, bl);
        this.app = mCreator;
        this.initGUI();
        super.finalizeGUI();
    }

    protected void initGUI() {
        this.textureSelectorDialog = new TypedTextureSelectorDialog(this.app, TextureType.ITEM);
        this.itemTexture = new TextureSelectionButton(this.textureSelectorDialog, 72);
        this.creativeTabsField = new TabListField(this.app);
        this.creativeTabsField.setPreferredSize(new Dimension(0, 44));
        this.creativeTabsField.setListElements(List.of(new TabEntry(this.app.getWorkspace(), "TOOLS")));
        this.initMultiTypeImagePickers();
        this.itemModel.setRenderer((ListCellRenderer)new ModelComboBoxRenderer());
        this.itemModel.setPreferredSize(new Dimension(360, 42));
        this.reloadModelChoices();
        this.configureCompactPropertyFields();
        this.addPage(CustomBookGUI.tr("page.visuals"), this.createVisualsPage());
        this.addPage(CustomBookGUI.tr("page.properties"), this.createPropertiesPage());
        this.addPage(CustomBookGUI.tr("page.book_editor"), this.createEditorPage(), false);
        this.bindLiveSpinner(this.elementX, () -> this.geometryChangedFromEditor(this.elementX, "X"));
        this.bindLiveSpinner(this.elementY, () -> this.geometryChangedFromEditor(this.elementY, "Y"));
        this.bindLiveSpinner(this.elementW, () -> this.geometryChangedFromEditor(this.elementW, "W"));
        this.bindLiveSpinner(this.elementH, () -> this.geometryChangedFromEditor(this.elementH, "H"));
        this.bindLiveSpinner(this.fontSize, this::applyLiveFontSize);
    }

    private void initMultiTypeImagePickers() {
        this.buttonImageScreenTexture = this.createTextureButton(TextureType.SCREEN, 48, "BUTTON", "SCREEN");
        this.buttonImageItemTexture = this.createTextureButton(TextureType.ITEM, 48, "BUTTON", "ITEM");
        this.buttonImageBlockTexture = this.createTextureButton(TextureType.BLOCK, 48, "BUTTON", "BLOCK");
        this.buttonImagePicker.add((Component)this.buttonImageScreenTexture, "SCREEN");
        this.buttonImagePicker.add((Component)this.buttonImageItemTexture, "ITEM");
        this.buttonImagePicker.add((Component)this.buttonImageBlockTexture, "BLOCK");
        this.navImageScreenTexture = this.createTextureButton(TextureType.SCREEN, 48, "NAV", "SCREEN");
        this.navImageItemTexture = this.createTextureButton(TextureType.ITEM, 48, "NAV", "ITEM");
        this.navImageBlockTexture = this.createTextureButton(TextureType.BLOCK, 48, "NAV", "BLOCK");
        this.navImagePicker.add((Component)this.navImageScreenTexture, "SCREEN");
        this.navImagePicker.add((Component)this.navImageItemTexture, "ITEM");
        this.navImagePicker.add((Component)this.navImageBlockTexture, "BLOCK");
        Dimension dimension = new Dimension(52, 52);
        this.buttonImagePicker.setPreferredSize(dimension);
        this.buttonImagePicker.setMinimumSize(dimension);
        this.buttonImagePicker.setMaximumSize(dimension);
        this.navImagePicker.setPreferredSize(dimension);
        this.navImagePicker.setMinimumSize(dimension);
        this.navImagePicker.setMaximumSize(dimension);
        this.buttonImageType.addActionListener(actionEvent -> {
            String string = CustomBookGUI.textureTypeInternal(String.valueOf(this.buttonImageType.getSelectedItem()));
            this.buttonImagePickerLayout.show(this.buttonImagePicker, string);
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type)) {
                this.activeElement.buttonImageType = string;
                this.activeElement.buttonImageName = "";
                this.previewCanvas.repaint();
            }
        });
        this.navImageType.addActionListener(actionEvent -> {
            String string = CustomBookGUI.textureTypeInternal(String.valueOf(this.navImageType.getSelectedItem()));
            this.navImagePickerLayout.show(this.navImagePicker, string);
            if (!this.loadingElement && CustomBookGUI.isNavigationElement(this.activeElement)) {
                this.activeElement.buttonImageType = string;
                this.activeElement.buttonImageName = "";
                this.previewCanvas.repaint();
            }
        });
    }

    private TextureSelectionButton createTextureButton(TextureType textureType, int n, String string, String string2) {
        TypedTextureSelectorDialog typedTextureSelectorDialog = new TypedTextureSelectorDialog(this.app, textureType);
        TextureSelectionButton textureSelectionButton = new TextureSelectionButton(typedTextureSelectorDialog, n);
        textureSelectionButton.addTextureSelectedListener(actionEvent -> {
            if (this.loadingElement || this.activeElement == null) {
                return;
            }
            if ("BUTTON".equals(string) && "BUTTON".equals(this.activeElement.type)) {
                this.activeElement.buttonImageType = string2;
                this.activeElement.buttonImageName = textureSelectionButton.hasTexture() ? textureSelectionButton.getTextureHolder().getRawTextureName() : "";
            } else if ("NAV".equals(string) && CustomBookGUI.isNavigationElement(this.activeElement)) {
                this.activeElement.buttonImageType = string2;
                this.activeElement.buttonImageName = textureSelectionButton.hasTexture() ? textureSelectionButton.getTextureHolder().getRawTextureName() : "";
            }
            this.previewCanvas.repaint();
        });
        return textureSelectionButton;
    }

    private void reloadModelChoices() {
        Model model2 = this.itemModel.getSelectedItem();
        List<Model> object = new ArrayList<>();
        object.add(new Model.BuiltInModel("Normal"));
        try {
            for (Model model3 : Model.getModelsWithTextureMaps((Workspace)this.app.getWorkspace())) {
                if (model3.getType() != Model.Type.JSON && model3.getType() != Model.Type.OBJ) continue;
                object.add(model3);
            }
        }
        catch (RuntimeException exception) {
            System.err.println("[CustomBookCreator] Could not load custom item models; keeping the built-in model available.");
            exception.printStackTrace();
        }
        this.itemModel.setItems(object);
        if (model2 != null) {
            for (int i = 0; i < this.itemModel.getItemCount(); ++i) {
                if (!model2.equals(this.itemModel.getItemAt(i))) continue;
                this.itemModel.setSelectedIndex(i);
                return;
            }
        }
        if (this.itemModel.getItemCount() > 0) {
            this.itemModel.setSelectedIndex(0);
        }
    }

    private void configureCompactPropertyFields() {
        CustomBookGUI.setCompactWidth(this.displayName, 320);
        CustomBookGUI.setCompactWidth(this.bookTitle, 320);
        CustomBookGUI.setCompactWidth(this.author, 320);
        CustomBookGUI.setCompactWidth(this.rarity, 320);
        CustomBookGUI.setCompactWidth(this.stackSize, 120);
        CustomBookGUI.setCompactWidth(this.enchantability, 120);
        CustomBookGUI.setCompactWidth(this.generation, 120);
        CustomBookGUI.setCompactWidth(this.hideNextArrowAtCategoryEnd, 390);
        this.creativeTabsField.setPreferredSize(new Dimension(430, 44));
        this.creativeTabsField.setMinimumSize(new Dimension(330, 44));
        this.creativeTabsField.setMaximumSize(new Dimension(430, 44));
    }

    private static void setCompactWidth(JComponent jComponent, int n) {
        Dimension dimension = jComponent.getPreferredSize();
        int n2 = Math.max(26, dimension.height);
        Dimension dimension2 = new Dimension(n, n2);
        jComponent.setPreferredSize(dimension2);
        jComponent.setMinimumSize(new Dimension(Math.min(n, 120), n2));
        jComponent.setMaximumSize(dimension2);
    }

    private static String textureTypeInternal(String string) {
        return "GUI".equalsIgnoreCase(string) ? "SCREEN" : string;
    }

    private static String textureTypeDisplay(String string) {
        return "SCREEN".equalsIgnoreCase(string) ? "GUI" : string;
    }

    private JComponent createPropertiesPage() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        jPanel2.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.item_properties")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints.anchor = 17;
        gridBagConstraints.fill = 2;
        int n = 0;
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.in_game_name"), this.displayName, "custombook/properties/in_game_name");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.rarity"), this.rarity, "custombook/properties/rarity");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.creative_tabs"), (JComponent)this.creativeTabsField, "custombook/properties/creative_tabs");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.max_stack_size"), this.stackSize, "custombook/properties/max_stack_size");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.enchantability"), this.enchantability, "custombook/properties/enchantability");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.fire_immune"), this.immuneToFire, "custombook/properties/fire_immune");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.piglin_currency"), this.piglinCurrency, "custombook/properties/piglin_currency");
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.destroy_any_block"), this.destroyAnyBlock, "custombook/properties/destroy_any_block");
        JPanel jPanel3 = new JPanel(new GridBagLayout());
        jPanel3.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.book_properties")));
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.insets = new Insets(6, 10, 6, 10);
        gridBagConstraints2.anchor = 17;
        gridBagConstraints2.fill = 2;
        int n2 = 0;
        this.addRow(jPanel3, gridBagConstraints2, n2++, CustomBookGUI.tr("field.book_title"), this.bookTitle, null);
        this.addRow(jPanel3, gridBagConstraints2, n2++, CustomBookGUI.tr("field.author"), this.author, null);
        this.addRow(jPanel3, gridBagConstraints2, n2++, CustomBookGUI.tr("field.book_generation"), this.generation, "custombook/properties/book_generation");
        this.addRow(jPanel3, gridBagConstraints2, n2++, CustomBookGUI.tr("field.starting_book"), this.startingBook, "custombook/properties/starting_book");
        this.addRow(jPanel3, gridBagConstraints2, n2++, CustomBookGUI.tr("field.category_navigation"), this.hideNextArrowAtCategoryEnd, null);
        JPanel jPanel4 = new JPanel();
        jPanel4.setLayout(new BoxLayout(jPanel4, 1));
        jPanel2.setAlignmentX(0.0f);
        jPanel3.setAlignmentX(0.0f);
        jPanel4.add(jPanel2);
        jPanel4.add(Box.createVerticalStrut(12));
        jPanel4.add(jPanel3);
        jPanel.add((Component)jPanel4, "North");
        return jPanel;
    }

    private JComponent createVisualsPage() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        jPanel2.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.visuals")));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(7, 10, 7, 10);
        gridBagConstraints.anchor = 17;
        gridBagConstraints.fill = 2;
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 0, 0));
        jPanel3.setOpaque(false);
        jPanel3.add((Component)this.itemTexture);
        JPanel jPanel4 = new JPanel(new FlowLayout(0, 0, 0));
        jPanel4.setOpaque(false);
        jPanel4.add((Component)this.itemModel);
        int n = 0;
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.item_texture"), jPanel3, null);
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.item_model"), jPanel4, null);
        JPanel jPanel5 = new JPanel(new FlowLayout(0, 0, 0));
        jPanel5.setOpaque(false);
        jPanel5.add(this.glow);
        this.addRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.glowing_effect"), jPanel5, "custombook/visuals/glow");
        jPanel.add((Component)jPanel2, "North");
        return jPanel;
    }

    private JComponent createEditorPage() {
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel jPanel2 = this.createNavigationPanel();
        JPanel jPanel3 = this.createInspectorPanel();
        JPanel jPanel4 = this.createPreviewPanel();
        JSplitPane jSplitPane = new JSplitPane(1, jPanel3, jPanel4);
        jSplitPane.setResizeWeight(0.5);
        jSplitPane.setContinuousLayout(true);
        jSplitPane.setDividerLocation(590);
        JSplitPane jSplitPane2 = new JSplitPane(1, jPanel2, jSplitPane);
        jSplitPane2.setResizeWeight(0.16);
        jSplitPane2.setContinuousLayout(true);
        jSplitPane2.setDividerLocation(215);
        jPanel.add((Component)jSplitPane2, "Center");
        this.ensureDefaultStructure();
        this.bookTree.expandRow(0);
        this.selectFirstPage();
        return jPanel;
    }

    private JPanel createNavigationPanel() {
        JPanel jPanel = new JPanel(new BorderLayout(6, 6));
        jPanel.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.categories_pages")));
        this.bookTree.setRootVisible(false);
        this.bookTree.setShowsRootHandles(true);
        this.bookTree.setRowHeight(24);
        this.bookTree.setDragEnabled(true);
        this.bookTree.setDropMode(DropMode.ON_OR_INSERT);
        this.bookTree.setTransferHandler(new BookTreeTransferHandler());
        this.bookTree.setToolTipText(CustomBookGUI.tr("tooltip.drag_reorder"));
        this.bookTree.addTreeSelectionListener(treeSelectionEvent -> this.onTreeSelectionChanged());
        JLabel dragHint = new JLabel("<html><small>" + CustomBookGUI.tr("tooltip.drag_reorder") + "</small></html>");
        dragHint.setBorder(new EmptyBorder(0, 3, 2, 3));
        jPanel.add((Component)dragHint, "North");
        jPanel.add((Component)new JScrollPane(this.bookTree), "Center");
        JPanel jPanel2 = new JPanel(new GridLayout(0, 1, 4, 4));
        JButton jButton = new JButton(CustomBookGUI.tr("action.add_category"));
        JButton jButton2 = new JButton(CustomBookGUI.tr("action.add_page"));
        JButton jButton3 = new JButton(CustomBookGUI.tr("action.rename"));
        JButton jButton4 = new JButton(CustomBookGUI.tr("action.delete"));
        jButton.addActionListener(actionEvent -> this.addCategory());
        jButton2.addActionListener(actionEvent -> this.addPageToSelectedCategory());
        jButton3.addActionListener(actionEvent -> this.renameSelectedNode());
        jButton4.addActionListener(actionEvent -> this.removeSelectedNode());
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel2.add(jButton3);
        jPanel2.add(jButton4);
        jPanel.add((Component)jPanel2, "South");
        jPanel.setMinimumSize(new Dimension(180, 320));
        return jPanel;
    }

    private JPanel createInspectorPanel() {
        JPanel jPanel = new JPanel(new BorderLayout(7, 7));
        jPanel.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.page_elements")));
        JPanel jPanel2 = new JPanel(new BorderLayout(6, 6));
        JPanel jPanel3 = new JPanel(new BorderLayout(6, 0));
        jPanel3.add((Component)new JLabel(CustomBookGUI.tr("field.page_title")), "West");
        jPanel3.add((Component)this.pageTitle, "Center");
        jPanel3.add((Component)this.pageTitleVisible, "East");
        jPanel2.add((Component)jPanel3, "North");
        jPanel2.add((Component)this.createElementToolbar(), "Center");
        jPanel.add((Component)jPanel2, "North");
        this.inspectorCards.add((Component)this.createNoSelectionCard(), "NONE");
        this.inspectorCards.add((Component)this.createTextCard(), "TEXT");
        this.inspectorCards.add((Component)this.createMediaCard(), "MEDIA");
        this.inspectorCards.add((Component)this.createButtonCard(), "BUTTON");
        this.inspectorCards.add((Component)this.createNavigationCard(), "NAV");
        jPanel.add((Component)this.inspectorCards, "Center");
        JPanel jPanel4 = new JPanel(new GridBagLayout());
        jPanel4.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.position_size")));
        this.addGeometryField(jPanel4, 0, "X", this.elementX);
        this.addGeometryField(jPanel4, 1, "Y", this.elementY);
        this.addGeometryField(jPanel4, 2, CustomBookGUI.tr("field.width"), this.elementW);
        this.addGeometryField(jPanel4, 3, CustomBookGUI.tr("field.height"), this.elementH);
        jPanel.add((Component)jPanel4, "South");
        this.pageTitle.getDocument().addDocumentListener(CustomBookGUI.simpleListener(() -> {
            if (!this.loadingPage && this.activePage != null) {
                this.activePage.title = this.pageTitle.getText().isBlank() ? CustomBookGUI.tr("default.page_plain") : this.pageTitle.getText();
                DefaultMutableTreeNode defaultMutableTreeNode = this.selectedPageNode();
                if (defaultMutableTreeNode != null) {
                    this.treeModel.nodeChanged(defaultMutableTreeNode);
                }
                this.previewCanvas.repaint();
            }
        }));
        this.pageTitleVisible.addActionListener(actionEvent -> {
            if (!this.loadingPage && this.activePage != null) {
                this.activePage.showTitle = this.pageTitleVisible.isSelected();
                this.previewCanvas.repaint();
            }
        });
        this.textEditor.getDocument().addDocumentListener(CustomBookGUI.simpleListener(() -> {
            if (!this.loadingElement && this.activeElement != null && "TEXT".equals(this.activeElement.type)) {
                this.activeElement.content = CustomBookGUI.limit(this.textEditor.getText(), MAX_TEXT_LENGTH);
                this.syncLegacyPageContent();
                this.previewCanvas.repaint();
            }
        }));
        this.buttonLabel.getDocument().addDocumentListener(CustomBookGUI.simpleListener(() -> {
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type)) {
                this.activeElement.label = this.buttonLabel.getText().isBlank() ? CustomBookGUI.tr("default.button") : this.buttonLabel.getText();
                this.previewCanvas.repaint();
            }
        }));
        this.buttonTarget.addActionListener(actionEvent -> {
            Object object;
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type) && (object = this.buttonTarget.getSelectedItem()) instanceof CategoryRef) {
                CategoryRef categoryRef = (CategoryRef)object;
                this.activeElement.targetCategoryId = categoryRef.id;
                object = this.activeElement.targetPageId;
                this.refreshButtonPageTargets(categoryRef.id, (String)object);
                Object object2 = this.buttonPageTarget.getSelectedItem();
                if (object2 instanceof PageRef) {
                    PageRef pageRef = (PageRef)object2;
                    this.activeElement.targetPageId = pageRef.id;
                }
                this.previewCanvas.repaint();
            }
        });
        this.buttonPageTarget.addActionListener(actionEvent -> {
            Object object;
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type) && (object = this.buttonPageTarget.getSelectedItem()) instanceof PageRef) {
                PageRef pageRef = (PageRef)object;
                this.activeElement.targetPageId = pageRef.id;
                this.previewCanvas.repaint();
            }
        });
        this.buttonStyle.addActionListener(actionEvent -> {
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type)) {
                this.activeElement.buttonStyle = (String)this.buttonStyle.getSelectedItem();
                this.previewCanvas.repaint();
            }
        });
        this.buttonImageMode.addActionListener(actionEvent -> {
            if (!this.loadingElement && this.activeElement != null && "BUTTON".equals(this.activeElement.type)) {
                this.activeElement.buttonImageMode = (String)this.buttonImageMode.getSelectedItem();
                this.previewCanvas.repaint();
            }
        });
        this.buttonBackgroundColor.addActionListener(actionEvent -> this.chooseButtonColor("BACKGROUND"));
        this.buttonBorderColor.addActionListener(actionEvent -> this.chooseButtonColor("BORDER"));
        this.buttonTextColor.addActionListener(actionEvent -> this.chooseButtonColor("TEXT"));
        this.navContainerStyle.addActionListener(actionEvent -> {
            this.updateNavigationImageVisibility();
            if (!this.loadingElement && CustomBookGUI.isNavigationElement(this.activeElement)) {
                this.activeElement.buttonStyle = (String)this.navContainerStyle.getSelectedItem();
                this.previewCanvas.repaint();
            }
        });
        this.navGlyphStyle.addActionListener(actionEvent -> {
            if (!this.loadingElement && CustomBookGUI.isNavigationElement(this.activeElement)) {
                this.activeElement.align = CustomBookGUI.navGlyphToAlign((String)this.navGlyphStyle.getSelectedItem());
                this.previewCanvas.repaint();
            }
        });
        this.navBackgroundColor.addActionListener(actionEvent -> this.chooseNavigationColor("BACKGROUND"));
        this.navBorderColor.addActionListener(actionEvent -> this.chooseNavigationColor("BORDER"));
        this.navIconColor.addActionListener(actionEvent -> this.chooseNavigationColor("ICON"));
        this.resizeBySides.addActionListener(actionEvent -> {
            if (!this.loadingElement && this.activeElement != null && ("IMAGE".equals(this.activeElement.type) || "GIF".equals(this.activeElement.type))) {
                this.activeElement.resizeBySides = this.resizeBySides.isSelected();
                this.previewCanvas.repaint();
            }
        });
        this.resizeByCorners.addActionListener(actionEvent -> {
            if (!this.loadingElement && this.activeElement != null && ("IMAGE".equals(this.activeElement.type) || "GIF".equals(this.activeElement.type))) {
                this.activeElement.resizeByCorners = this.resizeByCorners.isSelected();
                this.previewCanvas.repaint();
            }
        });
        this.textAlignment.addActionListener(actionEvent -> {
            if (!this.loadingElement && this.activeElement != null && "TEXT".equals(this.activeElement.type)) {
                this.activeElement.align = (String)this.textAlignment.getSelectedItem();
                this.previewCanvas.repaint();
            }
        });
        return jPanel;
    }

    private JToolBar createElementToolbar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.setFloatable(false);
        jToolBar.setBorder(new EmptyBorder(4, 0, 4, 0));
        JButton jButton = CustomBookGUI.compactButton(CustomBookGUI.tr("action.add_text"), CustomBookGUI.tr("tooltip.add_text"));
        JButton jButton2 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.add_image"), CustomBookGUI.tr("tooltip.add_image"));
        JButton jButton3 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.add_gif"), CustomBookGUI.tr("tooltip.add_gif"));
        JButton jButton4 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.add_button"), CustomBookGUI.tr("tooltip.add_button"));
        JButton jButton5 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.previous_arrow"), CustomBookGUI.tr("tooltip.previous_arrow"));
        JButton jButton6 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.next_arrow"), CustomBookGUI.tr("tooltip.next_arrow"));
        JButton jButton7 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.duplicate"), CustomBookGUI.tr("tooltip.duplicate"));
        JButton jButton8 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.delete"), CustomBookGUI.tr("tooltip.delete_element"));
        jButton.addActionListener(actionEvent -> this.addTextElement());
        jButton2.addActionListener(actionEvent -> this.importImageElement());
        jButton3.addActionListener(actionEvent -> this.importGifElement());
        jButton4.addActionListener(actionEvent -> this.addCategoryButton());
        jButton5.addActionListener(actionEvent -> this.selectNavigationElement("NAV_PREV"));
        jButton6.addActionListener(actionEvent -> this.selectNavigationElement("NAV_NEXT"));
        jButton7.addActionListener(actionEvent -> this.duplicateActiveElement());
        jButton8.addActionListener(actionEvent -> this.deleteActiveElement());
        jToolBar.add(jButton);
        jToolBar.add(jButton2);
        jToolBar.add(jButton3);
        jToolBar.add(jButton4);
        jToolBar.addSeparator();
        jToolBar.add(jButton5);
        jToolBar.add(jButton6);
        jToolBar.addSeparator();
        jToolBar.add(jButton7);
        jToolBar.add(jButton8);
        return jToolBar;
    }

    private JPanel createNoSelectionCard() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JLabel jLabel = new JLabel(CustomBookGUI.tr("hint.no_selection"), 0);
        jPanel.add((Component)jLabel, "Center");
        return jPanel;
    }

    private JPanel createTextCard() {
        JPanel jPanel = new JPanel(new BorderLayout(6, 6));
        jPanel.add((Component)this.createFormattingToolbar(), "North");
        this.textEditor.setFont(new Font("Monospaced", 0, 14));
        this.textEditor.setMargin(new Insets(10, 10, 10, 10));
        jPanel.add((Component)new JScrollPane(this.textEditor), "Center");
        JLabel jLabel = new JLabel(CustomBookGUI.tr("hint.text_formatting"));
        jPanel.add((Component)jLabel, "South");
        return jPanel;
    }

    private JPanel createMediaCard() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        this.mediaInfo.setHorizontalAlignment(0);
        jPanel.add((Component)this.mediaInfo, "Center");
        JPanel jPanel2 = new JPanel(new GridLayout(0, 1, 4, 4));
        jPanel2.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.resize_handles")));
        this.resizeBySides.setToolTipText(CustomBookGUI.tr("tooltip.resize_sides"));
        this.resizeByCorners.setToolTipText(CustomBookGUI.tr("tooltip.resize_corners"));
        jPanel2.add(this.resizeBySides);
        jPanel2.add(this.resizeByCorners);
        jPanel.add((Component)jPanel2, "South");
        return jPanel;
    }

    private JPanel createButtonCard() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 8, 5, 8);
        gridBagConstraints.fill = 2;
        gridBagConstraints.anchor = 17;
        int n = 0;
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.button_text"), this.buttonLabel);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.target_category"), this.buttonTarget);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.target_page"), this.buttonPageTarget);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.style"), this.buttonStyle);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.background_color"), this.buttonBackgroundColor);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.border_color"), this.buttonBorderColor);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.text_color"), this.buttonTextColor);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.texture_type"), this.buttonImageType);
        this.buttonImagePicker.setToolTipText(CustomBookGUI.tr("tooltip.texture_library"));
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.button_image"), this.compactSquareHolder(this.buttonImagePicker));
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.image_placement"), this.buttonImageMode);
        JLabel jLabel = new JLabel(CustomBookGUI.tr("hint.button_styles"));
        jLabel.setBorder(new EmptyBorder(3, 8, 3, 8));
        jPanel.add((Component)jPanel2, "North");
        jPanel.add((Component)jLabel, "Center");
        return jPanel;
    }

    private JPanel createNavigationCard() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 8, 5, 8);
        gridBagConstraints.fill = 2;
        gridBagConstraints.anchor = 17;
        this.navDirectionInfo.setFont(this.navDirectionInfo.getFont().deriveFont(1));
        this.navDirectionInfo.setBorder(new EmptyBorder(4, 8, 8, 8));
        jPanel.add((Component)this.navDirectionInfo, "North");
        int n = 0;
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.container_style"), this.navContainerStyle);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.arrow_shape"), this.navGlyphStyle);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.background_color"), this.navBackgroundColor);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.border_color"), this.navBorderColor);
        this.addButtonInspectorRow(jPanel2, gridBagConstraints, n++, CustomBookGUI.tr("field.arrow_color"), this.navIconColor);
        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.insets = new Insets(2, 0, 2, 0);
        gridBagConstraints2.fill = 2;
        gridBagConstraints2.weightx = 1.0;
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 0;
        this.navImageOptionsPanel.add((Component)new JLabel(CustomBookGUI.tr("field.texture_type")), gridBagConstraints2);
        gridBagConstraints2.gridx = 1;
        this.navImageOptionsPanel.add(this.navImageType, gridBagConstraints2);
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 1;
        this.navImageOptionsPanel.add((Component)new JLabel(CustomBookGUI.tr("field.custom_image")), gridBagConstraints2);
        gridBagConstraints2.gridx = 1;
        this.navImageOptionsPanel.add((Component)this.compactSquareHolder(this.navImagePicker), gridBagConstraints2);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = n++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add((Component)this.navImageOptionsPanel, gridBagConstraints);
        JButton jButton = new JButton(CustomBookGUI.tr("action.reset_navigation"));
        jButton.addActionListener(actionEvent -> this.resetSelectedNavigation());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = n;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add((Component)jButton, gridBagConstraints);
        JLabel jLabel = new JLabel(CustomBookGUI.tr("hint.navigation"));
        jLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        jPanel.add((Component)jPanel2, "Center");
        jPanel.add((Component)jLabel, "South");
        this.updateNavigationImageVisibility();
        return jPanel;
    }

    private void updateNavigationImageVisibility() {
        boolean bl = "IMAGE".equals(String.valueOf(this.navContainerStyle.getSelectedItem()));
        this.navImageOptionsPanel.setVisible(bl);
        this.navGlyphStyle.setEnabled(!bl);
        this.navBackgroundColor.setEnabled(!bl);
        this.navBorderColor.setEnabled(!bl);
        this.navIconColor.setEnabled(!bl);
        this.navImageOptionsPanel.revalidate();
        this.navImageOptionsPanel.repaint();
    }

    private JPanel compactSquareHolder(JComponent jComponent) {
        JPanel jPanel = new JPanel(new FlowLayout(0, 0, 0));
        jPanel.setOpaque(false);
        Dimension dimension = new Dimension(54, 54);
        jPanel.setPreferredSize(dimension);
        jPanel.setMinimumSize(dimension);
        jPanel.setMaximumSize(dimension);
        jPanel.add(jComponent);
        return jPanel;
    }

    private void addButtonInspectorRow(JPanel jPanel, GridBagConstraints gridBagConstraints, int n, String string, Component component) {
        GridBagConstraints gridBagConstraints2 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = n;
        gridBagConstraints2.weightx = 0.0;
        gridBagConstraints2.fill = 0;
        jPanel.add((Component)new JLabel(string), gridBagConstraints2);
        GridBagConstraints gridBagConstraints3 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints3.gridx = 1;
        gridBagConstraints3.gridy = n;
        gridBagConstraints3.weightx = 0.0;
        gridBagConstraints3.fill = 0;
        jPanel.add(component, gridBagConstraints3);
        GridBagConstraints gridBagConstraints4 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints4.gridx = 2;
        gridBagConstraints4.gridy = n;
        gridBagConstraints4.weightx = 1.0;
        gridBagConstraints4.fill = 2;
        jPanel.add(Box.createHorizontalGlue(), gridBagConstraints4);
    }

    private JToolBar createFormattingToolbar() {
        JToolBar jToolBar = new JToolBar();
        jToolBar.setFloatable(false);
        JButton jButton = CustomBookGUI.compactButton("B", CustomBookGUI.tr("format.bold"));
        jButton.setFont(jButton.getFont().deriveFont(1));
        JButton jButton2 = CustomBookGUI.compactButton("I", CustomBookGUI.tr("format.italic"));
        jButton2.setFont(jButton2.getFont().deriveFont(2));
        JButton jButton3 = CustomBookGUI.compactButton("U", CustomBookGUI.tr("format.underline"));
        JButton jButton4 = CustomBookGUI.compactButton("S", CustomBookGUI.tr("format.strikethrough"));
        JButton jButton5 = CustomBookGUI.compactButton("\u00a7k", CustomBookGUI.tr("format.obfuscated"));
        jButton.addActionListener(actionEvent -> this.wrapSelection("[b]", "[/b]", CustomBookGUI.tr("placeholder.bold_text")));
        jButton2.addActionListener(actionEvent -> this.wrapSelection("[i]", "[/i]", CustomBookGUI.tr("placeholder.italic_text")));
        jButton3.addActionListener(actionEvent -> this.wrapSelection("[u]", "[/u]", CustomBookGUI.tr("placeholder.underlined_text")));
        jButton4.addActionListener(actionEvent -> this.wrapSelection("[s]", "[/s]", CustomBookGUI.tr("placeholder.struck_text")));
        jButton5.addActionListener(actionEvent -> this.wrapSelection("[obf]", "[/obf]", CustomBookGUI.tr("placeholder.obfuscated_text")));
        jToolBar.add(jButton);
        jToolBar.add(jButton2);
        jToolBar.add(jButton3);
        jToolBar.add(jButton4);
        jToolBar.add(jButton5);
        jToolBar.addSeparator();
        jToolBar.add(new JLabel(" " + CustomBookGUI.tr("format.size") + " "));
        this.fontSize.setMaximumSize(new Dimension(62, 28));
        this.fontSize.setToolTipText(CustomBookGUI.tr("tooltip.font_size"));
        jToolBar.add(this.fontSize);
        JButton jButton6 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.color"), CustomBookGUI.tr("field.text_color"));
        jButton6.addActionListener(actionEvent -> this.chooseColor());
        jToolBar.add(jButton6);
        jToolBar.addSeparator();
        jToolBar.add(new JLabel(" " + CustomBookGUI.tr("format.alignment") + " "));
        this.textAlignment.setMaximumSize(new Dimension(86, 28));
        jToolBar.add(this.textAlignment);
        jToolBar.addSeparator();
        JButton jButton7 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.web_link"), CustomBookGUI.tr("tooltip.web_link"));
        JButton jButton8 = CustomBookGUI.compactButton(CustomBookGUI.tr("action.page_link"), CustomBookGUI.tr("tooltip.page_link"));
        jButton7.addActionListener(actionEvent -> this.insertUrlLink());
        jButton8.addActionListener(actionEvent -> this.insertPageLink());
        jToolBar.add(jButton7);
        jToolBar.add(jButton8);
        return jToolBar;
    }

    private JPanel createPreviewPanel() {
        JPanel jPanel = new JPanel(new BorderLayout(6, 6));
        jPanel.setBorder(BorderFactory.createTitledBorder(CustomBookGUI.tr("section.interactive_preview")));
        jPanel.add((Component)this.previewCanvas, "Center");
        JPanel jPanel2 = new JPanel(new BorderLayout(4, 4));
        this.selectionInfo.setBorder(new EmptyBorder(3, 5, 1, 5));
        jPanel2.add((Component)this.selectionInfo, "North");
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 7, 2));
        jPanel3.setOpaque(false);
        this.snapToGrid.setToolTipText(CustomBookGUI.tr("tooltip.snap_to_grid"));
        this.showGrid.setToolTipText(CustomBookGUI.tr("tooltip.show_grid"));
        this.gridSize.setPreferredSize(new Dimension(58, 26));
        this.gridSize.setToolTipText(CustomBookGUI.tr("tooltip.grid_size"));
        jPanel3.add(this.snapToGrid);
        jPanel3.add(this.showGrid);
        jPanel3.add(new JLabel(CustomBookGUI.tr("field.grid_step")));
        jPanel3.add(this.gridSize);
        jPanel3.add(new JLabel("px"));
        jPanel2.add((Component)jPanel3, "Center");
        JLabel jLabel = new JLabel(CustomBookGUI.tr("hint.preview_controls"));
        jLabel.setBorder(new EmptyBorder(1, 5, 5, 5));
        jPanel2.add((Component)jLabel, "South");
        this.snapToGrid.addActionListener(actionEvent -> this.previewCanvas.repaint());
        this.showGrid.addActionListener(actionEvent -> this.previewCanvas.repaint());
        this.gridSize.addChangeListener(changeEvent -> this.previewCanvas.repaint());
        jPanel.add((Component)jPanel2, "South");
        jPanel.setMinimumSize(new Dimension(360, 420));
        return jPanel;
    }

    private void addGeometryField(JPanel jPanel, int n, String string, JSpinner jSpinner) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(4, 5, 4, 5);
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = n;
        gridBagConstraints.weightx = 0.25;
        gridBagConstraints.fill = 2;
        JPanel jPanel2 = new JPanel(new BorderLayout(3, 0));
        jPanel2.add((Component)new JLabel(string), "West");
        jPanel2.add((Component)jSpinner, "Center");
        jPanel.add((Component)jPanel2, gridBagConstraints);
    }

    private void geometryChanged() {
        if (this.loadingElement || this.activeElement == null) {
            return;
        }
        this.activeElement.x = (Integer)this.elementX.getValue();
        this.activeElement.y = (Integer)this.elementY.getValue();
        this.activeElement.width = (Integer)this.elementW.getValue();
        this.activeElement.height = (Integer)this.elementH.getValue();
        this.activeElement.normalize();
        this.syncGeometryControls();
        this.updateSelectionInfo();
        this.previewCanvas.repaint();
    }

    private void syncGeometryControls() {
        if (this.activeElement == null) {
            return;
        }
        boolean previousLoadingState = this.loadingElement;
        this.loadingElement = true;
        try {
            this.elementX.setValue(this.activeElement.x);
            this.elementY.setValue(this.activeElement.y);
            this.elementW.setValue(this.activeElement.width);
            this.elementH.setValue(this.activeElement.height);
        }
        finally {
            this.loadingElement = previousLoadingState;
        }
    }

    private void bindLiveSpinner(JSpinner jSpinner, final Runnable runnable) {
        jSpinner.addChangeListener(changeEvent -> runnable.run());
        JComponent jComponent = jSpinner.getEditor();
        if (jComponent instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor defaultEditor = (JSpinner.DefaultEditor)jComponent;
            defaultEditor.getTextField().getDocument().addDocumentListener(CustomBookGUI.simpleListener(runnable));
            defaultEditor.getTextField().addActionListener(actionEvent -> runnable.run());
            defaultEditor.getTextField().addFocusListener(new FocusAdapter(){

                @Override
                public void focusLost(FocusEvent focusEvent) {
                    runnable.run();
                }
            });
        }
    }

    private int spinnerEditorInt(JSpinner jSpinner, int n) {
        int n2;
        Object object;
        Object object2;
        try {
            object2 = jSpinner.getEditor();
            if (object2 instanceof JSpinner.DefaultEditor && !((String)(object2 = ((JSpinner.DefaultEditor)(object = (JSpinner.DefaultEditor)object2)).getTextField().getText().trim())).isEmpty() && !((String)object2).equals("-")) {
                return Integer.parseInt((String)object2);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        object = jSpinner.getValue();
        if (object instanceof Number) {
            object2 = (Number)object;
            n2 = ((Number)object2).intValue();
        } else {
            n2 = n;
        }
        return n2;
    }

    private void geometryChangedFromEditor(JSpinner jSpinner, String string) {
        if (this.loadingElement || this.activeElement == null) {
            return;
        }
        int n = this.spinnerEditorInt(jSpinner, switch (string) {
            case "X" -> this.activeElement.x;
            case "Y" -> this.activeElement.y;
            case "W" -> this.activeElement.width;
            default -> this.activeElement.height;
        });
        switch (string) {
            case "X": {
                this.activeElement.x = Math.max(0, Math.min(255, n));
                break;
            }
            case "Y": {
                this.activeElement.y = Math.max(0, Math.min(319, n));
                break;
            }
            case "W": {
                this.activeElement.width = Math.max(1, Math.min(256, n));
                break;
            }
            case "H": {
                this.activeElement.height = Math.max(1, Math.min(320, n));
            }
        }
        this.activeElement.x = Math.max(0, Math.min(256 - this.activeElement.width, this.activeElement.x));
        this.activeElement.y = Math.max(0, Math.min(320 - this.activeElement.height, this.activeElement.y));
        this.updateSelectionInfo();
        this.previewCanvas.repaint();
    }

    private void addRow(JPanel jPanel, GridBagConstraints gridBagConstraints, int n, String string, JComponent jComponent, String string2) {
        GridBagConstraints gridBagConstraints2 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints2.gridy = n;
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridwidth = 1;
        gridBagConstraints2.weightx = 0.0;
        gridBagConstraints2.fill = 0;
        gridBagConstraints2.anchor = 17;
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 4, 0));
        jPanel2.setOpaque(false);
        jPanel2.add(new JLabel(string + " :"));
        if (string2 != null) {
            jPanel2.add(HelpUtils.helpButton((IHelpContext)this.withEntry(string2)));
        }
        jPanel.add((Component)jPanel2, gridBagConstraints2);
        GridBagConstraints gridBagConstraints3 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints3.gridy = n;
        gridBagConstraints3.gridx = 1;
        gridBagConstraints3.gridwidth = 1;
        gridBagConstraints3.weightx = 0.0;
        gridBagConstraints3.fill = 0;
        gridBagConstraints3.anchor = 17;
        jPanel.add((Component)jComponent, gridBagConstraints3);
        GridBagConstraints gridBagConstraints4 = (GridBagConstraints)gridBagConstraints.clone();
        gridBagConstraints4.gridy = n;
        gridBagConstraints4.gridx = 2;
        gridBagConstraints4.gridwidth = 1;
        gridBagConstraints4.weightx = 1.0;
        gridBagConstraints4.fill = 2;
        jPanel.add(Box.createHorizontalGlue(), gridBagConstraints4);
    }

    private static JButton compactButton(String string, String string2) {
        JButton jButton = new JButton(string);
        jButton.setToolTipText(string2);
        jButton.setFocusable(false);
        return jButton;
    }

    private static DocumentListener simpleListener(final Runnable runnable) {
        return new DocumentListener(){

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                runnable.run();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                runnable.run();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                runnable.run();
            }
        };
    }

    private void ensureDefaultStructure() {
        if (this.rootNode.getChildCount() > 0) {
            return;
        }
        CustomBook.BookCategory bookCategory = new CustomBook.BookCategory(CustomBookGUI.tr("default.general"));
        CustomBook.BookPage bookPage = new CustomBook.BookPage(CustomBookGUI.tr("default.page", 1), "");
        this.ensureNavigationElements(bookPage);
        bookCategory.pages.add(bookPage);
        DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(bookCategory);
        defaultMutableTreeNode.add(new DefaultMutableTreeNode(bookPage));
        this.rootNode.add(defaultMutableTreeNode);
        this.treeModel.reload();
        this.syncCategoryPageListsFromTree();
        this.refreshCategoryTargets();
    }

    private void onTreeSelectionChanged() {
        Object object;
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedNode();
        if (defaultMutableTreeNode == null || !((object = defaultMutableTreeNode.getUserObject()) instanceof CustomBook.BookPage)) {
            this.activePage = null;
            boolean previousLoadingState = this.loadingPage;
            this.loadingPage = true;
            try {
                this.pageTitle.setText("");
                this.pageTitle.setEnabled(false);
                this.pageTitleVisible.setEnabled(false);
            }
            finally {
                this.loadingPage = previousLoadingState;
            }
            this.selectElement(null);
            this.previewCanvas.setPage(null);
            return;
        }
        CustomBook.BookPage bookPage = (CustomBook.BookPage)object;
        this.activePage = bookPage;
        this.activePage.normalizeElements();
        this.ensureNavigationElements(this.activePage);
        boolean previousLoadingState = this.loadingPage;
        this.loadingPage = true;
        try {
            this.pageTitle.setEnabled(true);
            this.pageTitleVisible.setEnabled(true);
            this.pageTitle.setText(bookPage.title == null ? CustomBookGUI.tr("default.page_plain") : bookPage.title);
            this.pageTitleVisible.setSelected(bookPage.showTitle);
        }
        finally {
            this.loadingPage = previousLoadingState;
        }
        this.previewCanvas.setPage(this.activePage);
        this.selectElement(this.activePage.elements.isEmpty() ? null : this.activePage.elements.get(0));
    }

    private void selectElement(CustomBook.BookElement bookElement) {
        this.activeElement = bookElement;
        this.previewCanvas.selected = bookElement;
        boolean previousLoadingState = this.loadingElement;
        this.loadingElement = true;
        try {
            if (bookElement == null) {
                this.inspectorCardLayout.show(this.inspectorCards, "NONE");
                this.setGeometryEnabled(false);
                this.selectionInfo.setText(CustomBookGUI.tr("message.no_selection"));
            } else {
                bookElement.normalize();
                this.setGeometryEnabled(true);
                this.elementX.setValue(bookElement.x);
                this.elementY.setValue(bookElement.y);
                this.elementW.setValue(bookElement.width);
                this.elementH.setValue(bookElement.height);
                switch (bookElement.type) {
                case "TEXT": {
                    this.inspectorCardLayout.show(this.inspectorCards, "TEXT");
                    this.textEditor.setText(bookElement.content == null ? "" : bookElement.content);
                    this.textEditor.setCaretPosition(0);
                    this.textAlignment.setSelectedItem(bookElement.align == null ? "LEFT" : bookElement.align);
                    break;
                }
                case "IMAGE": {
                    this.inspectorCardLayout.show(this.inspectorCards, "MEDIA");
                    this.mediaInfo.setText("<html><div style='text-align:center'><b>" + CustomBookGUI.tr("element.image") + "</b><br>" + CustomBookGUI.escapeHtml(bookElement.mediaName) + "</div></html>");
                    this.resizeBySides.setSelected(bookElement.resizeBySides);
                    this.resizeByCorners.setSelected(bookElement.resizeByCorners);
                    break;
                }
                case "GIF": {
                    this.inspectorCardLayout.show(this.inspectorCards, "MEDIA");
                    this.mediaInfo.setText("<html><div style='text-align:center'><b>" + CustomBookGUI.tr("element.animated_gif") + "</b><br>" + CustomBookGUI.tr("message.frame_count", bookElement.frames.size(), bookElement.frameDelay) + "</div></html>");
                    this.resizeBySides.setSelected(bookElement.resizeBySides);
                    this.resizeByCorners.setSelected(bookElement.resizeByCorners);
                    break;
                }
                case "BUTTON": {
                    this.inspectorCardLayout.show(this.inspectorCards, "BUTTON");
                    this.refreshCategoryTargets();
                    this.buttonLabel.setText(bookElement.label == null ? CustomBookGUI.tr("default.button") : bookElement.label);
                    this.selectCategoryRef(bookElement.targetCategoryId);
                    this.refreshButtonPageTargets(bookElement.targetCategoryId, bookElement.targetPageId);
                    if ("IMAGE".equals(bookElement.buttonStyle)) {
                        bookElement.buttonStyle = "TRANSPARENT";
                        if (bookElement.buttonImageMode == null || "NONE".equals(bookElement.buttonImageMode)) {
                            bookElement.buttonImageMode = "BACKGROUND";
                        }
                    }
                    this.buttonStyle.setSelectedItem(bookElement.buttonStyle);
                    this.buttonImageMode.setSelectedItem(bookElement.buttonImageMode);
                    CustomBookGUI.setColorSwatch(this.buttonBackgroundColor, bookElement.buttonBackgroundColor);
                    CustomBookGUI.setColorSwatch(this.buttonBorderColor, bookElement.buttonBorderColor);
                    CustomBookGUI.setColorSwatch(this.buttonTextColor, bookElement.buttonTextColor);
                    this.buttonImageType.setSelectedItem(CustomBookGUI.textureTypeDisplay(bookElement.buttonImageType == null ? "SCREEN" : bookElement.buttonImageType));
                    this.buttonImagePickerLayout.show(this.buttonImagePicker, CustomBookGUI.textureTypeInternal(String.valueOf(this.buttonImageType.getSelectedItem())));
                    this.setMultiTypeTextureSelection(false, bookElement.buttonImageType, bookElement.buttonImageName);
                    break;
                }
                case "NAV_PREV": 
                case "NAV_NEXT": {
                    this.inspectorCardLayout.show(this.inspectorCards, "NAV");
                    this.navDirectionInfo.setText("NAV_PREV".equals(bookElement.type) ? CustomBookGUI.tr("element.previous_arrow") : CustomBookGUI.tr("element.next_arrow"));
                    this.navContainerStyle.setSelectedItem(bookElement.buttonStyle);
                    this.navGlyphStyle.setSelectedItem(CustomBookGUI.alignToNavGlyph(bookElement.align));
                    CustomBookGUI.setColorSwatch(this.navBackgroundColor, bookElement.buttonBackgroundColor);
                    CustomBookGUI.setColorSwatch(this.navBorderColor, bookElement.buttonBorderColor);
                    CustomBookGUI.setColorSwatch(this.navIconColor, bookElement.buttonTextColor);
                    this.navImageType.setSelectedItem(CustomBookGUI.textureTypeDisplay(bookElement.buttonImageType == null ? "SCREEN" : bookElement.buttonImageType));
                    this.navImagePickerLayout.show(this.navImagePicker, CustomBookGUI.textureTypeInternal(String.valueOf(this.navImageType.getSelectedItem())));
                    this.setMultiTypeTextureSelection(true, bookElement.buttonImageType, bookElement.buttonImageName);
                    this.updateNavigationImageVisibility();
                    break;
                }
                default: {
                    this.inspectorCardLayout.show(this.inspectorCards, "NONE");
                }
                }
                this.updateSelectionInfo();
            }
        }
        finally {
            this.loadingElement = previousLoadingState;
        }
        this.previewCanvas.repaint();
    }

    private void setMultiTypeTextureSelection(boolean bl, String string, String string2) {
        TextureSelectionButton textureSelectionButton;
        String string3;
        String string4 = string3 = string == null ? "SCREEN" : string;
        if (bl) {
            switch (string3) {
                case "ITEM": {
                    textureSelectionButton = this.navImageItemTexture;
                    break;
                }
                case "BLOCK": {
                    textureSelectionButton = this.navImageBlockTexture;
                    break;
                }
                default: {
                    textureSelectionButton = this.navImageScreenTexture;
                    break;
                }
            }
        } else {
            switch (string3) {
                case "ITEM": {
                    textureSelectionButton = this.buttonImageItemTexture;
                    break;
                }
                case "BLOCK": {
                    textureSelectionButton = this.buttonImageBlockTexture;
                    break;
                }
                default: {
                    textureSelectionButton = this.buttonImageScreenTexture;
                }
            }
        }
        TextureSelectionButton textureSelectionButton2 = textureSelectionButton;
        this.clearTextureSelection(textureSelectionButton2);
        if (string2 != null && !string2.isBlank()) {
            try {
                textureSelectionButton2.setTexture(new TextureHolder(this.app.getWorkspace(), string2));
            }
            catch (RuntimeException exception) {
                System.err.println("[CustomBookCreator] Ignoring an invalid or missing button texture: " + string2);
            }
        }
    }

    private void clearTextureSelection(TextureSelectionButton textureSelectionButton) {
        try {
            Field field = TextureSelectionButton.class.getDeclaredField("selectedTexture");
            field.setAccessible(true);
            field.set(textureSelectionButton, null);
        }
        catch (Exception exception) {
            // empty catch block
        }
        textureSelectionButton.setIcon(null);
        textureSelectionButton.setToolTipText(null);
    }

    private void setGeometryEnabled(boolean bl) {
        this.elementX.setEnabled(bl);
        this.elementY.setEnabled(bl);
        this.elementW.setEnabled(bl);
        this.elementH.setEnabled(bl);
    }

    private void updateSelectionInfo() {
        if (this.activeElement == null) {
            this.selectionInfo.setText(CustomBookGUI.tr("message.no_selection"));
        } else {
            this.selectionInfo.setText(this.elementDisplayName(this.activeElement) + "  \u2022  X=" + this.activeElement.x + "  Y=" + this.activeElement.y + "  \u2022  " + this.activeElement.width + "\u00d7" + this.activeElement.height);
        }
    }

    private String elementDisplayName(CustomBook.BookElement element) {
        if (element == null) {
            return CustomBookGUI.tr("element.text");
        }
        return switch (element.type) {
            case "IMAGE" -> CustomBookGUI.tr("element.image");
            case "GIF" -> CustomBookGUI.tr("element.animated_gif");
            case "BUTTON" -> CustomBookGUI.tr("element.button_named", element.label == null ? CustomBookGUI.tr("default.button") : element.label);
            case "NAV_PREV" -> CustomBookGUI.tr("element.previous_arrow");
            case "NAV_NEXT" -> CustomBookGUI.tr("element.next_arrow");
            default -> CustomBookGUI.tr("element.text");
        };
    }

    private void addTextElement() {
        if (this.activePage == null) {
            return;
        }
        CustomBook.BookElement bookElement = CustomBook.BookElement.text(CustomBookGUI.tr("default.new_text"));
        int n = Math.min(80, this.activePage.elements.size() * 8);
        bookElement.x = 18 + n / 2;
        bookElement.y = 48 + n;
        bookElement.width = Math.max(80, 220 - n / 2);
        bookElement.height = 70;
        this.activePage.elements.add(bookElement);
        this.syncLegacyPageContent();
        this.selectElement(bookElement);
    }

    private void duplicateActiveElement() {
        if (this.activePage == null || this.activeElement == null || CustomBookGUI.isNavigationElement(this.activeElement)) {
            return;
        }
        CustomBook.BookElement bookElement = CustomBookGUI.copyElement(this.activeElement);
        bookElement.id = UUID.randomUUID().toString();
        bookElement.x = Math.min(256 - bookElement.width, Math.max(0, bookElement.x + 8));
        bookElement.y = Math.min(320 - bookElement.height, Math.max(0, bookElement.y + 8));
        this.activePage.elements.add(bookElement);
        this.syncLegacyPageContent();
        this.selectElement(bookElement);
    }

    private void syncLegacyPageContent() {
        if (this.activePage == null) {
            return;
        }
        this.activePage.content = CustomBookGUI.firstTextContent(this.activePage);
    }

    private static String firstTextContent(CustomBook.BookPage bookPage) {
        if (bookPage == null || bookPage.elements == null) {
            return "";
        }
        for (CustomBook.BookElement bookElement : bookPage.elements) {
            if (bookElement == null || !"TEXT".equals(bookElement.type)) continue;
            return bookElement.content == null ? "" : bookElement.content;
        }
        return "";
    }

    private void importImageElement() {
        if (this.activePage == null) {
            return;
        }
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle(CustomBookGUI.tr("dialog.import_image"));
        jFileChooser.setFileFilter(new FileNameExtensionFilter(CustomBookGUI.tr("filter.png_image"), "png"));
        if (jFileChooser.showOpenDialog((Component)((Object)this)) != 0) {
            return;
        }
        try {
            File file = jFileChooser.getSelectedFile();
            BufferedImage bufferedImage = CustomBookGUI.decodePng(file);
            String string = this.copyScreenTexture(file, CustomBookGUI.baseName(file));
            int n = Math.min(180, Math.max(16, bufferedImage.getWidth()));
            int n2 = Math.min(180, Math.max(16, bufferedImage.getHeight() * n / Math.max(1, bufferedImage.getWidth())));
            CustomBook.BookElement bookElement = CustomBook.BookElement.image(string, n, n2);
            this.activePage.elements.add(bookElement);
            this.selectElement(bookElement);
        }
        catch (Exception exception) {
            this.showImportError(exception);
        }
    }

    private void importGifElement() {
        if (this.activePage == null) {
            return;
        }
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle(CustomBookGUI.tr("dialog.import_gif"));
        jFileChooser.setFileFilter(new FileNameExtensionFilter(CustomBookGUI.tr("filter.animated_gif"), "gif"));
        if (jFileChooser.showOpenDialog((Component)((Object)this)) != 0) {
            return;
        }
        try {
            int n;
            GifData gifData = CustomBookGUI.decodeGif(jFileChooser.getSelectedFile());
            if (gifData.frames.isEmpty()) {
                throw new IOException(CustomBookGUI.tr("error.gif_no_frames"));
            }
            File file = this.screenTextureFolder();
            String string = CustomBookGUI.uniqueResourceBase(CustomBookGUI.baseName(jFileChooser.getSelectedFile()) + "_gif", file);
            ArrayList<String> arrayList = new ArrayList<String>();
            ArrayList<File> writtenFiles = new ArrayList<File>();
            try {
                for (n = 0; n < gifData.frames.size(); ++n) {
                    String string2 = string + "_" + n;
                    File outputFile = new File(file, string2 + ".png");
                    if (!ImageIO.write((RenderedImage)gifData.frames.get(n), "png", outputFile)) {
                        throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                    }
                    writtenFiles.add(outputFile);
                    arrayList.add(string2);
                }
            }
            catch (Exception exception) {
                for (File writtenFile : writtenFiles) {
                    try {
                        Files.deleteIfExists(writtenFile.toPath());
                    }
                    catch (IOException ignored) {
                    }
                }
                throw exception;
            }
            n = Math.min(180, Math.max(16, gifData.width));
            int n2 = Math.min(180, Math.max(16, gifData.height * n / Math.max(1, gifData.width)));
            CustomBook.BookElement bookElement = CustomBook.BookElement.gif(arrayList, gifData.averageDelayMs(), n, n2);
            this.activePage.elements.add(bookElement);
            this.selectElement(bookElement);
        }
        catch (Exception exception) {
            this.showImportError(exception);
        }
    }

    private void addCategoryButton() {
        if (this.activePage == null) {
            return;
        }
        List<CategoryRef> list = this.collectCategoryRefs();
        if (list.isEmpty()) {
            return;
        }
        JComboBox<CategoryRef> jComboBox = new JComboBox<CategoryRef>(list.toArray(new CategoryRef[0]));
        JComboBox jComboBox2 = new JComboBox();
        Runnable runnable = () -> {
            jComboBox2.removeAllItems();
            Object selection = jComboBox.getSelectedItem();
            if (selection instanceof CategoryRef) {
                CategoryRef categoryRef = (CategoryRef)selection;
                jComboBox2.addItem(new PageRef("", categoryRef.id, CustomBookGUI.tr("target.first_category_page")));
                for (PageRef pageRef : this.collectPageRefs()) {
                    if (!pageRef.categoryId.equals(categoryRef.id)) continue;
                    jComboBox2.addItem(pageRef);
                }
            }
        };
        jComboBox.addActionListener(actionEvent -> runnable.run());
        runnable.run();
        JTextField jTextField = new JTextField(CustomBookGUI.tr("default.open_category", list.get((int)0).label));
        JPanel jPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        jPanel.add(new JLabel(CustomBookGUI.tr("field.target_category")));
        jPanel.add(jComboBox);
        jPanel.add(new JLabel(CustomBookGUI.tr("field.target_page")));
        jPanel.add(jComboBox2);
        jPanel.add(new JLabel(CustomBookGUI.tr("field.button_text")));
        jPanel.add(jTextField);
        if (JOptionPane.showConfirmDialog((Component)((Object)this), jPanel, CustomBookGUI.tr("dialog.add_button"), 2, -1) != 0) {
            return;
        }
        CategoryRef categoryRef = (CategoryRef)jComboBox.getSelectedItem();
        PageRef pageRef = (PageRef)jComboBox2.getSelectedItem();
        if (categoryRef == null) {
            return;
        }
        CustomBook.BookElement bookElement = CustomBook.BookElement.button(jTextField.getText().isBlank() ? categoryRef.label : jTextField.getText(), categoryRef.id);
        bookElement.targetPageId = pageRef == null ? "" : pageRef.id;
        this.activePage.elements.add(bookElement);
        this.selectElement(bookElement);
    }

    private void deleteActiveElement() {
        if (this.activePage == null || this.activeElement == null || CustomBookGUI.isNavigationElement(this.activeElement)) {
            return;
        }
        this.activePage.elements.remove(this.activeElement);
        this.syncLegacyPageContent();
        this.selectElement(this.activePage.elements.isEmpty() ? null : this.activePage.elements.get(0));
    }

    private void ensureNavigationElements(CustomBook.BookPage bookPage) {
        if (bookPage == null) {
            return;
        }
        if (bookPage.elements == null) {
            bookPage.elements = new ArrayList<CustomBook.BookElement>();
        }
        CustomBook.BookElement bookElement = null;
        CustomBook.BookElement bookElement2 = null;
        Iterator<CustomBook.BookElement> iterator = bookPage.elements.iterator();
        while (iterator.hasNext()) {
            CustomBook.BookElement bookElement3 = iterator.next();
            if (bookElement3 == null) continue;
            if ("NAV_PREV".equals(bookElement3.type)) {
                if (bookElement == null) {
                    bookElement = bookElement3;
                    bookElement.normalize();
                    continue;
                }
                iterator.remove();
                continue;
            }
            if (!"NAV_NEXT".equals(bookElement3.type)) continue;
            if (bookElement2 == null) {
                bookElement2 = bookElement3;
                bookElement2.normalize();
                continue;
            }
            iterator.remove();
        }
        if (bookElement == null) {
            bookPage.elements.add(this.createDefaultNavigationElement(false));
        }
        if (bookElement2 == null) {
            bookPage.elements.add(this.createDefaultNavigationElement(true));
        }
    }

    private CustomBook.BookElement createDefaultNavigationElement(boolean bl) {
        CustomBook.BookElement bookElement = new CustomBook.BookElement();
        bookElement.type = bl ? "NAV_NEXT" : "NAV_PREV";
        bookElement.x = bl ? 226 : 12;
        bookElement.y = 278;
        bookElement.width = 18;
        bookElement.height = 18;
        bookElement.buttonStyle = "TRANSPARENT";
        bookElement.buttonBackgroundColor = "#FFF4D6";
        bookElement.buttonBorderColor = "#6D5237";
        bookElement.buttonTextColor = "#6A5842";
        bookElement.buttonImageName = "";
        bookElement.buttonImageMode = "NONE";
        bookElement.align = "LEFT";
        return bookElement;
    }

    private void selectNavigationElement(String string) {
        if (this.activePage == null) {
            return;
        }
        this.ensureNavigationElements(this.activePage);
        for (CustomBook.BookElement bookElement : this.activePage.elements) {
            if (bookElement == null || !string.equals(bookElement.type)) continue;
            this.selectElement(bookElement);
            return;
        }
    }

    private static boolean isNavigationElement(CustomBook.BookElement bookElement) {
        return bookElement != null && ("NAV_PREV".equals(bookElement.type) || "NAV_NEXT".equals(bookElement.type));
    }

    private static String navGlyphToAlign(String string) {
        return switch (string == null ? "CHEVRON" : string) {
            case "TRIANGLE" -> "CENTER";
            case "DOUBLE" -> "RIGHT";
            default -> "LEFT";
        };
    }

    private static String alignToNavGlyph(String string) {
        return switch (string == null ? "LEFT" : string) {
            case "CENTER" -> "TRIANGLE";
            case "RIGHT" -> "DOUBLE";
            default -> "CHEVRON";
        };
    }

    private void resetSelectedNavigation() {
        if (!CustomBookGUI.isNavigationElement(this.activeElement)) {
            return;
        }
        boolean bl = "NAV_NEXT".equals(this.activeElement.type);
        CustomBook.BookElement bookElement = this.createDefaultNavigationElement(bl);
        this.activeElement.x = bookElement.x;
        this.activeElement.y = bookElement.y;
        this.activeElement.width = bookElement.width;
        this.activeElement.height = bookElement.height;
        this.activeElement.buttonStyle = bookElement.buttonStyle;
        this.activeElement.buttonBackgroundColor = bookElement.buttonBackgroundColor;
        this.activeElement.buttonBorderColor = bookElement.buttonBorderColor;
        this.activeElement.buttonTextColor = bookElement.buttonTextColor;
        this.activeElement.buttonImageName = "";
        this.activeElement.align = bookElement.align;
        this.selectElement(this.activeElement);
    }

    private void chooseNavigationColor(String string) {
        Color color;
        if (this.loadingElement || !CustomBookGUI.isNavigationElement(this.activeElement)) {
            return;
        }
        JButton jButton = switch (string) {
            case "BACKGROUND" -> this.navBackgroundColor;
            case "BORDER" -> this.navBorderColor;
            default -> this.navIconColor;
        };
        String string2 = String.valueOf(jButton.getClientProperty("customBook.hex"));
        try {
            color = Color.decode(CustomBookGUI.safeHex(string2, "#000000"));
        }
        catch (Exception exception) {
            color = Color.BLACK;
        }
        Color color2 = JColorChooser.showDialog((Component)((Object)this), CustomBookGUI.tr("dialog.navigation_color"), color);
        if (color2 == null) {
            return;
        }
        String string3 = String.format("#%02X%02X%02X", color2.getRed(), color2.getGreen(), color2.getBlue());
        CustomBookGUI.setColorSwatch(jButton, string3);
        switch (string) {
            case "BACKGROUND": {
                this.activeElement.buttonBackgroundColor = string3;
                break;
            }
            case "BORDER": {
                this.activeElement.buttonBorderColor = string3;
                break;
            }
            default: {
                this.activeElement.buttonTextColor = string3;
            }
        }
        this.previewCanvas.repaint();
    }

    private void applyLiveFontSize() {
        if (this.loadingElement || this.activeElement == null || !"TEXT".equals(this.activeElement.type)) {
            return;
        }
        int n = Math.max(6, Math.min(48, this.spinnerEditorInt(this.fontSize, 12)));
        int n2 = this.textEditor.getSelectionStart();
        int n3 = this.textEditor.getSelectionEnd();
        String string = this.textEditor.getText();
        if (string == null) {
            string = "";
        }
        if (n2 == n3) {
            n2 = 0;
            n3 = string.length();
        }
        if (n2 < 0 || n3 > string.length() || n2 > n3) {
            return;
        }
        String string2 = string.substring(n2, n3);
        Matcher matcher = Pattern.compile("(?s)^\\[size=\\d+\\](.*)\\[/size\\]$").matcher(string2);
        String string3 = matcher.matches() ? matcher.group(1) : string2;
        String string4 = "[size=" + n + "]" + string3 + "[/size]";
        boolean previousLoadingState = this.loadingElement;
        this.loadingElement = true;
        try {
            this.textEditor.select(n2, n3);
            this.textEditor.replaceSelection(string4);
            this.textEditor.select(n2, n2 + string4.length());
        }
        finally {
            this.loadingElement = previousLoadingState;
        }
        this.activeElement.content = CustomBookGUI.limit(this.textEditor.getText(), MAX_TEXT_LENGTH);
        this.syncLegacyPageContent();
        this.previewCanvas.repaint();
    }

    private void wrapSelection(String string, String string2, String string3) {
        if (this.activeElement == null || !"TEXT".equals(this.activeElement.type)) {
            return;
        }
        int n = this.textEditor.getSelectionStart();
        String string4 = this.textEditor.getSelectedText();
        if (string4 == null || string4.isEmpty()) {
            string4 = string3;
        }
        this.textEditor.replaceSelection(string + string4 + string2);
        this.textEditor.requestFocusInWindow();
        this.textEditor.select(n + string.length(), n + string.length() + string4.length());
    }

    private static JButton colorSwatch(String string, String string2) {
        JButton jButton = new JButton(string);
        jButton.setToolTipText(string2 + " \u2014 " + CustomBookGUI.tr("tooltip.click_to_edit"));
        CustomBookGUI.setColorSwatch(jButton, string);
        return jButton;
    }

    private static void setColorSwatch(JButton jButton, String string) {
        String string2 = CustomBookGUI.safeHex(string, "#000000");
        Color color = Color.decode(string2);
        jButton.setText(string2.toUpperCase(Locale.ROOT));
        jButton.setBackground(color);
        int n = (int)(0.299 * (double)color.getRed() + 0.587 * (double)color.getGreen() + 0.114 * (double)color.getBlue());
        jButton.setForeground(n < 145 ? Color.WHITE : Color.BLACK);
        jButton.putClientProperty("customBook.hex", string2.toUpperCase(Locale.ROOT));
    }

    private void chooseButtonColor(String string) {
        Color color;
        if (this.loadingElement || this.activeElement == null || !"BUTTON".equals(this.activeElement.type)) {
            return;
        }
        JButton jButton = switch (string) {
            case "BACKGROUND" -> this.buttonBackgroundColor;
            case "BORDER" -> this.buttonBorderColor;
            default -> this.buttonTextColor;
        };
        String string2 = String.valueOf(jButton.getClientProperty("customBook.hex"));
        try {
            color = Color.decode(CustomBookGUI.safeHex(string2, "#000000"));
        }
        catch (Exception exception) {
            color = Color.BLACK;
        }
        Color color2 = JColorChooser.showDialog((Component)((Object)this), CustomBookGUI.tr("dialog.button_color"), color);
        if (color2 == null) {
            return;
        }
        String string3 = String.format("#%02X%02X%02X", color2.getRed(), color2.getGreen(), color2.getBlue());
        CustomBookGUI.setColorSwatch(jButton, string3);
        switch (string) {
            case "BACKGROUND": {
                this.activeElement.buttonBackgroundColor = string3;
                break;
            }
            case "BORDER": {
                this.activeElement.buttonBorderColor = string3;
                break;
            }
            default: {
                this.activeElement.buttonTextColor = string3;
            }
        }
        this.previewCanvas.repaint();
    }

    private static String safeHex(String string, String string2) {
        return string != null && string.matches("#[0-9a-fA-F]{6}") ? string : string2;
    }

    private void chooseColor() {
        Color color = JColorChooser.showDialog((Component)((Object)this), CustomBookGUI.tr("dialog.text_color"), Color.BLACK);
        if (color == null) {
            return;
        }
        String string = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        this.wrapSelection("[color=" + string + "]", "[/color]", CustomBookGUI.tr("placeholder.colored_text"));
    }

    private void insertUrlLink() {
        if (this.activeElement == null || !"TEXT".equals(this.activeElement.type)) {
            return;
        }
        String string = JOptionPane.showInputDialog((Component)((Object)this), CustomBookGUI.tr("dialog.url_prompt"), "https://");
        if (string == null || string.isBlank()) {
            return;
        }
        this.wrapSelection("[url=" + string.trim().replace("]", "") + "]", "[/url]", CustomBookGUI.tr("placeholder.link"));
    }

    private void insertPageLink() {
        Object object;
        if (this.activeElement == null || !"TEXT".equals(this.activeElement.type)) {
            return;
        }
        List<PageRef> list = this.collectPageRefs();
        if (list.isEmpty()) {
            return;
        }
        JComboBox<PageRef> jComboBox = new JComboBox<PageRef>(list.toArray(new PageRef[0]));
        int n = JOptionPane.showConfirmDialog((Component)((Object)this), jComboBox, CustomBookGUI.tr("dialog.target_page"), 2, -1);
        if (n != 0 || !((object = jComboBox.getSelectedItem()) instanceof PageRef)) {
            return;
        }
        PageRef pageRef = (PageRef)object;
        this.wrapSelection("[page=" + pageRef.id + "]", "[/page]", pageRef.label);
    }

    private DefaultMutableTreeNode selectedNode() {
        TreePath treePath = this.bookTree.getSelectionPath();
        return treePath == null ? null : (DefaultMutableTreeNode)treePath.getLastPathComponent();
    }

    private DefaultMutableTreeNode selectedPageNode() {
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedNode();
        return defaultMutableTreeNode != null && defaultMutableTreeNode.getUserObject() instanceof CustomBook.BookPage ? defaultMutableTreeNode : null;
    }

    private DefaultMutableTreeNode selectedCategoryNode() {
        TreeNode treeNode;
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedNode();
        if (defaultMutableTreeNode == null) {
            return null;
        }
        if (defaultMutableTreeNode.getUserObject() instanceof CustomBook.BookCategory) {
            return defaultMutableTreeNode;
        }
        if (defaultMutableTreeNode.getUserObject() instanceof CustomBook.BookPage && (treeNode = defaultMutableTreeNode.getParent()) instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode defaultMutableTreeNode2 = (DefaultMutableTreeNode)treeNode;
            return defaultMutableTreeNode2;
        }
        return null;
    }

    private void addCategory() {
        String string = JOptionPane.showInputDialog((Component)((Object)this), CustomBookGUI.tr("dialog.new_category_name"), CustomBookGUI.tr("default.new_category"), -1);
        if (string == null || string.isBlank()) {
            return;
        }
        CustomBook.BookCategory bookCategory = new CustomBook.BookCategory(string.trim());
        CustomBook.BookPage bookPage = new CustomBook.BookPage(CustomBookGUI.tr("default.page", 1), "");
        this.ensureNavigationElements(bookPage);
        bookCategory.pages.add(bookPage);
        DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(bookCategory);
        DefaultMutableTreeNode defaultMutableTreeNode2 = new DefaultMutableTreeNode(bookPage);
        defaultMutableTreeNode.add(defaultMutableTreeNode2);
        this.rootNode.add(defaultMutableTreeNode);
        this.treeModel.reload(this.rootNode);
        this.syncCategoryPageListsFromTree();
        this.refreshCategoryTargets();
        this.bookTree.expandPath(new TreePath(defaultMutableTreeNode.getPath()));
        this.bookTree.setSelectionPath(new TreePath(defaultMutableTreeNode2.getPath()));
    }

    private void addPageToSelectedCategory() {
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedCategoryNode();
        if (defaultMutableTreeNode == null) {
            this.ensureDefaultStructure();
            defaultMutableTreeNode = (DefaultMutableTreeNode)this.rootNode.getChildAt(0);
        }
        CustomBook.BookCategory bookCategory = (CustomBook.BookCategory)defaultMutableTreeNode.getUserObject();
        String string = CustomBookGUI.tr("default.page", defaultMutableTreeNode.getChildCount() + 1);
        String string2 = JOptionPane.showInputDialog((Component)((Object)this), CustomBookGUI.tr("dialog.page_title"), string);
        if (string2 == null) {
            return;
        }
        CustomBook.BookPage bookPage = new CustomBook.BookPage((String)(string2.isBlank() ? string : string2.trim()), "");
        this.ensureNavigationElements(bookPage);
        bookCategory.pages.add(bookPage);
        DefaultMutableTreeNode defaultMutableTreeNode2 = new DefaultMutableTreeNode(bookPage);
        defaultMutableTreeNode.add(defaultMutableTreeNode2);
        this.treeModel.reload(defaultMutableTreeNode);
        this.syncCategoryPageListsFromTree();
        this.bookTree.expandPath(new TreePath(defaultMutableTreeNode.getPath()));
        this.bookTree.setSelectionPath(new TreePath(defaultMutableTreeNode2.getPath()));
    }

    private void renameSelectedNode() {
        Object object;
        String string;
        Object object2;
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedNode();
        if (defaultMutableTreeNode == null) {
            return;
        }
        Object object3 = defaultMutableTreeNode.getUserObject();
        if (object3 instanceof CustomBook.BookCategory) {
            object2 = (CustomBook.BookCategory)object3;
            string = ((CustomBook.BookCategory)object2).name;
        } else if (object3 instanceof CustomBook.BookPage) {
            object = (CustomBook.BookPage)object3;
            string = ((CustomBook.BookPage)object).title;
        } else {
            string = "";
        }
        String string2 = string;
        object = JOptionPane.showInputDialog((Component)((Object)this), CustomBookGUI.tr("dialog.new_name"), string2);
        if (object == null || ((String)object).isBlank()) {
            return;
        }
        if (object3 instanceof CustomBook.BookCategory) {
            object2 = (CustomBook.BookCategory)object3;
            ((CustomBook.BookCategory)object2).name = ((String)object).trim();
        }
        if (object3 instanceof CustomBook.BookPage) {
            object2 = (CustomBook.BookPage)object3;
            ((CustomBook.BookPage)object2).title = ((String)object).trim();
            if (object2 == this.activePage) {
                this.pageTitle.setText(((CustomBook.BookPage)object2).title);
            }
        }
        this.treeModel.nodeChanged(defaultMutableTreeNode);
        this.refreshCategoryTargets();
        this.previewCanvas.repaint();
    }

    private void removeSelectedNode() {
        DefaultMutableTreeNode defaultMutableTreeNode = this.selectedNode();
        if (defaultMutableTreeNode == null) {
            return;
        }
        if (defaultMutableTreeNode.getUserObject() instanceof CustomBook.BookCategory) {
            if (this.rootNode.getChildCount() <= 1) {
                JOptionPane.showMessageDialog((Component)((Object)this), CustomBookGUI.tr("error.keep_one_category"));
                return;
            }
            this.rootNode.remove(defaultMutableTreeNode);
            this.treeModel.reload(this.rootNode);
        } else if (defaultMutableTreeNode.getUserObject() instanceof CustomBook.BookPage) {
            DefaultMutableTreeNode defaultMutableTreeNode2 = (DefaultMutableTreeNode)defaultMutableTreeNode.getParent();
            if (defaultMutableTreeNode2.getChildCount() <= 1) {
                JOptionPane.showMessageDialog((Component)((Object)this), CustomBookGUI.tr("error.keep_one_page"));
                return;
            }
            defaultMutableTreeNode2.remove(defaultMutableTreeNode);
            this.treeModel.reload(defaultMutableTreeNode2);
        }
        this.syncCategoryPageListsFromTree();
        this.repairButtonTargets();
        this.refreshCategoryTargets();
        this.activePage = null;
        this.selectFirstPage();
    }

    private void selectFirstPage() {
        this.ensureDefaultStructure();
        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)this.rootNode.getChildAt(0);
        if (defaultMutableTreeNode.getChildCount() == 0) {
            return;
        }
        DefaultMutableTreeNode defaultMutableTreeNode2 = (DefaultMutableTreeNode)defaultMutableTreeNode.getChildAt(0);
        this.bookTree.expandPath(new TreePath(defaultMutableTreeNode.getPath()));
        this.bookTree.setSelectionPath(new TreePath(defaultMutableTreeNode2.getPath()));
    }

    private void syncCategoryPageListsFromTree() {
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            if (!(this.rootNode.getChildAt(i) instanceof DefaultMutableTreeNode categoryNode)
                    || !(categoryNode.getUserObject() instanceof CustomBook.BookCategory category)) {
                continue;
            }
            category.pages.clear();
            for (int j = 0; j < categoryNode.getChildCount(); ++j) {
                if (categoryNode.getChildAt(j) instanceof DefaultMutableTreeNode pageNode
                        && pageNode.getUserObject() instanceof CustomBook.BookPage page) {
                    category.pages.add(page);
                }
            }
        }
    }

    private void updateMovedPageButtonTargets(String pageId, String categoryId) {
        if (pageId == null || pageId.isBlank() || categoryId == null || categoryId.isBlank()) {
            return;
        }
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            if (!(this.rootNode.getChildAt(i) instanceof DefaultMutableTreeNode categoryNode)) {
                continue;
            }
            for (int j = 0; j < categoryNode.getChildCount(); ++j) {
                if (!(categoryNode.getChildAt(j) instanceof DefaultMutableTreeNode pageNode)
                        || !(pageNode.getUserObject() instanceof CustomBook.BookPage page)
                        || page.elements == null) {
                    continue;
                }
                for (CustomBook.BookElement element : page.elements) {
                    if (element != null && "BUTTON".equals(element.type) && pageId.equals(element.targetPageId)) {
                        element.targetCategoryId = categoryId;
                    }
                }
            }
        }
    }

    private void repairButtonTargets() {
        Map<String, String> pageCategories = new HashMap<String, String>();
        Set<String> categoryIds = new HashSet<String>();
        String fallbackCategoryId = "";
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            if (!(this.rootNode.getChildAt(i) instanceof DefaultMutableTreeNode categoryNode)
                    || !(categoryNode.getUserObject() instanceof CustomBook.BookCategory category)) {
                continue;
            }
            if (fallbackCategoryId.isBlank()) {
                fallbackCategoryId = category.id;
            }
            categoryIds.add(category.id);
            for (int j = 0; j < categoryNode.getChildCount(); ++j) {
                if (categoryNode.getChildAt(j) instanceof DefaultMutableTreeNode pageNode
                        && pageNode.getUserObject() instanceof CustomBook.BookPage page) {
                    pageCategories.putIfAbsent(page.id, category.id);
                }
            }
        }
        if (fallbackCategoryId.isBlank()) {
            return;
        }
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            if (!(this.rootNode.getChildAt(i) instanceof DefaultMutableTreeNode categoryNode)) {
                continue;
            }
            for (int j = 0; j < categoryNode.getChildCount(); ++j) {
                if (!(categoryNode.getChildAt(j) instanceof DefaultMutableTreeNode pageNode)
                        || !(pageNode.getUserObject() instanceof CustomBook.BookPage page)
                        || page.elements == null) {
                    continue;
                }
                for (CustomBook.BookElement element : page.elements) {
                    if (element == null || !"BUTTON".equals(element.type)) {
                        continue;
                    }
                    String targetPageId = element.targetPageId == null ? "" : element.targetPageId;
                    element.targetPageId = targetPageId;
                    if (!targetPageId.isBlank()) {
                        String owningCategory = pageCategories.get(targetPageId);
                        if (owningCategory != null) {
                            element.targetCategoryId = owningCategory;
                            continue;
                        }
                        element.targetPageId = "";
                    }
                    if (element.targetCategoryId == null || !categoryIds.contains(element.targetCategoryId)) {
                        element.targetCategoryId = fallbackCategoryId;
                    }
                }
            }
        }
    }

    private List<PageRef> collectPageRefs() {
        ArrayList<PageRef> arrayList = new ArrayList<PageRef>();
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode)this.rootNode.getChildAt(i);
            CustomBook.BookCategory bookCategory = (CustomBook.BookCategory)defaultMutableTreeNode.getUserObject();
            for (int j = 0; j < defaultMutableTreeNode.getChildCount(); ++j) {
                CustomBook.BookPage bookPage = (CustomBook.BookPage)((DefaultMutableTreeNode)defaultMutableTreeNode.getChildAt(j)).getUserObject();
                arrayList.add(new PageRef(bookPage.id, bookCategory.id, bookCategory.name + " \u203a " + bookPage.title));
            }
        }
        return arrayList;
    }

    private List<CategoryRef> collectCategoryRefs() {
        ArrayList<CategoryRef> arrayList = new ArrayList<CategoryRef>();
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            CustomBook.BookCategory bookCategory = (CustomBook.BookCategory)((DefaultMutableTreeNode)this.rootNode.getChildAt(i)).getUserObject();
            arrayList.add(new CategoryRef(bookCategory.id, bookCategory.name));
        }
        return arrayList;
    }

    private void refreshCategoryTargets() {
        String string = this.activeElement != null ? this.activeElement.targetCategoryId : "";
        boolean previousLoadingState = this.loadingElement;
        this.loadingElement = true;
        try {
            this.buttonTarget.removeAllItems();
            for (CategoryRef categoryRef : this.collectCategoryRefs()) {
                this.buttonTarget.addItem(categoryRef);
            }
            this.selectCategoryRef(string);
        }
        finally {
            this.loadingElement = previousLoadingState;
        }
    }

    private void refreshButtonPageTargets(String string, String string2) {
        boolean previousLoadingState = this.loadingElement;
        this.loadingElement = true;
        try {
            this.buttonPageTarget.removeAllItems();
            if (string == null) {
                string = "";
            }
            this.buttonPageTarget.addItem(new PageRef("", string, CustomBookGUI.tr("target.first_category_page")));
            for (PageRef pageRef : this.collectPageRefs()) {
                if (!pageRef.categoryId.equals(string)) continue;
                this.buttonPageTarget.addItem(pageRef);
            }
            for (int i = 0; i < this.buttonPageTarget.getItemCount(); ++i) {
                PageRef pageRef = this.buttonPageTarget.getItemAt(i);
                if (!pageRef.id.equals(string2 == null ? "" : string2)) continue;
                this.buttonPageTarget.setSelectedIndex(i);
                return;
            }
            if (this.buttonPageTarget.getItemCount() > 0) {
                this.buttonPageTarget.setSelectedIndex(0);
            }
        }
        finally {
            this.loadingElement = previousLoadingState;
        }
    }

    private void selectCategoryRef(String string) {
        for (int i = 0; i < this.buttonTarget.getItemCount(); ++i) {
            CategoryRef categoryRef = this.buttonTarget.getItemAt(i);
            if (!categoryRef.id.equals(string)) continue;
            this.buttonTarget.setSelectedIndex(i);
            return;
        }
        if (this.buttonTarget.getItemCount() > 0) {
            this.buttonTarget.setSelectedIndex(0);
        }
    }

    private File screenTextureFolder() throws IOException {
        File file = this.app.getWorkspace().getFolderManager().getTexturesFolder(TextureType.SCREEN);
        if (file == null) {
            throw new IOException(CustomBookGUI.tr("error.no_screen_texture_folder"));
        }
        Files.createDirectories(file.toPath(), new FileAttribute[0]);
        return file;
    }

    private String copyScreenTexture(File file, String string) throws IOException {
        File file2 = this.screenTextureFolder();
        String string2 = CustomBookGUI.uniqueResourceBase(string, file2);
        Files.copy(file.toPath(), new File(file2, string2 + ".png").toPath(), new CopyOption[0]);
        return string2;
    }

    private static String uniqueResourceBase(String string, File file) {
        String string2 = CustomBookGUI.sanitizeResourceName(string);
        if (string2.isBlank()) {
            string2 = "book_media";
        }
        String object = string2;
        int n = 2;
        while (new File(file, (String)object + ".png").exists()) {
            object = string2 + "_" + n++;
        }
        return object;
    }

    private static String sanitizeResourceName(String string) {
        if (string == null) {
            return "";
        }
        String string2 = string.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        string2 = string2.replaceAll("_+", "_");
        return string2.replaceAll("^_+|_+$", "");
    }

    private static String baseName(File file) {
        String string = file.getName();
        int n = string.lastIndexOf(46);
        return n > 0 ? string.substring(0, n) : string;
    }

    private void showImportError(Exception exception) {
        JOptionPane.showMessageDialog((Component)((Object)this), CustomBookGUI.tr("error.import_media", exception.getMessage()), "Custom Book Creator", 0);
    }

    private BufferedImage loadScreenTexture(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            File file = this.app.getWorkspace().getFolderManager().getTexturesFolder(TextureType.SCREEN);
            if (file == null) {
                return null;
            }
            File file2 = new File(file, string + ".png");
            return file2.isFile() ? ImageIO.read(file2) : null;
        }
        catch (Exception exception) {
            return null;
        }
    }

    private Image loadWorkspaceTexture(String string, String string2) {
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            TextureType textureType = switch (string2 == null ? "SCREEN" : string2) {
                case "ITEM" -> TextureType.ITEM;
                case "BLOCK" -> TextureType.BLOCK;
                default -> TextureType.SCREEN;
            };
            return new TextureHolder(this.app.getWorkspace(), string).getImageIcon(textureType).getImage();
        }
        catch (Exception exception) {
            return null;
        }
    }

    private String markupToHtml(String string) {
        Pattern pattern = Pattern.compile("\\[(/?)(b|i|u|s|obf|size|color|url|page)(?:=([^\\]]+))?\\]", 2);
        String markup = string == null ? "" : string;
        Matcher matcher = pattern.matcher(markup);
        StringBuilder stringBuilder = new StringBuilder();
        Deque<String> openTags = new ArrayDeque<String>();
        int n = 0;
        while (matcher.find()) {
            stringBuilder.append(CustomBookGUI.escapeHtmlBreakable(markup.substring(n, matcher.start())));
            boolean bl = !matcher.group(1).isEmpty();
            String string2 = matcher.group(2).toLowerCase(Locale.ROOT);
            String string3 = matcher.group(3);
            if (bl) {
                // Match the runtime parser: ignore an unmatched close, otherwise
                // close it and any styles still nested inside it.
                if (openTags.contains(string2)) {
                    String closedTag;
                    do {
                        closedTag = openTags.pop();
                        stringBuilder.append(closingHtmlTag(closedTag));
                    } while (!closedTag.equals(string2));
                }
            } else {
                openTags.push(string2);
                switch (string2) {
                    case "b": {
                        stringBuilder.append("<b>");
                        break;
                    }
                    case "i": {
                        stringBuilder.append("<i>");
                        break;
                    }
                    case "u": {
                        stringBuilder.append("<u>");
                        break;
                    }
                    case "s": {
                        stringBuilder.append("<span style='text-decoration:line-through'>");
                        break;
                    }
                    case "obf": {
                        stringBuilder.append("<span style='font-family:monospace;background:#ddd'>");
                        break;
                    }
                    case "size": {
                        stringBuilder.append("<span style='font-size:").append(CustomBookGUI.safeInt(string3, 12, 6, 48)).append("px'>");
                        break;
                    }
                    case "color": {
                        stringBuilder.append("<span style='color:").append(CustomBookGUI.safeColor(string3)).append("'>");
                        break;
                    }
                    case "url": 
                    case "page": {
                        stringBuilder.append("<span style='text-decoration:underline;color:#2f62c9;font-size:inherit'>");
                    }
                }
            }
            n = matcher.end();
        }
        stringBuilder.append(CustomBookGUI.escapeHtmlBreakable(markup.substring(n)));
        while (!openTags.isEmpty()) {
            stringBuilder.append(closingHtmlTag(openTags.pop()));
        }
        return stringBuilder.toString();
    }

    private static String closingHtmlTag(String tag) {
        return switch (tag) {
            case "b", "i", "u" -> "</" + tag + ">";
            default -> "</span>";
        };
    }

    private static String escapeHtmlBreakable(String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        String string2 = CustomBookGUI.escapeHtml(string).replace("\n", "<br>");
        StringBuilder stringBuilder = new StringBuilder(string2.length() * 2);
        boolean bl = false;
        boolean bl2 = false;
        for (int i = 0; i < string2.length();) {
            int c2 = string2.codePointAt(i);
            i += Character.charCount(c2);
            if (c2 == '<') {
                bl2 = true;
            }
            if (c2 == '&') {
                bl = true;
            }
            stringBuilder.appendCodePoint(c2);
            if (c2 == '>') {
                bl2 = false;
            }
            if (c2 == ';' && bl) {
                bl = false;
            }
            if (bl2 || bl || c2 == ' ' || c2 == '>' || c2 == ';' || c2 == '\n' || i >= string2.length()) continue;
            int next = string2.codePointAt(i);
            if (next == '<' || next == '&' || next == ' ') continue;
            stringBuilder.append("&#8203;");
        }
        return stringBuilder.toString();
    }

    private static String safeColor(String string) {
        return string != null && string.matches("#[0-9a-fA-F]{6}") ? string : "#000000";
    }

    private static int safeInt(String string, int n, int n2, int n3) {
        try {
            return Math.max(n2, Math.min(n3, Integer.parseInt(string)));
        }
        catch (Exception exception) {
            return n;
        }
    }

    private static String escapeHtml(String string) {
        if (string == null) {
            return "";
        }
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public void reloadDataLists() {
        this.reloadModelChoices();
    }

    public void openInEditingMode(CustomBook customBook) {
        ArrayList<TabEntry> arrayList;
        this.displayName.setText(customBook.name == null ? "Custom Book" : customBook.name);
        this.bookTitle.setText(customBook.getSafeBookTitle());
        this.author.setText(customBook.getSafeAuthor());
        this.generation.setValue(customBook.getSafeGeneration());
        this.rarity.setSelectedItem(customBook.getSafeRarity());
        this.stackSize.setValue(customBook.getSafeStackSize());
        this.enchantability.setValue(customBook.getSafeEnchantability());
        this.immuneToFire.setSelected(customBook.immuneToFire);
        this.piglinCurrency.setSelected(customBook.isPiglinCurrency);
        this.destroyAnyBlock.setSelected(customBook.destroyAnyBlock);
        this.startingBook.setSelected(customBook.startingBook);
        this.hideNextArrowAtCategoryEnd.setSelected(customBook.hideNextArrowAtCategoryEnd);
        this.glow.setSelected(customBook.glow);
        ArrayList<TabEntry> arrayList2 = arrayList = customBook.creativeTabs == null ? new ArrayList<TabEntry>() : new ArrayList<TabEntry>(customBook.creativeTabs);
        arrayList.removeIf(tabEntry -> tabEntry == null);
        if (arrayList.isEmpty()) {
            arrayList.add(new TabEntry(this.app.getWorkspace(), customBook.creativeTab == null ? "TOOLS" : customBook.creativeTab));
        }
        this.creativeTabsField.setListElements(arrayList);
        String string = customBook.getEffectiveItemTexture();
        this.clearTextureSelection(this.itemTexture);
        if (!string.isBlank()) {
            try {
                this.itemTexture.setTexture(new TextureHolder(this.app.getWorkspace(), string));
            }
            catch (RuntimeException exception) {
                System.err.println("[CustomBookCreator] Ignoring an invalid or missing item texture: " + string);
                exception.printStackTrace();
            }
        }
        this.selectItemModel(customBook.renderType, customBook.customModelName);
        this.rootNode.removeAllChildren();
        for (CustomBook.BookCategory bookCategory : customBook.getBookCategories()) {
            CustomBook.BookCategory bookCategory2 = CustomBookGUI.copyCategory(bookCategory);
            DefaultMutableTreeNode defaultMutableTreeNode = new DefaultMutableTreeNode(bookCategory2);
            for (CustomBook.BookPage bookPage : bookCategory2.pages) {
                defaultMutableTreeNode.add(new DefaultMutableTreeNode(bookPage));
            }
            this.rootNode.add(defaultMutableTreeNode);
        }
        this.treeModel.reload();
        this.ensureDefaultStructure();
        this.syncCategoryPageListsFromTree();
        this.repairButtonTargets();
        this.refreshCategoryTargets();
        this.selectFirstPage();
    }

    private void selectItemModel(int n, String string) {
        Model.Type type = CustomBook.decodeModelType(n);
        String string2 = string == null || string.isBlank() ? "Normal" : string;
        this.reloadModelChoices();
        for (int i = 0; i < this.itemModel.getItemCount(); ++i) {
            Model model = (Model)this.itemModel.getItemAt(i);
            if (model.getType() != type || !model.getReadableName().equals(string2)) continue;
            this.itemModel.setSelectedIndex(i);
            return;
        }
        this.itemModel.setSelectedIndex(0);
    }

    private static CustomBook.BookCategory copyCategory(CustomBook.BookCategory bookCategory) {
        CustomBook.BookCategory bookCategory2 = new CustomBook.BookCategory(bookCategory.name == null ? "Category" : bookCategory.name);
        bookCategory2.id = bookCategory.id == null || bookCategory.id.isBlank() ? UUID.randomUUID().toString() : bookCategory.id;
        bookCategory2.pages.clear();
        if (bookCategory.pages != null) {
            for (CustomBook.BookPage bookPage : bookCategory.pages) {
                if (bookPage == null) {
                    continue;
                }
                bookPage.normalizeElements();
                CustomBook.BookPage bookPage2 = new CustomBook.BookPage(bookPage.title == null ? "Page" : bookPage.title, "");
                bookPage2.id = bookPage.id == null || bookPage.id.isBlank() ? UUID.randomUUID().toString() : bookPage.id;
                bookPage2.showTitle = bookPage.showTitle;
                bookPage2.content = bookPage.content == null ? "" : bookPage.content;
                bookPage2.elements.clear();
                for (CustomBook.BookElement bookElement : bookPage.elements) {
                    bookPage2.elements.add(CustomBookGUI.copyElement(bookElement));
                }
                if (bookPage2.elements.isEmpty()) {
                    bookPage2.elements.add(CustomBook.BookElement.text(bookPage2.content));
                }
                bookCategory2.pages.add(bookPage2);
            }
        }
        if (bookCategory2.pages.isEmpty()) {
            bookCategory2.pages.add(new CustomBook.BookPage("Page 1", ""));
        }
        return bookCategory2;
    }

    private static CustomBook.BookElement copyElement(CustomBook.BookElement bookElement) {
        CustomBook.BookElement bookElement2 = new CustomBook.BookElement();
        bookElement2.id = bookElement.id == null || bookElement.id.isBlank() ? UUID.randomUUID().toString() : bookElement.id;
        bookElement2.type = bookElement.type;
        bookElement2.x = bookElement.x;
        bookElement2.y = bookElement.y;
        bookElement2.width = bookElement.width;
        bookElement2.height = bookElement.height;
        bookElement2.content = bookElement.content;
        bookElement2.align = bookElement.align;
        bookElement2.mediaName = bookElement.mediaName;
        bookElement2.frames = bookElement.frames == null ? new ArrayList<String>() : new ArrayList<String>(bookElement.frames);
        bookElement2.frameDelay = bookElement.frameDelay;
        bookElement2.label = bookElement.label;
        bookElement2.targetCategoryId = bookElement.targetCategoryId;
        bookElement2.targetPageId = bookElement.targetPageId;
        bookElement2.buttonStyle = bookElement.buttonStyle;
        bookElement2.buttonBackgroundColor = bookElement.buttonBackgroundColor;
        bookElement2.buttonBorderColor = bookElement.buttonBorderColor;
        bookElement2.buttonTextColor = bookElement.buttonTextColor;
        bookElement2.buttonImageName = bookElement.buttonImageName;
        bookElement2.buttonImageType = bookElement.buttonImageType;
        bookElement2.buttonImageMode = bookElement.buttonImageMode;
        bookElement2.resizeBySides = bookElement.resizeBySides;
        bookElement2.resizeByCorners = bookElement.resizeByCorners;
        bookElement2.normalize();
        return bookElement2;
    }

    @Override
    public CustomBook getElementFromGUI() {
        this.syncCategoryPageListsFromTree();
        this.repairButtonTargets();
        CustomBook customBook = new CustomBook(this.modElement);
        customBook.name = CustomBookGUI.safeText(this.displayName, this.modElement.getName());
        customBook.bookTitle = CustomBookGUI.limit(CustomBookGUI.safeText(this.bookTitle, customBook.name), 32);
        customBook.author = CustomBookGUI.limit(CustomBookGUI.safeText(this.author, "Unknown"), 256);
        customBook.generation = this.safeSpinnerInt(this.generation, 0, 0, 3);
        customBook.rarity = CustomBookGUI.safeComboString(this.rarity, "COMMON");
        customBook.stackSize = this.safeSpinnerInt(this.stackSize, 1, 1, 64);
        customBook.enchantability = this.safeSpinnerInt(this.enchantability, 0, 0, 128000);
        customBook.immuneToFire = this.immuneToFire.isSelected();
        customBook.isPiglinCurrency = this.piglinCurrency.isSelected();
        customBook.destroyAnyBlock = this.destroyAnyBlock.isSelected();
        customBook.startingBook = this.startingBook.isSelected();
        customBook.hideNextArrowAtCategoryEnd = this.hideNextArrowAtCategoryEnd.isSelected();
        customBook.glow = this.glow.isSelected();
        customBook.itemTexture = "";
        try {
            if (this.itemTexture != null && this.itemTexture.hasTexture()) {
                customBook.itemTexture = this.itemTexture.getTextureHolder().getRawTextureName();
            }
        }
        catch (RuntimeException runtimeException) {
            System.err.println("[CustomBookCreator] Could not read selected item texture; using vanilla book texture.");
            runtimeException.printStackTrace();
        }
        customBook.texturePath = customBook.itemTexture.isBlank() ? "minecraft:item/written_book" : customBook.itemTexture;
        customBook.texture = new TextureHolder(this.modElement.getWorkspace(), customBook.itemTexture.isBlank() ? "minecraft:written_book" : customBook.itemTexture);
        customBook.renderType = 0;
        customBook.customModelName = "Normal";
        try {
            Object object2 = this.itemModel.getSelectedItem();
            if (object2 instanceof Model selectedModel && selectedModel.getType() != Model.Type.BUILTIN) {
                customBook.renderType = CustomBook.encodeModelType(selectedModel.getType());
                customBook.customModelName = selectedModel.getReadableName();
            }
        }
        catch (RuntimeException runtimeException) {
            System.err.println("[CustomBookCreator] Could not read selected model; falling back to Normal.");
            runtimeException.printStackTrace();
        }
        customBook.categories = new ArrayList<CustomBook.BookCategory>();
        for (int i = 0; i < this.rootNode.getChildCount(); ++i) {
            TreeNode categoryNode = this.rootNode.getChildAt(i);
            if (!(categoryNode instanceof DefaultMutableTreeNode categoryTreeNode)) {
                continue;
            }
            Object categoryValue = categoryTreeNode.getUserObject();
            if (!(categoryValue instanceof CustomBook.BookCategory bookCategory)) {
                continue;
            }
            CustomBook.BookCategory savedCategory = new CustomBook.BookCategory(
                    bookCategory.name == null || bookCategory.name.isBlank() ? "Category" : bookCategory.name);
            savedCategory.id = bookCategory.id == null || bookCategory.id.isBlank()
                    ? UUID.randomUUID().toString() : bookCategory.id;
            savedCategory.pages.clear();
            for (int j = 0; j < categoryTreeNode.getChildCount(); ++j) {
                TreeNode pageNode = categoryTreeNode.getChildAt(j);
                if (!(pageNode instanceof DefaultMutableTreeNode pageTreeNode)) {
                    continue;
                }
                Object pageValue = pageTreeNode.getUserObject();
                if (!(pageValue instanceof CustomBook.BookPage bookPage)) {
                    continue;
                }
                CustomBook.BookPage savedPage = new CustomBook.BookPage(
                        bookPage.title == null || bookPage.title.isBlank() ? "Page" : bookPage.title, "");
                savedPage.id = bookPage.id == null || bookPage.id.isBlank()
                        ? UUID.randomUUID().toString() : bookPage.id;
                savedPage.showTitle = bookPage.showTitle;
                savedPage.content = bookPage.content == null ? "" : bookPage.content;
                savedPage.elements.clear();
                if (bookPage.elements != null) {
                    for (CustomBook.BookElement element : bookPage.elements) {
                        if (element != null) {
                            savedPage.elements.add(CustomBookGUI.copyElement(element));
                        }
                    }
                }
                if (savedPage.elements.stream().noneMatch(
                        element -> element != null && "TEXT".equals(element.type))) {
                    savedPage.elements.add(0, CustomBook.BookElement.text(savedPage.content));
                }
                savedPage.content = CustomBookGUI.firstTextContent(savedPage);
                this.ensureNavigationElements(savedPage);
                savedPage.normalizeElements();
                savedCategory.pages.add(savedPage);
            }
            if (savedCategory.pages.isEmpty()) {
                CustomBook.BookPage bookPage = new CustomBook.BookPage("Page 1", "");
                this.ensureNavigationElements(bookPage);
                savedCategory.pages.add(bookPage);
            }
            customBook.categories.add(savedCategory);
        }
        if (customBook.categories.isEmpty()) {
            CustomBook.BookCategory bookCategory = new CustomBook.BookCategory("General");
            CustomBook.BookPage defaultPage = new CustomBook.BookPage("Page 1", "");
            this.ensureNavigationElements(defaultPage);
            bookCategory.pages.add(defaultPage);
            customBook.categories.add(bookCategory);
        }
        customBook.pages = new ArrayList<String>();
        customBook.creativeTabs = new ArrayList<TabEntry>();
        try {
            List list;
            List list2 = list = this.creativeTabsField == null ? List.of() : this.creativeTabsField.getListElements();
            if (list != null) {
                customBook.creativeTabs.addAll(list);
            }
        }
        catch (RuntimeException runtimeException) {
            System.err.println("[CustomBookCreator] Could not read creative tabs; using Tools & Utilities.");
            runtimeException.printStackTrace();
        }
        if (customBook.creativeTabs.isEmpty()) {
            customBook.creativeTabs.add(new TabEntry(this.modElement.getWorkspace(), "TOOLS"));
        }
        customBook.creativeTab = "TOOLS";
        return customBook;
    }

    private static String safeText(JTextField jTextField, String string) {
        String string2 = jTextField == null ? null : jTextField.getText();
        return string2 == null || string2.isBlank() ? string : string2.trim();
    }

    private static String safeComboString(JComboBox<?> jComboBox, String string) {
        Object object = jComboBox == null ? null : jComboBox.getSelectedItem();
        return object == null ? string : String.valueOf(object);
    }

    private int safeSpinnerInt(JSpinner jSpinner, int n, int n2, int n3) {
        return Math.max(n2, Math.min(n3, this.spinnerEditorInt(jSpinner, n)));
    }

    private static String limit(String string, int n) {
        if (string == null) {
            return "";
        }
        if (string.length() <= n) {
            return string;
        }
        int end = Math.max(0, n);
        if (end > 0 && Character.isHighSurrogate(string.charAt(end - 1))
                && Character.isLowSurrogate(string.charAt(end))) {
            --end;
        }
        return string.substring(0, end);
    }

    public URI contextURL() {
        return null;
    }

    private static BufferedImage decodePng(File file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
            }
            ImageReader reader = readers.next();
            try {
                if (!"png".equalsIgnoreCase(reader.getFormatName())) {
                    throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                }
                reader.setInput(input, true, true);
                CustomBookGUI.validateMediaDimensions(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                }
                return image;
            }
            finally {
                reader.dispose();
            }
        }
    }

    private static GifData decodeGif(File file) throws IOException {
        ArrayList<BufferedImage> arrayList = new ArrayList<BufferedImage>();
        ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);){
            if (imageInputStream == null) {
                throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
            }
            ImageReader imageReader;
            Iterator<ImageReader> iterator = ImageIO.getImageReaders(imageInputStream);
            ImageReader imageReader2 = imageReader = iterator.hasNext() ? iterator.next() : null;
            if (imageReader == null) {
                throw new IOException(CustomBookGUI.tr("error.gif_reader_unavailable"));
            }
            try {
                if (!"gif".equalsIgnoreCase(imageReader.getFormatName())) {
                    throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                }
                Object object;
                Object object2;
                imageReader.setInput(imageInputStream, false, false);
                int n = imageReader.getNumImages(true);
                if (n <= 0) {
                    throw new IOException(CustomBookGUI.tr("error.gif_no_frames"));
                }
                if (n > MAX_GIF_FRAMES) {
                    throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                }
                int n2 = 0;
                int n3 = 0;
                long decodedPixels = 0L;
                IIOMetadata iIOMetadata = imageReader.getStreamMetadata();
                if (iIOMetadata != null && (object2 = CustomBookGUI.findNode((Node)(object = iIOMetadata.getAsTree(iIOMetadata.getNativeMetadataFormatName())), "LogicalScreenDescriptor")) != null) {
                    n2 = CustomBookGUI.intAttr((Node)object2, "logicalScreenWidth", 0);
                    n3 = CustomBookGUI.intAttr((Node)object2, "logicalScreenHeight", 0);
                    if (n2 > 0 && n3 > 0) {
                        CustomBookGUI.validateMediaDimensions(n2, n3);
                        if ((long)n2 * (long)n3 * (long)n > MAX_DECODED_GIF_PIXELS) {
                            throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                        }
                    }
                }
                object = null;
                object2 = null;
                String string = "none";
                Rectangle rectangle = null;
                for (int i = 0; i < n; ++i) {
                    Graphics2D graphics2D;
                    String string2;
                    CustomBookGUI.validateMediaDimensions(imageReader.getWidth(i), imageReader.getHeight(i));
                    BufferedImage bufferedImage = imageReader.read(i);
                    IIOMetadata iIOMetadata2 = imageReader.getImageMetadata(i);
                    Node node = iIOMetadata2.getAsTree(iIOMetadata2.getNativeMetadataFormatName());
                    Node node2 = CustomBookGUI.findNode(node, "ImageDescriptor");
                    Node node3 = CustomBookGUI.findNode(node, "GraphicControlExtension");
                    int n4 = node2 == null ? 0 : CustomBookGUI.intAttr(node2, "imageLeftPosition", 0);
                    int n5 = node2 == null ? 0 : CustomBookGUI.intAttr(node2, "imageTopPosition", 0);
                    int n6 = node2 == null ? bufferedImage.getWidth() : CustomBookGUI.intAttr(node2, "imageWidth", bufferedImage.getWidth());
                    int n7 = node2 == null ? bufferedImage.getHeight() : CustomBookGUI.intAttr(node2, "imageHeight", bufferedImage.getHeight());
                    int n8 = node3 == null ? 10 : CustomBookGUI.intAttr(node3, "delayTime", 10);
                    String string3 = string2 = node3 == null ? "none" : CustomBookGUI.strAttr(node3, "disposalMethod", "none");
                    if (n2 <= 0) {
                        n2 = Math.max(bufferedImage.getWidth(), n4 + n6);
                    }
                    if (n3 <= 0) {
                        n3 = Math.max(bufferedImage.getHeight(), n5 + n7);
                    }
                    CustomBookGUI.validateMediaDimensions(n2, n3);
                    decodedPixels += (long)n2 * (long)n3;
                    if (decodedPixels > MAX_DECODED_GIF_PIXELS) {
                        throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
                    }
                    if (object == null) {
                        object = new BufferedImage(n2, n3, 2);
                    }
                    if (i > 0 && rectangle != null) {
                        if ("restoreToBackgroundColor".equals(string)) {
                            graphics2D = ((BufferedImage)object).createGraphics();
                            graphics2D.setComposite(AlphaComposite.Clear);
                            graphics2D.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                            graphics2D.dispose();
                        } else if ("restoreToPrevious".equals(string) && object2 != null) {
                            object = CustomBookGUI.deepCopy((BufferedImage)object2);
                        }
                    }
                    if ("restoreToPrevious".equals(string2)) {
                        object2 = CustomBookGUI.deepCopy((BufferedImage)object);
                    }
                    graphics2D = ((BufferedImage)object).createGraphics();
                    graphics2D.setComposite(AlphaComposite.SrcOver);
                    graphics2D.drawImage((Image)bufferedImage, n4, n5, null);
                    graphics2D.dispose();
                    arrayList.add(CustomBookGUI.deepCopy((BufferedImage)object));
                    arrayList2.add(Math.max(20, n8 * 10));
                    string = string2;
                    rectangle = new Rectangle(n4, n5, n6, n7);
                }
                GifData gifData = new GifData(arrayList, arrayList2, n2, n3);
                imageReader.dispose();
                return gifData;
            }
            catch (Throwable throwable) {
                imageReader.dispose();
                throw throwable;
            }
        }
    }

    private static void validateMediaDimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0 || width > MAX_MEDIA_DIMENSION || height > MAX_MEDIA_DIMENSION
                || (long)width * (long)height > MAX_DECODED_GIF_PIXELS) {
            throw new IOException(CustomBookGUI.tr("error.unrecognized_image"));
        }
    }

    private static BufferedImage deepCopy(BufferedImage bufferedImage) {
        BufferedImage bufferedImage2 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage((Image)bufferedImage, 0, 0, null);
        graphics2D.dispose();
        return bufferedImage2;
    }

    private static Node findNode(Node node, String string) {
        if (node == null) {
            return null;
        }
        if (string.equals(node.getNodeName())) {
            return node;
        }
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node2 = CustomBookGUI.findNode(nodeList.item(i), string);
            if (node2 == null) continue;
            return node2;
        }
        return null;
    }

    private static int intAttr(Node node, String string, int n) {
        try {
            NamedNodeMap namedNodeMap = node.getAttributes();
            Node node2 = namedNodeMap == null ? null : namedNodeMap.getNamedItem(string);
            return node2 == null ? n : Integer.parseInt(node2.getNodeValue());
        }
        catch (Exception exception) {
            return n;
        }
    }

    private static String strAttr(Node node, String string, String string2) {
        NamedNodeMap namedNodeMap = node.getAttributes();
        Node node2 = namedNodeMap == null ? null : namedNodeMap.getNamedItem(string);
        return node2 == null ? string2 : node2.getNodeValue();
    }

    private final class PagePreviewCanvas
    extends JPanel {
        private static final int LOGICAL_W = 256;
        private static final int LOGICAL_H = 320;
        private static final int HANDLE_RADIUS = 5;
        private CustomBook.BookPage page;
        private CustomBook.BookElement selected;
        private int dragOffsetX;
        private int dragOffsetY;
        private boolean dragging;
        private boolean resizing;
        private ResizeHandle resizeHandle = ResizeHandle.NONE;

        PagePreviewCanvas() {
            this.setPreferredSize(new Dimension(420, 520));
            this.setMinimumSize(new Dimension(300, 380));
            this.setBackground(new Color(45, 45, 45));
            this.addMouseListener(new MouseAdapter(){

                @Override
                public void mousePressed(MouseEvent mouseEvent) {
                    ResizeHandle resizeHandle;
                    if (PagePreviewCanvas.this.page == null || !SwingUtilities.isLeftMouseButton(mouseEvent)) {
                        return;
                    }
                    Point point = PagePreviewCanvas.this.toLogical(mouseEvent.getPoint());
                    ResizeHandle resizeHandle2 = resizeHandle = PagePreviewCanvas.this.selected == null ? ResizeHandle.NONE : PagePreviewCanvas.this.handleAt(point.x, point.y, PagePreviewCanvas.this.selected);
                    if (resizeHandle != ResizeHandle.NONE) {
                        PagePreviewCanvas.this.resizing = true;
                        PagePreviewCanvas.this.dragging = false;
                        PagePreviewCanvas.this.resizeHandle = resizeHandle;
                        return;
                    }
                    CustomBook.BookElement bookElement = PagePreviewCanvas.this.hitTest(point.x, point.y);
                    CustomBookGUI.this.selectElement(bookElement);
                    if (bookElement != null) {
                        resizeHandle = PagePreviewCanvas.this.handleAt(point.x, point.y, bookElement);
                        if (resizeHandle != ResizeHandle.NONE) {
                            PagePreviewCanvas.this.resizing = true;
                            PagePreviewCanvas.this.dragging = false;
                            PagePreviewCanvas.this.resizeHandle = resizeHandle;
                        } else {
                            PagePreviewCanvas.this.dragOffsetX = point.x - bookElement.x;
                            PagePreviewCanvas.this.dragOffsetY = point.y - bookElement.y;
                            PagePreviewCanvas.this.dragging = true;
                            PagePreviewCanvas.this.resizing = false;
                        }
                    }
                    PagePreviewCanvas.this.updateCursorFor(point);
                }

                @Override
                public void mouseReleased(MouseEvent mouseEvent) {
                    PagePreviewCanvas.this.dragging = false;
                    PagePreviewCanvas.this.resizing = false;
                    PagePreviewCanvas.this.resizeHandle = ResizeHandle.NONE;
                    PagePreviewCanvas.this.updateCursorFor(PagePreviewCanvas.this.toLogical(mouseEvent.getPoint()));
                }

                @Override
                public void mouseExited(MouseEvent mouseEvent) {
                    if (!PagePreviewCanvas.this.dragging && !PagePreviewCanvas.this.resizing) {
                        PagePreviewCanvas.this.setCursor(Cursor.getDefaultCursor());
                    }
                }
            });
            this.addMouseMotionListener(new MouseMotionAdapter(){

                @Override
                public void mouseDragged(MouseEvent mouseEvent) {
                    if (PagePreviewCanvas.this.selected == null) {
                        return;
                    }
                    Point point = PagePreviewCanvas.this.toLogical(mouseEvent.getPoint());
                    if (PagePreviewCanvas.this.resizing) {
                        PagePreviewCanvas.this.resizeSelected(point.x, point.y);
                    } else if (PagePreviewCanvas.this.dragging) {
                        PagePreviewCanvas.this.moveSelected(point.x, point.y);
                    }
                    CustomBookGUI.this.syncLegacyPageContent();
                    CustomBookGUI.this.syncGeometryControls();
                    CustomBookGUI.this.updateSelectionInfo();
                    PagePreviewCanvas.this.repaint();
                }

                @Override
                public void mouseMoved(MouseEvent mouseEvent) {
                    PagePreviewCanvas.this.updateCursorFor(PagePreviewCanvas.this.toLogical(mouseEvent.getPoint()));
                }
            });
        }

        void setPage(CustomBook.BookPage bookPage) {
            this.page = bookPage;
            this.selected = null;
            this.dragging = false;
            this.resizing = false;
            this.resizeHandle = ResizeHandle.NONE;
            this.repaint();
        }

        private double scale() {
            return Math.max(0.05, Math.min(((double)this.getWidth() - 24.0) / 256.0, ((double)this.getHeight() - 24.0) / 320.0));
        }

        private int originX() {
            return (int)Math.round(((double)this.getWidth() - 256.0 * this.scale()) / 2.0);
        }

        private int originY() {
            return (int)Math.round(((double)this.getHeight() - 320.0 * this.scale()) / 2.0);
        }

        private Point toLogical(Point point) {
            double d = this.scale();
            return new Point((int)Math.floor((double)(point.x - this.originX()) / d), (int)Math.floor((double)(point.y - this.originY()) / d));
        }

        private int gridStep() {
            return Math.max(2, (Integer)CustomBookGUI.this.gridSize.getValue());
        }

        private int snap(int n) {
            if (!CustomBookGUI.this.snapToGrid.isSelected()) {
                return n;
            }
            int n2 = this.gridStep();
            return Math.round((float)n / (float)n2) * n2;
        }

        private void moveSelected(int n, int n2) {
            int n3 = this.snap(n - this.dragOffsetX);
            int n4 = this.snap(n2 - this.dragOffsetY);
            this.selected.x = Math.max(0, Math.min(256 - this.selected.width, n3));
            this.selected.y = Math.max(0, Math.min(320 - this.selected.height, n4));
        }

        private void resizeSelected(int n, int n2) {
            int n3 = "TEXT".equals(this.selected.type) ? 24 : 12;
            int n4 = "TEXT".equals(this.selected.type) ? 14 : 10;
            int n5 = this.selected.x;
            int n6 = this.selected.y;
            int n7 = this.selected.x + this.selected.width;
            int n8 = this.selected.y + this.selected.height;
            int n9 = Math.max(0, Math.min(256, this.snap(n)));
            int n10 = Math.max(0, Math.min(320, this.snap(n2)));
            switch (this.resizeHandle.ordinal()) {
                case 1: {
                    n5 = Math.min(n9, n7 - n3);
                    n6 = Math.min(n10, n8 - n4);
                    break;
                }
                case 3: {
                    n7 = Math.max(n9, n5 + n3);
                    n6 = Math.min(n10, n8 - n4);
                    break;
                }
                case 7: {
                    n5 = Math.min(n9, n7 - n3);
                    n8 = Math.max(n10, n6 + n4);
                    break;
                }
                case 5: {
                    n7 = Math.max(n9, n5 + n3);
                    n8 = Math.max(n10, n6 + n4);
                    break;
                }
                case 2: {
                    n6 = Math.min(n10, n8 - n4);
                    break;
                }
                case 6: {
                    n8 = Math.max(n10, n6 + n4);
                    break;
                }
                case 8: {
                    n5 = Math.min(n9, n7 - n3);
                    break;
                }
                case 4: {
                    n7 = Math.max(n9, n5 + n3);
                    break;
                }
                default: {
                    return;
                }
            }
            n5 = Math.max(0, n5);
            n6 = Math.max(0, n6);
            n7 = Math.min(256, n7);
            n8 = Math.min(320, n8);
            if (n7 - n5 < n3) {
                if (this.resizeHandle == ResizeHandle.NW || this.resizeHandle == ResizeHandle.SW || this.resizeHandle == ResizeHandle.W) {
                    n5 = Math.max(0, n7 - n3);
                } else {
                    n7 = Math.min(256, n5 + n3);
                }
            }
            if (n8 - n6 < n4) {
                if (this.resizeHandle == ResizeHandle.NW || this.resizeHandle == ResizeHandle.NE || this.resizeHandle == ResizeHandle.N) {
                    n6 = Math.max(0, n8 - n4);
                } else {
                    n8 = Math.min(320, n6 + n4);
                }
            }
            this.selected.x = n5;
            this.selected.y = n6;
            this.selected.width = Math.max(n3, n7 - n5);
            this.selected.height = Math.max(n4, n8 - n6);
            this.selected.normalize();
        }

        private CustomBook.BookElement hitTest(int n, int n2) {
            if (this.page == null || this.page.elements == null) {
                return null;
            }
            for (int i = this.page.elements.size() - 1; i >= 0; --i) {
                CustomBook.BookElement bookElement = this.page.elements.get(i);
                if (bookElement == null || n < bookElement.x || n >= bookElement.x + bookElement.width || n2 < bookElement.y || n2 >= bookElement.y + bookElement.height) continue;
                return bookElement;
            }
            return null;
        }

        private ResizeHandle handleAt(int n, int n2, CustomBook.BookElement bookElement) {
            boolean bl;
            if (bookElement == null) {
                return ResizeHandle.NONE;
            }
            boolean bl2 = "IMAGE".equals(bookElement.type) || "GIF".equals(bookElement.type);
            boolean bl3 = !bl2 || bookElement.resizeByCorners;
            boolean bl4 = bl = !bl2 || bookElement.resizeBySides;
            if (bl3) {
                if (this.near(n, n2, bookElement.x, bookElement.y)) {
                    return ResizeHandle.NW;
                }
                if (this.near(n, n2, bookElement.x + bookElement.width, bookElement.y)) {
                    return ResizeHandle.NE;
                }
                if (this.near(n, n2, bookElement.x, bookElement.y + bookElement.height)) {
                    return ResizeHandle.SW;
                }
                if (this.near(n, n2, bookElement.x + bookElement.width, bookElement.y + bookElement.height)) {
                    return ResizeHandle.SE;
                }
            }
            if (bl) {
                if (this.near(n, n2, bookElement.x + bookElement.width / 2, bookElement.y)) {
                    return ResizeHandle.N;
                }
                if (this.near(n, n2, bookElement.x + bookElement.width / 2, bookElement.y + bookElement.height)) {
                    return ResizeHandle.S;
                }
                if (this.near(n, n2, bookElement.x, bookElement.y + bookElement.height / 2)) {
                    return ResizeHandle.W;
                }
                if (this.near(n, n2, bookElement.x + bookElement.width, bookElement.y + bookElement.height / 2)) {
                    return ResizeHandle.E;
                }
            }
            return ResizeHandle.NONE;
        }

        private boolean near(int n, int n2, int n3, int n4) {
            return Math.abs(n - n3) <= 5 && Math.abs(n2 - n4) <= 5;
        }

        private void updateCursorFor(Point point) {
            ResizeHandle resizeHandle;
            if (this.resizing) {
                this.setCursor(this.cursorFor(this.resizeHandle));
                return;
            }
            ResizeHandle resizeHandle2 = resizeHandle = this.selected == null ? ResizeHandle.NONE : this.handleAt(point.x, point.y, this.selected);
            if (resizeHandle != ResizeHandle.NONE) {
                this.setCursor(this.cursorFor(resizeHandle));
            } else if (this.hitTest(point.x, point.y) != null) {
                this.setCursor(Cursor.getPredefinedCursor(13));
            } else {
                this.setCursor(Cursor.getDefaultCursor());
            }
        }

        private Cursor cursorFor(ResizeHandle resizeHandle) {
            return switch (resizeHandle.ordinal()) {
                case 1, 5 -> Cursor.getPredefinedCursor(6);
                case 3, 7 -> Cursor.getPredefinedCursor(7);
                case 2, 6 -> Cursor.getPredefinedCursor(8);
                case 4, 8 -> Cursor.getPredefinedCursor(11);
                default -> Cursor.getDefaultCursor();
            };
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D)graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double d = this.scale();
            graphics2D.translate(this.originX(), this.originY());
            graphics2D.scale(d, d);
            graphics2D.setColor(new Color(68, 48, 31));
            graphics2D.fillRoundRect(-3, -3, 262, 326, 6, 6);
            graphics2D.setColor(new Color(255, 246, 218));
            graphics2D.fillRect(0, 0, 256, 320);
            if (CustomBookGUI.this.showGrid.isSelected()) {
                int n;
                int n2 = CustomBookGUI.this.snapToGrid.isSelected() ? Math.max(4, this.gridStep()) : 16;
                graphics2D.setColor(CustomBookGUI.this.snapToGrid.isSelected() ? new Color(216, 202, 170) : new Color(232, 220, 190));
                for (n = n2; n < 256; n += n2) {
                    graphics2D.drawLine(n, 0, n, 320);
                }
                for (n = n2; n < 320; n += n2) {
                    graphics2D.drawLine(0, n, 256, n);
                }
            }
            graphics2D.setColor(new Color(55, 42, 30));
            graphics2D.setFont(new Font("SansSerif", 1, 12));
            if (this.page == null || this.page.showTitle) {
                String string = this.page == null || this.page.title == null ? "Page" : this.page.title;
                FontMetrics fontMetrics = graphics2D.getFontMetrics();
                graphics2D.drawString(string, Math.max(6, (256 - fontMetrics.stringWidth(string)) / 2), 25);
                graphics2D.setColor(new Color(132, 110, 82));
                graphics2D.drawLine(14, 33, 242, 33);
            }
            if (this.page != null) {
                this.page.normalizeElements();
                for (CustomBook.BookElement bookElement : this.page.elements) {
                    if (bookElement == null) continue;
                    this.paintElement(graphics2D, bookElement);
                }
            }
            graphics2D.dispose();
        }

        private void paintElement(Graphics2D graphics2D, CustomBook.BookElement bookElement) {
            bookElement.normalize();
            Shape shape = graphics2D.getClip();
            graphics2D.clipRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
            switch (bookElement.type) {
                case "TEXT": {
                    this.paintTextElement(graphics2D, bookElement);
                    break;
                }
                case "IMAGE": {
                    this.paintImageElement(graphics2D, bookElement.mediaName, bookElement);
                    break;
                }
                case "GIF": {
                    this.paintImageElement(graphics2D, bookElement.frames.isEmpty() ? "" : bookElement.frames.get(0), bookElement);
                    break;
                }
                case "BUTTON": {
                    this.paintButtonElement(graphics2D, bookElement);
                    break;
                }
                case "NAV_PREV": 
                case "NAV_NEXT": {
                    this.paintNavigationElement(graphics2D, bookElement);
                }
            }
            graphics2D.setClip(shape);
            if (bookElement == this.selected) {
                boolean bl;
                graphics2D.setColor(new Color(45, 125, 225));
                graphics2D.setStroke(new BasicStroke(1.5f));
                graphics2D.drawRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                boolean bl2 = bl = "IMAGE".equals(bookElement.type) || "GIF".equals(bookElement.type);
                if (!bl || bookElement.resizeByCorners) {
                    this.paintHandle(graphics2D, bookElement.x, bookElement.y);
                    this.paintHandle(graphics2D, bookElement.x + bookElement.width, bookElement.y);
                    this.paintHandle(graphics2D, bookElement.x, bookElement.y + bookElement.height);
                    this.paintHandle(graphics2D, bookElement.x + bookElement.width, bookElement.y + bookElement.height);
                }
                if (!bl || bookElement.resizeBySides) {
                    this.paintHandle(graphics2D, bookElement.x + bookElement.width / 2, bookElement.y);
                    this.paintHandle(graphics2D, bookElement.x + bookElement.width / 2, bookElement.y + bookElement.height);
                    this.paintHandle(graphics2D, bookElement.x, bookElement.y + bookElement.height / 2);
                    this.paintHandle(graphics2D, bookElement.x + bookElement.width, bookElement.y + bookElement.height / 2);
                }
            } else {
                graphics2D.setColor(new Color(125, 112, 91, 90));
                graphics2D.drawRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
            }
        }

        private void paintHandle(Graphics2D graphics2D, int n, int n2) {
            graphics2D.fillRect(n - 3, n2 - 3, 7, 7);
            graphics2D.setColor(new Color(235, 246, 255));
            graphics2D.drawRect(n - 3, n2 - 3, 7, 7);
            graphics2D.setColor(new Color(45, 125, 225));
        }

        private void paintTextElement(Graphics2D graphics2D, CustomBook.BookElement bookElement) {
            String string = switch (bookElement.align) {
                case "CENTER" -> "center";
                case "RIGHT" -> "right";
                default -> "left";
            };
            Object object = new JEditorPane("text/html", "<html><body style='margin:0;padding:1px;background:transparent;color:#241b13;font-family:sans-serif;font-size:12px;text-align:" + string + "'>" + CustomBookGUI.this.markupToHtml(bookElement.content) + "</body></html>");
            ((JTextComponent)object).setEditable(false);
            ((JComponent)object).setOpaque(false);
            ((JComponent)object).setBorder(null);
            ((Component)object).setSize(bookElement.width, bookElement.height);
            Graphics2D graphics2D2 = (Graphics2D)graphics2D.create(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
            ((JComponent)object).paint(graphics2D2);
            graphics2D2.dispose();
        }

        private void paintImageElement(Graphics2D graphics2D, String string, CustomBook.BookElement bookElement) {
            BufferedImage bufferedImage = CustomBookGUI.this.loadScreenTexture(string);
            if (bufferedImage != null) {
                graphics2D.drawImage(bufferedImage, bookElement.x, bookElement.y, bookElement.width, bookElement.height, null);
            } else {
                graphics2D.setColor(new Color(205, 190, 164));
                graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                graphics2D.setColor(new Color(95, 80, 65));
                graphics2D.drawString(CustomBookGUI.tr("element.media"), bookElement.x + 5, bookElement.y + 14);
            }
        }

        private void paintButtonElement(Graphics2D graphics2D, CustomBook.BookElement bookElement) {
            boolean bl;
            bookElement.normalize();
            int n = Color.decode(CustomBookGUI.safeHex(bookElement.buttonBackgroundColor, "#8A6846")).getRGB();
            int n2 = Color.decode(CustomBookGUI.safeHex(bookElement.buttonBorderColor, "#6D5237")).getRGB();
            int n3 = Color.decode(CustomBookGUI.safeHex(bookElement.buttonTextColor, "#FFEDC5")).getRGB();
            boolean bl2 = bookElement.buttonImageName != null && !bookElement.buttonImageName.isBlank();
            Image image = bl2 ? CustomBookGUI.this.loadWorkspaceTexture(bookElement.buttonImageName, bookElement.buttonImageType) : null;
            boolean bl3 = bl = image != null && ("BACKGROUND".equals(bookElement.buttonImageMode) || "IMAGE".equals(bookElement.buttonStyle));
            if (bl) {
                graphics2D.drawImage(image, bookElement.x, bookElement.y, bookElement.width, bookElement.height, null);
            }
            switch (bookElement.buttonStyle) {
                case "FLAT": {
                    if (!bl) {
                        graphics2D.setColor(new Color(n, true));
                        graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                    }
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.drawRect(bookElement.x, bookElement.y, Math.max(1, bookElement.width - 1), Math.max(1, bookElement.height - 1));
                    break;
                }
                case "OUTLINE": {
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.setStroke(new BasicStroke(2.0f));
                    graphics2D.drawRect(bookElement.x + 1, bookElement.y + 1, Math.max(1, bookElement.width - 3), Math.max(1, bookElement.height - 3));
                    break;
                }
                case "TRANSPARENT": {
                    break;
                }
                case "IMAGE": {
                    if (!bl) {
                        graphics2D.setColor(new Color(n, true));
                        graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                    }
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.drawRect(bookElement.x, bookElement.y, Math.max(1, bookElement.width - 1), Math.max(1, bookElement.height - 1));
                    break;
                }
                default: {
                    if (!bl) {
                        graphics2D.setColor(new Color(n2, true));
                        graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                        graphics2D.setColor(new Color(n, true));
                        graphics2D.fillRect(bookElement.x + 2, bookElement.y + 2, Math.max(1, bookElement.width - 4), Math.max(1, bookElement.height - 4));
                        break;
                    }
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.drawRect(bookElement.x, bookElement.y, Math.max(1, bookElement.width - 1), Math.max(1, bookElement.height - 1));
                }
            }
            int n4 = bookElement.x + 4;
            int n5 = bookElement.x + bookElement.width - 4;
            if (image != null && "ICON_LEFT".equals(bookElement.buttonImageMode)) {
                int n6 = Math.max(4, Math.min(bookElement.height - 6, Math.min(24, bookElement.width / 3)));
                int n7 = bookElement.y + Math.max(2, (bookElement.height - n6) / 2);
                graphics2D.drawImage(image, bookElement.x + 4, n7, n6, n6, null);
                n4 += n6 + 4;
            }
            graphics2D.setColor(new Color(n3, true));
            graphics2D.setFont(new Font("SansSerif", 1, Math.max(8, Math.min(16, bookElement.height / 2))));
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            String string = bookElement.label == null ? CustomBookGUI.tr("default.button") : bookElement.label;
            int n8 = Math.max(1, n5 - n4);
            while (string.length() > 1 && fontMetrics.stringWidth(string) > n8) {
                string = string.substring(0, string.length() - 1);
            }
            int n9 = n4 + Math.max(0, (n8 - fontMetrics.stringWidth(string)) / 2);
            int n10 = bookElement.y + Math.max(fontMetrics.getAscent(), (bookElement.height + fontMetrics.getAscent() - fontMetrics.getDescent()) / 2);
            graphics2D.drawString(string, n9, n10);
        }

        private void paintNavigationElement(Graphics2D graphics2D, CustomBook.BookElement bookElement) {
            Image image;
            int n = Color.decode(CustomBookGUI.safeHex(bookElement.buttonBackgroundColor, "#FFF4D6")).getRGB();
            int n2 = Color.decode(CustomBookGUI.safeHex(bookElement.buttonBorderColor, "#6D5237")).getRGB();
            int n3 = Color.decode(CustomBookGUI.safeHex(bookElement.buttonTextColor, "#6A5842")).getRGB();
            Image image2 = image = bookElement.buttonImageName == null || bookElement.buttonImageName.isBlank() ? null : CustomBookGUI.this.loadWorkspaceTexture(bookElement.buttonImageName, bookElement.buttonImageType);
            if ("IMAGE".equals(bookElement.buttonStyle) && image != null) {
                graphics2D.drawImage(image, bookElement.x, bookElement.y, bookElement.width, bookElement.height, null);
                return;
            }
            switch (bookElement.buttonStyle) {
                case "CLASSIC": {
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                    graphics2D.setColor(new Color(n, true));
                    graphics2D.fillRect(bookElement.x + 2, bookElement.y + 2, Math.max(1, bookElement.width - 4), Math.max(1, bookElement.height - 4));
                    break;
                }
                case "FLAT": {
                    graphics2D.setColor(new Color(n, true));
                    graphics2D.fillRect(bookElement.x, bookElement.y, bookElement.width, bookElement.height);
                    break;
                }
                case "OUTLINE": {
                    graphics2D.setColor(new Color(n2, true));
                    graphics2D.drawRect(bookElement.x, bookElement.y, Math.max(1, bookElement.width - 1), Math.max(1, bookElement.height - 1));
                    break;
                }
            }
            boolean bl = "NAV_NEXT".equals(bookElement.type);
            String string = CustomBookGUI.alignToNavGlyph(bookElement.align);
            graphics2D.setColor(new Color(n3, true));
            int n4 = bookElement.x + bookElement.width / 2;
            int n5 = bookElement.y + bookElement.height / 2;
            int n6 = Math.max(3, Math.min(bookElement.width, bookElement.height) / 3);
            if ("TRIANGLE".equals(string)) {
                Polygon polygon = bl ? new Polygon(new int[]{n4 - n6, n4 - n6, n4 + n6}, new int[]{n5 - n6, n5 + n6, n5}, 3) : new Polygon(new int[]{n4 + n6, n4 + n6, n4 - n6}, new int[]{n5 - n6, n5 + n6, n5}, 3);
                graphics2D.fillPolygon(polygon);
            } else {
                graphics2D.setStroke(new BasicStroke(Math.max(1.5f, (float)n6 / 3.0f), 1, 1));
                int n7 = bl ? -1 : 1;
                graphics2D.drawLine(n4 + n7 * n6 / 2, n5 - n6, n4 - n7 * n6 / 2, n5);
                graphics2D.drawLine(n4 - n7 * n6 / 2, n5, n4 + n7 * n6 / 2, n5 + n6);
                if ("DOUBLE".equals(string)) {
                    int n8 = Math.max(3, n6 / 2);
                    graphics2D.drawLine(n4 + n7 * n6 / 2 + n7 * n8, n5 - n6, n4 - n7 * n6 / 2 + n7 * n8, n5);
                    graphics2D.drawLine(n4 - n7 * n6 / 2 + n7 * n8, n5, n4 + n7 * n6 / 2 + n7 * n8, n5 + n6);
                }
            }
        }

        private static enum ResizeHandle {
            NONE,
            NW,
            N,
            NE,
            E,
            SE,
            S,
            SW,
            W;

        }
    }

    private final class BookTreeTransferHandler extends TransferHandler {
        private final DataFlavor nodeFlavor = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName(),
                "Custom Book tree node");
        private DefaultMutableTreeNode draggedNode;

        @Override
        protected Transferable createTransferable(JComponent component) {
            if (component != CustomBookGUI.this.bookTree) {
                return null;
            }
            DefaultMutableTreeNode node = CustomBookGUI.this.selectedNode();
            if (node == null || node == CustomBookGUI.this.rootNode || node.getParent() == null) {
                return null;
            }
            this.draggedNode = node;
            return new TreeNodeTransferable(node, this.nodeFlavor);
        }

        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || support.getComponent() != CustomBookGUI.this.bookTree
                    || !support.isDataFlavorSupported(this.nodeFlavor) || this.draggedNode == null
                    || this.draggedNode == CustomBookGUI.this.rootNode
                    || this.draggedNode.getRoot() != CustomBookGUI.this.rootNode) {
                return false;
            }
            support.setDropAction(MOVE);
            support.setShowDropLocation(true);
            try {
                TreeDropDestination destination = this.destinationFor(support, this.draggedNode);
                if (destination == null || destination.parent == null || this.draggedNode == destination.parent) {
                    return false;
                }
                return true;
            }
            catch (RuntimeException exception) {
                return false;
            }
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!this.canImport(support)) {
                return false;
            }
            DefaultMutableTreeNode source = this.draggedNode;
            DefaultMutableTreeNode oldParent = source == null ? null : (DefaultMutableTreeNode)source.getParent();
            DefaultMutableTreeNode replacementNode = null;
            int oldIndex = oldParent == null || source == null ? -1 : oldParent.getIndex(source);
            try {
                TreeDropDestination destination = this.destinationFor(support, source);
                if (destination == null || oldParent == null || oldIndex < 0) {
                    return false;
                }
                int insertionIndex = destination.index;
                if (oldParent == destination.parent && oldIndex < insertionIndex) {
                    insertionIndex--;
                }
                insertionIndex = Math.max(0, Math.min(insertionIndex, destination.parent.getChildCount()));

                CustomBookGUI.this.treeModel.removeNodeFromParent(source);
                if (oldParent != destination.parent && oldParent.getChildCount() == 0
                        && source.getUserObject() instanceof CustomBook.BookPage) {
                    CustomBook.BookPage replacementPage = new CustomBook.BookPage(CustomBookGUI.tr("default.page", 1), "");
                    CustomBookGUI.this.ensureNavigationElements(replacementPage);
                    replacementNode = new DefaultMutableTreeNode(replacementPage);
                    CustomBookGUI.this.treeModel.insertNodeInto(replacementNode, oldParent, 0);
                }
                CustomBookGUI.this.treeModel.insertNodeInto(source, destination.parent, insertionIndex);
                if (oldParent != destination.parent
                        && source.getUserObject() instanceof CustomBook.BookPage movedPage
                        && destination.parent.getUserObject() instanceof CustomBook.BookCategory destinationCategory) {
                    CustomBookGUI.this.updateMovedPageButtonTargets(movedPage.id, destinationCategory.id);
                }
                CustomBookGUI.this.syncCategoryPageListsFromTree();
                CustomBookGUI.this.repairButtonTargets();
                CustomBookGUI.this.refreshCategoryTargets();

                TreePath parentPath = new TreePath(destination.parent.getPath());
                CustomBookGUI.this.bookTree.expandPath(parentPath);
                CustomBookGUI.this.bookTree.setSelectionPath(new TreePath(source.getPath()));
                CustomBookGUI.this.bookTree.scrollPathToVisible(new TreePath(source.getPath()));
                return true;
            }
            catch (RuntimeException exception) {
                try {
                    if (source != null && source.getParent() instanceof DefaultMutableTreeNode currentParent) {
                        CustomBookGUI.this.treeModel.removeNodeFromParent(source);
                    }
                    if (replacementNode != null && replacementNode.getParent() != null) {
                        CustomBookGUI.this.treeModel.removeNodeFromParent(replacementNode);
                    }
                    if (source != null && oldParent != null && source.getParent() == null) {
                        CustomBookGUI.this.treeModel.insertNodeInto(source, oldParent,
                                Math.max(0, Math.min(oldIndex, oldParent.getChildCount())));
                    }
                    CustomBookGUI.this.syncCategoryPageListsFromTree();
                    CustomBookGUI.this.repairButtonTargets();
                    CustomBookGUI.this.refreshCategoryTargets();
                }
                catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                System.err.println("[CustomBookCreator] Book tree move failed and was rolled back.");
                exception.printStackTrace();
                return false;
            }
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            this.draggedNode = null;
        }

        private TreeDropDestination destinationFor(TransferSupport support, DefaultMutableTreeNode source) {
            if (!(support.getDropLocation() instanceof JTree.DropLocation location)) {
                return null;
            }
            TreePath path = location.getPath();
            if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode target)
                    || target.getRoot() != CustomBookGUI.this.rootNode
                    || !path.equals(new TreePath(target.getPath()))) {
                return null;
            }
            int childIndex = location.getChildIndex();

            if (source.getUserObject() instanceof CustomBook.BookCategory) {
                if (target == CustomBookGUI.this.rootNode) {
                    return new TreeDropDestination(target, normalizedIndex(childIndex, target.getChildCount()));
                }
                if (target.getUserObject() instanceof CustomBook.BookCategory) {
                    return new TreeDropDestination(CustomBookGUI.this.rootNode,
                            CustomBookGUI.this.rootNode.getIndex(target) + (this.isLowerHalf(location, target) ? 1 : 0));
                }
                if (target.getUserObject() instanceof CustomBook.BookPage
                        && target.getParent() instanceof DefaultMutableTreeNode targetCategory) {
                    return new TreeDropDestination(CustomBookGUI.this.rootNode,
                            CustomBookGUI.this.rootNode.getIndex(targetCategory) + (this.isLowerHalf(location, target) ? 1 : 0));
                }
                return null;
            }

            if (!(source.getUserObject() instanceof CustomBook.BookPage)) {
                return null;
            }
            if (target == CustomBookGUI.this.rootNode) {
                int categoryCount = target.getChildCount();
                if (categoryCount == 0) {
                    return null;
                }
                int categoryIndex = normalizedIndex(childIndex, categoryCount);
                if (categoryIndex <= 0) {
                    DefaultMutableTreeNode firstCategory = (DefaultMutableTreeNode)target.getChildAt(0);
                    return new TreeDropDestination(firstCategory, 0);
                }
                if (categoryIndex >= categoryCount) {
                    DefaultMutableTreeNode lastCategory = (DefaultMutableTreeNode)target.getChildAt(categoryCount - 1);
                    return new TreeDropDestination(lastCategory, lastCategory.getChildCount());
                }
                DefaultMutableTreeNode nextCategory = (DefaultMutableTreeNode)target.getChildAt(categoryIndex);
                return new TreeDropDestination(nextCategory, 0);
            }
            if (target.getUserObject() instanceof CustomBook.BookCategory) {
                return new TreeDropDestination(target, normalizedIndex(childIndex, target.getChildCount()));
            }
            if (target.getUserObject() instanceof CustomBook.BookPage
                    && target.getParent() instanceof DefaultMutableTreeNode targetCategory) {
                return new TreeDropDestination(targetCategory,
                        targetCategory.getIndex(target) + (this.isLowerHalf(location, target) ? 1 : 0));
            }
            return null;
        }

        private boolean isLowerHalf(JTree.DropLocation location, DefaultMutableTreeNode target) {
            Rectangle bounds = CustomBookGUI.this.bookTree.getPathBounds(new TreePath(target.getPath()));
            return bounds != null && location.getDropPoint().y >= bounds.y + bounds.height / 2;
        }

        private int normalizedIndex(int requestedIndex, int childCount) {
            return requestedIndex < 0 ? childCount : Math.max(0, Math.min(requestedIndex, childCount));
        }
    }

    private static final class TreeNodeTransferable implements Transferable {
        private final DefaultMutableTreeNode node;
        private final DataFlavor flavor;

        private TreeNodeTransferable(DefaultMutableTreeNode node, DataFlavor flavor) {
            this.node = node;
            this.flavor = flavor;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{this.flavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor candidate) {
            return this.flavor.equals(candidate);
        }

        @Override
        public Object getTransferData(DataFlavor candidate) throws UnsupportedFlavorException {
            if (!this.isDataFlavorSupported(candidate)) {
                throw new UnsupportedFlavorException(candidate);
            }
            return this.node;
        }
    }

    private record TreeDropDestination(DefaultMutableTreeNode parent, int index) {
    }

    private record GifData(List<BufferedImage> frames, List<Integer> delays, int width, int height) {
        int averageDelayMs() {
            if (this.delays.isEmpty()) {
                return 100;
            }
            long l = 0L;
            for (int n : this.delays) {
                l += (long)n;
            }
            return (int)Math.max(20L, l / (long)this.delays.size());
        }
    }

    private record CategoryRef(String id, String label) {
        @Override
        public String toString() {
            return this.label;
        }
    }

    private record PageRef(String id, String categoryId, String label) {
        @Override
        public String toString() {
            return this.label;
        }
    }
}
