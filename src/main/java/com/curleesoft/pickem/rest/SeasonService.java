package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.rest.dto.SeasonDTO;

@Stateless
@Path("/season")
public class SeasonService extends BaseEntityService<SeasonDTO, Season> {
	
	public SeasonService() {
		super(SeasonDTO.class, Season.class);
	}
	
}
