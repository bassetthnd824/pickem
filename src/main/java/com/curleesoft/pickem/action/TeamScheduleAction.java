package com.curleesoft.pickem.action;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.curleesoft.pickem.bean.MatchupBean;
import com.curleesoft.pickem.bean.TeamBean;
import com.curleesoft.pickem.form.TeamSchedule;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

public class TeamScheduleAction extends ActionSupport implements Preparable, ServletRequestAware {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private MatchupBean matchupBean;
	
	@Inject
	private TeamBean teamBean;
	
	private Long teamId;
	private List<Team> teams;
	private List<TeamSchedule> matchups;
	private HttpServletRequest request;
	
	@Override
	public String execute() throws Exception {
		return SUCCESS;
	}
	
	public String getSchedule() throws Exception {
		
		Season currentSeason = (Season) request.getSession(false).getAttribute(Globals.CURRENT_SEASON);
		
		matchups = matchupBean.getMatchupsBySeasonTeam(currentSeason.getId(), teamId);
		
		return SUCCESS;
	}
	
	public Long getTeamId() {
		return teamId;
	}
	
	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}
	
	public List<Team> getTeams() {
		return teams;
	}
	
	public List<TeamSchedule> getMatchups() {
		return matchups;
	}
	
	@Override
	public void prepare() throws Exception {
		teams = teamBean.getConferenceTeams();
	}
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
}
