package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.ws.rs.Path;

import com.curleesoft.pickem.model.Theme;
import com.curleesoft.pickem.rest.dto.ThemeDTO;

@Stateless
@Path("/theme")
public class ThemeService extends BaseEntityService<ThemeDTO, Theme> {
	
	public ThemeService() {
		super(ThemeDTO.class, Theme.class);
	}
	
}
