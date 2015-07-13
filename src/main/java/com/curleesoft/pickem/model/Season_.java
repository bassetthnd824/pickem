package com.curleesoft.pickem.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.603-0500")
@StaticMetamodel(Season.class)
public class Season_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Season, Long> id;
	public static volatile SingularAttribute<Season, Date> beginDate;
	public static volatile SingularAttribute<Season, Date> endDate;
	public static volatile SingularAttribute<Season, String> season;
	public static volatile SetAttribute<Season, SeasonWeek> seasonWeeks;
}
