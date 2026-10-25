param([Parameter(Mandatory = $true)][string]$SourceRoot)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $SourceRoot).Path
$utf8 = [Text.UTF8Encoding]::new($false)
foreach ($file in Get-ChildItem -LiteralPath $root -Filter "*.java" -File -Recurse) {
    $text = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
    if ($file.Name -eq "PluginElementTypes.java") {
        $text = $text.Replace('new ModElementType("custombook", "custombook",', 'new ModElementType("custombook",')
    }
    if ($file.Name -eq "BookTranslations.java") {
        $text = $text.Replace("var values = new LinkedHashMap<String, String>();", "var values = new java.util.concurrent.ConcurrentHashMap<String, String>();")
    }
    if ($file.Name -eq "CustomBookGUI.java") {
        $text = $text.Replace('this.itemModel.setItems(object);',
            'this.itemModel.removeAllItems(); for (Model model : object) this.itemModel.addItem(model);')
        $text = $text.Replace('refreshListKeepSelected(AbstractProcedureSelector.ReloadContext.create(app.getWorkspace()))',
            'refreshListKeepSelected()')
        $text = $text.Replace('    public void reloadDataLists() {', @'
    @Override protected net.mcreator.ui.validation.AggregatedValidationResult validatePage(int page) {
        if (page == 2 && writingPermission != null && onBookSigned != null)
            return new net.mcreator.ui.validation.AggregatedValidationResult(writingPermission, onBookSigned);
        return new net.mcreator.ui.validation.AggregatedValidationResult();
    }

    public void reloadDataLists() {
'@)
    }
    [IO.File]::WriteAllText($file.FullName, $text, $utf8)
}
