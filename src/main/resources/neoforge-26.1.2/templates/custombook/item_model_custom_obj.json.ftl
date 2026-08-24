{
  "forge_marker": 1,
  "parent": "neoforge:item/default",
  "loader": "neoforge:obj",
  "model": "${modid}:models/item/${data.customModelName.split(":")[0]}.obj",
  "textures": {
    <#list data.getTextureMap().entrySet() as texture>
    "${texture.getKey()}": "${texture.getValue().format("%s:block/%s")}",
    </#list>
    "particle": "${data.texture.format("%s:item/%s")}"
  }
}
