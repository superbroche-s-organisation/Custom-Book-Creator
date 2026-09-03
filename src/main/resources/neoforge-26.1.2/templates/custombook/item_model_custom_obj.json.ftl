<#assign tx = data.getEffectiveItemTexture()>
{
  "forge_marker": 1,
  "parent": "neoforge:item/default",
  "loader": "neoforge:obj",
  "model": "${modid}:models/item/${data.getSafeCustomModelName()}.obj",
  "textures": {
    <#list data.getTextureMap().entrySet() as texture>
    "${texture.getKey()}": "${texture.getValue().format("%s:block/%s")}",
    </#list>
    "particle": <#if tx?has_content><#if tx?contains(":")>"${tx?keep_before(":")}:item/${tx?keep_after(":")}"<#else>"${modid}:item/${tx}"</#if><#else>"minecraft:item/written_book"</#if>
  }
}
