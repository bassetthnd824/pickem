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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * The persistent class for the PCKM_USER_PICK database table.
 * 
 */
@Entity
@Table(name = "PCKM_USER_PICK")
public class UserPick extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long rank;
	private Matchup matchup;
	private Team pickedTeam;
	private User user;
	
	public UserPick() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_USER_PICK_ID_GENERATOR", sequenceName = "PCKM_USER_PICK_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_USER_PICK_ID_GENERATOR")
	@Column(name = "USER_PICK_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(nullable = false)
	public Long getRank() {
		return this.rank;
	}
	
	public void setRank(Long rank) {
		this.rank = rank;
	}
	
	// bi-directional many-to-one association to Matchup
	@ManyToOne
	@JoinColumn(name = "MATCHUP_ID", nullable = false)
	@Fetch(FetchMode.JOIN)
	public Matchup getMatchup() {
		return this.matchup;
	}
	
	public void setMatchup(Matchup matchup) {
		this.matchup = matchup;
	}
	
	// uni-directional many-to-one association to Team
	@ManyToOne
	@JoinColumn(name = "PICKED_TEAM_ID", nullable = false)
	@Fetch(FetchMode.JOIN)
	public Team getPickedTeam() {
		return this.pickedTeam;
	}
	
	public void setPickedTeam(Team pickedTeam) {
		this.pickedTeam = pickedTeam;
	}
	
	// bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name = "USER_GUID", nullable = false)
	@Fetch(FetchMode.JOIN)
	public User getUser() {
		return this.user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UserPick that = (UserPick) o;

		if (matchup != null ? !matchup.equals(that.matchup) : that.matchup != null) {
			return false;
		}
		
		if (user != null ? !user.equals(that.user) : that.user != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = matchup != null ? matchup.hashCode() : 0;
		result = 31 * result + (user != null ? user.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return ((matchup != null) ? matchup.toString() : "") + " user: " + ((user != null) ? user.toString() : "");
	}
	
}