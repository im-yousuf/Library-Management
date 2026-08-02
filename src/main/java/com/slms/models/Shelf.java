package com.slms.models;

public class Shelf {
    private int shelfId;
    private int rackId;
    private String shelfName;
    private Rack parentRack; // optional reference

    public Shelf() {}

    public Shelf(int shelfId, int rackId, String shelfName) {
        this.shelfId = shelfId;
        this.rackId = rackId;
        this.shelfName = shelfName;
    }

    public int getShelfId() { return shelfId; }
    public void setShelfId(int shelfId) { this.shelfId = shelfId; }

    public int getRackId() { return rackId; }
    public void setRackId(int rackId) { this.rackId = rackId; }

    public String getShelfName() { return shelfName; }
    public void setShelfName(String shelfName) { this.shelfName = shelfName; }

    public Rack getParentRack() { return parentRack; }
    public void setParentRack(Rack parentRack) { this.parentRack = parentRack; }

    @Override
    public String toString() {
        if (parentRack != null) {
            return parentRack.getRackName() + " \u2192 " + shelfName;
        }
        return shelfName;
    }
}
