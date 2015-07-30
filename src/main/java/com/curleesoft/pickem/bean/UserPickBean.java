package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserPick;
import com.curleesoft.pickem.model.UserPick_;
import com.curleesoft.pickem.model.User_;

@Stateless
public class UserPickBean extends GenericHibernateBean<UserPick, Long> {
	
	public UserPickBean() {
		super(UserPick.class);
	}
	
	public List<UserPick> getUserPicksByUser(Long userId) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<UserPick> criteriaQuery = criteriaBuilder.createQuery(UserPick.class);
		Root<UserPick> root = criteriaQuery.from(UserPick.class);
		Join<UserPick, User> userRoot = root.join(UserPick_.user);
		
		criteriaQuery.select(criteriaQuery.getSelection()).where(criteriaBuilder.equal(userRoot.get(User_.id), userId));
		criteriaQuery.orderBy(getDefaultOrder(criteriaBuilder, root));
		TypedQuery<UserPick> query = getEntityManager().createQuery(criteriaQuery);
		
		return query.getResultList();
	}
	
	public List<UserPick> persistList(List<UserPick> userPicks) {
		for (UserPick userPick : userPicks) {
			makePersistent(userPick);
		}
		
		return userPicks;
	}
	
	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<UserPick> root) {
		Join<UserPick, User> userRoot = root.join(UserPick_.user);
		Join<UserPick, Matchup> matchupRoot = root.join(UserPick_.matchup);
		Join<Matchup, SeasonWeek> seasonWeekRoot = matchupRoot.join(Matchup_.seasonWeek);
		Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
		Join<Matchup, Team> homeTeamRoot = matchupRoot.join(Matchup_.homeTeam);
		
		return new Order[] {
				criteriaBuilder.asc(userRoot.get(User_.userId)),
				criteriaBuilder.asc(seasonRoot.get(Season_.season)),
				criteriaBuilder.asc(seasonWeekRoot.get(SeasonWeek_.weekNumber)),
				criteriaBuilder.asc(root.get(UserPick_.rank)),
				criteriaBuilder.asc(matchupRoot.get(Matchup_.matchupDate)),
				criteriaBuilder.asc(homeTeamRoot.get(Team_.teamName)),
		};
	}

	@Override
	public Predicate[] extractPredicates(UserPick exampleInstance, CriteriaBuilder criteriaBuilder, Root<UserPick> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (exampleInstance.getUser() != null && exampleInstance.getUser().getId() != null) {
			Join<UserPick, User> userRoot = root.join(UserPick_.user);
			predicates.add(criteriaBuilder.equal(userRoot.get(User_.id), exampleInstance.getUser().getId()));
		}
		
		if (exampleInstance.getMatchup() != null && 
				exampleInstance.getMatchup().getSeasonWeek() != null && 
				exampleInstance.getMatchup().getSeasonWeek().getSeason() != null && 
				exampleInstance.getMatchup().getSeasonWeek().getSeason().getId() != null) {
			Join<UserPick, Matchup> matchupRoot = root.join(UserPick_.matchup);
			Join<Matchup, SeasonWeek> seasonWeekRoot = matchupRoot.join(Matchup_.seasonWeek);
			Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
			predicates.add(criteriaBuilder.equal(seasonRoot.get(Season_.id), exampleInstance.getMatchup().getSeasonWeek().getSeason().getId()));
		}
		
		if (exampleInstance.getMatchup() != null && 
				exampleInstance.getMatchup().getSeasonWeek() != null && 
				exampleInstance.getMatchup().getSeasonWeek().getId() != null) {
			Join<UserPick, Matchup> matchupRoot = root.join(UserPick_.matchup);
			Join<Matchup, SeasonWeek> seasonWeekRoot = matchupRoot.join(Matchup_.seasonWeek);
			predicates.add(criteriaBuilder.equal(seasonWeekRoot.get(SeasonWeek_.id), exampleInstance.getMatchup().getSeasonWeek().getId()));
		}
		
		if (exampleInstance.getMatchup() != null && 
				exampleInstance.getMatchup().getMatchupDate() != null) {
			Join<UserPick, Matchup> matchupRoot = root.join(UserPick_.matchup);
			predicates.add(criteriaBuilder.equal(matchupRoot.get(Matchup_.matchupDate), exampleInstance.getMatchup().getMatchupDate()));
		}
		
		if (exampleInstance.getMatchup() != null && 
				exampleInstance.getMatchup().getHomeTeam() != null && 
				exampleInstance.getMatchup().getHomeTeam().getId() != null) {
			Join<UserPick, Matchup> matchupRoot = root.join(UserPick_.matchup);
			Join<Matchup, Team> homeTeamRoot = matchupRoot.join(Matchup_.homeTeam);
			Join<Matchup, Team> awayTeamRoot = matchupRoot.join(Matchup_.awayTeam);
			
			predicates.add(
					criteriaBuilder.or(
							criteriaBuilder.equal(homeTeamRoot.get(Team_.id), exampleInstance.getMatchup().getHomeTeam().getId()),
							criteriaBuilder.equal(awayTeamRoot.get(Team_.id), exampleInstance.getMatchup().getHomeTeam().getId())
					)
			);
		}
				
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
