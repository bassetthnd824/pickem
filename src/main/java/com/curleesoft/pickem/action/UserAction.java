package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.Iterator;
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
	
	private List<User> users;
	private List<Group> groups;
	private Long[] selectedGroups;
	private List<Theme> themes;
	
	public UserAction() throws InstantiationException, IllegalAccessException {
		super(User.class);
	}
	
	@Override
	public void prepare() throws Exception {
		themes = themeBean.findAll();
	}
	
	public void prepareInit() throws Exception {
		users = new ArrayList<User>();
	}
	
	public void prepareSearch() throws Exception {
		model.setTheme(new Theme());
		users = new ArrayList<User>();
	}
	
	public void prepareAdd() throws Exception {
		groups = groupBean.findAll();
	}
	
	public void prepareEdit() throws Exception {
		groups = groupBean.findAll();
		model = getBean().findById(model.getId(), false);
		selectedGroups = new Long[model.getGroups().size()];
		int i = 0;
		
		for (Group group : model.getGroups()) {
			selectedGroups[i] = group.getId();
			i++;
		}
	}
	
	public void prepareSave() throws Exception {
		groups = groupBean.findAll();
		selectedGroups = new Long[groups.size()];
	}
	
	@Override
	protected UserBean getBean() {
		return userBean;
	}

	@Override
	public List<User> getModelList() {
		return users;
	}

	@Override
	public void setModelList(List<User> modelList) {
		this.users = modelList;
	}
	
	public Long[] getSelectedGroups() {
		return selectedGroups;
	}
	
	public void setSelectedGroups(Long[] selectedGroups) {
		this.selectedGroups = selectedGroups;
	}
	
	public List<Group> getGrps() {
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
		
		for (Long groupId : selectedGroups) {
			if (groupId != null) {
				boolean found = false;
				
				for (Group group : existingModel.getGroups()) {
					if (group.getId().equals(groupId)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					existingModel.getGroups().add(groupBean.findById(groupId, false));
				}
			}
		}
		
		for (Iterator<Group> it = existingModel.getGroups().iterator(); it.hasNext();) {
			Group group = it.next();
			boolean found = false;
			
			for (Long groupId : selectedGroups) {
				if (groupId != null && group.getId().equals(groupId)) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				it.remove();
			}
		}
		
		if (existingModel.getGroups().size() < 1) {
			throw new IllegalArgumentException("User must belong to at least one group.");
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
