package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Venue;

public class NestedVenueDTO extends AbstractBaseDTO implements DataTransferObject {
	
	private String cityState;
	private String venueName;
	
	public NestedVenueDTO() {}
	
	public NestedVenueDTO(final Venue entity) {
		super(entity);
		
		if (entity != null) {
			this.cityState = entity.getCityState();
			this.venueName = entity.getVenueName();
		}
	}

	public String getCityState() {
		return cityState;
	}

	public void setCityState(String cityState) {
		this.cityState = cityState;
	}

	public String getVenueName() {
		return venueName;
	}

	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}
	
	public Venue fromDTO(Venue entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Venue();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setCityState(this.cityState);
		entity.setVenueName(this.venueName);
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
