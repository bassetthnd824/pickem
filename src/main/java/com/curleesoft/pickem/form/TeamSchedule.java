package com.curleesoft.pickem.form;

import java.util.Date;

public class TeamSchedule {
	
	private Date matchupDate;
	private String opponentName;
	private String scoreResult;
	private String winLoss;
	
	public Date getMatchupDate() {
		return matchupDate;
	}
	
	public void setMatchupDate(Date matchupDate) {
		this.matchupDate = matchupDate;
	}
	
	public String getOpponentName() {
		return opponentName;
	}
	
	public void setOpponentName(String opponentName) {
		this.opponentName = opponentName;
	}
	
	public String getScoreResult() {
		return scoreResult;
	}
	
	public void setScoreResult(String scoreResult) {
		this.scoreResult = scoreResult;
	}
	
	public String getWinLoss() {
		return winLoss;
	}
	
	public void setWinLoss(String winLoss) {
		this.winLoss = winLoss;
	}
}
