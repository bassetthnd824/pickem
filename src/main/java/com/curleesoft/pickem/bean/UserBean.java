package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.User_;

@Stateful
public class UserBean extends GenericHibernateBean<User, Long> {

	public UserBean() {
		super(User.class);
	}

	public User getUserById(String userId) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
		Root<User> root = criteriaQuery.from(User.class);
		
		criteriaQuery.select(criteriaQuery.getSelection()).where(criteriaBuilder.equal(root.get(User_.userId), userId));
		criteriaQuery.orderBy(criteriaBuilder.asc(root.get(User_.userId)));
		
		TypedQuery<User> query = getEntityManager().createQuery(criteriaQuery);
		return query.getResultList().get(0);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<User> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(User_.userId))
		};
	}

	@Override
	protected org.hibernate.criterion.Order[] getDefaultHibernateOrder() {
		return new org.hibernate.criterion.Order[] {
				org.hibernate.criterion.Order.asc("userId")
		};
	}
	
}
