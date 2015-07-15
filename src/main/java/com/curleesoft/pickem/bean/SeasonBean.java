package com.curleesoft.pickem.bean;

import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

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
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Season> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Season_.season))
		};
	}

	@Override
	protected org.hibernate.criterion.Order[] getDefaultHibernateOrder() {
		return new org.hibernate.criterion.Order[] {
				org.hibernate.criterion.Order.asc("season")
		};
	}
}
