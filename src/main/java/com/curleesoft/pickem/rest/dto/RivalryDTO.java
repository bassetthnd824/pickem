package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Rivalry;

public class RivalryDTO extends AbstractBaseDTO<Rivalry> implements DataTransferObject<Rivalry> {
	
	private String rivalryName;
	private NestedTeamDTO team1;
	private NestedTeamDTO team2;
	
	public RivalryDTO() {}
	
	public RivalryDTO(final Rivalry entity) {
		super(entity);
		
		if (entity != null) {
			this.rivalryName = entity.getRivalryName();
			this.team1 = new NestedTeamDTO(entity.getTeam1());
			this.team2 = new NestedTeamDTO(entity.getTeam2());
		}
	}

	public String getRivalryName() {
		return rivalryName;
	}

	public void setRivalryName(String rivalryName) {
		this.rivalryName = rivalryName;
	}

	public NestedTeamDTO getTeam1() {
		return team1;
	}

	public void setTeam1(NestedTeamDTO team1) {
		this.team1 = team1;
	}

	public NestedTeamDTO getTeam2() {
		return team2;
	}

	public void setTeam2(NestedTeamDTO team2) {
		this.team2 = team2;
	}
	
	public Rivalry fromDTO(Rivalry entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Rivalry();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setRivalryName(this.rivalryName);
		
		if (this.team1 != null) {
			entity.setTeam1(this.team1.fromDTO(entity.getTeam1(), entityManager));
		}
		
		if (this.team2 != null) {
			entity.setTeam2(this.team2.fromDTO(entity.getTeam2(), entityManager));
		}
		
		return entity;
	}
}
