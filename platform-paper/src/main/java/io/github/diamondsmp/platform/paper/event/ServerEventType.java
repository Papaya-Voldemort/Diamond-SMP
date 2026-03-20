package io.github.diamondsmp.platform.paper.event;

public enum ServerEventType {
    NAME_TAG("nametag", "Name Tag"),
    CAT_HUNT("cat_hunt", "Cat Hunt");

    private final String key;
    private final String displayName;

    ServerEventType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public static ServerEventType fromKey(String key) {
        for (ServerEventType value : values()) {
            if (value.key.equalsIgnoreCase(key) || value.name().equalsIgnoreCase(key)) {
                return value;
            }
        }
        return null;
    }
}
