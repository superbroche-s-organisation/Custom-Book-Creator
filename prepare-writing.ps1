param([Parameter(Mandatory = $true)][string]$ResourcesRoot)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $ResourcesRoot).Path
$templatesRoot = Join-Path $PSScriptRoot "compatibility/templates"
$utf8 = [System.Text.UTF8Encoding]::new($false)
function Write-WritingTemplate([string]$Path, [string]$Text) {
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}
foreach ($profile in Get-ChildItem -LiteralPath $root -Directory) {
    $definition = Join-Path $profile.FullName "custombook.definition.yaml"
    if (-not (Test-Path -LiteralPath $definition)) { continue }
    $id = $profile.Name
    $parts = $id.Split("-", 2)
    $loader = $parts[0]
    $version = [version]$parts[1]
    $modern = $version.Major -ge 26
    $components = $modern -or $version -ge [version]"1.20.5"
    $optionalNbt = $modern -or $version -ge [version]"1.21.5"
    $newBox = $modern -or $version -ge [version]"1.21.6"
    $autoBus = $modern -or $version -ge [version]"1.21.8"
    $forgeApi = $loader -eq "forge" -or ($loader -eq "neoforge" -and $version -eq [version]"1.20.1")
    $templateDir = Join-Path $profile.FullName "templates/custombook"
    $assign = "<#assign cbcComponents=" + $components.ToString().ToLowerInvariant() +
        " cbcOptionalNbt=" + $optionalNbt.ToString().ToLowerInvariant() +
        " cbcModernProfile=" + $modern.ToString().ToLowerInvariant() +
        " cbcNewTextBox=" + $newBox.ToString().ToLowerInvariant() +
        " cbcAutoBus=" + $autoBus.ToString().ToLowerInvariant() + ">`n"
    $outputs = [ordered]@{
        "writing_data.java.ftl" = "book/@NAMEBookWritingData.java"
        "writing_server.java.ftl" = "book/@NAMEBookWritingServer.java"
        "writing_client.java.ftl" = "client/@NAMEBookWritingClient.java"
        "writing_screen.java.ftl" = "client/gui/@NAMEBookWritingScreen.java"
        "writing_network.java.ftl" = "network/@NAMEBookWritingMessage.java"
    }
    if ($loader -eq "fabric") { $outputs["writing_fabric_client.java.ftl"] = "client/@NAMEBookWritingFabricClient.java" }
    $entries = ""
    foreach ($entry in $outputs.GetEnumerator()) {
        $sourceName = $entry.Key
        if ($sourceName -eq "writing_network.java.ftl") {
            $family = if ($loader -eq "fabric") { "fabric" } elseif ($forgeApi) { "forge" } else { "neoforge" }
            $sourceName = "writing_network_$family.java.ftl"
        }
        $source = [System.IO.File]::ReadAllText((Join-Path $templatesRoot $sourceName)).Replace("`r`n", "`n")
        if ($version -lt [version]"1.21") {
            $source = $source.Replace("ResourceLocation.fromNamespaceAndPath(", "new ResourceLocation(")
        }
        if ($loader -eq "neoforge" -and $version -ge [version]"1.21.8") {
            $source = $source.Replace("PacketDistributor.sendToServer(", "net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(")
        }
        if ($modern) {
            $source = $source.Replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier").Replace("ResourceLocation.", "Identifier.")
            $source = $source.Replace("GuiGraphics", "GuiGraphicsExtractor")
            $source = $source.Replace("public void render(", "public void extractRenderState(")
            $source = $source.Replace("super.render(graphics,", "super.extractRenderState(graphics,")
            $source = $source.Replace("graphics.drawCenteredString(", "graphics.centeredText(")
            $source = $source.Replace("graphics.drawString(", "graphics.text(")
            $source = $source.Replace("graphics.drawWordWrap(", "graphics.textWithWordWrap(")
        }
        Write-WritingTemplate (Join-Path $templateDir $entry.Key) ($assign + $source)
        $entries += "  - template: custombook/" + $entry.Key + "`n    name: `"@SRCROOT/@BASEPACKAGEPATH/" +
                $entry.Value + "`"`n    condition: hasWritingZones()`n"
    }
    $yaml = [System.IO.File]::ReadAllText($definition).Replace("`r`n", "`n")
    Write-WritingTemplate $definition ($yaml.Replace("templates:`n", "templates:`n" + $entries))

    $interaction = [IO.File]::ReadAllText((Join-Path $templatesRoot "page_interaction.java.ftl")).Replace("`r`n", "`n")
    if ($modern) {
        $interaction = $interaction.Replace("GuiGraphics", "GuiGraphicsExtractor")
        $interaction = $interaction.Replace(".render(graphics,", ".extractRenderState(graphics,")
        $interaction = $interaction.Replace("keyPressed(int key, int scanCode, int modifiers)", "keyPressed(net.minecraft.client.input.KeyEvent event)")
        $interaction = $interaction.Replace("keyPressed(key, scanCode, modifiers)", "keyPressed(event)")
        $interaction = $interaction.Replace("keyInventory.matches(key, scanCode)", "keyInventory.matches(event)")
    }
    Write-WritingTemplate (Join-Path $templateDir "page_interaction.java.ftl") ($assign + $interaction)

    if ($version -lt [version]"1.20.2") {
        $screenPath = Join-Path $templateDir "screen.java.ftl"
        $screen = [IO.File]::ReadAllText($screenPath)
        $screen = $screen.Replace("mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY)", "mouseScrolled(double mouseX, double mouseY, double deltaY)")
        $screen = $screen.Replace("super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)", "super.mouseScrolled(mouseX, mouseY, deltaY)")
        Write-WritingTemplate $screenPath $screen
    }

    $clientPath = Join-Path $templateDir "client_events.java.ftl"
    $client = [System.IO.File]::ReadAllText($clientPath)
    if ($loader -eq "fabric") {
        $client = $client.Replace('public static void open() {', 'public static void open() { open(net.minecraft.world.InteractionHand.MAIN_HAND); }' +
            "`n    public static void open(net.minecraft.world.InteractionHand hand) {")
        $client = $client.Replace('new ${name}BookScreen()', 'new ${name}BookScreen(hand)')
        $client = $client.Replace("        open();", "        open(hand);")
        $itemPath = Join-Path $templateDir "item.java.ftl"
        $item = [System.IO.File]::ReadAllText($itemPath)
        $item = $item.Replace("        );", @'
        );
        <#if data.hasWritingZones()>
        ${package}.network.${name}BookWritingMessage.register();
        </#if>
'@)
        Write-WritingTemplate $itemPath $item
    } else {
        $client = $client.Replace('public static void open() {', 'public static void open() { open(net.minecraft.world.InteractionHand.MAIN_HAND); }' +
            "`n    public static void open(net.minecraft.world.InteractionHand hand) {")
        $client = $client.Replace('minecraft.setScreen(new ${name}BookScreen());',
            'minecraft.setScreen(new ${name}BookScreen(event.getHand()));')
        $client = $client.Replace('Minecraft.getInstance().setScreen(new ${name}BookScreen());',
            'Minecraft.getInstance().setScreen(new ${name}BookScreen(hand));')
    }
    Write-WritingTemplate $clientPath $client
    $itemPath = Join-Path $templateDir "item.java.ftl"
    $item = [System.IO.File]::ReadAllText($itemPath)
    $item = $item.Replace('openCustomBookScreen();', 'openCustomBookScreen(hand);')
    $item = $item.Replace('private static void openCustomBookScreen()', 'private static void openCustomBookScreen(InteractionHand hand)')
    $item = $item.Replace('BookClientEvents.open();', 'BookClientEvents.open(hand);')
    $item = $item.Replace('screenClass.getConstructor().newInstance()', 'screenClass.getConstructor(InteractionHand.class).newInstance(hand)')
    Write-WritingTemplate $itemPath $item
    if ($version -ge [version]"26.2") {
        # In 26.2 screen ownership moved from Minecraft to its Gui instance.
        foreach ($file in Get-ChildItem -LiteralPath $templateDir -Filter "*.java.ftl" -File) {
            $text = [IO.File]::ReadAllText($file.FullName)
            $text = $text.Replace('.setScreen(', '.gui.setScreen(')
            $text = [regex]::Replace($text, '(Minecraft\.getInstance\(\)|minecraft|client)\.screen\b', '$1.gui.screen()')
            if ($loader -eq "fabric") {
                $text = $text.Replace('PayloadTypeRegistry.playC2S()', 'PayloadTypeRegistry.serverboundPlay()')
                $text = $text.Replace('PayloadTypeRegistry.playS2C()', 'PayloadTypeRegistry.clientboundPlay()')
            }
            Write-WritingTemplate $file.FullName $text
        }
    }
}
Write-Host "Prepared server-validated writing templates."
