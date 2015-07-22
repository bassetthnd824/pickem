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
	
	private Venue venue;
	private List<Venue> venues;
	
	@Override
	public void prepare() throws Exception {
	}
	
	public void prepareInit() throws Exception {
		venues = new ArrayList<Venue>();
	}
	
	public void prepareSearch() throws Exception {
		venue = new Venue();
		venues = new ArrayList<Venue>();
	}
	
	public void prepareAdd() throws Exception {
		venue = new Venue();
	}
	
	public void prepareSave() throws Exception {
		venue = new Venue();
	}
	
	@Override
	protected VenueBean getBean() {
		return venueBean;
	}

	@Override
	public Venue getModel() {
		return venue;
	}

	@Override
	public void setModel(Venue venue) {
		this.venue = venue;
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
