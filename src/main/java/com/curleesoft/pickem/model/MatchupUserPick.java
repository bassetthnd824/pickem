package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.Date;

import com.curleesoft.pickem.util.NativeQueryResultColumn;
import com.curleesoft.pickem.util.NativeQueryResultEntity;

@NativeQueryResultEntity
public class MatchupUserPick implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@NativeQueryResultColumn(index = 0)
	private Long matchupId;
	@NativeQueryResultColumn(index = 1)
	private Long seasonId;
	@NativeQueryResultColumn(index = 2)
	private String season;
	@NativeQueryResultColumn(index = 3)
	private Long seasonWeekId;
	@NativeQueryResultColumn(index = 4)
	private Long weekNumber;
	@NativeQueryResultColumn(index = 5)
	private Date weekBeginDate;
	@NativeQueryResultColumn(index = 6)
	private Date weekEndDate;
	@NativeQueryResultColumn(index = 7)
	private Date matchupDate;
	@NativeQueryResultColumn(index = 8)
	private Long awayTeamId;
	@NativeQueryResultColumn(index = 9)
	private String awayTeamName;
	@NativeQueryResultColumn(index = 10)
	private String awaySquadName;
	@NativeQueryResultColumn(index = 11)
	private Long awayTeamScore;
	@NativeQueryResultColumn(index = 12)
	private Long homeTeamId;
	@NativeQueryResultColumn(index = 13)
	private String homeTeamName;
	@NativeQueryResultColumn(index = 14)
	private String homeSquadName;
	@NativeQueryResultColumn(index = 15)
	private Long homeTeamScore;
	@NativeQueryResultColumn(index = 16)
	private String venueName;
	@NativeQueryResultColumn(index = 17)
	private String cityState;
	@NativeQueryResultColumn(index = 18)
	private String rivalryName;
	@NativeQueryResultColumn(index = 19)
	private Long userPickId;
	@NativeQueryResultColumn(index = 20)
	private Long userGuid;
	@NativeQueryResultColumn(index = 21)
	private String userId;
	@NativeQueryResultColumn(index = 22)
	private Long pickedTeamId;
	@NativeQueryResultColumn(index = 23)
	private Long rank;
	
	public MatchupUserPick() {}
	
	
	public String getAwaySquadName() {
		return this.awaySquadName;
	}
	
	public void setAwaySquadName(String awaySquadName) {
		this.awaySquadName = awaySquadName;
	}
	
	public Long getAwayTeamId() {
		return this.awayTeamId;
	}
	
	public void setAwayTeamId(Long awayTeamId) {
		this.awayTeamId = awayTeamId;
	}
	
	public String getAwayTeamName() {
		return this.awayTeamName;
	}
	
	public void setAwayTeamName(String awayTeamName) {
		this.awayTeamName = awayTeamName;
	}
	
	public Long getAwayTeamScore() {
		return this.awayTeamScore;
	}
	
	public void setAwayTeamScore(Long awayTeamScore) {
		this.awayTeamScore = awayTeamScore;
	}
	
	public String getCityState() {
		return this.cityState;
	}
	
	public void setCityState(String cityState) {
		this.cityState = cityState;
	}
	
	public String getHomeSquadName() {
		return this.homeSquadName;
	}
	
	public void setHomeSquadName(String homeSquadName) {
		this.homeSquadName = homeSquadName;
	}
	
	public Long getHomeTeamId() {
		return this.homeTeamId;
	}
	
	public void setHomeTeamId(Long homeTeamId) {
		this.homeTeamId = homeTeamId;
	}
	
	public String getHomeTeamName() {
		return this.homeTeamName;
	}
	
	public void setHomeTeamName(String homeTeamName) {
		this.homeTeamName = homeTeamName;
	}
	
	public Long getHomeTeamScore() {
		return this.homeTeamScore;
	}
	
	public void setHomeTeamScore(Long homeTeamScore) {
		this.homeTeamScore = homeTeamScore;
	}
	
	public Date getMatchupDate() {
		return this.matchupDate;
	}
	
	public void setMatchupDate(Date matchupDate) {
		this.matchupDate = matchupDate;
	}
	
	public Long getMatchupId() {
		return this.matchupId;
	}
	
	public void setMatchupId(Long matchupId) {
		this.matchupId = matchupId;
	}
	
	public Long getPickedTeamId() {
		return this.pickedTeamId;
	}
	
	public void setPickedTeamId(Long pickedTeamId) {
		this.pickedTeamId = pickedTeamId;
	}
	
	public Long getRank() {
		return this.rank;
	}
	
	public void setRank(Long rank) {
		this.rank = rank;
	}
	
	public String getRivalryName() {
		return this.rivalryName;
	}
	
	public void setRivalryName(String rivalryName) {
		this.rivalryName = rivalryName;
	}
	
	public String getSeason() {
		return this.season;
	}
	
	public void setSeason(String season) {
		this.season = season;
	}
	
	public Long getSeasonId() {
		return this.seasonId;
	}
	
	public void setSeasonId(Long seasonId) {
		this.seasonId = seasonId;
	}
	
	public Long getUserPickId() {
		return this.userPickId;
	}
	
	public void setUserPickId(Long userPickId) {
		this.userPickId = userPickId;
	}
	
	public String getVenueName() {
		return this.venueName;
	}
	
	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}
	
	public Long getSeasonWeekId() {
		return this.seasonWeekId;
	}
	
	public void setSeasonWeekId(Long seasonWeekId) {
		this.seasonWeekId = seasonWeekId;
	}
	
	public Date getWeekBeginDate() {
		return this.weekBeginDate;
	}
	
	public void setWeekBeginDate(Date weekBeginDate) {
		this.weekBeginDate = weekBeginDate;
	}
	
	public Date getWeekEndDate() {
		return this.weekEndDate;
	}
	
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}
	
	public Long getWeekNumber() {
		return this.weekNumber;
	}
	
	public void setWeekNumber(Long weekNumber) {
		this.weekNumber = weekNumber;
	}
	
	public Long getScore() {
		if (homeTeamScore != null && awayTeamScore != null) {
			if (pickedTeamId.equals(getWinningTeamId())) {
				return rank;
			}
		}
		
		return 0L;
	}
	
	public Long getWinningTeamId() {
		return (homeTeamScore == null || awayTeamScore == null) ? -1 : ((homeTeamScore.compareTo(awayTeamScore) > 0) ? homeTeamId : awayTeamId);
	}
	
	public boolean isCurrentOrPastWeek() {
		Date now = new Date();
//		Date now;
//		try {
//			now = new SimpleDateFormat("dd-MMM-yyyy").parse("08-Sep-2011");
//		} catch (ParseException e) {
//			now = new Date();
//		}
		return now.compareTo(weekBeginDate) >= 0;
	}

}
