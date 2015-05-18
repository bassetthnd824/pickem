package com.curleesoft.pickem.rest;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.curleesoft.pickem.model.PickemEntity;
import com.curleesoft.pickem.rest.dto.AbstractBaseDTO;

public abstract class BaseEntityService<D extends AbstractBaseDTO, E extends PickemEntity> {
	
	@Inject
	private EntityManager entityManager;
	
	private Class<D> dtoClass;
	private Class<E> entityClass;
	
	public BaseEntityService() {}
	
	public BaseEntityService(Class<D> dtoClass, Class<E> entityClass) {
		this.dtoClass = dtoClass;
		this.entityClass = entityClass;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response create(D dto) {
		E entity = dto.fromDTO(null, entityManager);
		entityManager.persist(entity);
		return Response.created(UriBuilder.fromResource(this.getClass()).path(String.valueOf(entity.getId())).build()).build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	public Response deleteById(@PathParam("id") Long id) {
		E entity = entityManager.find(entityClass, id);
		
		if (entity == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		entityManager.remove(entity);
		return Response.noContent().build();
	}
	
	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes("application/json")
	public Response update(@PathParam("id") Long id, D dto) {
		E entity = entityManager.find(entityClass, id);
		
		entity = dto.fromDTO(entity, entityManager);
		
		try {
			entity = entityManager.merge(entity);
		} catch (OptimisticLockException e) {
			return Response.status(Response.Status.CONFLICT).entity(e.getEntity()).build();
		}
		
		return Response.noContent().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<D> getAll(@Context UriInfo uriInfo) {
		return getAll(uriInfo.getQueryParameters());
	}
	
	public List<D> getAll(MultivaluedMap<String, String> queryParameters) {
		List<D> results = new ArrayList<D>();
		
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> root = criteriaQuery.from(entityClass);
		Predicate[] predicates = extractPredicates(queryParameters, criteriaBuilder, root);
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates);
		criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
		TypedQuery<E> query = entityManager.createQuery(criteriaQuery);
		
		if (queryParameters.containsKey("first")) {
			Integer firstRecord = Integer.parseInt(queryParameters.getFirst("first")) - 1;
			query.setFirstResult(firstRecord);
		}
		
		if (queryParameters.containsKey("maxResults")) {
			Integer maxResults = Integer.parseInt(queryParameters.getFirst("maxResults"));
			query.setMaxResults(maxResults);
		}
		
		List<E> queryResults = query.getResultList();
		
		for (E entity : queryResults) {
			try {
				results.add(dtoClass.getConstructor(entityClass).newInstance(entity));
				
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		
		return results;
	}
	
	@GET
	@Path("/count")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Long> getCount(@Context UriInfo uriInfo) {
		Map<String, Long> result = new HashMap<String, Long>();
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		Root<E> root = criteriaQuery.from(entityClass);
		criteriaQuery.select(criteriaBuilder.count(root));
		Predicate[] predicates = extractPredicates(uriInfo.getQueryParameters(), criteriaBuilder, root);
		criteriaQuery.where(predicates);
		result.put("count", entityManager.createQuery(criteriaQuery).getSingleResult());
		
		return result;
	}
	
	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public D getSingleInstance(@PathParam("id") Long id) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> root = criteriaQuery.from(entityClass);
		Predicate condition = criteriaBuilder.equal(root.get("id"), id);
		criteriaQuery.select(criteriaBuilder.createQuery(entityClass).getSelection()).where(condition);
		
		try {
			return dtoClass.getConstructor(entityClass).newInstance(entityManager.createQuery(criteriaQuery).getSingleResult());
			
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Predicate[] extractPredicates(MultivaluedMap<String, String> queryParameters, CriteriaBuilder criteriaBuilder, Root<E> root) {
		return new Predicate[]{};
	}
	
	protected EntityManager getEntityManager() {
		return entityManager;
	}
	
}
