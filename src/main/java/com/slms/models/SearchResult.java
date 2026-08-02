package com.slms.models;

public class SearchResult {
    public enum ResultType { BOOK, MEMBER, SHELF }

    private ResultType type;
    private int id; // bookId, memberId, or shelfId
    private String label;
    private String subtitle;

    public SearchResult(ResultType type, int id, String label, String subtitle) {
        this.type = type;
        this.id = id;
        this.label = label;
        this.subtitle = subtitle;
    }

    public ResultType getType() { return type; }
    public int getId() { return id; }
    public String getLabel() { return label; }
    public String getSubtitle() { return subtitle; }

    @Override
    public String toString() {
        return "[" + type.name() + "] " + label + (subtitle != null && !subtitle.isEmpty() ? " (" + subtitle + ")" : "");
    }
}
