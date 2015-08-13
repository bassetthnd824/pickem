package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.ThemeBean;
import com.curleesoft.pickem.model.Theme;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class ThemeAction extends BaseAction<Theme, Long, ThemeBean> implements ModelDriven<Theme>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private ThemeBean themeBean;
	
	private List<Theme> themes;
	
	public ThemeAction() throws InstantiationException, IllegalAccessException {
		super(Theme.class);
	}
	
	@Override
	public void prepare() throws Exception {
	}
	
	public void prepareInit() throws Exception {
		themes = new ArrayList<Theme>();
	}
	
	public void prepareSearch() throws Exception {
		themes = new ArrayList<Theme>();
	}
	
	public void prepareEdit() throws Exception {
		model = getBean().findById(model.getId(), false);
	}
	
	@Override
	protected ThemeBean getBean() {
		return themeBean;
	}

	@Override
	public List<Theme> getModelList() {
		return themes;
	}

	@Override
	public void setModelList(List<Theme> modelList) {
		this.themes = modelList;
	}
	
	@Override
	protected void setExistingModelFields(Theme existingModel, Theme model) {
		existingModel.setActive(model.getActive());
		existingModel.setThemeName(model.getThemeName());
		existingModel.setThemePath(model.getThemePath());
	}
	
	@Override
	protected void setParentEntities(Theme model) {}
}
