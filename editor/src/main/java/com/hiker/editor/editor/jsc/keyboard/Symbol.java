package com.hiker.editor.editor.jsc.keyboard;

public class Symbol {
    private String showText;
    private String writeText;
    private int pos;

    public Symbol(String showText, String writeText, int pos) {
        this.showText = showText;
        this.writeText = writeText;
        this.pos = pos;
    }

    public String getShowText() {
        return showText;
    }

    public String getWriteText() {
        return writeText;
    }

    public int getPos() {
        return pos;
    }
}
