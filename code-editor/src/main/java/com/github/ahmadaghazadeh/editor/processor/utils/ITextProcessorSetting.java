package com.github.ahmadaghazadeh.editor.processor.utils;

public interface ITextProcessorSetting {

    public void setReadOnly(boolean readOnly) ;

    public boolean getReadOnly();

    public void setSyntaxHighlight(boolean syntaxHighlight) ;

    public boolean getSyntaxHighlight();

    public int getMaxTabsCount();

    public boolean getFullScreenMode();

    public boolean getConfirmExit();

    public boolean getResumeSession();

    public boolean getDisableSwipeGesture();

    public boolean getImeKeyboard();

    public String getCurrentTypeface();

    public int getFontSize();

    public boolean getWrapContent();

    public boolean getShowLineNumbers();

    public boolean getBracketMatching();

    public String getWorkingFolder() ;

    public void setWorkingFolder(String newWorkingFolder);

    public String getSortMode();

    public boolean getCreatingFilesAndFolders() ;

    public boolean getHighlightCurrentLine() ;

    public boolean getCodeCompletion();

    public boolean getShowHiddenFiles();

    public boolean getPinchZoom();

    public boolean getIndentLine();

    public boolean getInsertBracket();

    public boolean getExtendedKeyboard();
}
