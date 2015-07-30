package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.Type;

/**
 * The persistent class for the PCKM_TEAM database table.
 * 
 */
@Entity
@Table(name = "PCKM_TEAM")
@NamedNativeQueries({
	@NamedNativeQuery(name = "matchups", query = "SELECT MATCHUP_ID, SEASON_WEEK_ID, VENUE_ID, MATCHUP_DATE, HOME_TEAM_ID, AWAY_TEAM_ID, HOME_TEAM_SCORE, AWAY_TEAM_SCORE, LAST_UPDATE_DATE, LAST_UPDATE_USER, CREATE_DATE, CREATE_USER, SYS_MOD_COUNT FROM PCKM_MATCHUP WHERE HOME_TEAM_ID = ? OR AWAY_TEAM_ID = ?", resultClass = Matchup.class),
	@NamedNativeQuery(name = "rivalries", query = "SELECT RIVALRY_ID, RIVALRY_NAME, TEAM_ID1, TEAM_ID2, LAST_UPDATE_DATE, LAST_UPDATE_USER, CREATE_DATE, CREATE_USER, SYS_MOD_COUNT FROM PCKM_RIVALRY WHERE TEAM_ID1 = ? OR TEAM_ID2 = ?", resultClass = Rivalry.class)
})
public class Team extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private boolean conferenceMember;
	private String squadName;
	private String teamName;
	private Venue homeVenue;
	private Set<Rivalry> rivalries;
	
	public Team() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_TEAM_ID_GENERATOR", sequenceName = "PCKM_TEAM_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_TEAM_ID_GENERATOR")
	@Column(name = "TEAM_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "CONFERENCE_MEMBER", nullable = false, length = 1)
	@Type(type = "yes_no")
	public boolean getConferenceMember() {
		return this.conferenceMember;
	}
	
	public void setConferenceMember(boolean conferenceMember) {
		this.conferenceMember = conferenceMember;
	}
	
	@Column(name = "SQUAD_NAME", nullable = false, length = 40)
	@NotNull(message = "squad name cannot be blank")
	public String getSquadName() {
		return this.squadName;
	}
	
	public void setSquadName(String squadName) {
		this.squadName = squadName;
	}
	
	@Column(name = "TEAM_NAME", nullable = false, unique = true, length = 40)
	@NotNull(message = "team name cannot be blank")
	public String getTeamName() {
		return this.teamName;
	}
	
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	
	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "HOME_VENUE_ID", nullable = false)
	@NotNull(message = "home venue cannot be blank")
	public Venue getHomeVenue() {
		return homeVenue;
	}

	public void setHomeVenue(Venue homeVenue) {
		this.homeVenue = homeVenue;
	}

	// bi-directional many-to-one association to Rivalry
	@OneToMany
	@JoinColumns({
		@JoinColumn(name = "TEAM_ID1", referencedColumnName = "TEAM_ID"),
		@JoinColumn(name = "TEAM_ID2", referencedColumnName = "TEAM_ID")
	})
	@Loader(namedQuery = "rivalries")
	public Set<Rivalry> getRivalries() {
		return this.rivalries;
	}
	
	public void setRivalries(Set<Rivalry> rivalries) {
		this.rivalries = rivalries;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Team that = (Team) o;

		if (teamName != null ? !teamName.equals(that.teamName) : that.teamName != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return StringUtils.defaultString(teamName).hashCode();
	}

	@Override
	public String toString() {
		return StringUtils.defaultString(teamName) + " " + StringUtils.defaultString(squadName);
	}
	
}