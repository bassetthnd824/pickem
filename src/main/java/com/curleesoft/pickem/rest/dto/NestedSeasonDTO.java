package com.curleesoft.pickem.rest.dto;

import java.util.Date;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Season;

public class NestedSeasonDTO extends AbstractBaseDTO<Season> implements DataTransferObject<Season> {
	
	private Date beginDate;
	private Date endDate;
	private String season;
	
	public NestedSeasonDTO() {}
	
	public NestedSeasonDTO(final Season entity) {
		super(entity);
		
		if (entity != null) {
			this.beginDate = entity.getBeginDate();
			this.endDate = entity.getEndDate();
			this.season = entity.getSeason();
		}
	}

	public Date getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getSeason() {
		return season;
	}

	public void setSeason(String season) {
		this.season = season;
	}
	
	public Season fromDTO(Season entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Season();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setBeginDate(this.beginDate);
		entity.setEndDate(this.endDate);
		entity.setSeason(this.season);
		
		return entity;

	}
}
