package com.curleesoft.pickem.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.rest.dto.UserDTO;

@Stateless
@Path("/user")
public class UserService {
	
	@Inject
	private EntityManager entityManager;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<UserDTO> getAll(@Context UriInfo uriInfo) {
		return getAll(uriInfo.getQueryParameters());
	}

	public List<UserDTO> getAll(MultivaluedMap<String, String> queryParameters) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
		Root<User> root = criteriaQuery.from(User.class);
		Predicate[] predicates = extractPredicates(queryParameters, criteriaBuilder, root);
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates);
		criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
		TypedQuery<User> query = entityManager.createQuery(criteriaQuery);
		
		if (queryParameters.containsKey("first")) {
			Integer firstRecord = Integer.parseInt(queryParameters.getFirst("first")) - 1;
			query.setFirstResult(firstRecord);
		}
		
		if (queryParameters.containsKey("maxResults")) {
			Integer maxResults = Integer.parseInt(queryParameters.getFirst("maxResults"));
			query.setMaxResults(maxResults);
		}
		
		List<User> users = query.getResultList();
		List<UserDTO> results = new ArrayList<UserDTO>();
		
		for (User user : users) {
			results.add(new UserDTO(user));
		}
		
		return results;
	}
	
	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public UserDTO getSingleInstance(@PathParam("id") Long id) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
		Root<User> root = criteriaQuery.from(User.class);
		Predicate condition = criteriaBuilder.equal(root.get("id"), id);
		criteriaQuery.select(criteriaBuilder.createQuery(User.class).getSelection()).where(condition);
		User user = entityManager.createQuery(criteriaQuery).getSingleResult();
		return new UserDTO(user);
	}
	
	@GET
	@Path("active")
	@Produces(MediaType.APPLICATION_JSON)
	public UserDTO getActiveUser(@Context HttpServletRequest request) {
		User user = (User) request.getSession().getAttribute("com.curleesoft.pickem.ActiveUser");
		return new UserDTO(user);
	}
	
	protected Predicate[] extractPredicates(MultivaluedMap<String, String> queryParameters, CriteriaBuilder criteriaBuilder, Root<User> root) {
		return new Predicate[] {};
	}

}
