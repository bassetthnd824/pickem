package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.model.Theme;
import com.curleesoft.pickem.model.Theme_;

@Stateful
public class ThemeBean extends GenericHibernateBean<Theme, Long> {
	
	public ThemeBean() {
		super(Theme.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Theme> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Theme_.themeName))
		};
	}

	@Override
	protected org.hibernate.criterion.Order[] getDefaultHibernateOrder() {
		return new org.hibernate.criterion.Order[] {
				org.hibernate.criterion.Order.asc("themeName")
		};
	}
}
