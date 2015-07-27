package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.MatchupBean;
import com.curleesoft.pickem.bean.SeasonBean;
import com.curleesoft.pickem.bean.SeasonWeekBean;
import com.curleesoft.pickem.bean.TeamBean;
import com.curleesoft.pickem.bean.VenueBean;
import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Venue;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class MatchupAction extends BaseAction<Matchup, Long, MatchupBean> implements ModelDriven<Matchup>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private MatchupBean matchupBean;
	
	@Inject
	private SeasonBean seasonBean;
	
	@Inject
	private SeasonWeekBean seasonWeekBean;
	
	@Inject
	private TeamBean teamBean;
	
	@Inject
	private VenueBean venueBean;
	
	private Matchup matchup;
	private List<Matchup> matchups;
	private List<Season> seasons;
	private List<SeasonWeek> seasonWeeks;
	private List<Team> teams;
	private List<Venue> venues;
	
	@Override
	public void prepare() throws Exception {
		seasons = seasonBean.findAll();
		seasonWeeks = new ArrayList<SeasonWeek>();
		teams = teamBean.findAll();
		venues = venueBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		matchups = new ArrayList<Matchup>();
	}
	
	public void prepareSearch() throws Exception {
		matchup = new Matchup();
		matchup.setSeasonWeek(new SeasonWeek());
		matchup.getSeasonWeek().setSeason(new Season());
		matchup.setHomeTeam(new Team());
		matchup.setAwayTeam(new Team());
		matchup.setVenue(new Venue());
		matchups = new ArrayList<Matchup>();
	}
	
	public void prepareAdd() throws Exception {
		matchup = new Matchup();
		matchup.setSeasonWeek(new SeasonWeek());
		matchup.getSeasonWeek().setSeason(new Season());
		matchup.setHomeTeam(new Team());
		matchup.setAwayTeam(new Team());
		matchup.setVenue(new Venue());
	}
	
	public void prepareSave() throws Exception {
		matchup = new Matchup();
		matchup.setSeasonWeek(new SeasonWeek());
		matchup.getSeasonWeek().setSeason(new Season());
		matchup.setHomeTeam(new Team());
		matchup.setAwayTeam(new Team());
		matchup.setVenue(new Venue());
	}
	
	@Override
	protected MatchupBean getBean() {
		return matchupBean;
	}

	@Override
	public Matchup getModel() {
		return matchup;
	}

	@Override
	public void setModel(Matchup venue) {
		this.matchup = venue;
	}

	@Override
	public List<Matchup> getModelList() {
		return matchups;
	}

	@Override
	public void setModelList(List<Matchup> modelList) {
		this.matchups = modelList;
	}
	
	public List<Season> getSeasons() {
		return seasons;
	}
	
	public List<SeasonWeek> getSeasonWeeks() {
		return seasonWeeks;
	}
	
	public List<Team> getTeams() {
		return teams;
	}
	
	public List<Venue> getVenues() {
		return venues;
	}
	
	@Override
	protected void setExistingModelFields(Matchup existingModel, Matchup model) {
		existingModel.setSeasonWeek(seasonWeekBean.findById(model.getSeasonWeek().getId(), false));
		existingModel.setMatchupDate(model.getMatchupDate());
		existingModel.setHomeTeam(teamBean.findById(model.getHomeTeam().getId(), false));
		existingModel.setHomeTeamScore(model.getHomeTeamScore());
		existingModel.setAwayTeam(teamBean.findById(model.getAwayTeam().getId(), false));
		existingModel.setAwayTeamScore(model.getAwayTeamScore());
		existingModel.setVenue(venueBean.findById(model.getVenue().getId(), false));
	}
	
	@Override
	protected void setParentEntities(Matchup model) {
		model.setSeasonWeek(seasonWeekBean.findById(model.getSeasonWeek().getId(), false));
		model.setHomeTeam(teamBean.findById(model.getHomeTeam().getId(), false));
		model.setAwayTeam(teamBean.findById(model.getAwayTeam().getId(), false));
		model.setVenue(venueBean.findById(model.getVenue().getId(), false));
	}
}
