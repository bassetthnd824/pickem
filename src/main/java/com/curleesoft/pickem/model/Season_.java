package com.curleesoft.pickem.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-15T09:36:02.464-0500")
@StaticMetamodel(Season.class)
public class Season_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Season, Long> id;
	public static volatile SingularAttribute<Season, Date> beginDate;
	public static volatile SingularAttribute<Season, Date> endDate;
	public static volatile SingularAttribute<Season, String> season;
}
