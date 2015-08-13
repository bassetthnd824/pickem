package com.curleesoft.pickem.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-08-12T15:00:20.013-0500")
@StaticMetamodel(Matchup.class)
public class Matchup_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Matchup, Long> id;
	public static volatile SingularAttribute<Matchup, Long> awayTeamScore;
	public static volatile SingularAttribute<Matchup, Long> homeTeamScore;
	public static volatile SingularAttribute<Matchup, Date> matchupDate;
	public static volatile SingularAttribute<Matchup, SeasonWeek> seasonWeek;
	public static volatile SingularAttribute<Matchup, Team> homeTeam;
	public static volatile SingularAttribute<Matchup, Team> awayTeam;
	public static volatile SingularAttribute<Matchup, Venue> venue;
	public static volatile ListAttribute<Matchup, UserPick> userPicks;
}
