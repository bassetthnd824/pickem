package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.613-0500")
@StaticMetamodel(UserPick.class)
public class UserPick_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<UserPick, Long> id;
	public static volatile SingularAttribute<UserPick, Long> rank;
	public static volatile SingularAttribute<UserPick, Matchup> matchup;
	public static volatile SingularAttribute<UserPick, Team> pickedTeam;
	public static volatile SingularAttribute<UserPick, User> user;
}
