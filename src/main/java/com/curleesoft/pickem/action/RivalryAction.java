package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.RivalryBean;
import com.curleesoft.pickem.bean.TeamBean;
import com.curleesoft.pickem.model.Rivalry;
import com.curleesoft.pickem.model.Team;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class RivalryAction extends BaseAction<Rivalry, Long, RivalryBean> implements ModelDriven<Rivalry>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private RivalryBean rivalryBean;
	
	@Inject
	private TeamBean teamBean;
	
	private Rivalry rivalry;
	private List<Rivalry> rivalries;
	private List<Team> teams;
	
	@Override
	public void prepare() throws Exception {
		teams = teamBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		rivalries = new ArrayList<Rivalry>();
	}
	
	public void prepareSearch() throws Exception {
		rivalry = new Rivalry();
		rivalry.setTeam1(new Team());
		rivalry.setTeam2(new Team());
		rivalries = new ArrayList<Rivalry>();
	}
	
	public void prepareAdd() throws Exception {
		rivalry = new Rivalry();
		rivalry.setTeam1(new Team());
		rivalry.setTeam2(new Team());
	}
	
	public void prepareSave() throws Exception {
		rivalry = new Rivalry();
		rivalry.setTeam1(new Team());
		rivalry.setTeam2(new Team());
	}
	
	@Override
	protected RivalryBean getBean() {
		return rivalryBean;
	}

	@Override
	public Rivalry getModel() {
		return rivalry;
	}

	@Override
	public void setModel(Rivalry venue) {
		this.rivalry = venue;
	}

	@Override
	public List<Rivalry> getModelList() {
		return rivalries;
	}

	@Override
	public void setModelList(List<Rivalry> modelList) {
		this.rivalries = modelList;
	}
	
	public List<Team> getTeams() {
		return teams;
	}
	
	@Override
	protected void setExistingModelFields(Rivalry existingModel, Rivalry model) {
		existingModel.setRivalryName(model.getRivalryName());
		existingModel.setTeam1(teamBean.findById(model.getTeam1().getId(), false));
		existingModel.setTeam2(teamBean.findById(model.getTeam2().getId(), false));
	}
	
	@Override
	protected void setParentEntities(Rivalry model) {
		model.setTeam1(teamBean.findById(model.getTeam1().getId(), false));
		model.setTeam2(teamBean.findById(model.getTeam2().getId(), false));
	}
}
