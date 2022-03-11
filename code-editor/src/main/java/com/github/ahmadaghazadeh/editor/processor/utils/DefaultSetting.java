package com.github.ahmadaghazadeh.editor.processor.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class DefaultSetting implements ITextProcessorSetting {

    private SharedPreferences pref;

    public DefaultSetting(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public boolean getReadOnly() {
        return pref.getBoolean("READ_ONLY", false);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        pref.edit().putBoolean("READ_ONLY", readOnly).apply();
    }

    @Override
    public boolean getSyntaxHighlight() {
        return pref.getBoolean("SYNTAX_HIGHLIGHT", true);
    }

    @Override
    public void setSyntaxHighlight(boolean syntaxHighlight) {
        pref.edit().putBoolean("SYNTAX_HIGHLIGHT", syntaxHighlight).apply();
    }

    @Override
    public int getMaxTabsCount() {
        return 5; //pref.getInt...
    }

    @Override
    public boolean getFullScreenMode() {
        return pref.getBoolean("FULLSCREEN_MODE", false);
    }

    @Override
    public boolean getConfirmExit() {
        return pref.getBoolean("CONFIRM_EXIT", true);
    }

    @Override
    public boolean getResumeSession() {
        return pref.getBoolean("RESUME_SESSION", true);
    }

    @Override
    public boolean getDisableSwipeGesture() {
        return pref.getBoolean("DISABLE_SWIPE", false);
    }

    @Override
    public boolean getImeKeyboard() {
        return pref.getBoolean("USE_IME_KEYBOARD", false);
    }

    @Override
    public String getCurrentTypeface() {
        return pref.getString("FONT_TYPE", "droid_sans_mono");
    }

    @Override
    public int getFontSize() {
        return pref.getInt("FONT_SIZE", 14); //default
    }

    @Override
    public boolean getWrapContent() {
        return pref.getBoolean("WRAP_CONTENT", true);
    }

    @Override
    public boolean getShowLineNumbers() {
        return pref.getBoolean("SHOW_LINE_NUMBERS", true);
    }

    @Override
    public boolean getBracketMatching() {
        return pref.getBoolean("BRACKET_MATCHING", true);
    }

    @Override
    public String getWorkingFolder() {
        return pref.getString("FEXPLORER_WORKING_FOLDER",
                Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    @Override
    public void setWorkingFolder(String newWorkingFolder) {
        pref.edit().putString("FEXPLORER_WORKING_FOLDER", newWorkingFolder).apply();
    }

    @Override
    public String getSortMode() {
        return pref.getString("FILE_SORT_MODE", "SORT_BY_NAME");
    }

    @Override
    public boolean getCreatingFilesAndFolders() {
        return pref.getBoolean("ALLOW_CREATING_FILES", true);
    }

    @Override
    public boolean getHighlightCurrentLine() {
        return pref.getBoolean("HIGHLIGHT_CURRENT_LINE", true);
    }

    @Override
    public boolean getCodeCompletion() {
        return pref.getBoolean("CODE_COMPLETION", true);
    }

    @Override
    public boolean getShowHiddenFiles() {
        return pref.getBoolean("SHOW_HIDDEN_FILES", false);
    }

    @Override
    public boolean getPinchZoom() {
        return pref.getBoolean("PINCH_ZOOM", true);
    }

    @Override
    public boolean getIndentLine() {
        return pref.getBoolean("INDENT_LINE", true);
    }

    @Override
    public boolean getInsertBracket() {
        return pref.getBoolean("INSERT_BRACKET", true);
    }

    @Override
    public boolean getExtendedKeyboard() {
        return pref.getBoolean("USE_EXTENDED_KEYS", false);
    }
}
