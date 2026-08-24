import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UrlOpeningTemplateTest {
    public static void main(String[] args) throws Exception {
        verify("neoforge-1.21.1", "net.minecraft.Util", "net.minecraft.util.Util");
        verify("neoforge-26.1.2", "net.minecraft.util.Util", "net.minecraft.Util");
    }

    private static void verify(String generator, String expectedImport, String rejectedImport) throws Exception {
        Path template = Path.of("src", "main", "resources", generator, "templates", "custombook", "screen.java.ftl");
        String source = Files.readString(template, StandardCharsets.UTF_8);

        require(source.contains("import " + expectedImport + ";"), generator + ": missing version-correct Util import");
        require(!source.contains("import " + rejectedImport + ";"), generator + ": contains Util import from the other Minecraft version");
        require(source.contains("Util.getPlatform().openUri(url);"), generator + ": missing external URL opener");
        require(source.contains("new ConfirmLinkScreen"), generator + ": external URLs must keep Minecraft's confirmation screen");

        System.out.println("URL_OPENING_TEMPLATE_OK " + generator);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
