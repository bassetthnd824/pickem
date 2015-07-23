package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.TeamBean;
import com.curleesoft.pickem.bean.VenueBean;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Venue;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class TeamAction extends BaseAction<Team, Long, TeamBean> implements ModelDriven<Team>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private TeamBean teamBean;
	
	@Inject
	private VenueBean venueBean;
	
	private Team team;
	private List<Team> teams;
	private List<Venue> venues;
	
	@Override
	public void prepare() throws Exception {
		venues = venueBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		teams = new ArrayList<Team>();
	}
	
	public void prepareSearch() throws Exception {
		team = new Team();
		team.setHomeVenue(new Venue());
		teams = new ArrayList<Team>();
	}
	
	public void prepareAdd() throws Exception {
		team = new Team();
	}
	
	public void prepareSave() throws Exception {
		team = new Team();
	}
	
	@Override
	protected TeamBean getBean() {
		return teamBean;
	}

	@Override
	public Team getModel() {
		return team;
	}

	@Override
	public void setModel(Team venue) {
		this.team = venue;
	}

	@Override
	public List<Team> getModelList() {
		return teams;
	}

	@Override
	public void setModelList(List<Team> modelList) {
		this.teams = modelList;
	}
	
	public List<Venue> getVenues() {
		return venues;
	}
	
	@Override
	protected void setExistingModelFields(Team existingModel, Team model) {
		existingModel.setTeamName(model.getTeamName());
		existingModel.setSquadName(model.getSquadName());
		existingModel.setConferenceMember(model.getConferenceMember());
		existingModel.setHomeVenue(venueBean.findById(model.getHomeVenue().getId(), false));
	}
	
	@Override
	protected void setParentEntities(Team model) {
		model.setHomeVenue(venueBean.findById(model.getHomeVenue().getId(), false));
	}
}
