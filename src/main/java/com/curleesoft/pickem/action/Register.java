package com.curleesoft.pickem.action;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.curleesoft.pickem.bean.GroupBean;
import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.form.Registration;
import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

public class Register extends ActionSupport implements Action, ServletRequestAware, ModelDriven<Registration> {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private UserBean userBean;
	
	@Inject
	private GroupBean groupBean;
	
	private HttpServletRequest request;
	private Registration registration;
	
	public String init() {
		return "init";
	}
	
	public String add() {
		Registration userRegistration = getModel();
		Group playerGroup = groupBean.getGroupByName("player");
		User user = new User();
		UserGroup userGroup = new UserGroup();
		
		user.setUserId(userRegistration.getEmailAddress());
		user.setEmailAddr(userRegistration.getEmailAddress());
		user.setUserPass(Globals.generateHash(userRegistration.getUserPass()));
		user.setFirstName(userRegistration.getFirstName());
		user.setLastName(userRegistration.getLastName());
		user.setCreateUser(userRegistration.getEmailAddress());
		user.setLastUpdateUser(userRegistration.getEmailAddress());
		userGroup.setUser(user);
		userGroup.setGroup(playerGroup);
		userGroup.setCreateUser(userRegistration.getEmailAddress());
		userGroup.setLastUpdateUser(userRegistration.getEmailAddress());
		user.getUserGroups().add(userGroup);
		
		userBean.makePersistent(user);
		
		try {
			request.login(userRegistration.getEmailAddress(), userRegistration.getUserPass());
			
		} catch (ServletException e) {
			return "loginError";
		}
		
		return SUCCESS;
	}

	@Override
	public Registration getModel() {
		if (registration == null) {
			registration = new Registration();
		}
		
		return registration;
	}

	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
}
