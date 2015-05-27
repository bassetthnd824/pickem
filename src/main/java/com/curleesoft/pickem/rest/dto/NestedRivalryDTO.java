package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Rivalry;

public class NestedRivalryDTO extends AbstractBaseDTO<Rivalry> implements DataTransferObject<Rivalry> {
	
	private String rivalryName;
	
	public NestedRivalryDTO() {}
	
	public NestedRivalryDTO(final Rivalry entity) {
		super(entity);
		
		if (entity != null) {
			this.rivalryName = entity.getRivalryName();
		}
	}

	public String getRivalryName() {
		return rivalryName;
	}

	public void setRivalryName(String rivalryName) {
		this.rivalryName = rivalryName;
	}
	
	public Rivalry fromDTO(Rivalry entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Rivalry();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setRivalryName(this.rivalryName);
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
