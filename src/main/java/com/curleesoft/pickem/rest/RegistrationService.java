package com.curleesoft.pickem.rest;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;
import com.curleesoft.pickem.service.UserRegistration;

@Stateless
@Path("/registration")
public class RegistrationService {
	
	@Inject
	private EntityManager entityManager;
	
	@POST
	@Consumes({ "application/json" })
	public Response create(final UserRegistration userRegistration) {
		User user = new User();
		UserGroup userGroup = new UserGroup();
		
		Group group = entityManager.createQuery("Select g from Group g where g.groupName = :groupName", Group.class).setParameter("groupName", "player").getSingleResult();
		
		user.setUserId(userRegistration.getEmailAddress());
		user.setEmailAddr(userRegistration.getEmailAddress());
		user.setFirstName(userRegistration.getFirstName());
		user.setLastName(userRegistration.getLastName());
		user.setUserPass(userRegistration.getUserPass());
		userGroup.setUser(user);
		userGroup.setGroup(group);
		user.getUserGroups().add(userGroup);
		
		entityManager.persist(user);
		
		return Response.created(UriBuilder.fromResource(RegistrationService.class).path(String.valueOf(userRegistration.getEmailAddress())).build()).build();
	}

}
