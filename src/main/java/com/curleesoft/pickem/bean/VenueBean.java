package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.Venue;
import com.curleesoft.pickem.model.Venue_;

@Stateless
public class VenueBean extends GenericHibernateBean<Venue, Long> {
	
	public VenueBean() {
		super(Venue.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Venue> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Venue_.venueName))
		};
	}

	@Override
	public Predicate[] extractPredicates(Venue exampleInstance, CriteriaBuilder criteriaBuilder, Root<Venue> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getVenueName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Venue_.venueName)), StringUtils.lowerCase("%" + exampleInstance.getVenueName() + "%")));
		}
		
		if (StringUtils.isNotBlank(exampleInstance.getCityState())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Venue_.cityState)), StringUtils.lowerCase("%" + exampleInstance.getCityState() + "%")));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
