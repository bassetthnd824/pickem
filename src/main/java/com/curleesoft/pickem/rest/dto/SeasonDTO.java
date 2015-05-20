package com.curleesoft.pickem.rest.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;

public class SeasonDTO extends AbstractBaseDTO implements DataTransferObject {
	
	private Date beginDate;
	private Date endDate;
	private String season;
	private Set<NestedSeasonWeekDTO> seasonWeeks;
	
	public SeasonDTO() {}
	
	public SeasonDTO(final Season entity) {
		super(entity);
		
		if (entity != null) {
			this.beginDate = entity.getBeginDate();
			this.endDate = entity.getEndDate();
			this.season = entity.getSeason();
			this.seasonWeeks = new HashSet<NestedSeasonWeekDTO>();
			
			for (SeasonWeek seasonWeek : entity.getSeasonWeeks()) {
				this.seasonWeeks.add(new NestedSeasonWeekDTO(seasonWeek));
			}
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

	public Set<NestedSeasonWeekDTO> getSeasonWeeks() {
		return seasonWeeks;
	}

	public void setSeasonWeeks(Set<NestedSeasonWeekDTO> seasonWeeks) {
		this.seasonWeeks = seasonWeeks;
	}
	
	public Season fromDTO(Season entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Season();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, SeasonWeek.class, "seasonWeeks", entityManager);
		
		entity.setBeginDate(this.beginDate);
		entity.setEndDate(this.endDate);
		entity.setSeason(this.season);
		
		entity = entityManager.merge(entity);
		return entity;

	}
}
