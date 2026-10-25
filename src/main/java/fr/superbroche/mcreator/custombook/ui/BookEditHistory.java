package fr.superbroche.mcreator.custombook.ui;

import java.util.ArrayList;
import java.util.List;

/** Bounded, editor-local snapshots. Restoring never mutates a saved workspace or imported resources. */
public final class BookEditHistory {
    public record State(String json, String pageId, String elementId, boolean title) {}
    private final List<State> states = new ArrayList<>();
    private int cursor = -1;
    public void reset(State initial) { states.clear(); cursor = -1; push(initial); }
    public void push(State state) {
        if (cursor >= 0 && states.get(cursor).json().equals(state.json())) {
            states.set(cursor, state);
            return;
        }
        while (states.size() > cursor + 1) states.remove(states.size() - 1);
        states.add(state); cursor++;
        long characters = states.stream().mapToLong(value -> value.json().length()).sum();
        while (states.size() > 1 && (states.size() > 64 || characters > 8_000_000)) {
            characters -= states.remove(0).json().length(); cursor--;
        }
    }
    public boolean canUndo() { return cursor > 0; }
    public void replaceCurrent(State state) { if (cursor >= 0) states.set(cursor, state); }
    public boolean canRedo() { return cursor >= 0 && cursor + 1 < states.size(); }
    public State undo() { return canUndo() ? states.get(--cursor) : null; }
    public State redo() { return canRedo() ? states.get(++cursor) : null; }
}
