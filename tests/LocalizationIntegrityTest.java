import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalizationIntegrityTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+(?:,[^}]*)?}");

    public static void main(String[] args) throws Exception {
        Path langRoot = RESOURCES.resolve("lang");
        List<Path> catalogs;
        try (var stream = Files.list(langRoot)) {
            catalogs = stream.filter(path -> path.getFileName().toString().matches("texts(?:_[A-Za-z_]+)?\\.properties"))
                    .sorted().toList();
        }
        require(catalogs.size() == 30, "expected 30 language catalogs, found " + catalogs.size());

        Properties baseline = load(langRoot.resolve("texts.properties"));
        require(!baseline.isEmpty(), "default language catalog is empty");
        Set<String> baselineKeys = baseline.stringPropertyNames();
        for (Path catalog : catalogs) {
            Properties translated = load(catalog);
            require(translated.stringPropertyNames().equals(baselineKeys),
                    catalog.getFileName() + " does not have the exact default key set");
            for (String key : baselineKeys) {
                String value = translated.getProperty(key);
                require(value != null && !value.isBlank(), catalog.getFileName() + ": blank translation for " + key);
                require(placeholders(value).equals(placeholders(baseline.getProperty(key))),
                        catalog.getFileName() + ": placeholder mismatch for " + key);
            }
        }

        Path helpRoot = RESOURCES.resolve("help");
        List<Path> localeDirectories;
        try (var stream = Files.list(helpRoot)) {
            localeDirectories = stream.filter(Files::isDirectory).sorted().toList();
        }
        require(localeDirectories.size() == 30, "expected 30 help locales, found " + localeDirectories.size());
        Set<String> expectedHelpFiles = relativeFiles(helpRoot.resolve("default"));
        require(expectedHelpFiles.size() == 13, "default help tree must contain 13 files");
        for (Path localeDirectory : localeDirectories) {
            require(relativeFiles(localeDirectory).equals(expectedHelpFiles),
                    localeDirectory.getFileName() + " help tree is incomplete");
        }
        System.out.println("LOCALIZATION_INTEGRITY_OK catalogs=30 keys=" + baselineKeys.size() + " helpFiles=13");
    }

    private static Properties load(Path path) throws Exception {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static List<String> placeholders(String value) {
        ArrayList<String> result = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(value == null ? "" : value);
        while (matcher.find()) {
            result.add(matcher.group());
        }
        result.sort(String::compareTo);
        return result;
    }

    private static Set<String> relativeFiles(Path root) throws Exception {
        HashSet<String> files = new HashSet<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> files.add(root.relativize(path).toString().replace('\\', '/')));
        }
        return files;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
