package com.curleesoft.pickem.rest.dto;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.Rivalry;
import com.curleesoft.pickem.model.Team;

public class TeamDTO extends AbstractBaseDTO<Team> implements DataTransferObject<Team> {
	
	private boolean conferenceMember;
	private String squadName;
	private String teamName;
	private NestedVenueDTO homeVenue;
	private Set<MatchupDTO> matchups;
	private Set<NestedRivalryDTO> rivalries;
	
	public TeamDTO() {}
	
	public TeamDTO(final Team entity) {
		super(entity);
		
		if (entity != null) {
			this.conferenceMember = entity.getConferenceMember();
			this.squadName = entity.getSquadName();
			this.teamName = entity.getTeamName();
			this.homeVenue = new NestedVenueDTO(entity.getHomeVenue());
			this.matchups = new HashSet<MatchupDTO>();
			this.rivalries = new HashSet<NestedRivalryDTO>();
			
			for (Matchup matchup : entity.getMatchups()) {
				this.matchups.add(new MatchupDTO(matchup, this.getId()));
			}
			
			for (Rivalry rivalry : entity.getRivalries()) {
				this.rivalries.add(new NestedRivalryDTO(rivalry));
			}
		}
	}

	public boolean isConferenceMember() {
		return conferenceMember;
	}

	public void setConferenceMember(boolean conferenceMember) {
		this.conferenceMember = conferenceMember;
	}

	public String getSquadName() {
		return squadName;
	}

	public void setSquadName(String squadName) {
		this.squadName = squadName;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public NestedVenueDTO getHomeVenue() {
		return homeVenue;
	}

	public void setHomeVenue(NestedVenueDTO homeVenue) {
		this.homeVenue = homeVenue;
	}

	public Set<MatchupDTO> getMatchups() {
		return matchups;
	}

	public void setMatchups(Set<MatchupDTO> matchups) {
		this.matchups = matchups;
	}

	public Set<NestedRivalryDTO> getRivalries() {
		return rivalries;
	}

	public void setRivalries(Set<NestedRivalryDTO> rivalries) {
		this.rivalries = rivalries;
	}
	
	public Team fromDTO(Team entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Team();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, Matchup.class, "matchups", entityManager);
		populateEntityCollection(entity, Rivalry.class, "rivalries", entityManager);
		
		entity.setConferenceMember(this.conferenceMember);
		entity.setSquadName(this.squadName);
		entity.setTeamName(this.teamName);
		
		if (this.homeVenue != null) {
			entity.setHomeVenue(this.homeVenue.fromDTO(entity.getHomeVenue(), entityManager));
		}
		
		return entity;
	}
}
