package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.Group_;

@Stateful
public class GroupBean extends GenericHibernateBean<Group, Long> {

	public GroupBean() {
		super(Group.class);
	}

	public Group getGroupByName(String groupName) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Group> criteriaQuery = criteriaBuilder.createQuery(Group.class);
		Root<Group> root = criteriaQuery.from(Group.class);
		
		criteriaQuery.select(criteriaQuery.getSelection()).where(criteriaBuilder.equal(root.get(Group_.groupName), groupName));
		criteriaQuery.orderBy(criteriaBuilder.asc(root.get(Group_.groupName)));
		
		TypedQuery<Group> query = getEntityManager().createQuery(criteriaQuery);
		return query.getResultList().get(0);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Group> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Group_.groupName))
		};
	}

	@Override
	protected org.hibernate.criterion.Order[] getDefaultHibernateOrder() {
		return new org.hibernate.criterion.Order[] {
				org.hibernate.criterion.Order.asc("groupName")
		};
	}

}
