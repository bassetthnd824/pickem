package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.600-0500")
@StaticMetamodel(Rivalry.class)
public class Rivalry_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Rivalry, Long> id;
	public static volatile SingularAttribute<Rivalry, String> rivalryName;
	public static volatile SingularAttribute<Rivalry, Team> team1;
	public static volatile SingularAttribute<Rivalry, Team> team2;
}
