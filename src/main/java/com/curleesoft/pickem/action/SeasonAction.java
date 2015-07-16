package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.SeasonBean;
import com.curleesoft.pickem.model.Season;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class SeasonAction extends BaseAction<Season, Long, SeasonBean> implements ModelDriven<Season>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private SeasonBean seasonBean;
	
	private Season season;
	private List<Season> seasons;
	
	@Override
	public void prepare() throws Exception {}
	
	public void prepareInit() throws Exception {
		seasons = new ArrayList<Season>();
	}
	
	public void prepareSearch() throws Exception {
		season = new Season();
		seasons = new ArrayList<Season>();
	}
	
	public void prepareAdd() throws Exception {
		season = new Season();
	}
	
	public void prepareSave() throws Exception {
		season = new Season();
	}
	
	@Override
	protected SeasonBean getBean() {
		return seasonBean;
	}

	@Override
	public Season getModel() {
		return season;
	}

	@Override
	public void setModel(Season season) {
		this.season = season;
	}

	@Override
	public List<Season> getModelList() {
		return seasons;
	}

	@Override
	public void setModelList(List<Season> modelList) {
		this.seasons = modelList;
	}
	
	@Override
	protected void setExistingModelFields(Season existingModel, Season model) {
		existingModel.setSeason(model.getSeason());
		existingModel.setBeginDate(model.getBeginDate());
		existingModel.setEndDate(model.getEndDate());
	}
	
	@Override
	protected void setParentEntities(Season model) {}
}
