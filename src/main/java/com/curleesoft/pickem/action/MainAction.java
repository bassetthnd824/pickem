package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.curleesoft.pickem.bean.MatchupBean;
import com.curleesoft.pickem.bean.SeasonWeekBean;
import com.curleesoft.pickem.bean.TeamBean;
import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.bean.UserPickBean;
import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.MatchupUserPick;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserPick;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class MainAction extends ActionSupport implements Action, ModelDriven<List<UserPick>>, Preparable, ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	@Inject
	private SeasonWeekBean seasonWeekBean;
	
	@Inject
	private MatchupBean matchupBean;
	
	@Inject
	private TeamBean teamBean;
	
	@Inject
	private UserBean userBean;
	
	@Inject
	private UserPickBean userPickBean;
	
	private List<UserPick> userPicks;
	private List<SeasonWeek> seasonWeeks;
	private Map<Long, List<MatchupUserPick>> matchupUserPicks;
	private Long numberOfConferenceTeams;
	
	private HttpServletRequest request;

	public String getWelcomeMessage() {
		return "Welcome " + ((User) request.getSession(false).getAttribute(Globals.ACTIVE_USER)).getFirstName() + 
				", Season " + ((Season) request.getSession(false).getAttribute(Globals.CURRENT_SEASON)).getSeason();
	}
	
	@Override
	public String execute() throws Exception {
		User currentUser = (User) request.getSession(false).getAttribute(Globals.ACTIVE_USER);
		Season currentSeason = (Season) request.getSession(false).getAttribute(Globals.CURRENT_SEASON);
		
		seasonWeeks = seasonWeekBean.getSeasonWeeksBySeason(currentSeason.getId());
		
		this.matchupUserPicks = new HashMap<Long, List<MatchupUserPick>>();
		List<MatchupUserPick> matchupUserPicks = matchupBean.getMatchupUserPicksByUserSeason(currentUser.getId(), currentSeason.getId());
		
		for (MatchupUserPick matchupUserPick : matchupUserPicks) {
			List<MatchupUserPick> matchupUserPickList = this.matchupUserPicks.get(matchupUserPick.getSeasonWeekId());
			
			if (matchupUserPickList == null) {
				matchupUserPickList = new ArrayList<MatchupUserPick>();
				this.matchupUserPicks.put(matchupUserPick.getSeasonWeekId(), matchupUserPickList);
			}
			
			matchupUserPickList.add(matchupUserPick);
		}
		
		numberOfConferenceTeams = teamBean.getNumberOfConferenceTeams();
		
		return SUCCESS;
	}
	
	public String save() throws Exception {
		Date now = new Date();
		User currentUser = (User) request.getSession(false).getAttribute(Globals.ACTIVE_USER);
		
		for (UserPick userPick : userPicks) {
			userPick.setUser(userBean.findById(currentUser.getId(), false));
			userPick.setMatchup(matchupBean.findById(userPick.getMatchup().getId(), false));
			userPick.setPickedTeam(teamBean.findById(userPick.getPickedTeam().getId(), false));
			userPick.setLastUpdateDate(now);
			userPick.setLastUpdateUser(request.getUserPrincipal().getName());

			if (userPick.getId() == null) {
				userPick.setCreateDate(now);
				userPick.setCreateUser(request.getUserPrincipal().getName());
			}
		}
		
		userPicks = userPickBean.persistList(userPicks);
		
		return SUCCESS;
	}
	
	public List<SeasonWeek> getSeasonWeeks() {
		return seasonWeeks;
	}
	
	public Map<Long, List<MatchupUserPick>> getMatchupUserPicks() {
		return matchupUserPicks;
	}
	
	public Long getNumberOfConferenceTeams() {
		return numberOfConferenceTeams;
	}
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public List<UserPick> getModel() {
		return userPicks;
	}

	@Override
	public void prepare() throws Exception {}
	
	public void prepareSave() throws Exception {
		int rowCount = getRowCount();
		userPicks = new ArrayList<UserPick>(rowCount);
		
		for (UserPick userPick : userPicks) {
			userPick = new UserPick();
			userPick.setMatchup(new Matchup());
			userPick.setPickedTeam(new Team());
		}
	}
	
	private int getRowCount() {
		int count = 0;
		Pattern p = Pattern.compile("model\\[[0-9]*\\]\\.matchup.id");
		
		for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
			String parameterName = e.nextElement();
			Matcher m = p.matcher(parameterName);
			
			if (m.matches()) {
				count++;
			}
		}
		
		return count;
	}
}
