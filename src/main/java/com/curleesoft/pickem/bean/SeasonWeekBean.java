package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.SeasonWeek_;

@Stateful
public class SeasonWeekBean extends GenericHibernateBean<SeasonWeek, Long> {
	
	public SeasonWeekBean() {
		super(SeasonWeek.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<SeasonWeek> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(SeasonWeek_.season)),
				criteriaBuilder.asc(root.get(SeasonWeek_.weekNumber))
		};
	}

	@Override
	protected org.hibernate.criterion.Order[] getDefaultHibernateOrder() {
		return new org.hibernate.criterion.Order[] {
				org.hibernate.criterion.Order.asc("season"),
				org.hibernate.criterion.Order.asc("weekNumber")
		};
	}
}
