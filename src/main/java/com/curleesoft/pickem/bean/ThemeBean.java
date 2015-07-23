package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.Theme;
import com.curleesoft.pickem.model.Theme_;

@Stateless
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
	public Predicate[] extractPredicates(Theme exampleInstance, CriteriaBuilder criteriaBuilder, Root<Theme> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getThemeName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Theme_.themeName)), StringUtils.lowerCase("%" + exampleInstance.getThemeName() + "%")));
		}
		
		if (StringUtils.isNotBlank(exampleInstance.getThemePath())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Theme_.themePath)), StringUtils.lowerCase("%" + exampleInstance.getThemePath() + "%")));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
