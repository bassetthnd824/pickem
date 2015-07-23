package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.curleesoft.pickem.filter.LoginFilter;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.Season_;

@Stateless
public class SeasonBean extends GenericHibernateBean<Season, Long> {
	
	private static final Log log = LogFactory.getLog(LoginFilter.class);
	
	public SeasonBean() {
		super(Season.class);
	}

	public Season getCurrentSeason() {
		Season season = null;
		
		try {
			season = (Season) getEntityManager().createNamedQuery("getCurrentSeason").getSingleResult();
		} catch (NoResultException e) {
			log.debug("No current season found", e);
		}
		
		return season;
	}
	
	@Override
	public Predicate[] extractPredicates(Season exampleInstance, CriteriaBuilder criteriaBuilder, Root<Season> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getSeason())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Season_.season)), StringUtils.lowerCase("%" + exampleInstance.getSeason() + "%")));
		}
		
		if (exampleInstance.getBeginDate() != null) {
			predicates.add(criteriaBuilder.equal(root.get(Season_.beginDate), exampleInstance.getBeginDate()));
		}
		
		if (exampleInstance.getEndDate() != null) {
			predicates.add(criteriaBuilder.equal(root.get(Season_.endDate), exampleInstance.getEndDate()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}
	
	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Season> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Season_.season))
		};
	}
}
