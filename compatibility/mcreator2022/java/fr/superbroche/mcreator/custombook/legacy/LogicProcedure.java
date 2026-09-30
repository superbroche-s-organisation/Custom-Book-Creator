package fr.superbroche.mcreator.custombook.legacy;

import net.mcreator.element.parts.Procedure;

public final class LogicProcedure extends Procedure {
    private boolean fixedValue = true;

    public LogicProcedure(String name, boolean fixedValue) {
        super(name);
        this.fixedValue = fixedValue;
    }

    public boolean getFixedValue() { return fixedValue; }
}
