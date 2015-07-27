package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.SeasonBean;
import com.curleesoft.pickem.bean.SeasonWeekBean;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class SeasonWeekAction extends BaseAction<SeasonWeek, Long, SeasonWeekBean> implements ModelDriven<SeasonWeek>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private SeasonWeekBean seasonWeekBean;
	
	@Inject
	private SeasonBean seasonBean;
	
	private SeasonWeek seasonWeek;
	private List<SeasonWeek> seasonWeeks;
	private List<Season> seasons;
	
	@Override
	public void prepare() throws Exception {
		seasons = seasonBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		seasonWeeks = new ArrayList<SeasonWeek>();
	}
	
	public void prepareSearch() throws Exception {
		seasonWeek = new SeasonWeek();
		seasonWeek.setSeason(new Season());
		seasonWeeks = new ArrayList<SeasonWeek>();
	}
	
	public void prepareAdd() throws Exception {
		seasonWeek = new SeasonWeek();
	}
	
	public void prepareSave() throws Exception {
		seasonWeek = new SeasonWeek();
	}
	
	public void prepareGetSeasonWeeksBySeason() throws Exception {
		seasonWeek = new SeasonWeek();
		seasonWeek.setSeason(new Season());
	}
	
	@Override
	protected SeasonWeekBean getBean() {
		return seasonWeekBean;
	}

	@Override
	public SeasonWeek getModel() {
		return seasonWeek;
	}

	@Override
	public void setModel(SeasonWeek seasonWeek) {
		this.seasonWeek = seasonWeek;
	}

	@Override
	public List<SeasonWeek> getModelList() {
		return seasonWeeks;
	}

	@Override
	public void setModelList(List<SeasonWeek> modelList) {
		this.seasonWeeks = modelList;
	}
	
	public List<Season> getSeasons() {
		return seasons;
	}
	
	public String getSeasonWeeksBySeason() {
		setModelList(seasonWeekBean.getSeasonWeeksBySeason(seasonWeek.getSeason().getId()));
		return JSONLIST;
	}
	
	@Override
	protected void setExistingModelFields(SeasonWeek existingModel, SeasonWeek model) {
		existingModel.setSeason(seasonBean.findById(model.getSeason().getId(), false));
		existingModel.setWeekNumber(model.getWeekNumber());
		existingModel.setBeginDate(model.getBeginDate());
		existingModel.setEndDate(model.getEndDate());
	}
	
	@Override
	protected void setParentEntities(SeasonWeek model) {
		model.setSeason(seasonBean.findById(model.getSeason().getId(), false));
	}
}
