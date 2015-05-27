package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.rest.dto.Login;
import com.curleesoft.pickem.rest.dto.UserDTO;
import com.curleesoft.pickem.util.Globals;

@Stateless
@Path("/")
public class LoginService {
	
	@Inject
	private EntityManager entityManager;
	
	@POST
	@Path("login")
	@Consumes({ "application/json" })
	public Response create(final Login login, @Context HttpServletRequest request) {
		try {
			request.login(login.getUsername(), login.getPassword());
		} catch (ServletException e) {
			return Response.status(403).build();
		}
		
		User user = entityManager.createQuery("Select u from User u where u.userId = :userId", User.class).setParameter("userId", login.getUsername()).getSingleResult();
		HttpSession session = request.getSession();
		session.setAttribute(Globals.ACTIVE_USER, new UserDTO(user));
		
		return Response.created(UriBuilder.fromResource(UserService.class).path(String.valueOf(user.getId())).build()).build();
	}
	
	@DELETE
	@Path("logout")
	public Response delete(final Login login, @Context HttpServletRequest request) {
		HttpSession session = request.getSession();
		session.removeAttribute(Globals.ACTIVE_USER);
		session.invalidate();
		return Response.noContent().build();
	}
}
