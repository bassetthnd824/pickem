package com.curleesoft.pickem.action;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.validation.SkipValidation;

import com.curleesoft.pickem.bean.MatchupBean;
import com.curleesoft.pickem.form.UserScore;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.ActionSupport;

public class LeaderBoardAction extends ActionSupport implements ServletRequestAware {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private MatchupBean matchupBean;
	
	private List<UserScore> userScores;
	private HttpServletRequest request;
	
	@Override
	@SkipValidation
	public String execute() throws Exception {
		Season currentSeason = (Season) request.getSession(false).getAttribute(Globals.CURRENT_SEASON);
		
		userScores = matchupBean.getLeaderBoardForSeason(currentSeason.getId());
		return super.execute();
	}
	
	public List<UserScore> getUserScores() {
		return userScores;
	}
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
}
