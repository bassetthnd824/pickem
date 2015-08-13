package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.struts2.interceptor.validation.SkipValidation;

import com.curleesoft.pickem.bean.SeasonBean;
import com.curleesoft.pickem.model.Season;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class SeasonAction extends BaseAction<Season, Long, SeasonBean> implements ModelDriven<Season>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private SeasonBean seasonBean;
	
	private List<Season> seasons;
	
	public SeasonAction() throws InstantiationException, IllegalAccessException {
		super(Season.class);
	}
	
	@Override
	public void prepare() throws Exception {}
	
	public void prepareInit() throws Exception {
		seasons = new ArrayList<Season>();
	}
	
	public void prepareEdit() throws Exception {
		model = getBean().findById(model.getId(), false);
	}
	
	public void prepareSearch() throws Exception {
		seasons = new ArrayList<Season>();
	}
	
	@SkipValidation
	public String getSeasonById() {
		model = seasonBean.findById(model.getId(), false);
		return JSON_OBJECT;
	}
	
	@Override
	protected SeasonBean getBean() {
		return seasonBean;
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
