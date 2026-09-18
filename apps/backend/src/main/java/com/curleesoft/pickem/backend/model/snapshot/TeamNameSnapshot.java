package com.curleesoft.pickem.backend.model.snapshot;

/**
 * Denormalized team fields embedded on rivalries.
 */
public class TeamNameSnapshot {

  private String id;
  private String name;

  public TeamNameSnapshot() {}

  public TeamNameSnapshot(String id, String name) {
    this.id = id;
    this.name = name;
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
}
