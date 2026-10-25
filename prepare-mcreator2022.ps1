param([Parameter(Mandatory = $true)][string]$SourceRoot)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $SourceRoot).Path
$utf8 = [Text.UTF8Encoding]::new($false)
foreach ($file in Get-ChildItem -LiteralPath $root -Filter "*.java" -File -Recurse) {
    $text = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
    $text = $text.Replace('net.mcreator.element.parts.procedure.Procedure;', 'net.mcreator.element.parts.Procedure;')
    foreach ($class in @("TextureHolder", "LogicProcedure", "LogicProcedureSelector", "TypedTextureSelectorDialog", "TextureSelectionButton", "TabListField")) {
        $text = [regex]::Replace($text, 'import net\.mcreator\.[\w.]+\.' + $class + ';',
            'import fr.superbroche.mcreator.custombook.legacy.' + $class + ';')
    }
    $text = $text.Replace('import net.mcreator.workspace.references.TextureReference;', '')
    $text = $text.Replace('@TextureReference(value=TextureType.ITEM)', '')
    $text = $text.Replace('case Model.Type.JSON', 'case JSON').Replace('case Model.Type.OBJ', 'case OBJ')
    if ($file.Name -eq "BookTranslations.java") {
        $text = $text.Replace("var values = new LinkedHashMap<String, String>();", "var values = new java.util.concurrent.ConcurrentHashMap<String, String>();")
    }
    if ($file.Name -eq "PluginElementTypes.java") {
        $text = [regex]::Replace($text, 'CUSTOMBOOK = .*?;', @'
CUSTOMBOOK = ModElementTypeLoader.register(new ModElementType<CustomBook>("custombook", null,
                net.mcreator.element.BaseType.ITEM, net.mcreator.element.RecipeType.ITEM,
                CustomBookGUI::new, CustomBook.class));
'@)
    }
    if ($file.Name -eq "CustomBook.java") {
        $text = [regex]::Replace($text, '(?s)    public TextureHolder getTexture\(\) \{.*?(?=    public Model getItemModel)', @'
    public String getTexture() {
        String name = getEffectiveItemTexture();
        return name.isBlank() ? "minecraft:written_book" : name;
    }

'@)
        $text = [regex]::Replace($text, '(?s)    public Map<String, TextureHolder> getTextureMap\(\).*?(?=    public boolean hasNormalModel)',
            [Text.RegularExpressions.MatchEvaluator]{ param($match) $match.Value.Replace('TextureHolder', 'String') })
        $text = [regex]::Replace($text, '(?s)    public List<MCItem> providedMCItems\(\) \{.*?(?=    public List<TabEntry> getCreativeTabs)', @'
    public List<MCItem> providedMCItems() {
        return List.of();
    }

    public TabEntry getCreativeTab() {
        return getCreativeTabs().isEmpty()
            ? new TabEntry(getModElement().getWorkspace(), creativeTab == null ? "TOOLS" : creativeTab)
            : getCreativeTabs().get(0);
    }

'@)
    }
    if ($file.Name -eq "CustomBookGUI.java") {
        $text = $text.Replace('this.itemModel.setItems(object);',
            'this.itemModel.removeAllItems(); for (Model model : object) this.itemModel.addItem(model);')
        $text = $text.Replace('refreshListKeepSelected(AbstractProcedureSelector.ReloadContext.create(app.getWorkspace()))',
            'refreshListKeepSelected()')
        $text = $text.Replace('this.createEditorPage(), false)', 'this.createEditorPage())')
        $text = $text.Replace('    public void reloadDataLists() {', @'
    @Override protected net.mcreator.ui.validation.AggregatedValidationResult validatePage(int page) {
        return new net.mcreator.ui.validation.AggregatedValidationResult();
    }

    public void reloadDataLists() {
        if (creativeTabsField != null) creativeTabsField.refreshListKeepSelected();
'@)
    }
    [IO.File]::WriteAllText($file.FullName, $text, $utf8)
}
Copy-Item -Path (Join-Path $PSScriptRoot "compatibility\mcreator2022\java\*") -Destination $root -Recurse -Force
