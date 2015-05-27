package com.curleesoft.pickem.rest.dto;

import java.util.Date;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.SeasonWeek;

public class SeasonWeekDTO extends AbstractBaseDTO<SeasonWeek> implements DataTransferObject<SeasonWeek> {
	
	private Date beginDate;
	private Date endDate;
	private Long weekNumber;
	private NestedSeasonDTO season;
	
	public SeasonWeekDTO() {}
	
	public SeasonWeekDTO(final SeasonWeek entity) {
		super(entity);
		
		if (entity != null) {
			this.beginDate = entity.getBeginDate();
			this.endDate = entity.getEndDate();
			this.weekNumber = entity.getWeekNumber();
			this.season = new NestedSeasonDTO(entity.getSeason());
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

	public Long getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(Long weekNumber) {
		this.weekNumber = weekNumber;
	}

	public NestedSeasonDTO getSeason() {
		return season;
	}

	public void setSeason(NestedSeasonDTO season) {
		this.season = season;
	}
	
	public SeasonWeek fromDTO(SeasonWeek entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new SeasonWeek();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setBeginDate(this.beginDate);
		entity.setEndDate(this.endDate);
		entity.setWeekNumber(this.weekNumber);
		
		if (this.season != null) {
			entity.setSeason(this.season.fromDTO(entity.getSeason(), entityManager));
		}
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
