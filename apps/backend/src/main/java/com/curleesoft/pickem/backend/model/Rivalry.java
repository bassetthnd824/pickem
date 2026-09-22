package com.curleesoft.pickem.backend.model;

import com.curleesoft.pickem.backend.model.snapshot.TeamNameSnapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code rivalries} document. Embeds both team snapshots.
 */
public class Rivalry extends AuditableDocument {

    @NotBlank
    @Size(max = 60)
    private String rivalryName;

    @NotBlank
    private String team1Id;

    @NotBlank
    private String team2Id;

    @NotNull
    @Valid
    private TeamNameSnapshot team1;

    @NotNull
    @Valid
    private TeamNameSnapshot team2;

    public String getRivalryName() {
        return rivalryName;
    }

    public void setRivalryName(String rivalryName) {
        this.rivalryName = rivalryName;
    }

    public String getTeam1Id() {
        return team1Id;
    }

    public void setTeam1Id(String team1Id) {
        this.team1Id = team1Id;
    }

    public String getTeam2Id() {
        return team2Id;
    }

    public void setTeam2Id(String team2Id) {
        this.team2Id = team2Id;
    }

    public TeamNameSnapshot getTeam1() {
        return team1;
    }

    public void setTeam1(TeamNameSnapshot team1) {
        this.team1 = team1;
    }

    public TeamNameSnapshot getTeam2() {
        return team2;
    }

    public void setTeam2(TeamNameSnapshot team2) {
        this.team2 = team2;
    }
}
