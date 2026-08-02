package com.slms.models;

import java.util.ArrayList;
import java.util.List;

public class Rack {
    private int rackId;
    private String rackName;
    private List<Shelf> shelves = new ArrayList<>();

    public Rack() {}

    public Rack(int rackId, String rackName) {
        this.rackId = rackId;
        this.rackName = rackName;
    }

    public int getRackId() { return rackId; }
    public void setRackId(int rackId) { this.rackId = rackId; }

    public String getRackName() { return rackName; }
    public void setRackName(String rackName) { this.rackName = rackName; }

    public List<Shelf> getShelves() { return shelves; }
    public void setShelves(List<Shelf> shelves) { this.shelves = shelves; }

    @Override
    public String toString() { return rackName; }
}
