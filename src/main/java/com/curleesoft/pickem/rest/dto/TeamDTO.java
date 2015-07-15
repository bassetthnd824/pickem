package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Team;

public class TeamDTO extends AbstractBaseDTO<Team> implements DataTransferObject<Team> {
	
	private boolean conferenceMember;
	private String squadName;
	private String teamName;
	private NestedVenueDTO homeVenue;
	
	public TeamDTO() {}
	
	public TeamDTO(final Team entity) {
		super(entity);
		
		if (entity != null) {
			this.conferenceMember = entity.getConferenceMember();
			this.squadName = entity.getSquadName();
			this.teamName = entity.getTeamName();
			this.homeVenue = new NestedVenueDTO(entity.getHomeVenue());
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

	public Team fromDTO(Team entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Team();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setConferenceMember(this.conferenceMember);
		entity.setSquadName(this.squadName);
		entity.setTeamName(this.teamName);
		
		if (this.homeVenue != null) {
			entity.setHomeVenue(this.homeVenue.fromDTO(entity.getHomeVenue(), entityManager));
		}
		
		return entity;
	}
}
