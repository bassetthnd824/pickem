package com.curleesoft.pickem.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-15T09:36:26.555-0500")
@StaticMetamodel(SeasonWeek.class)
public class SeasonWeek_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<SeasonWeek, Long> id;
	public static volatile SingularAttribute<SeasonWeek, Date> beginDate;
	public static volatile SingularAttribute<SeasonWeek, Date> endDate;
	public static volatile SingularAttribute<SeasonWeek, Long> weekNumber;
	public static volatile SingularAttribute<SeasonWeek, Season> season;
}
