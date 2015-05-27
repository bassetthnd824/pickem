package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.rest.dto.UserDTO;

@Stateless
@Path("/user")
public class UserService extends BaseEntityService<UserDTO, User> {
	
	public UserService() {
		super(UserDTO.class, User.class);
	}
	
	@GET
	@Path("active")
	@Produces(MediaType.APPLICATION_JSON)
	public UserDTO getActiveUser(@Context HttpServletRequest request) {
		UserDTO user = (UserDTO) request.getSession().getAttribute("com.curleesoft.pickem.ActiveUser");
		return user;
	}
	

}
