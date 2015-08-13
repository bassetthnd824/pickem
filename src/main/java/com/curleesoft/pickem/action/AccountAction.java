package com.curleesoft.pickem.action;

import java.util.Date;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.validation.SkipValidation;

import com.curleesoft.pickem.bean.UserBean;
import com.curleesoft.pickem.form.Account;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.util.Globals;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;

public class AccountAction extends ActionSupport implements Action, ModelDriven<Account>, ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	@Inject
	private UserBean userBean;
	
	private Account account = new Account();
	private HttpServletRequest request;
	
	@Override
	@SkipValidation
	public String execute() throws Exception {
		User currentUser = (User) request.getSession(false).getAttribute(Globals.ACTIVE_USER);
		
		account.setEmailAddr(currentUser.getEmailAddr());
		account.setFirstName(currentUser.getFirstName());
		account.setLastName(currentUser.getLastName());
		account.setNickName(currentUser.getNickName());
		
		return SUCCESS;
	}
	
	public String save() throws Exception {
		Date now = new Date();
		User currentUser = (User) request.getSession(false).getAttribute(Globals.ACTIVE_USER);
		
		if (StringUtils.isNotBlank(account.getUserPass())) {
			if (!StringUtils.equals(Globals.generateHash(account.getOldPass()), currentUser.getUserPass())) {
				addActionError("Old password invalid");
				return ERROR;
			}
		}
		
		User user = userBean.findById(currentUser.getId(), false);
		user.setUserId(account.getEmailAddr());
		user.setEmailAddr(account.getEmailAddr());
		user.setFirstName(account.getFirstName());
		user.setLastName(account.getLastName());
		user.setNickName(account.getNickName());
		user.setLastUpdateDate(now);
		user.setLastUpdateUser(request.getUserPrincipal().getName());
		
		if (StringUtils.isNotBlank(account.getUserPass())) {
			user.setUserPass(Globals.generateHash(account.getUserPass()));
		}
		
		user = userBean.makePersistent(user);
		request.getSession(false).setAttribute(Globals.ACTIVE_USER, user);
		
		return SUCCESS;
	}
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public Account getModel() {
		return account;
	}
	
	public void setModel(Account account) {
		this.account = account;
	}
	
}
