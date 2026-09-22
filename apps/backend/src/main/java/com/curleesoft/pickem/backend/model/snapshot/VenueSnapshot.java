package com.curleesoft.pickem.backend.model.snapshot;

/**
 * Denormalized venue fields embedded on teams and matchups.
 */
public class VenueSnapshot {

    private String id;
    private String name;
    private String cityState;

    public VenueSnapshot() {
    }

    public VenueSnapshot(String id, String name, String cityState) {
        this.id = id;
        this.name = name;
        this.cityState = cityState;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCityState() {
        return cityState;
    }

    public void setCityState(String cityState) {
        this.cityState = cityState;
    }
}
