package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.GroupBean;
import com.curleesoft.pickem.bean.ThemeBean;
import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.Theme;
import com.curleesoft.pickem.model.User;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class UserAction extends BaseAction<User, Long, UserBean> implements ModelDriven<User>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private UserBean userBean;
	
	@Inject
	private GroupBean groupBean;
	
	@Inject
	private ThemeBean themeBean;
	
	private User user;
	private List<User> users;
	private List<Group> groups;
	private List<Theme> themes;
	
	@Override
	public void prepare() throws Exception {
		groups = groupBean.findAll();
		themes = themeBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		users = new ArrayList<User>();
	}
	
	public void prepareSearch() throws Exception {
		user = new User();
		user.setTheme(new Theme());
		users = new ArrayList<User>();
	}
	
	public void prepareAdd() throws Exception {
		user = new User();
	}
	
	public void prepareSave() throws Exception {
		user = new User();
	}
	
	@Override
	protected UserBean getBean() {
		return userBean;
	}

	@Override
	public User getModel() {
		return user;
	}

	@Override
	public void setModel(User user) {
		this.user = user;
	}

	@Override
	public List<User> getModelList() {
		return users;
	}

	@Override
	public void setModelList(List<User> modelList) {
		this.users = modelList;
	}
	
	public List<Group> getGroups() {
		return groups;
	}
	
	public List<Theme> getThemes() {
		return themes;
	}
	
	@Override
	protected void setExistingModelFields(User existingModel, User model) {
		existingModel.setEmailAddr(model.getEmailAddr());
		existingModel.setFirstName(model.getFirstName());
		existingModel.setLastName(model.getLastName());
		existingModel.setNickName(model.getNickName());
		
		if (model.getTheme() != null && model.getTheme().getId() != null) {
			existingModel.setTheme(themeBean.findById(model.getTheme().getId(), false));
		}
		
		existingModel.setUserId(model.getEmailAddr());
	}
	
	@Override
	protected void setParentEntities(User model) {
		if (model.getTheme() != null && model.getTheme().getId() != null) {
			model.setTheme(themeBean.findById(model.getTheme().getId(), false));
		}
	}
}
