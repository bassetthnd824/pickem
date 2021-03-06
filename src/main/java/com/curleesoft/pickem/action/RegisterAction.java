package com.curleesoft.pickem.action;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.validation.SkipValidation;

import com.curleesoft.pickem.bean.GroupBean;
import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.form.Registration;
import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

public class RegisterAction extends ActionSupport implements Action, ServletRequestAware, ModelDriven<Registration> {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private UserBean userBean;
	
	@Inject
	private GroupBean groupBean;
	
	private HttpServletRequest request;
	
	private Registration registration;
	
	@SkipValidation
	public String execute() {
		return SUCCESS;
	}
	
	public String save() {
		Registration userRegistration = getModel();
		Group playerGroup = groupBean.getGroupByName("player");
		User user = new User();
		
		user.setUserId(userRegistration.getEmailAddr());
		user.setEmailAddr(userRegistration.getEmailAddr());
		user.setUserPass(Globals.generateHash(userRegistration.getUserPass()));
		user.setFirstName(userRegistration.getFirstName());
		user.setLastName(userRegistration.getLastName());
		user.setNickName(userRegistration.getNickName());
		user.setCreateUser(userRegistration.getEmailAddr());
		user.setLastUpdateUser(userRegistration.getEmailAddr());
		user.getGroups().add(playerGroup);
		
		userBean.makePersistent(user);
		
		try {
			request.login(userRegistration.getEmailAddr(), userRegistration.getUserPass());
			
		} catch (ServletException e) {
			return "loginError";
		}
		
		return SUCCESS;
	}

	@Override
	@Valid
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
