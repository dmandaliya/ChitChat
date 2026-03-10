package com.chitchat.shared;

import java.io.Serializable;

public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    // Display / Theme
    private boolean darkMode = false;
    private int fontSize = 2;         // 1=small, 2=medium, 3=large
    private String bubbleColour = "blue";
    private String fontStyle;

    // Notifications
    private boolean notis = true;

    // Chat behaviour
    private boolean showReadReceipts = true;

    // Privacy
    private boolean onlineStatus = true;
    private boolean lastSeen = true;

    public UserPreferences() {}

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public String getBubbleColour() { return bubbleColour; }
    public void setBubbleColour(String bubbleColour) { this.bubbleColour = bubbleColour; }

    public String getFontStyle() { return fontStyle; }
    public void setFontStyle(String fontStyle) { this.fontStyle = fontStyle; }

    public boolean isNotis() { return notis; }
    public void setNotis(boolean notis) { this.notis = notis; }

    public boolean isShowReadReceipts() { return showReadReceipts; }
    public void setShowReadReceipts(boolean showReadReceipts) { this.showReadReceipts = showReadReceipts; }

    public boolean isOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(boolean onlineStatus) { this.onlineStatus = onlineStatus; }

    public boolean isLastSeen() { return lastSeen; }
    public void setLastSeen(boolean lastSeen) { this.lastSeen = lastSeen; }
}
