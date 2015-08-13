package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-08-12T15:01:19.909-0500")
@StaticMetamodel(Team.class)
public class Team_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Team, Long> id;
	public static volatile SingularAttribute<Team, Boolean> conferenceMember;
	public static volatile SingularAttribute<Team, String> squadName;
	public static volatile SingularAttribute<Team, String> teamName;
	public static volatile SingularAttribute<Team, Venue> homeVenue;
	public static volatile ListAttribute<Team, Rivalry> rivalries;
}
