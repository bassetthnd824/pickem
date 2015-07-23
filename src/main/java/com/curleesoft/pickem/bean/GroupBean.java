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

import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.Group_;

@Stateless
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
	public Predicate[] extractPredicates(Group exampleInstance, CriteriaBuilder criteriaBuilder, Root<Group> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getGroupName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Group_.groupName)), StringUtils.lowerCase("%" + exampleInstance.getGroupName() + "%")));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
