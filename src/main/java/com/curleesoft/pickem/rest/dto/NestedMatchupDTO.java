package com.curleesoft.pickem.rest.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.UserPick;

public class NestedMatchupDTO extends AbstractBaseDTO<Matchup> implements DataTransferObject<Matchup> {
	
	private Long awayTeamScore;
	private Long homeTeamScore;
	private Date matchupDate;
	private NestedTeamDTO homeTeam;
	private NestedTeamDTO awayTeam;
	private NestedVenueDTO venue;
	private Set<UserPickDTO> userPicks;
	
	public NestedMatchupDTO() {}
	
	public NestedMatchupDTO(final Matchup entity) {
		super(entity);
		
		if (entity != null) {
			this.awayTeamScore = entity.getAwayTeamScore();
			this.homeTeamScore = entity.getHomeTeamScore();
			this.matchupDate = entity.getMatchupDate();
			this.homeTeam = new NestedTeamDTO(entity.getHomeTeam());
			this.awayTeam = new NestedTeamDTO(entity.getAwayTeam());
			this.venue = new NestedVenueDTO(entity.getVenue());
			this.userPicks = new HashSet<UserPickDTO>();
			
			for (UserPick userPick : entity.getUserPicks()) {
				this.userPicks.add(new UserPickDTO(userPick));
			}
		}
	}

	public Long getAwayTeamScore() {
		return awayTeamScore;
	}

	public void setAwayTeamScore(Long awayTeamScore) {
		this.awayTeamScore = awayTeamScore;
	}

	public Long getHomeTeamScore() {
		return homeTeamScore;
	}

	public void setHomeTeamScore(Long homeTeamScore) {
		this.homeTeamScore = homeTeamScore;
	}

	public Date getMatchupDate() {
		return matchupDate;
	}

	public void setMatchupDate(Date matchupDate) {
		this.matchupDate = matchupDate;
	}
	
	public NestedTeamDTO getHomeTeam() {
		return homeTeam;
	}

	public void setHomeTeam(NestedTeamDTO homeTeam) {
		this.homeTeam = homeTeam;
	}

	public NestedTeamDTO getAwayTeam() {
		return awayTeam;
	}

	public void setAwayTeam(NestedTeamDTO awayTeam) {
		this.awayTeam = awayTeam;
	}

	public NestedVenueDTO getVenue() {
		return venue;
	}

	public void setVenue(NestedVenueDTO venue) {
		this.venue = venue;
	}

	public Set<UserPickDTO> getUserPicks() {
		return userPicks;
	}

	public void setUserPicks(Set<UserPickDTO> userPicks) {
		this.userPicks = userPicks;
	}
	
	public Matchup fromDTO(Matchup entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Matchup();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, UserPick.class, "userPicks", entityManager);
		
		entity.setAwayTeamScore(this.awayTeamScore);
		entity.setHomeTeamScore(this.homeTeamScore);
		entity.setMatchupDate(this.matchupDate);
		
		if (this.homeTeam != null) {
			entity.setHomeTeam(this.homeTeam.fromDTO(entity.getHomeTeam(), entityManager));
		}
		
		if (this.awayTeam != null) {
			entity.setAwayTeam(this.awayTeam.fromDTO(entity.getAwayTeam(), entityManager));
		}
		
		if (this.venue != null) {
			entity.setVenue(this.venue.fromDTO(entity.getVenue(), entityManager));
		}
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
