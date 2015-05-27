package com.curleesoft.pickem.rest.dto;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.UserPick;

public class MatchupDTO extends AbstractBaseDTO<Matchup> implements DataTransferObject<Matchup> {
	
	private Long awayTeamScore;
	private Long homeTeamScore;
	private Date matchupDate;
	private SeasonWeekDTO seasonWeek;
	private NestedTeamDTO otherTeam;
	private NestedVenueDTO venue;
	private Set<UserPickDTO> userPicks;
	
	public MatchupDTO() {}
	
	public MatchupDTO(final Matchup entity, Long parentTeamId) {
		super(entity);
		
		if (entity != null) {
			this.awayTeamScore = entity.getAwayTeamScore();
			this.homeTeamScore = entity.getHomeTeamScore();
			this.matchupDate = entity.getMatchupDate();
			this.otherTeam = (entity.getHomeTeam() != null) ? (entity.getHomeTeam().getId().equals(parentTeamId)) ? new NestedTeamDTO(entity.getHomeTeam()) : new NestedTeamDTO(entity.getAwayTeam()) : null;
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

	public SeasonWeekDTO getSeasonWeek() {
		return seasonWeek;
	}

	public void setSeasonWeek(SeasonWeekDTO seasonWeek) {
		this.seasonWeek = seasonWeek;
	}

	public NestedTeamDTO getOtherTeam() {
		return otherTeam;
	}

	public void setOtherTeam(NestedTeamDTO otherTeam) {
		this.otherTeam = otherTeam;
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
	
	public Matchup fromDTO(Matchup entity, Long parentTeamId, EntityManager entityManager) {
		if (entity == null) {
			entity = new Matchup();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, UserPick.class, "userPicks", entityManager);
		
		entity.setAwayTeamScore(this.awayTeamScore);
		entity.setHomeTeamScore(this.homeTeamScore);
		entity.setMatchupDate(this.matchupDate);
		
		if (this.otherTeam != null) {
			if (entity.getHomeTeam() != null) {
				if (parentTeamId.equals(entity.getHomeTeam().getId())) {
					entity.setAwayTeam(this.otherTeam.fromDTO(entity.getAwayTeam(), entityManager));
				} else {
					entity.setHomeTeam(this.otherTeam.fromDTO(entity.getHomeTeam(), entityManager));
				}
			}
		}
		
		if (this.venue != null) {
			entity.setVenue(this.venue.fromDTO(entity.getVenue(), entityManager));
		}
		
		return entity;
	}
}
