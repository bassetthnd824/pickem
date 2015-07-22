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
	
	private Theme theme;
	private List<Theme> themes;
	
	@Override
	public void prepare() throws Exception {
	}
	
	public void prepareInit() throws Exception {
		themes = new ArrayList<Theme>();
	}
	
	public void prepareSearch() throws Exception {
		theme = new Theme();
		themes = new ArrayList<Theme>();
	}
	
	public void prepareAdd() throws Exception {
		theme = new Theme();
	}
	
	public void prepareSave() throws Exception {
		theme = new Theme();
	}
	
	@Override
	protected ThemeBean getBean() {
		return themeBean;
	}

	@Override
	public Theme getModel() {
		return theme;
	}

	@Override
	public void setModel(Theme theme) {
		this.theme = theme;
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
