package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.UserPick;

public class UserPickDTO extends AbstractBaseDTO<UserPick> implements DataTransferObject<UserPick> {
	
	private Long rank;
	private NestedTeamDTO pickedTeam;
	
	public UserPickDTO() {}
	
	public UserPickDTO(final UserPick entity) {
		super(entity);
		
		if (entity != null) {
			this.rank = entity.getRank();
			this.pickedTeam = new NestedTeamDTO(entity.getPickedTeam());
		}
	}

	public Long getRank() {
		return rank;
	}

	public void setRank(Long rank) {
		this.rank = rank;
	}

	public NestedTeamDTO getPickedTeam() {
		return pickedTeam;
	}

	public void setPickedTeam(NestedTeamDTO pickedTeam) {
		this.pickedTeam = pickedTeam;
	}
	
	public UserPick fromDTO(UserPick entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new UserPick();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setRank(this.rank);
		
		if (this.pickedTeam != null) {
			entity.setPickedTeam(this.pickedTeam.fromDTO(entity.getPickedTeam(), entityManager));
		}
		
		return entity;
	}
}
