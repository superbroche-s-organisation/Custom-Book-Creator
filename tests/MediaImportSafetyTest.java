import fr.superbroche.mcreator.custombook.ui.modgui.CustomBookGUI;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import net.mcreator.ui.init.L10N;

/** Exercises the real PNG/GIF decoders using tiny files, never allocating oversized test images. */
public final class MediaImportSafetyTest {
    private static int assertions;
    private static final List<Path> FIXTURES = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        initializeTranslations();
        Path directory = Files.createTempDirectory("custombook-media-test-");
        try {
            testPng(directory);
            testGif(directory);
            testDimensionBounds();
            System.out.println("MEDIA_IMPORT_SAFETY_OK (" + assertions + " dynamic assertions)");
        } finally {
            for (Path fixture : FIXTURES) Files.deleteIfExists(fixture);
            Files.deleteIfExists(directory);
        }
    }

    private static void testPng(Path directory) throws Exception {
        BufferedImage source = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(2, 1, 0x8034ABEF);
        Path png = fixture(directory, "normal.png");
        require(ImageIO.write(source, "png", png.toFile()), "test PNG writer is unavailable");
        BufferedImage decoded = (BufferedImage) invoke("decodePng", new Class<?>[]{File.class}, png.toFile());
        require(decoded.getWidth() == 4 && decoded.getHeight() == 3, "PNG dimensions changed");
        require(decoded.getRGB(2, 1) == source.getRGB(2, 1), "PNG alpha/pixels changed");

        Path invalid = fixture(directory, "invalid.png");
        Files.writeString(invalid, "this is not an image", StandardCharsets.UTF_8);
        expectImportRejection("decodePng", invalid, "invalid PNG must be rejected");
        Path empty = fixture(directory, "empty.png");
        Files.write(empty, new byte[0]);
        expectImportRejection("decodePng", empty, "empty PNG must be rejected");

        Path disguisedJpeg = fixture(directory, "actually-jpeg.png");
        require(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpeg", disguisedJpeg.toFile()),
                "test JPEG writer is unavailable");
        expectImportRejection("decodePng", disguisedJpeg, "renamed JPEG must not be copied as PNG");

        Path oversized = fixture(directory, "too-wide.png");
        Files.write(oversized, pngMetadataOnly(8193, 1));
        expectImportRejection("decodePng", oversized, "PNG metadata must reject width before decoding missing pixels");
        Path pixelBudget = fixture(directory, "too-many-pixels.png");
        Files.write(pixelBudget, pngMetadataOnly(8000, 8001));
        expectImportRejection("decodePng", pixelBudget, "PNG metadata must reject pixel budget before allocation");
        Path truncated = fixture(directory, "missing-pixels.png");
        Files.write(truncated, pngMetadataOnly(2, 2));
        expectIOException("decodePng", truncated, "truncated PNG must fail without returning an image");
    }

    private static void testGif(Path directory) throws Exception {
        Path simple = fixture(directory, "simple.gif");
        Files.write(simple, gif(1, 1, 1, 1, 1));
        Object decoded = invoke("decodeGif", new Class<?>[]{File.class}, simple.toFile());
        List<?> frames = (List<?>) recordValue(decoded, "frames");
        List<?> delays = (List<?>) recordValue(decoded, "delays");
        require(frames.size() == 1 && delays.size() == 1, "single-frame GIF frame/delay count changed");
        require(recordValue(decoded, "width").equals(1) && recordValue(decoded, "height").equals(1),
                "single-frame GIF logical dimensions changed");
        require(((BufferedImage) frames.get(0)).getRGB(0, 0) == 0xFF000000, "GIF pixel was not decoded");
        require(delays.get(0).equals(20), "GIF delay must retain the minimum 20 ms");

        Path animated = fixture(directory, "animated.gif");
        Files.write(animated, gif(2, 3, 1, 1, 2));
        Object animation = invoke("decodeGif", new Class<?>[]{File.class}, animated.toFile());
        List<?> animatedFrames = (List<?>) recordValue(animation, "frames");
        require(animatedFrames.size() == 2, "animated GIF lost a frame");
        require(animatedFrames.get(0) != animatedFrames.get(1), "GIF frames must be independent images");
        require(((BufferedImage) animatedFrames.get(0)).getWidth() == 2
                && ((BufferedImage) animatedFrames.get(0)).getHeight() == 3, "GIF logical canvas was ignored");

        Path disguisedPng = fixture(directory, "actually-png.gif");
        require(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", disguisedPng.toFile()),
                "test PNG writer is unavailable");
        expectImportRejection("decodeGif", disguisedPng, "renamed PNG must not be decoded as an animation");
        Path invalid = fixture(directory, "invalid.gif");
        Files.write(invalid, new byte[]{1, 2, 3});
        expectImportRejection("decodeGif", invalid, "invalid GIF must be rejected");
        Path canvas = fixture(directory, "too-wide-canvas.gif");
        Files.write(canvas, gif(8193, 1, 1, 1, 1));
        expectImportRejection("decodeGif", canvas, "oversized GIF logical screen must fail before allocation");
        Path frame = fixture(directory, "too-wide-frame.gif");
        Files.write(frame, gif(1, 1, 8193, 1, 1));
        expectImportRejection("decodeGif", frame, "oversized GIF frame metadata must fail before pixel decoding");
        Path excessiveFrames = fixture(directory, "too-many-frames.gif");
        Files.write(excessiveFrames, gif(1, 1, 1, 1, 501));
        expectImportRejection("decodeGif", excessiveFrames, "GIF frame count must be capped");
        Path pixelBudget = fixture(directory, "too-many-decoded-pixels.gif");
        Files.write(pixelBudget, gif(8000, 8000, 1, 1, 2));
        expectImportRejection("decodeGif", pixelBudget, "total GIF pixel budget must fail before canvas allocation");
    }

    private static void testDimensionBounds() throws Exception {
        for (int[] valid : new int[][]{{1, 1}, {8192, 1}, {1, 8192}, {8000, 8000}}) {
            invoke("validateMediaDimensions", new Class<?>[]{int.class, int.class}, valid[0], valid[1]);
            require(true, "valid image dimensions should be accepted");
        }
        for (int[] invalid : new int[][]{{0, 1}, {1, 0}, {-1, 1}, {1, -1}, {8193, 1}, {1, 8193},
                {8000, 8001}, {Integer.MAX_VALUE, Integer.MAX_VALUE}}) {
            try {
                invoke("validateMediaDimensions", new Class<?>[]{int.class, int.class}, invalid[0], invalid[1]);
                throw new AssertionError("unsafe image dimensions were accepted: " + invalid[0] + "x" + invalid[1]);
            } catch (IOException expected) {
                require(expected.getClass() == IOException.class, "dimension guard must reject safely");
            }
        }
    }

    private static Path fixture(Path directory, String name) {
        Path fixture = directory.resolve(name);
        FIXTURES.add(fixture);
        return fixture;
    }

    private static void expectImportRejection(String decoder, Path file, String message) throws Exception {
        try {
            invoke(decoder, new Class<?>[]{File.class}, file.toFile());
            throw new AssertionError(message);
        } catch (IOException expected) {
            // A plain IOException comes from the plugin's metadata/format guard, not an ImageIO raster read failure.
            require(expected.getClass() == IOException.class, message + ": reached raster decoding instead: " + expected);
        }
    }

    private static void expectIOException(String decoder, Path file, String message) throws Exception {
        try {
            invoke(decoder, new Class<?>[]{File.class}, file.toFile());
            throw new AssertionError(message);
        } catch (IOException expected) {
            require(true, message);
        }
    }

    private static Object invoke(String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = CustomBookGUI.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception cause) throw cause;
            if (exception.getCause() instanceof Error cause) throw cause;
            throw exception;
        }
    }

    private static Object recordValue(Object record, String name) throws Exception {
        Method accessor = record.getClass().getDeclaredMethod(name);
        accessor.setAccessible(true);
        return accessor.invoke(record);
    }

    private static byte[] pngMetadataOnly(int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeLong(0x89504E470D0A1A0AL);
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            try (DataOutputStream fields = new DataOutputStream(header)) {
                fields.writeInt(width);
                fields.writeInt(height);
                fields.write(new byte[]{8, 6, 0, 0, 0});
            }
            pngChunk(out, "IHDR", header.toByteArray());
            pngChunk(out, "IEND", new byte[0]);
        }
        return bytes.toByteArray();
    }

    private static void pngChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.writeInt(data.length);
        out.write(name);
        out.write(data);
        out.writeInt((int) crc.getValue());
    }

    private static byte[] gif(int width, int height, int frameWidth, int frameHeight, int frames) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("GIF89a".getBytes(StandardCharsets.US_ASCII));
        littleEndian(out, width);
        littleEndian(out, height);
        out.write(new byte[]{(byte) 0x80, 0, 0, 0, 0, 0, (byte) 255, (byte) 255, (byte) 255});
        for (int frame = 0; frame < frames; frame++) {
            out.write(new byte[]{0x21, (byte) 0xF9, 4, 0, 1, 0, 0, 0, 0x2C});
            littleEndian(out, 0);
            littleEndian(out, 0);
            littleEndian(out, frameWidth);
            littleEndian(out, frameHeight);
            out.write(new byte[]{0, 2, 2, 0x44, 1, 0});
        }
        out.write(0x3B);
        return out.toByteArray();
    }

    private static void littleEndian(ByteArrayOutputStream out, int value) {
        out.write(value & 255);
        out.write((value >>> 8) & 255);
    }

    private static void initializeTranslations() throws Exception {
        ResourceBundle bundle;
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/lang/texts.properties"), StandardCharsets.UTF_8)) {
            bundle = new PropertyResourceBundle(reader);
        }
        for (String name : List.of("rb", "rb_en")) {
            Field field = L10N.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, bundle);
        }
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
