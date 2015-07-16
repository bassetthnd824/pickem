package com.curleesoft.pickem.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;

import com.curleesoft.pickem.bean.GenericHibernateBean;
import com.curleesoft.pickem.model.AbstractBaseEntity;
import com.opensymphony.xwork2.ActionSupport;

public abstract class BaseAction<E extends AbstractBaseEntity, ID extends Serializable, B extends GenericHibernateBean<E, ID>> extends ActionSupport implements ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	public static final String INIT = "init";
	public static final String LIST = "list";
	public static final String ADD = "add";
	public static final String EDIT = "edit";
	
	private String formMode;
	private ID modelId;
	private HttpServletRequest request;
	private Boolean reuseCriteria;
	
	public String init() {
		formMode = "init";
		return INIT;
	}
	
	@SuppressWarnings("unchecked")
	public String search() {
		formMode = "search";
		
		if (reuseCriteria != null && reuseCriteria.booleanValue()) {
			setModel((E) request.getSession(false).getAttribute("searchCriteria"));
		} else {
			request.getSession(false).setAttribute("searchCriteria", getModel());
		}
		
		setModelList(getBean().findByExample(getModel()));
		return LIST;
	}
	
	@SuppressWarnings("unchecked")
	public String cancel() {
		setModel((E) request.getSession(false).getAttribute("searchCriteria"));
		return SUCCESS;
	}
	
	public String add() {
		formMode = "add";
		return ADD;
	}
	
	public String edit() {
		formMode = "edit";
		setModel(getBean().findById(getModelId(), false));
		return EDIT;
	}
	
	@SuppressWarnings("unchecked")
	public String delete() {
		E model = getBean().findById(getModelId(), false);
		
		getBean().makeTransient(model);
		
		setModel((E) request.getSession(false).getAttribute("searchCriteria"));
		return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	public String save() {
		Date now = new Date();
		E model = getModel();
		
		if (StringUtils.equals(formMode, "edit")) {
			E existingModel = getBean().findById(getModelId(), false);
			
			setExistingModelFields(existingModel, model);
			
			existingModel.setLastUpdateDate(now);
			existingModel.setLastUpdateUser(request.getUserPrincipal().getName());
			
			getBean().makePersistent(existingModel);
			
		} else {
			setParentEntities(model);
			model.setLastUpdateDate(now);
			model.setLastUpdateUser(request.getUserPrincipal().getName());
			model.setCreateDate(now);
			model.setCreateUser(request.getUserPrincipal().getName());
			
			getBean().makePersistent(model);
		}
		
		
		setModel((E) request.getSession(false).getAttribute("searchCriteria"));
		return SUCCESS;
	}
	
	public String getFormMode() {
		return formMode;
	}
	
	public void setFormMode(String formMode) {
		this.formMode = formMode;
	}
	
	public ID getModelId() {
		return modelId;
	}
	
	public void setModelId(ID modelId) {
		this.modelId = modelId;
	}
	
	public Boolean getReuseCriteria() {
		return reuseCriteria;
	}
	
	public void setReuseCriteria(Boolean reuseCriteria) {
		this.reuseCriteria = reuseCriteria;
	}
	
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	public abstract E getModel();
	
	public abstract void setModel(E model);
	
	public abstract List<E> getModelList();
	
	public abstract void setModelList(List<E> modelList);
	
	protected abstract B getBean();
	
	protected abstract void setExistingModelFields(E existingModel, E model);
	
	protected abstract void setParentEntities(E model);
}
