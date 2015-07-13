package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import com.curleesoft.pickem.model.constraints.ValidSeasonBeginDate;
import com.curleesoft.pickem.model.constraints.ValidSeasonEndDate;

/**
 * The persistent class for the PCKM_SEASON database table.
 * 
 */
@Entity
@Table(name = "PCKM_SEASON")
@NamedNativeQuery(name = "getCurrentSeason", query = "select * from pckm_season where current date between cast({fn TIMESTAMPADD(SQL_TSI_MONTH, -5, cast(begin_date as timestamp))} as date) and cast({fn TIMESTAMPADD(SQL_TSI_MONTH, 1, cast(end_date as timestamp))} as date)", resultClass = Season.class)
@ValidSeasonBeginDate
@ValidSeasonEndDate
public class Season extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private Date beginDate;
	private Date endDate;
	private String season;
	private Set<SeasonWeek> seasonWeeks;
	
	public Season() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_SEASON_ID_GENERATOR", sequenceName = "PCKM_SEASON_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_SEASON_ID_GENERATOR")
	@Column(name = "SEASON_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "BEGIN_DATE")
	@Temporal(TemporalType.DATE)
	public Date getBeginDate() {
		return this.beginDate;
	}
	
	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}
	
	@Column(name = "END_DATE")
	@Temporal(TemporalType.DATE)
	public Date getEndDate() {
		return this.endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	@Column(name = "SEASON", nullable = false, unique = true, length = 4)
	@NotNull(message = "season cannot be blank")
	public String getSeason() {
		return this.season;
	}
	
	public void setSeason(String season) {
		this.season = season;
	}
	
	// bi-directional many-to-one association to SeasonWeek
	@OneToMany(mappedBy = "season")
	public Set<SeasonWeek> getSeasonWeeks() {
		return this.seasonWeeks;
	}
	
	public void setSeasonWeeks(Set<SeasonWeek> seasonWeeks) {
		this.seasonWeeks = seasonWeeks;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Season that = (Season) o;

		if (season != null ? !season.equals(that.season) : that.season != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return season.hashCode();
	}

	@Override
	public String toString() {
		return season;
	}

}