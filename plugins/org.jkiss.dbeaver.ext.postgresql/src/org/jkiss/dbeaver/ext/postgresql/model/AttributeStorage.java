package org.jkiss.dbeaver.ext.postgresql.model;

public enum AttributeStorage {
    PLAIN("plain"),
    MAIN("main"),
    EXTERNAL("external"),
    EXTENDED("extended"),
    UNKNOWN("unknown");

    private final String displayName;

    AttributeStorage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static AttributeStorage fromValue(String value) {
        switch (value) {
            case "p":
                return PLAIN;
            case "m":
                return MAIN;
            case "e":
                return EXTERNAL;
            case "x":
                return EXTENDED;
            default:
                return UNKNOWN;
        }
    }
}
