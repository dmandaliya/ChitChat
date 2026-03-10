package com.chitchat.app;

public class PreferenceService {

    // ----- Display / Theme -----
    private boolean darkMode = false;
    private int fontSize = 2; // 1 -> small, 2 -> medium, 3 -> large
    private String bubbleColour = "blue";

    // ----- Notifications -----
    private boolean notis = true; // notifications in general

    // ----- Chat Behaviour -----

    private boolean showReadReceipts = true; // Shows other user(s) when you read their msg.
                                             /* If in a group chat, then there will be a tiny arrow under
                                                the msg if more than 1 person has viewed it. */

    // ----- Privacy -----
    private boolean onlineStatus = true; // Shows if a user is online / when they were last online.
    private boolean lastSeen = true; // Shows when a user was last online.

    // ----- Extra features -----
    private String fontStyle; // Chooses font style out of a list of them.
    // -----  -----
    // -----  -----


    // ---------- GETTERS / SETTERS ----------

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getBubbleColour() {
        return bubbleColour;
    }

    public void setBubbleColour(String bubbleColour) {
        this.bubbleColour = bubbleColour;
    }

    public boolean isNotis() {
        return notis;
    }

    public void setNotis(boolean notis) {
        this.notis = notis;
    }

    public boolean isShowReadReceipts() {
        return showReadReceipts;
    }

    public void setShowReadReceipts(boolean showReadReceipts) {
        this.showReadReceipts = showReadReceipts;
    }

    public boolean isOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(boolean onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public boolean isLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(boolean lastSeen) {
        this.lastSeen = lastSeen;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }



    // ---------- ACTUAL LOGIC ----------

}
