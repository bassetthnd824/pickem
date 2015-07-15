package com.curleesoft.pickem.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
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

import com.curleesoft.pickem.model.AbstractBaseEntity;

public abstract class GenericHibernateBean<E extends AbstractBaseEntity, ID extends Serializable> {
	
	@Inject
	private EntityManager entityManager;
	
	@Resource
	private SessionContext sessionContext;
	
	private Class<E> entityClass;
	
	public GenericHibernateBean() {}
	
	public GenericHibernateBean(Class<E> entityClass) {
		this.entityClass = entityClass;
	}
	
	public E findById(ID id, boolean lock) {
		E entity;
		
		if (lock) {
			entity = entityManager.find(entityClass, id, LockModeType.PESSIMISTIC_READ);
		} else {
			entity = entityManager.find(entityClass, id);
		}
		
		return entity;
	}
	
	public List<E> findAll() {
		return findAll(new MultivaluedHashMap<String, String>());
	}
	
	public List<E> findAll(MultivaluedMap<String, String> queryParameters) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> root = criteriaQuery.from(entityClass);
		
		Predicate[] predicates = extractPredicates(queryParameters, criteriaBuilder, root);
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates);
		criteriaQuery.orderBy(getDefaultOrder(criteriaBuilder, root));
		TypedQuery<E> query = entityManager.createQuery(criteriaQuery);
		
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
	public List<E> findByExample(E exampleInstance, String... excludeProperties) {
		Session session = entityManager.unwrap(Session.class);
		
		Criteria criteria = session.createCriteria(entityClass);
		Example example = Example.create(exampleInstance);
		
		for (String excludeProperty : excludeProperties) {
			example.excludeProperty(excludeProperty);
		}
		
		criteria.add(example);
		
		for (org.hibernate.criterion.Order order : getDefaultHibernateOrder()) {
			criteria.addOrder(order);
		}
		
		return criteria.list();
	}
	
	public E makePersistent(E entity) {
		entity = entityManager.merge(entity);
		return entity;
	}
	
	public void makeTransient(E entity) {
		entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
		//entityManager.remove(entity);
	}
	
	public void flush() {
		entityManager.flush();
	}
	
	public void clear() {
		entityManager.clear();
	}
	
	protected List<E> findByCriteria(List<Predicate> predicates, List<Order> orders) {
		final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery(entityClass);
		Root<E> root = criteriaQuery.from(entityClass);
		orders.add(criteriaBuilder.asc(root.get("id")));
		criteriaQuery.select(criteriaQuery.getSelection()).where(predicates.toArray(new Predicate[predicates.size()]));
		criteriaQuery.orderBy(orders.toArray(new Order[orders.size()]));
		TypedQuery<E> query = entityManager.createQuery(criteriaQuery);
		return query.getResultList();
	}
	
	protected Predicate[] extractPredicates(MultivaluedMap<String, String> queryParameters, CriteriaBuilder criteriaBuilder, Root<E> root) {
		return new Predicate[] {};
	}
	
	
	public Map<String, Long> getCount(MultivaluedMap<String, String> queryParameters) {
		Map<String, Long> result = new HashMap<String, Long>();
		
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		Root<E> root = criteriaQuery.from(entityClass);
		criteriaQuery.select(criteriaBuilder.count(root));
		Predicate[] predicates = extractPredicates(queryParameters, criteriaBuilder, root);
		criteriaQuery.where(predicates);
		result.put("count", entityManager.createQuery(criteriaQuery).getSingleResult());
		
		return result;
	}
	
	protected EntityManager getEntityManager() {
		return entityManager;
	}
	
	protected SessionContext getSessionContext() {
		return sessionContext;
	}
	
	protected abstract Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<E> root);
	
	protected abstract org.hibernate.criterion.Order[] getDefaultHibernateOrder();
	
}
