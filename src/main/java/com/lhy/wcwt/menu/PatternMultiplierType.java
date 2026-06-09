package com.lhy.wcwt.menu;

public enum PatternMultiplierType {
    SWAP("⇄", "gui.wcwt.multiplier.swap"),
    TIMES_2("×2", "gui.wcwt.multiplier.times2"),
    TIMES_3("×3", "gui.wcwt.multiplier.times3"),
    TIMES_5("×5", "gui.wcwt.multiplier.times5"),
    EQUALS_1("=1", "gui.wcwt.multiplier.equals1"),
    DIVIDE_2("÷2", "gui.wcwt.multiplier.divide2"),
    DIVIDE_3("÷3", "gui.wcwt.multiplier.divide3"),
    DIVIDE_5("÷5", "gui.wcwt.multiplier.divide5");

    private final String display;
    private final String translationKey;

    PatternMultiplierType(String display, String translationKey) {
        this.display = display;
        this.translationKey = translationKey;
    }

    public String getDisplay() {
        return display;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
