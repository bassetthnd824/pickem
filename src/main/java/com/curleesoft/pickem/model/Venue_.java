package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.615-0500")
@StaticMetamodel(Venue.class)
public class Venue_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Venue, Long> id;
	public static volatile SingularAttribute<Venue, String> cityState;
	public static volatile SingularAttribute<Venue, String> venueName;
	public static volatile SetAttribute<Venue, Matchup> matchups;
	public static volatile SetAttribute<Venue, Team> teams;
}
