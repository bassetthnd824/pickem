package com.curleesoft.pickem.action;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.validation.SkipValidation;

import com.curleesoft.pickem.bean.GenericHibernateBean;
import com.curleesoft.pickem.model.AbstractBaseEntity;
import com.opensymphony.xwork2.ActionSupport;

public abstract class BaseAction<E extends AbstractBaseEntity<ID>, ID extends Serializable, B extends GenericHibernateBean<E, ID>> extends ActionSupport implements ServletRequestAware {

	private static final long serialVersionUID = 1L;
	
	public static final String INIT = "init";
	public static final String LIST = "list";
	public static final String INPUT = "input";
	public static final String JSON_OBJECT = "jsonObject";
	public static final String JSON_LIST = "jsonList";
	
	protected E model;
	private String formMode;
	private HttpServletRequest request;
	private Boolean reuseCriteria;
	
	public BaseAction(Class<E> entityClass) throws InstantiationException, IllegalAccessException {
		model = entityClass.newInstance();
	}
	
	@SkipValidation
	public String init() {
		formMode = "init";
		return INIT;
	}
	
	@SuppressWarnings("unchecked")
	@SkipValidation
	public String search() {
		formMode = "search";
		
		if (reuseCriteria != null && reuseCriteria.booleanValue()) {
			model = (E) request.getSession(false).getAttribute("searchCriteria");
		} else {
			request.getSession(false).setAttribute("searchCriteria", getModel());
		}
		
		setModelList(getBean().findByExample(getModel()));
		return LIST;
	}
	
	@SuppressWarnings("unchecked")
	@SkipValidation
	public String cancel() {
		model = (E) request.getSession(false).getAttribute("searchCriteria");
		return SUCCESS;
	}
	
	@SkipValidation
	public String add() {
		formMode = "add";
		return INPUT;
	}
	
	@SkipValidation
	public String edit() {
		formMode = "edit";
		return INPUT;
	}
	
	@SuppressWarnings("unchecked")
	@SkipValidation
	public String delete() {
		model = getBean().findById(model.getId(), false);
		
		getBean().makeTransient(model);
		
		model = (E) request.getSession(false).getAttribute("searchCriteria");
		return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	public String save() {
		Date now = new Date();
		E model = getModel();
		
		if (StringUtils.equals(formMode, "edit")) {
			E existingModel = getBean().findById(model.getId(), false);
			
			try {
				setExistingModelFields(existingModel, model);
			} catch (IllegalArgumentException e) {
				addActionError(e.getMessage());
				return INPUT;
			}
			
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
		
		model = (E) request.getSession(false).getAttribute("searchCriteria");
		return SUCCESS;
	}
	
	public String getFormMode() {
		return formMode;
	}
	
	public void setFormMode(String formMode) {
		this.formMode = formMode;
	}
	
	public Boolean getReuseCriteria() {
		return reuseCriteria;
	}
	
	public void setReuseCriteria(Boolean reuseCriteria) {
		this.reuseCriteria = reuseCriteria;
	}
	
	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	public E getModel() {
		return model;
	}
	
	protected HttpServletRequest getRequest() {
		return request;
	}
	
	public abstract List<E> getModelList();
	public abstract void setModelList(List<E> modelList);
	
	protected abstract B getBean();
	
	protected abstract void setExistingModelFields(E existingModel, E model);
	
	protected abstract void setParentEntities(E model);
}
