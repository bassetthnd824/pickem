package com.curleesoft.pickem.backend.model.snapshot;

/**
 * Denormalized team fields embedded on matchups.
 */
public class TeamSquadSnapshot {

    private String id;
    private String name;
    private String squad;

    public TeamSquadSnapshot() {
    }

    public TeamSquadSnapshot(String id, String name, String squad) {
        this.id = id;
        this.name = name;
        this.squad = squad;
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

    public String getSquad() {
        return squad;
    }

    public void setSquad(String squad) {
        this.squad = squad;
    }
}
