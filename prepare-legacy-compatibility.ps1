param([Parameter(Mandatory = $true)][string]$ResourcesRoot)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $ResourcesRoot).Path
$source = Join-Path $root "forge-1.20.1"
$destination = Join-Path $root "forge-1.17.1"
if (Test-Path -LiteralPath $destination) { throw "Legacy profile already exists in staging" }
Copy-Item -LiteralPath $source -Destination $destination -Recurse
$templates = Join-Path $destination "templates/custombook"
$utf8 = [Text.UTF8Encoding]::new($false)
foreach ($file in Get-ChildItem -LiteralPath $templates -File) {
    $text = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
    $text = $text.Replace("Component.literal(", "new net.minecraft.network.chat.TextComponent(")
    $text = $text.Replace('Component.empty()', 'new net.minecraft.network.chat.TextComponent("")')
    $text = $text.Replace("entity.level()", "entity.level")
    $text = $text.Replace("net.minecraftforge.network.", "net.minecraftforge.fmllegacy.network.")
    $text = $text.Replace("JsonParser.parseString(", "new JsonParser().parse(")
    $text = $text.Replace("object.size()", "object.entrySet().size()")
    $text = $text.Replace('org.slf4j.LoggerFactory.getLogger(', 'org.apache.logging.log4j.LogManager.getLogger(')
    if ($file.Name -eq "screen.java.ftl") {
        $text = $text.Replace("net.minecraft.util.RandomSource.create(), ", "")
        $text = $text.Replace("net.minecraft.locale.Language.getInstance().getOrDefault(key, fallback)",
            "net.minecraft.locale.Language.getInstance().has(key) ? net.minecraft.locale.Language.getInstance().getOrDefault(key) : fallback")
        $text = $text.Replace("import net.minecraft.client.gui.GuiGraphics;", "")
        $text = $text.Replace("GuiGraphics", '${name}BookLegacyGraphics')
        $text = $text.Replace('public void renderBackground(${name}BookLegacyGraphics graphics) {', @'
public void renderBackground(PoseStack poseStack) {
        var graphics = new ${name}BookLegacyGraphics(poseStack);
'@)
        $text = $text.Replace('public void render(${name}BookLegacyGraphics graphics, int mouseX, int mouseY, float partialTick) {', @'
public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        var graphics = new ${name}BookLegacyGraphics(poseStack);
'@)
        $text = $text.Replace("renderBackground(graphics);", "renderBackground(poseStack);")
        $text = $text.Replace('new org.joml.Quaternionf().rotationZ((float) Math.toRadians(rotation))',
            'com.mojang.math.Vector3f.ZP.rotationDegrees(rotation)')
        # Old MCreator generators use plural folders for custom textures; vanilla remains singular.
        $text = $text.Replace('        if (!pathName.startsWith("textures/")) {', @'
        if (!namespace.equals("minecraft")) {
            if (folder.equals("item")) folder = "items";
            else if (folder.equals("block")) folder = "blocks";
        }
        if (!pathName.startsWith("textures/")) {
'@)
    }
    if ($file.Name -eq "client_events.java.ftl") {
        $text = $text.Replace("InputEvent.InteractionKeyMappingTriggered", "InputEvent.ClickInputEvent")
    }
    if ($file.Name -eq "starting_book_events.java.ftl") {
        $text = $text.Replace("event.getEntity()", "event.getPlayer()")
        $text = $text.Replace('.${data.getModElement().getRegistryNameUpper()}.get()', '.${data.getModElement().getRegistryNameUpper()}')
    }
    if ($file.Name -eq "item.java.ftl") {
        $text = $text.Replace('        );', '        );' + "`n" + '        setRegistryName("${registryname}");')
        $text = $text.Replace('super(new Item.Properties()', @'
super(new Item.Properties()
            <#if data.getModElement()??>
            .tab(${data.getCreativeTab()})
            <#else>
            .tab(net.minecraft.world.item.CreativeModeTab.TAB_MISC)
            </#if>
'@)
    }
    if ($file.Name -eq "writing_screen.java.ftl") {
        $text = $text.Replace("import net.minecraft.client.gui.GuiGraphics;", "import com.mojang.blaze3d.vertex.PoseStack;")
        $text = $text.Replace("import net.minecraft.client.gui.components.MultiLineEditBox;", "")
        $text = $text.Replace("MultiLineEditBox", '${name}BookLegacyTextBox')
        $text = $text.Replace("Button.builder(", '${name}BookLegacyButton.builder(')
        $text = $text.Replace("setInitialFocus(editor);", "setFocused(editor); editor.setFocused(true);")
        $text = $text.Replace("setInitialFocus(signatureTitle);", "setFocused(signatureTitle); signatureTitle.setFocus(true);")
        $text = $text.Replace("public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {", @'
public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        var graphics = new ${name}BookLegacyGraphics(poseStack);
'@)
        $text = $text.Replace("super.render(graphics,", "super.render(poseStack,")
    }
    if ($file.Name -eq "page_interaction.java.ftl") {
        $text = $text.Replace("net.minecraft.client.gui.components.MultiLineEditBox", '${name}BookLegacyTextBox')
        $text = $text.Replace("net.minecraft.client.gui.components.Button.builder(", '${name}BookLegacyButton.builder(')
        $text = $text.Replace("GuiGraphics", '${name}BookLegacyGraphics')
        $text = $text.Replace(".render(graphics,", ".render(graphics.pose(),")
    }
    if ($file.Extension -eq ".ftl" -and $file.Name.Contains(".json")) {
        $text = $text.Replace('${texture.getValue().format("%s:block/%s")}', '${modid}:blocks/${texture.getValue()}')
        $text = $text.Replace('"${modid}:item/${tx}"', '"${modid}:items/${tx}"')
    }
    [IO.File]::WriteAllText($file.FullName, $text, $utf8)
}
$definition = Join-Path $destination "custombook.definition.yaml"
$yaml = [IO.File]::ReadAllText($definition).Replace("`r`n", "`n")
foreach ($bridge in @("graphics", "button", "textbox")) {
    $suffix = switch ($bridge) { "graphics" {"Graphics"} "button" {"Button"} "textbox" {"TextBox"} }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "compatibility/templates/legacy_$bridge.java.ftl") -Destination $templates
    $condition = if ($bridge -eq "graphics") { "" } else { "    condition: hasWritingZones()`n" }
    $entry = "  - template: custombook/legacy_$bridge.java.ftl`n    name: `"@SRCROOT/@BASEPACKAGEPATH/client/gui/@NAMEBookLegacy$suffix.java`"`n" + $condition
    $yaml = $yaml.Replace("templates:`n", "templates:`n" + $entry)
}
[IO.File]::WriteAllText($definition, $yaml, $utf8)
Write-Host "Prepared Forge 1.17.1 extension for MCreator 2022.2."
