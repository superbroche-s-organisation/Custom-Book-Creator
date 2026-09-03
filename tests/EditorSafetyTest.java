import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EditorSafetyTest {
    public static void main(String[] args) throws Exception {
        Path guiPath = Path.of("src", "main", "java", "fr", "superbroche", "mcreator", "custombook", "ui", "modgui",
                "CustomBookGUI.java");
        String source = Files.readString(guiPath, StandardCharsets.UTF_8);

        require(source.contains("new String[]{\"CLASSIC\", \"FLAT\", \"OUTLINE\", \"TRANSPARENT\"}"),
                "IMAGE returned to the regular button style selector");
        require(source.contains("new String[]{\"TRANSPARENT\", \"OUTLINE\", \"FLAT\", \"CLASSIC\", \"IMAGE\"}"),
                "page navigation lost its image-container mode");
        require(source.contains("boolean previousLoadingState = this.loadingElement"),
                "editor listener state is not preserved while controls are populated");
        require(source.matches("(?s).*finally\\s*\\{\\s*this\\.loadingElement = previousLoadingState;.*"),
                "editor listener state is not restored in a finally block");
        require(source.contains("private void repairButtonTargets()"), "button target repair is missing");
        require(count(source, "repairButtonTargets();") >= 4,
                "button targets are not repaired after load, delete, move, and save");
        require(source.contains("Book tree move failed and was rolled back"),
                "drag-and-drop failure is not transactionally rolled back");
        require(source.contains("keeping the built-in model available"),
                "a damaged model library can still prevent the editor from opening");
        require(source.contains("Ignoring an invalid or missing item texture"),
                "a missing item texture can still prevent the editor from opening");
        require(source.contains("MAX_DECODED_GIF_PIXELS"), "GIF imports are not bounded");
        require(!source.contains("getActionListeners()") && !source.contains("finishModCreation"),
                "MCreator's native save listeners must not be replaced through reflection");
        System.out.println("EDITOR_SAFETY_OK");
    }

    private static int count(String source, String needle) {
        int count = 0;
        for (int index = 0; (index = source.indexOf(needle, index)) >= 0; index += needle.length()) {
            count++;
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
