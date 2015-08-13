package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.struts2.interceptor.validation.SkipValidation;

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
	
	private List<Team> teams;
	private List<Venue> venues;
	
	public TeamAction() throws InstantiationException, IllegalAccessException {
		super(Team.class);
	}
	
	@Override
	public void prepare() throws Exception {
		venues = venueBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		teams = new ArrayList<Team>();
	}
	
	public void prepareSearch() throws Exception {
		model.setHomeVenue(new Venue());
		teams = new ArrayList<Team>();
	}
	
	public void prepareEdit() throws Exception {
		model = getBean().findById(model.getId(), false);
	}
	
	@SkipValidation
	public String getTeamById() {
		model = teamBean.findById(model.getId(), false);
		return JSON_OBJECT;
	}
	
@Override
	protected TeamBean getBean() {
		return teamBean;
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
