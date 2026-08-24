<#assign tx = data.getEffectiveItemTexture()>
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": <#if tx?has_content><#if tx?contains(":")>"${tx?keep_before(":")}:item/${tx?keep_after(":")}"<#else>"${modid}:item/${tx}"</#if><#else>"minecraft:item/written_book"</#if>
  }
}
