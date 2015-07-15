package com.curleesoft.pickem.action;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;

public class MainAction extends ActionSupport implements Action, ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	private HttpServletRequest request;

	public String getWelcomeMessage() {
		return "Welcome " + ((User) request.getSession().getAttribute(Globals.ACTIVE_USER)).getFirstName();
	}
	
	@Override
	public String execute() throws Exception {
		return SUCCESS;
	}

	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}

}
