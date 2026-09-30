param([Parameter(Mandatory = $true)][string]$ResourcesRoot)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $ResourcesRoot).Path
$utf8 = [Text.UTF8Encoding]::new($false)
$source = Join-Path $root "neoforge-26.1.2"
$destination = Join-Path $root "neoforge-1.21.8"
Copy-Item -LiteralPath $source -Destination $destination -Recurse -Force
foreach ($file in Get-ChildItem -LiteralPath $destination -Recurse -File) {
    if ($file.Extension -notin @(".ftl", ".yaml")) { continue }
    $text = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
    $text = $text.Replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
    $text = $text.Replace("Identifier", "ResourceLocation")
    $text = $text.Replace("net.minecraft.util.Util", "net.minecraft.Util")
    $text = $text.Replace("GuiGraphicsExtractor", "GuiGraphics")
    $text = $text.Replace("extractRenderState(", "render(")
    $text = $text.Replace("extractBackground(", "renderBackground(")
    $text = $text.Replace("graphics.text(", "graphics.drawString(")
    $text = $text.Replace("import net.minecraft.client.input.MouseButtonEvent;`n", "")
    $text = $text.Replace("public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {`n        double mouseX = event.x();`n        double mouseY = event.y();`n        if (event.button() == 0) {",
        "public boolean mouseClicked(double mouseX, double mouseY, int button) {`n        if (button == 0) {")
    $text = $text.Replace("super.mouseClicked(event, doubleClick)", "super.mouseClicked(mouseX, mouseY, button)")
    $text = $text.Replace("() -> DataComponents.WRITTEN_BOOK_CONTENT", "DataComponents.WRITTEN_BOOK_CONTENT")
    $text = $text.Replace("        clickRegions.clear();", "        renderBackground(graphics, mouseX, mouseY, partialTick);`n        clickRegions.clear();")
    [IO.File]::WriteAllText($file.FullName, $text, $utf8)
}
Write-Host "Prepared NeoForge 1.21.8 extension."

# These profiles extend an installed 26.2 generator; they are not generator replacements.
$nextNeo = Join-Path $root "neoforge-26.2"
Copy-Item -LiteralPath $source -Destination $nextNeo -Recurse -Force
$nextFabric = Join-Path $root "fabric-26.2"
Copy-Item -LiteralPath $source -Destination $nextFabric -Recurse -Force
$fabricTemplates = Join-Path $nextFabric "templates/custombook"
$itemFile = Join-Path $fabricTemplates "item.java.ftl"
$item = [IO.File]::ReadAllText($itemFile).Replace("`r`n", "`n")
$item = $item.Replace("() -> DataComponents.WRITTEN_BOOK_CONTENT", "DataComponents.WRITTEN_BOOK_CONTENT")
$currency = $item.IndexOf('    <#if data.isPiglinCurrency>')
$currencyEnd = $item.IndexOf('    </#if>', $currency) + '    </#if>'.Length
if ($currency -lt 0) { throw "Cannot locate the optional NeoForge currency override" }
$item = $item.Remove($currency, $currencyEnd - $currency)
$item = $item.Replace("        );", @'
        );
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            ${package}.client.${name}BookClientEvents.register(this);
        }
        <#if data.startingBook>
        ${package}.event.${name}StartingBookEvents.register(this);
        </#if>
'@)
[IO.File]::WriteAllText($itemFile, $item, $utf8)
$client = [IO.File]::ReadAllText((Join-Path $PSScriptRoot "compatibility/templates/fabric_client.java.ftl")).Replace("`r`n", "`n")
$client = $client.Replace("import net.minecraft.world.InteractionResultHolder;`n", "")
$client = $client.Replace("InteractionResultHolder.success(player.getItemInHand(hand))", "InteractionResult.SUCCESS")
$client = $client.Replace("InteractionResultHolder.pass(player.getItemInHand(hand))", "InteractionResult.PASS")
[IO.File]::WriteAllText((Join-Path $fabricTemplates "client_events.java.ftl"), $client, $utf8)
Copy-Item -LiteralPath (Join-Path $PSScriptRoot "compatibility/templates/fabric_starting_book_modern.java.ftl") -Destination (Join-Path $fabricTemplates "starting_book_events.java.ftl") -Force
Copy-Item -LiteralPath (Join-Path $fabricTemplates "item_model.json.ftl") -Destination (Join-Path $fabricTemplates "item_model_custom_obj.json.ftl") -Force
Write-Host "Prepared NeoForge / Fabric 26.2 extensions."
