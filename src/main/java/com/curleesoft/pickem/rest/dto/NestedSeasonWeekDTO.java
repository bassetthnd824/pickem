package com.curleesoft.pickem.rest.dto;

import java.util.Date;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.SeasonWeek;

public class NestedSeasonWeekDTO extends AbstractBaseDTO<SeasonWeek> implements DataTransferObject<SeasonWeek> {
	
	private Date beginDate;
	private Date endDate;
	private Long weekNumber;
	
	public NestedSeasonWeekDTO() {}
	
	public NestedSeasonWeekDTO(final SeasonWeek entity) {
		super(entity);
		
		if (entity != null) {
			this.beginDate = entity.getBeginDate();
			this.endDate = entity.getEndDate();
			this.weekNumber = entity.getWeekNumber();
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

	public SeasonWeek fromDTO(SeasonWeek entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new SeasonWeek();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setBeginDate(this.beginDate);
		entity.setEndDate(this.endDate);
		entity.setWeekNumber(this.weekNumber);
		
		return entity;
	}
}
