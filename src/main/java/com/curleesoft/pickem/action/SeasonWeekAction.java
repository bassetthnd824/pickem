package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.apache.struts2.interceptor.validation.SkipValidation;

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
	
	private List<SeasonWeek> seasonWeeks;
	private List<Season> seasons;
	
	public SeasonWeekAction() throws InstantiationException, IllegalAccessException {
		super(SeasonWeek.class);
	}
	
	@Override
	public void prepare() throws Exception {
		seasons = seasonBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		seasonWeeks = new ArrayList<SeasonWeek>();
	}
	
	public void prepareSearch() throws Exception {
		model.setSeason(new Season());
		seasonWeeks = new ArrayList<SeasonWeek>();
	}
	
	public void prepareAdd() throws Exception {
		model.setSeason(new Season());
	}
	
	public void prepareEdit() throws Exception {
		model = getBean().findById(model.getId(), false);
	}
	
	public void prepareSave() throws Exception {
		model.setSeason(new Season());
	}
	
	public void prepareGetSeasonWeeksBySeason() throws Exception {
		model.setSeason(new Season());
	}
	
	@Override
	protected SeasonWeekBean getBean() {
		return seasonWeekBean;
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
	
	@SkipValidation
	public String getSeasonWeekById() {
		model = seasonWeekBean.findById(model.getId(), false);
		return JSON_OBJECT;
	}
	
	@SkipValidation
	public String getSeasonWeeksBySeason() {
		setModelList(seasonWeekBean.getSeasonWeeksBySeason(model.getSeason().getId()));
		return JSON_LIST;
	}
	
	@Override
	protected void setExistingModelFields(SeasonWeek existingModel, SeasonWeek model) {
		existingModel.setSeason(seasonBean.findById(model.getSeason().getId(), false));
		existingModel.setWeekNumber(model.getWeekNumber());
		existingModel.setBeginDate(model.getBeginDate());
		existingModel.setEndDate(model.getEndDate());
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(existingModel.getSeason().getBeginDate());
		int daysToAdd = (int) ((existingModel.getWeekNumber() - 1) * 7);
		cal.add(Calendar.DATE, daysToAdd);
		
		if (!cal.getTime().equals(existingModel.getBeginDate())) {
			throw new IllegalArgumentException("Week Begin Date is invalid");
		}
	}
	
	@Override
	protected void setParentEntities(SeasonWeek model) {
		model.setSeason(seasonBean.findById(model.getSeason().getId(), false));
	}

}
