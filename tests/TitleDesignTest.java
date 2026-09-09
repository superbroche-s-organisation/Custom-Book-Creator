import com.google.gson.Gson;
import fr.superbroche.mcreator.custombook.element.types.CustomBook;
import fr.superbroche.mcreator.custombook.element.types.CustomBook.BookPage;
import fr.superbroche.mcreator.custombook.element.types.CustomBook.TitleDesign;
import fr.superbroche.mcreator.custombook.element.types.CustomBook.TitleLayout;

/** Migration, copy and shared editor/runtime geometry regressions for rich page titles. */
public final class TitleDesignTest {
    private static int assertions;

    public static void main(String[] args) {
        BookPage page = new BookPage("Legacy heading", "");
        TitleLayout top = page.getTitleLayout();
        check(page.getTitleContent().equals("Legacy heading"), "Legacy title fallback");
        check(top.equals(new TitleLayout(14, 10, 228, 24, 0, 128, 33, 14, 33, 228, 1)),
                "Default title/bar geometry");
        page.titleDesign = null;
        check(page.getTitleDesign() != null && page.getTitleLayout().equals(top), "Null title migration");
        Gson gson = new Gson();
        BookPage legacy = gson.fromJson("{\"title\":\"Old page\",\"showTitle\":true,\"content\":\"Body\"}", BookPage.class);
        legacy.normalizeElements();
        check(legacy.getTitleContent().equals("Old page") && legacy.getTitleLayout().equals(top), "Absent serialized design migration");
        BookPage explicitNull = gson.fromJson("{\"title\":\"Old page\",\"titleDesign\":null}", BookPage.class);
        check(explicitNull.getTitleContent().equals("Old page"), "Explicit JSON null migration");

        TitleDesign design = page.getTitleDesign();
        design.content = "[align=LEFT][size=18][b]My title[/b][/size][/align]";
        design.position = "RIGHT";
        design.offset = 22;
        design.height = 35;
        design.align = "RIGHT";
        design.barVisible = false;
        design.barColor = "#ABCDEF";
        design.barLength = 250;
        design.barThickness = 3;
        BookPage roundTrip = gson.fromJson(gson.toJson(page), BookPage.class);
        check(roundTrip.getTitleContent().equals(design.content), "Rich title save/load");
        check(roundTrip.getTitleLayout().equals(page.getTitleLayout()), "Title layout save/load");
        check(!roundTrip.getTitleDesign().barVisible && roundTrip.getTitleDesign().barColor.equals("#ABCDEF"),
                "Hidden bar settings save/load");
        check(page.title.equals("Legacy heading"), "Rich content must not corrupt structural page name");
        TitleDesign copy = design.copy();
        copy.offset = 66;
        copy.content = "Changed";
        check(design.offset == 22 && !design.content.equals(copy.content), "Title copies are independent");

        for (String position : new String[] { "TOP", "BOTTOM", "LEFT", "RIGHT" }) {
            for (int height : new int[] { -100, 10, 24, 96, 10000 }) {
                for (int offset : new int[] { -100, 0, 22, 10000 }) {
                    TitleDesign candidate = new TitleDesign();
                    candidate.position = position;
                    candidate.height = height;
                    candidate.offset = offset;
                    candidate.barLength = 10000;
                    candidate.barThickness = 10000;
                    TitleLayout box = candidate.layout();
                    check(box.x() >= 0 && box.y() >= 0 && box.x() + box.width() <= 256
                                    && box.y() + box.height() <= 320, "Title stays on page: " + position);
                    check(box.barX() >= box.x() && box.barY() >= box.y()
                                    && box.barX() + box.barWidth() <= box.x() + box.width()
                                    && box.barY() + box.barHeight() <= box.y() + box.height(), "Bar stays inside title: " + position);
                    check(candidate.offsetForHandle(box.handleX(), box.handleY(), 0) == candidate.offset,
                            "Handle inverse geometry: " + position);
                }
            }
            TitleDesign snapped = new TitleDesign();
            snapped.position = position;
            snapped.offset = snapped.offsetForHandle(73, 91, 8);
            TitleLayout box = snapped.layout();
            boolean vertical = position.equals("LEFT") || position.equals("RIGHT");
            check((vertical ? box.handleX() : box.handleY()) % 8 == 0, "Handle honors grid: " + position);
        }
        design.content = null;
        design.align = null;
        design.position = "INVALID";
        design.barColor = "#invalid";
        design.normalize();
        check(design.content.isEmpty() && design.align.equals("CENTER") && design.position.equals("TOP")
                && design.barColor.equals("#DDCDAA"), "Invalid serialized values normalized");
        String rocket = new String(Character.toChars(0x1F680));
        design.content = rocket.repeat(20000);
        page.getTitleDesign();
        check(!Character.isHighSurrogate(design.content.charAt(design.content.length() - 1)), "Safe title truncation");
        check(String.join("", page.getTitleContentChunks()).equals(design.content), "Title constant chunks preserve Unicode");
        check(page.getTitleContentChunks().stream().allMatch(chunk -> chunk.length() <= 16000), "Title constants split safely");
        System.out.println("TITLE_DESIGN_OK assertions=" + assertions);
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
}
