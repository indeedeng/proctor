package com.indeed.proctor.webapp.model;

/** @author parker Generic model for containing selectable inputs (like countries) */
public class SelectableItem {
    final String label;
    final String value;
    final boolean selected;

    public SelectableItem(final String label, final String value, final boolean selected) {
        this.label = label;
        this.value = value;
        this.selected = selected;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public boolean isSelected() {
        return selected;
    }
}
