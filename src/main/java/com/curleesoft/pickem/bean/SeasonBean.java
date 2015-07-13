package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;
import javax.persistence.NoResultException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.curleesoft.pickem.filter.LoginFilter;
import com.curleesoft.pickem.model.Season;

@Stateful
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
}
