package com.qinghe.liteai.model;

public class FeatureOption {
    private final int iconResId;
    private final String title;

    public FeatureOption(int iconResId, String title) {
        this.iconResId = iconResId;
        this.title = title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getTitle() {
        return title;
    }
}
