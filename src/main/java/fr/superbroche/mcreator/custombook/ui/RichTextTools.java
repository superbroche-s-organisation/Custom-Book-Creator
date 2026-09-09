package fr.superbroche.mcreator.custombook.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-offset-aware formatting. Each visible code point has one value per style. */
public final class RichTextTools {
    private static final Pattern TAG = Pattern.compile(
            "\\[(/?)(b|i|u|s|obf|size|color|url|page|align)(?:=([^\\]]+))?\\]", Pattern.CASE_INSENSITIVE);
    private static final List<String> ORDER = List.of("align", "size", "color", "b", "i", "u", "s", "obf", "url", "page");
    private RichTextTools() {}

    public record Edit(String text, int selectionStart, int selectionEnd) {}
    public record Atom(String source, String display, Map<String, String> styles, int start, int end) {}
    private record Frame(String tag, Map<String, String> previous) {}

    public static List<Atom> parse(String input) {
        String text = input == null ? "" : input;
        List<Atom> atoms = new ArrayList<>();
        Deque<Frame> stack = new ArrayDeque<>();
        Map<String, String> styles = Map.of();
        Matcher matcher = TAG.matcher(text);
        int last = 0;
        while (matcher.find()) {
            appendLiteral(atoms, text, last, matcher.start(), styles);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String argument = matcher.group(3);
            boolean closing = !matcher.group(1).isEmpty();
            if (closing) {
                if (stack.stream().anyMatch(frame -> frame.tag.equals(tag))) {
                    Frame frame;
                    do {
                        frame = stack.pop();
                        styles = frame.previous;
                    } while (!frame.tag.equals(tag));
                }
            } else {
                stack.push(new Frame(tag, styles));
                Map<String, String> updated = new LinkedHashMap<>(styles);
                updated.put(tag, normalized(tag, argument));
                styles = Map.copyOf(updated);
            }
            last = matcher.end();
        }
        appendLiteral(atoms, text, last, text.length(), styles);
        return atoms;
    }

    private static void appendLiteral(List<Atom> atoms, String text, int from, int to, Map<String, String> styles) {
        for (int i = from; i < to;) {
            int next = i + Character.charCount(text.codePointAt(i));
            String value = text.substring(i, next);
            // JTextPane offsets are document offsets; line separators are normalized before editing.
            atoms.add(new Atom(value, value, styles, i, next));
            i = next;
        }
    }

    public static boolean validKey(String key) {
        return key != null && key.matches("[a-zA-Z0-9_.:/-]+");
    }

    private static String normalized(String style, String value) {
        if ("size".equals(style)) {
            try { return String.valueOf(Math.max(6, Math.min(48, Integer.parseInt(value)))); }
            catch (RuntimeException ignored) { return "12"; }
        }
        if ("align".equals(style)) {
            String align = value == null ? "LEFT" : value.toUpperCase(Locale.ROOT);
            return List.of("LEFT", "CENTER", "RIGHT").contains(align) ? align : "LEFT";
        }
        if ("color".equals(style)) {
            return value != null && value.matches("#[0-9a-fA-F]{6}") ? value.toUpperCase(Locale.ROOT) : "#241B13";
        }
        return value == null ? "" : value;
    }

    /** Applies an independent style to the selected visible characters (or touched paragraphs). */
    public static Edit format(String text, int start, int end, String style, String value, boolean paragraphs) {
        if (!ORDER.contains(style)) throw new IllegalArgumentException("Unknown style: " + style);
        text = text == null ? "" : text;
        start = Math.max(0, Math.min(text.length(), start));
        end = Math.max(start, Math.min(text.length(), end));
        List<Atom> atoms = new ArrayList<>(parse(text));
        if (atoms.isEmpty()) return new Edit(text, start, end);
        int first = 0;
        while (first < atoms.size() && atoms.get(first).end <= start) first++;
        int last = first;
        while (last < atoms.size() && atoms.get(last).start < end) last++;
        if (start == end) {
            if (paragraphs) {
                first = Math.min(first, atoms.size() - 1);
                last = first + 1;
            } else {
                first = 0;
                last = atoms.size();
            }
        }
        if (first == last) return new Edit(text, start, end);
        if (paragraphs) {
            while (first > 0 && !"\n".equals(atoms.get(first - 1).display)) first--;
            while (last < atoms.size() && !"\n".equals(atoms.get(last).display)
                    && !"\n".equals(atoms.get(last - 1).display)) last++;
        }
        for (int i = first; i < last; i++) {
            Atom atom = atoms.get(i);
            Map<String, String> updated = new LinkedHashMap<>(atom.styles);
            updated.put(style, normalized(style, value));
            atoms.set(i, new Atom(atom.source, atom.display, Map.copyOf(updated), atom.start, atom.end));
        }
        return serialize(atoms, first, last);
    }

    /** Serializes visual characters without introducing duplicate nested style tags. */
    public static String serializeMarkup(List<Atom> atoms) {
        return serialize(atoms, 0, atoms.size()).text();
    }

    private static Edit serialize(List<Atom> atoms, int first, int last) {
        StringBuilder result = new StringBuilder();
        List<String> open = new ArrayList<>();
        int selectionStart = -1;
        int selectionEnd = -1;
        for (int i = 0; i < atoms.size(); i++) {
            Atom atom = atoms.get(i);
            List<String> desired = new ArrayList<>();
            for (String key : ORDER) {
                if (atom.styles.containsKey(key)) {
                    String arg = atom.styles.get(key);
                    desired.add(key + (arg.isEmpty() ? "" : "=" + arg));
                }
            }
            int common = 0;
            while (common < open.size() && common < desired.size()
                    && open.get(common).equals(desired.get(common))) common++;
            for (int j = open.size() - 1; j >= common; j--) close(result, open.get(j));
            for (int j = common; j < desired.size(); j++) result.append('[').append(desired.get(j)).append(']');
            open = desired;
            if (i == first) selectionStart = result.length();
            result.append(atom.source);
            if (i + 1 == last) selectionEnd = result.length();
        }
        for (int j = open.size() - 1; j >= 0; j--) close(result, open.get(j));
        return new Edit(result.toString(), Math.max(0, selectionStart), Math.max(0, selectionEnd));
    }

    private static void close(StringBuilder out, String opening) {
        int equals = opening.indexOf('=');
        out.append("[/").append(equals < 0 ? opening : opening.substring(0, equals)).append(']');
    }

    /** The first non-newline character determines each paragraph's alignment, as in the game. */
    public static String previewHtml(String markup, String defaultAlignment) {
        List<Atom> atoms = parse(markup);
        StringBuilder result = new StringBuilder();
        int from = 0;
        while (from < atoms.size()) {
            int to = from;
            while (to < atoms.size() && !"\n".equals(atoms.get(to).display)) to++;
            String align = from < to ? atoms.get(from).styles.getOrDefault("align", defaultAlignment) : defaultAlignment;
            result.append("<div style='margin:0;text-align:").append(normalized("align", align).toLowerCase(Locale.ROOT)).append("'>");
            if (from == to) result.append("<br>");
            for (int i = from; i < to; i++) {
                Atom atom = atoms.get(i);
                Map<String, String> styles = atom.styles;
                result.append("<span style='font-size:").append(styles.getOrDefault("size", "12")).append("px;");
                if (styles.containsKey("color")) result.append("color:").append(styles.get("color")).append(';');
                if (styles.containsKey("b")) result.append("font-weight:bold;");
                if (styles.containsKey("i")) result.append("font-style:italic;");
                if (styles.containsKey("obf")) result.append("font-family:monospace;background:#ddd;");
                if (styles.containsKey("u") || styles.containsKey("url") || styles.containsKey("page")) result.append("text-decoration:underline;");
                if (styles.containsKey("s")) result.append("text-decoration:line-through;");
                result.append("'>");
                int runEnd = i + 1;
                while (runEnd < to && atoms.get(runEnd).styles.equals(styles)) runEnd++;
                for (int j = i; j < runEnd; j++) {
                    String display = atoms.get(j).display;
                    for (int c = 0; c < display.length();) {
                        int point = display.codePointAt(c);
                        c += Character.charCount(point);
                        result.append(escape(new String(Character.toChars(point))));
                        if (!Character.isWhitespace(point)) result.append("&#8203;");
                    }
                }
                result.append("</span>");
                i = runEnd - 1;
            }
            result.append("</div>");
            from = to + 1;
        }
        return result.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
