package com.curleesoft.pickem.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.curleesoft.pickem.model.constraints.AssertRivalryTeamsNotEqual;

/**
 * The persistent class for the PCKM_RIVALRY database table.
 * 
 */
@Entity
@Table(name = "PCKM_RIVALRY")
@AssertRivalryTeamsNotEqual
public class Rivalry extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private String rivalryName;
	private Team team1;
	private Team team2;
	
	public Rivalry() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_RIVALRY_ID_GENERATOR", sequenceName = "PCKM_RIVALRY_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_RIVALRY_ID_GENERATOR")
	@Column(name = "RIVALRY_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "RIVALRY_NAME", nullable = false, length = 60)
	public String getRivalryName() {
		return this.rivalryName;
	}
	
	public void setRivalryName(String rivalryName) {
		this.rivalryName = rivalryName;
	}
	
	// bi-directional many-to-one association to Team
	@ManyToOne
	@JoinColumn(name = "TEAM_ID1", nullable = false)
	@Fetch(FetchMode.JOIN)
	public Team getTeam1() {
		return this.team1;
	}
	
	public void setTeam1(Team team1) {
		this.team1 = team1;
	}
	
	// bi-directional many-to-one association to Team
	@ManyToOne
	@JoinColumn(name = "TEAM_ID2", nullable = false)
	@Fetch(FetchMode.JOIN)
	public Team getTeam2() {
		return this.team2;
	}
	
	public void setTeam2(Team team2) {
		this.team2 = team2;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Rivalry that = (Rivalry) o;

		if (team1 != null ? !team1.equals(that.team1) : that.team1 != null) {
			return false;
		}
		
		if (team2 != null ? !team2.equals(that.team2) : that.team2 != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = team1 != null ? team1.hashCode() : 0;
		result = 31 * result + (team2 != null ? team2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return team1.toString() + " vs. " + team2.toString() + " " + StringUtils.defaultString(rivalryName);
	}
	
}