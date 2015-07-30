package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.curleesoft.pickem.model.constraints.AssertMatchupTeamsNotEqual;
import com.curleesoft.pickem.model.constraints.ValidMatchupDate;

/**
 * The persistent class for the PCKM_MATCHUP database table.
 * 
 */
@Entity
@Table(name = "PCKM_MATCHUP")
@ValidMatchupDate
@AssertMatchupTeamsNotEqual
public class Matchup extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long awayTeamScore;
	private Long homeTeamScore;
	private Date matchupDate;
	private SeasonWeek seasonWeek;
	private Team homeTeam;
	private Team awayTeam;
	private Venue venue;
	private Set<UserPick> userPicks;
	
	public Matchup() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_MATCHUP_ID_GENERATOR", sequenceName = "PCKM_MATCHUP_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_MATCHUP_ID_GENERATOR")
	@Column(name = "MATCHUP_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "AWAY_TEAM_SCORE")
	public Long getAwayTeamScore() {
		return this.awayTeamScore;
	}
	
	public void setAwayTeamScore(Long awayTeamScore) {
		this.awayTeamScore = awayTeamScore;
	}
	
	@Column(name = "HOME_TEAM_SCORE")
	public Long getHomeTeamScore() {
		return this.homeTeamScore;
	}
	
	public void setHomeTeamScore(Long homeTeamScore) {
		this.homeTeamScore = homeTeamScore;
	}
	
	@Column(name = "MATCHUP_DATE", nullable = false)
	@Temporal(TemporalType.DATE)
	public Date getMatchupDate() {
		return this.matchupDate;
	}
	
	public void setMatchupDate(Date matchupDate) {
		this.matchupDate = matchupDate;
	}
	
	// bi-directional many-to-one association to SeasonWeek
	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "SEASON_WEEK_ID", nullable = false)
	public SeasonWeek getSeasonWeek() {
		return this.seasonWeek;
	}
	
	public void setSeasonWeek(SeasonWeek seasonWeek) {
		this.seasonWeek = seasonWeek;
	}
	
	// bi-directional many-to-one association to Team
	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "HOME_TEAM_ID", nullable = false)
	public Team getHomeTeam() {
		return this.homeTeam;
	}
	
	public void setHomeTeam(Team homeTeam) {
		this.homeTeam = homeTeam;
	}
	
	// bi-directional many-to-one association to Team
	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "AWAY_TEAM_ID", nullable = false)
	public Team getAwayTeam() {
		return this.awayTeam;
	}
	
	public void setAwayTeam(Team awayTeam) {
		this.awayTeam = awayTeam;
	}
	
	// bi-directional many-to-one association to Venue
	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "VENUE_ID", nullable = false)
	public Venue getVenue() {
		return this.venue;
	}
	
	public void setVenue(Venue venue) {
		this.venue = venue;
	}
	
	// bi-directional many-to-one association to UserPick
	@OneToMany(mappedBy = "matchup")
	public Set<UserPick> getUserPicks() {
		return this.userPicks;
	}
	
	public void setUserPicks(Set<UserPick> userPicks) {
		this.userPicks = userPicks;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Matchup that = (Matchup) o;

		if (seasonWeek != null ? !seasonWeek.equals(that.seasonWeek) : that.seasonWeek != null) {
			return false;
		}
		
		if (homeTeam != null ? !homeTeam.equals(that.homeTeam) : that.homeTeam != null) {
			return false;
		}
		
		if (awayTeam != null ? !awayTeam.equals(that.awayTeam) : that.awayTeam != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = seasonWeek != null ? seasonWeek.hashCode() : 0;
		result = 31 * result + (homeTeam != null ? homeTeam.hashCode() : 0);
		result = 31 * result + (awayTeam != null ? awayTeam.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return ((seasonWeek != null) ? seasonWeek.toString() : "") + " " + ((awayTeam != null) ? awayTeam.toString() : "") + " at " + ((homeTeam != null) ? homeTeam.toString() : "");
	}
	
	@Transient
	public Team getWinningTeam() {
		if (homeTeamScore.compareTo(awayTeamScore) > 0) {
			return homeTeam;
		} else {
			return awayTeam;
		}
	}
}