package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.User_;

@Stateless
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
	public Predicate[] extractPredicates(User exampleInstance, CriteriaBuilder criteriaBuilder, Root<User> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getEmailAddr())) {
			predicates.add(criteriaBuilder.equal(root.get(User_.emailAddr), exampleInstance.getEmailAddr()));
		}
		
		if (StringUtils.isNotBlank(exampleInstance.getFirstName())) {
			predicates.add(criteriaBuilder.equal(root.get(User_.firstName), exampleInstance.getFirstName()));
		}
		
		if (StringUtils.isNotBlank(exampleInstance.getLastName())) {
			predicates.add(criteriaBuilder.equal(root.get(User_.lastName), exampleInstance.getLastName()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
