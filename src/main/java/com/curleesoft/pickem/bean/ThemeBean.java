package com.curleesoft.pickem.bean;

import javax.ejb.Stateful;

import com.curleesoft.pickem.model.Theme;

@Stateful
public class ThemeBean extends GenericHibernateBean<Theme, Long> {
	
	public ThemeBean() {
		super(Theme.class);
	}
}
