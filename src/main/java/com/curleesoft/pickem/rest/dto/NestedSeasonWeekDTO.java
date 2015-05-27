package com.curleesoft.pickem.rest.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.SeasonWeek;

public class NestedSeasonWeekDTO extends AbstractBaseDTO<SeasonWeek> implements DataTransferObject<SeasonWeek> {
	
	private Date beginDate;
	private Date endDate;
	private Long weekNumber;
	private Set<NestedMatchupDTO> matchups;
	
	public NestedSeasonWeekDTO() {}
	
	public NestedSeasonWeekDTO(final SeasonWeek entity) {
		super(entity);
		
		if (entity != null) {
			this.beginDate = entity.getBeginDate();
			this.endDate = entity.getEndDate();
			this.weekNumber = entity.getWeekNumber();
			this.matchups = new HashSet<NestedMatchupDTO>();
			
			for (Matchup matchup : entity.getMatchups()) {
				this.matchups.add(new NestedMatchupDTO(matchup));
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

	public Long getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(Long weekNumber) {
		this.weekNumber = weekNumber;
	}

	public Set<NestedMatchupDTO> getMatchups() {
		return matchups;
	}

	public void setMatchups(Set<NestedMatchupDTO> matchups) {
		this.matchups = matchups;
	}
	
	public SeasonWeek fromDTO(SeasonWeek entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new SeasonWeek();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, Matchup.class, "matchups", entityManager);
		
		entity.setBeginDate(this.beginDate);
		entity.setEndDate(this.endDate);
		entity.setWeekNumber(this.weekNumber);
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
