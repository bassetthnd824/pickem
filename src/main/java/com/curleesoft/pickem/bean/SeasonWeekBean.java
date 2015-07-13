package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;

import com.curleesoft.pickem.model.SeasonWeek;

@Stateful
public class SeasonWeekBean extends GenericHibernateBean<SeasonWeek, Long> {
	
	public SeasonWeekBean() {
		super(SeasonWeek.class);
	}
}
