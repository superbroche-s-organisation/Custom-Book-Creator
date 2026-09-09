param(
    [Parameter(Mandatory = $true)]
    [string]$ResourcesRoot
)

$ErrorActionPreference = "Stop"
$resourceDirectory = (Resolve-Path -LiteralPath $ResourcesRoot).Path
$compatibilityRoot = Join-Path $PSScriptRoot "compatibility\templates"
$baseline = Join-Path $resourceDirectory "neoforge-1.21.1"
$utf8 = [System.Text.UTF8Encoding]::new($false)
function Read-Template([string]$Path) { return [System.IO.File]::ReadAllText($Path).Replace("`r`n", "`n") }
function Write-Template([string]$Path, [string]$Text) {
    [System.IO.Directory]::CreateDirectory((Split-Path -Parent $Path)) | Out-Null
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}

# These are extensions for matching installed generators, not complete workspace generators.
# Forge never published 1.20.5. NeoForge begins with its Forge-compatible 1.20.1 build.
$profiles = @(
    @{ Loader="forge"; Versions=@("1.20","1.20.1","1.20.2","1.20.3","1.20.4","1.20.6") },
    @{ Loader="neoforge"; Versions=@("1.20.1","1.20.2","1.20.3","1.20.4","1.20.5","1.20.6") },
    @{ Loader="fabric"; Versions=@("1.20","1.20.1","1.20.2","1.20.3","1.20.4","1.20.5","1.20.6") }
)
$catalog = @()
foreach ($family in $profiles) {
    foreach ($version in $family.Versions) {
        $patchVersion = if ($version -eq "1.20") { 0 } else { [int]($version.Split(".")[2]) }
        $fabric = $family.Loader -eq "fabric"
        $forgeApi = $family.Loader -eq "forge" -or ($family.Loader -eq "neoforge" -and $patchVersion -eq 1)
        $components = $patchVersion -ge 5
        $id = $family.Loader + "-" + $version
        $destination = Join-Path $resourceDirectory $id
        $templates = Join-Path $destination "templates\custombook"
        [System.IO.Directory]::CreateDirectory($templates) | Out-Null
        foreach ($template in Get-ChildItem -LiteralPath (Join-Path $baseline "templates\custombook") -File) {
            Copy-Item -LiteralPath $template.FullName -Destination (Join-Path $templates $template.Name) -Force
        }
        Copy-Item -LiteralPath (Join-Path $baseline "custombook.definition.yaml") -Destination $destination -Force

        $screenPath = Join-Path $templates "screen.java.ftl"
        $screen = Read-Template $screenPath
        $screen = $screen.Replace("ResourceLocation.fromNamespaceAndPath(", "new ResourceLocation(")
        if ($patchVersion -le 1) {
            $screen = $screen.Replace("renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)",
                "renderBackground(GuiGraphics graphics)")
            $screen = $screen.Replace("renderBackground(graphics, mouseX, mouseY, partialTick)", "renderBackground(graphics)")
        }
        Write-Template $screenPath $screen

        $itemPath = Join-Path $templates "item.java.ftl"
        $item = Read-Template $itemPath
        if (-not $components) {
            foreach ($line in @("import net.minecraft.core.component.DataComponents;",
                    "import net.minecraft.server.network.Filterable;", "import net.minecraft.world.item.component.WrittenBookContent;")) {
                $item = $item.Replace($line + "`n", "")
            }
            $componentStart = $item.IndexOf("            .component(")
            $componentEnd = $item.IndexOf("`n        );", $componentStart)
            if ($componentStart -lt 0 -or $componentEnd -lt 0) { throw "Written-book component block not found" }
            $item = $item.Remove($componentStart, $componentEnd - $componentStart)
            $item = $item.Replace("isCorrectToolForDrops(ItemStack itemstack, net.minecraft.world.level.block.state.BlockState state)",
                "isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state)")
            # Preserve the legacy written-book metadata when a stack is created or first opened.
            $legacy = @'
    private static void initializeBook(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("title")) tag.putString("title", "${data.getSafeBookTitle()?j_string}");
        if (!tag.contains("author")) tag.putString("author", "${data.getSafeAuthor()?j_string}");
        if (!tag.contains("generation")) tag.putInt("generation", ${data.getSafeGeneration()});
        if (!tag.contains("pages")) {
            net.minecraft.nbt.ListTag pages = new net.minecraft.nbt.ListTag();
            pages.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"\"}"));
            tag.put("pages", pages);
        }
        tag.putBoolean("resolved", true);
    }
    @Override public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        initializeBook(stack);
        return stack;
    }

'@
            $item = $item.Replace("    @Override`n    public InteractionResultHolder", $legacy + "    @Override`n    public InteractionResultHolder")
            $item = $item.Replace("        ItemStack stack = player.getItemInHand(hand);",
                "        ItemStack stack = player.getItemInHand(hand);`n        initializeBook(stack);")
        }
        # Typed references can be remapped in exported mods. This method runs only on the client.
        $openStart = $item.IndexOf("    /**")
        $openEnd = $item.IndexOf('    <#if data.isPiglinCurrency>', $openStart)
        if ($openStart -lt 0 -or $openEnd -lt 0) { throw "Client opening block not found" }
        $typedOpening = @'
    private static void openCustomBookScreen() {
        ${package}.client.${name}BookClientEvents.open();
    }

'@
        $item = $item.Remove($openStart, $openEnd - $openStart).Insert($openStart, $typedOpening)
        if ($fabric) {
            $currencyStart = $item.IndexOf('    <#if data.isPiglinCurrency>')
            $currencyEnd = $item.IndexOf('    </#if>', $currencyStart) + '    </#if>'.Length
            $item = $item.Remove($currencyStart, $currencyEnd - $currencyStart)
            # From Minecraft 1.20.5 onward the vanilla signature also takes ItemStack.
            $registration = @'
        );
        if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT) {
            ${package}.client.${name}BookClientEvents.register(this);
        }
        <#if data.startingBook>
        ${package}.event.${name}StartingBookEvents.register(this);
        </#if>
'@
            $item = $item.Replace("        );", $registration)
            Write-Template (Join-Path $templates "client_events.java.ftl") (Read-Template (Join-Path $compatibilityRoot "fabric_client.java.ftl"))
            $starting = Read-Template (Join-Path $compatibilityRoot "fabric_starting_book.java.ftl")
            $ledgerGet = if ($patchVersion -le 1) {
                'storage.computeIfAbsent(${name}StartingBookEvents::load, ${name}StartingBookEvents::new, DATA_ID)'
            } else {
                'storage.computeIfAbsent(new SavedData.Factory<>(${name}StartingBookEvents::new, ${name}StartingBookEvents::load, DataFixTypes.LEVEL), DATA_ID)'
            }
            $lookup = if ($components) { ", net.minecraft.core.HolderLookup.Provider registries" } else { "" }
            $starting = $starting.Replace("@@LEDGER_GET@@", $ledgerGet).Replace("@@LOOKUP_PARAMETER@@", $lookup)
            Write-Template (Join-Path $templates "starting_book_events.java.ftl") $starting
            # Vanilla Fabric does not ship an OBJ loader. Fall back to the item's texture.
            Write-Template (Join-Path $templates "item_model_custom_obj.json.ftl") (Read-Template (Join-Path $templates "item_model.json.ftl"))
        } else {
            $clientPath = Join-Path $templates "client_events.java.ftl"
            $client = Read-Template $clientPath
            $client = $client.Replace('    private ${name}BookClientEvents() {', @'
    public static void open() {
        Minecraft.getInstance().setScreen(new ${name}BookScreen());
    }
    private ${name}BookClientEvents() {
'@)
            Write-Template $clientPath $client
            foreach ($eventName in @("client_events.java.ftl","starting_book_events.java.ftl")) {
                $eventPath = Join-Path $templates $eventName
                $eventText = Read-Template $eventPath
                if ($forgeApi) {
                    $eventText = $eventText.Replace("net.neoforged.api.", "net.minecraftforge.api.")
                    $eventText = $eventText.Replace("net.neoforged.bus.", "net.minecraftforge.eventbus.")
                    $eventText = $eventText.Replace("net.neoforged.neoforge.", "net.minecraftforge.")
                    $eventText = $eventText.Replace("net.neoforged.fml.common.EventBusSubscriber",
                        "net.minecraftforge.fml.common.Mod.EventBusSubscriber")
                } elseif (-not $components) {
                    $eventText = $eventText.Replace("net.neoforged.fml.common.EventBusSubscriber",
                        "net.neoforged.fml.common.Mod.EventBusSubscriber")
                }
                Write-Template $eventPath $eventText
            }
            if ($forgeApi) {
                $objPath = Join-Path $templates "item_model_custom_obj.json.ftl"
                Write-Template $objPath ((Read-Template $objPath).Replace("neoforge:", "forge:"))
            }
        }
        Write-Template $itemPath $item
        $catalog += [ordered]@{ id=$id; minecraft=$version; loader=$family.Loader; java=$(if ($components) {21} else {17}); status="experimental"; requiresInstalledGenerator=$true }
    }
}
Write-Template (Join-Path $resourceDirectory "custombook-compatibility.json") ($catalog | ConvertTo-Json -Depth 4)
Write-Host ("Prepared " + $catalog.Count + " Minecraft 1.20.x generator extensions.")
