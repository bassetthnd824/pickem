package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.security.auth.spi.Util;

import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;
import com.curleesoft.pickem.rest.dto.UserRegistration;

@Stateless
@Path("/registration")
public class RegistrationService {
	
	@Inject
	private EntityManager entityManager;
	
	@POST
	@Consumes({ "application/json" })
	public Response create(final UserRegistration userRegistration, @Context HttpServletRequest request) {
		User user = new User();
		UserGroup userGroup = new UserGroup();
		
		Group group = entityManager.createQuery("Select g from Group g where g.groupName = :groupName", Group.class).setParameter("groupName", "player").getSingleResult();
		
		user.setUserId(userRegistration.getEmailAddress());
		user.setEmailAddr(userRegistration.getEmailAddress());
		user.setFirstName(userRegistration.getFirstName());
		user.setLastName(userRegistration.getLastName());
		user.setUserPass(generateHash(userRegistration.getUserPass()));
		user.setCreateUser(userRegistration.getEmailAddress());
		user.setLastUpdateUser(userRegistration.getEmailAddress());
		userGroup.setUser(user);
		userGroup.setGroup(group);
		userGroup.setCreateUser(userRegistration.getEmailAddress());
		userGroup.setLastUpdateUser(userRegistration.getEmailAddress());
		user.getUserGroups().add(userGroup);
		
		entityManager.persist(user);
		
		try {
			request.login(userRegistration.getEmailAddress(), userRegistration.getUserPass());
		} catch (ServletException e) {
			return Response.status(403).build();
		}
		
		return Response.created(UriBuilder.fromResource(User.class).path(String.valueOf(user.getId())).build()).build();
	}
	
	private String generateHash(final String stringToHash) {
		String hashedPassword = Util.createPasswordHash("SHA-256", "BASE64", null, null, stringToHash);
		return hashedPassword;
	}
}
