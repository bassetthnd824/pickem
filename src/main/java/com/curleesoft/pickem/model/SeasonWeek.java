package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.curleesoft.pickem.model.constraints.ValidSeasonWeekBeginDate;
import com.curleesoft.pickem.model.constraints.ValidSeasonWeekEndDate;

/**
 * The persistent class for the PCKM_SEASON_WEEK database table.
 * 
 */
@Entity
@Table(name = "PCKM_SEASON_WEEK")
@ValidSeasonWeekBeginDate
@ValidSeasonWeekEndDate
public class SeasonWeek extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private Date beginDate;
	private Date endDate;
	private Long weekNumber;
	private Season season;
	
	public SeasonWeek() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_SEASON_WEEK_ID_GENERATOR", sequenceName = "PCKM_SEASON_WEEK_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_SEASON_WEEK_ID_GENERATOR")
	@Column(name = "SEASON_WEEK_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "BEGIN_DATE", nullable = false)
	@Temporal(TemporalType.DATE)
	public Date getBeginDate() {
		return this.beginDate;
	}
	
	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}
	
	@Column(name = "END_DATE", nullable = false)
	@Temporal(TemporalType.DATE)
	public Date getEndDate() {
		return this.endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	@Column(name = "WEEK_NUMBER", nullable = false)
	public Long getWeekNumber() {
		return this.weekNumber;
	}
	
	public void setWeekNumber(Long weekNumber) {
		this.weekNumber = weekNumber;
	}
	
	// bi-directional many-to-one association to Season
	@ManyToOne
	@JoinColumn(name = "SEASON_ID", nullable = false)
	public Season getSeason() {
		return this.season;
	}
	
	public void setSeason(Season season) {
		this.season = season;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SeasonWeek that = (SeasonWeek) o;

		if (season != null ? !season.equals(that.season) : that.season != null) {
			return false;
		}
		
		if (weekNumber != null ? !weekNumber.equals(that.weekNumber) : that.weekNumber != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = season != null ? season.hashCode() : 0;
		result = 31 * result + (weekNumber != null ? weekNumber.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return season.toString() + " Week " + weekNumber.toString();
	}
	
}