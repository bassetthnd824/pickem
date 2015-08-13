package com.curleesoft.pickem.action;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.curleesoft.pickem.bean.VenueBean;
import com.curleesoft.pickem.model.Venue;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class VenueAction extends BaseAction<Venue, Long, VenueBean> implements ModelDriven<Venue>, Preparable {
	
	private static final long serialVersionUID = 1L;
	
	@Inject
	private VenueBean venueBean;
	
	private List<Venue> venues;
	
	public VenueAction() throws InstantiationException, IllegalAccessException {
		super(Venue.class);
	}
	
	@Override
	public void prepare() throws Exception {
	}
	
	public void prepareInit() throws Exception {
		venues = new ArrayList<Venue>();
	}
	
	public void prepareSearch() throws Exception {
		venues = new ArrayList<Venue>();
	}
	
	public void prepareEdit() throws Exception {
		model = getBean().findById(model.getId(), false);
	}
	
	@Override
	protected VenueBean getBean() {
		return venueBean;
	}

	@Override
	public List<Venue> getModelList() {
		return venues;
	}

	@Override
	public void setModelList(List<Venue> modelList) {
		this.venues = modelList;
	}
	
	@Override
	protected void setExistingModelFields(Venue existingModel, Venue model) {
		existingModel.setVenueName(model.getVenueName());
		existingModel.setCityState(model.getCityState());
	}
	
	@Override
	protected void setParentEntities(Venue model) {}
}
