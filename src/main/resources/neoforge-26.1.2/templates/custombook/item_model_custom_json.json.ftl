{
  "parent": "${modid}:custom/${data.customModelName.split(":")[0]}",
  "textures": {
    <#list data.getTextureMap().entrySet() as texture>
    "${texture.getKey()}": "${texture.getValue().format("%s:block/%s")}",
    </#list>
    "particle": "${data.texture.format("%s:item/%s")}"
  }
}
