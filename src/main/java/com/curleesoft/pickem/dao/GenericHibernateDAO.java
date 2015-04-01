package com.curleesoft.pickem.dao;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Example;

public abstract class GenericHibernateDAO<T, ID extends Serializable> implements GenericDAO<T, ID> {
	
	@Inject
	private EntityManager entityManager;
	private Class<T> entityClass;
	
	public GenericHibernateDAO() {}
	
	public GenericHibernateDAO(Class<T> entityClass) {
		this.entityClass = entityClass;
	}
	
	protected EntityManager getEntityManager() {
		return entityManager;
	}
	
	public T findById(ID id, boolean lock) {
		
		T entity = null;
		
		if (lock) {
			entity = entityManager.find(entityClass, id, LockModeType.PESSIMISTIC_READ);
		} else {
			entity = entityManager.find(entityClass, id);
		}
		
		return entity;
	}
	
	public List<T> findAll() {
		return findAll(new MultivaluedHashMap<String, String>());
	}
		
	public List<T> findAll(MultivaluedMap<String, String> queryParameters) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<T> root = criteriaQuery.from(entityClass);
		
		Predicate[] predicates = extractPredicates(queryParameters, criteriaBuilder, root);
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates);
		criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
		TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
		
		if (queryParameters.containsKey("first")) {
			Integer firstRecord = Integer.parseInt(queryParameters.getFirst("first"))-1;
			query.setFirstResult(firstRecord);
		}
		
		if (queryParameters.containsKey("maxResults")) {
			Integer maxResults = Integer.parseInt(queryParameters.getFirst("maxResults"));
			query.setMaxResults(maxResults);
		}
		
		return query.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public List<T> findByExample(T exampleInstance, String... excludeProperty) {
		Session session = entityManager.unwrap(Session.class);
		
		Criteria criteria = session.createCriteria(entityClass);
		Example example = Example.create(exampleInstance);
		
		for (String exclude : excludeProperty) {
			example.excludeProperty(exclude);
		}
		
		criteria.add(example);
		return criteria.list();
	}
	
	public T makePersistent(T entity) {
		entityManager.merge(entity);
		return entity;
	}
	
	public void makeTransient(T entity) {
		entityManager.remove(entity);
	}
	
	public void flush() {
		entityManager.flush();
	}
	
	public void clear() {
		entityManager.clear();
	}
	
	protected List<T> findByCriteria(List<Predicate> predicates, List<Order> orders) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<T> root = criteriaQuery.from(entityClass);
		orders.add(criteriaBuilder.asc(root.get("id")));
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates.toArray(new Predicate[predicates.size()]));
		criteriaQuery.orderBy(orders.toArray(new Order[orders.size()]));
		TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
		return query.getResultList();
	}
	
	/**
	 * <p>
	 * Subclasses may choose to expand the set of supported query parameters
	 * (for adding more filtering criteria on search and count) by overriding
	 * this method.
	 * </p>
	 * 
	 * @param queryParameters
	 *            - the HTTP query parameters received by the endpoint
	 * @param criteriaBuilder
	 *            - @{link CriteriaBuilder} used by the invoker
	 * @param root
	 *            @{link Root} used by the invoker
	 * @return a list of {@link Predicate}s that will added as query parameters
	 */
	protected Predicate[] extractPredicates(MultivaluedMap<String, String> queryParameters, CriteriaBuilder criteriaBuilder, Root<T> root) {
		return new Predicate[] {};
	}

}
