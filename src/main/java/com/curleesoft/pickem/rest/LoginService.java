package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.rest.dto.Login;

@Stateless
@Path("/login")
public class LoginService {
	
	@Inject
	private EntityManager entityManager;
	
	@POST
	@Consumes({ "application/json" })
	public Response create(final Login login, @Context HttpServletRequest request) {
		try {
			request.login(login.getUsername(), login.getPassword());
		} catch (ServletException e) {
			return Response.status(403).build();
		}
		
		User user = entityManager.createQuery("Select u from User u where u.userId = :userId", User.class).setParameter("userId", login.getUsername()).getSingleResult();
		HttpSession session = request.getSession();
		session.setAttribute("com.curleesoft.pickem.ActiveUser", user);
		
		return Response.status(200).build();
	}
}
