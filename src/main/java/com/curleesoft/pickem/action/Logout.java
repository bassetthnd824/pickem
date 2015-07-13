package com.curleesoft.pickem.action;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.opensymphony.xwork2.ActionSupport;

public class Logout extends ActionSupport implements ServletRequestAware {
	
	private static final long serialVersionUID = 1L;
	private HttpServletRequest request;
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	public String execute() {
		request.getSession().invalidate();
		return SUCCESS;
	}
}
