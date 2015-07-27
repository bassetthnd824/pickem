package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.Matchup_;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.SeasonWeek_;
import com.curleesoft.pickem.model.Season_;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Team_;
import com.curleesoft.pickem.model.Venue;
import com.curleesoft.pickem.model.Venue_;

@Stateless
public class MatchupBean extends GenericHibernateBean<Matchup, Long> {
	
	public MatchupBean() {
		super(Matchup.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Matchup> root) {
		Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
		Join<Matchup, Team> homeTeamRoot = root.join(Matchup_.homeTeam);
		
		return new Order[] {
				criteriaBuilder.asc(seasonWeekRoot.get(SeasonWeek_.weekNumber)),
				criteriaBuilder.asc(root.get(Matchup_.matchupDate)),
				criteriaBuilder.asc(homeTeamRoot.get(Team_.teamName))
		};
	}

	@Override
	public Predicate[] extractPredicates(Matchup exampleInstance, CriteriaBuilder criteriaBuilder, Root<Matchup> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (exampleInstance.getSeasonWeek() != null && exampleInstance.getSeasonWeek().getSeason() != null && exampleInstance.getSeasonWeek().getSeason().getId() != null) {
			Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
			Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
			predicates.add(criteriaBuilder.equal(seasonRoot.get(Season_.id), exampleInstance.getSeasonWeek().getSeason().getId()));
		}
		
		if (exampleInstance.getSeasonWeek() != null && exampleInstance.getSeasonWeek().getId() != null) {
			Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
			predicates.add(criteriaBuilder.equal(seasonWeekRoot.get(SeasonWeek_.id), exampleInstance.getSeasonWeek().getId()));
		}
		
		if (exampleInstance.getMatchupDate() != null) {
			predicates.add(criteriaBuilder.equal(root.get(Matchup_.matchupDate), exampleInstance.getMatchupDate()));
		}
		
		if (exampleInstance.getHomeTeam() != null && exampleInstance.getHomeTeam().getId() != null) {
			Join<Matchup, Team> homeTeamRoot = root.join(Matchup_.homeTeam);
			Join<Matchup, Team> awayTeamRoot = root.join(Matchup_.awayTeam);
			
			predicates.add(
					criteriaBuilder.or(
							criteriaBuilder.equal(homeTeamRoot.get(Team_.id), exampleInstance.getHomeTeam().getId()),
							criteriaBuilder.equal(awayTeamRoot.get(Team_.id), exampleInstance.getHomeTeam().getId())
					)
			);
		}
		
		if (exampleInstance.getVenue() != null && exampleInstance.getVenue().getId() != null) {
			Join<Matchup, Venue> venueRoot = root.join(Matchup_.venue);
			predicates.add(criteriaBuilder.equal(venueRoot.get(Venue_.id), exampleInstance.getVenue().getId()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
