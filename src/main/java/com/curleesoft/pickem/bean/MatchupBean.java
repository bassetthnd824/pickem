package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.curleesoft.pickem.form.TeamSchedule;
import com.curleesoft.pickem.form.UserScore;
import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.MatchupUserPick;
import com.curleesoft.pickem.model.Matchup_;
import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.SeasonWeek_;
import com.curleesoft.pickem.model.Season_;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Team_;
import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.Venue;
import com.curleesoft.pickem.model.Venue_;
import com.curleesoft.pickem.util.NativeQueryResultsMapper;

@Stateless
public class MatchupBean extends GenericHibernateBean<Matchup, Long> {
	
	@Inject
	private UserBean userBean;
	
	public MatchupBean() {
		super(Matchup.class);
	}
	
	public List<Matchup> getMatchupsBySeason(Long seasonId) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Matchup> criteriaQuery = criteriaBuilder.createQuery(Matchup.class);
		Root<Matchup> root = criteriaQuery.from(Matchup.class);
		Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
		Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
		
		criteriaQuery.select(criteriaQuery.getSelection()).where(criteriaBuilder.equal(seasonRoot.get(Season_.id), seasonId));
		criteriaQuery.orderBy(getDefaultOrder(criteriaBuilder, root));
		TypedQuery<Matchup> query = getEntityManager().createQuery(criteriaQuery);
		
		return query.getResultList();
	}
	
	public List<TeamSchedule> getMatchupsBySeasonTeam(Long seasonId, Long teamId) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<Matchup> criteriaQuery = criteriaBuilder.createQuery(Matchup.class);
		Root<Matchup> root = criteriaQuery.from(Matchup.class);
		Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
		Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
		Join<Matchup, Team> homeTeamRoot = root.join(Matchup_.homeTeam);
		Join<Matchup, Team> awayTeamRoot = root.join(Matchup_.awayTeam);
		
		criteriaQuery.select(
				criteriaQuery.getSelection()
		).where(
				criteriaBuilder.and(
						criteriaBuilder.equal(seasonRoot.get(Season_.id), seasonId),
						criteriaBuilder.or(
								criteriaBuilder.equal(homeTeamRoot.get(Team_.id), teamId),
								criteriaBuilder.equal(awayTeamRoot.get(Team_.id), teamId)
						)
				)
		);
		
		criteriaQuery.orderBy(getDefaultOrder(criteriaBuilder, root));
		TypedQuery<Matchup> query = getEntityManager().createQuery(criteriaQuery);
		
		List<Matchup> matchups = query.getResultList();
		List<TeamSchedule> results = new ArrayList<TeamSchedule>();
		
		for (Matchup matchup : matchups) {
			TeamSchedule teamSchedule = new TeamSchedule();
			boolean isHomeTeam = teamId.equals(matchup.getHomeTeam().getId());
			
			teamSchedule.setMatchupDate(matchup.getMatchupDate());
			teamSchedule.setOpponentName((isHomeTeam) ? matchup.getAwayTeam().getTeamName() : "at " + matchup.getHomeTeam().getTeamName());
			
			if (matchup.getHomeTeamScore() != null && matchup.getAwayTeamScore() != null) {
				teamSchedule.setScoreResult(matchup.getHomeTeamScore() + " - " + matchup.getAwayTeamScore());
				
				if (isHomeTeam) {
					if (matchup.getHomeTeamScore().compareTo(matchup.getAwayTeamScore()) > 0) {
						teamSchedule.setWinLoss("W");
					} else {
						teamSchedule.setWinLoss("L");
					}
				} else {
					if (matchup.getHomeTeamScore().compareTo(matchup.getAwayTeamScore()) > 0) {
						teamSchedule.setWinLoss("L");
					} else {
						teamSchedule.setWinLoss("W");
					}
				}
			}
			
			results.add(teamSchedule);
		}
		
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public List<MatchupUserPick> getMatchupUserPicksByUserSeason(Long userId, Long seasonId) {
		String sql = 
				"   SELECT M.MATCHUP_ID " +
				"        , S.SEASON_ID " +
				"        , S.SEASON " +
				"        , SW.SEASON_WEEK_ID " +
				"        , SW.WEEK_NUMBER " +
				"        , SW.BEGIN_DATE AS WEEK_BEGIN_DATE " +
				"        , SW.END_DATE AS WEEK_END_DATE " +
				"        , M.MATCHUP_DATE " +
				"        , M.AWAY_TEAM_ID " +
				"        , T2.TEAM_NAME AS AWAY_TEAM_NAME " +
				"        , T2.SQUAD_NAME AS AWAY_SQUAD_NAME " +
				"        , M.AWAY_TEAM_SCORE " +
				"        , M.HOME_TEAM_ID " +
				"        , T1.TEAM_NAME AS HOME_TEAM_NAME " +
				"        , T1.SQUAD_NAME AS HOME_SQUAD_NAME " +
				"        , M.HOME_TEAM_SCORE " +
				"        , V.VENUE_NAME " +
				"        , V.CITY_STATE " +
				"        , R.RIVALRY_NAME " +
				"        , UP.USER_PICK_ID " +
				"        , U.USER_GUID " +
				"        , U.USER_ID " +
				"        , UP.PICKED_TEAM_ID " +
				"        , UP.RANK " +
				"     FROM PCKM_MATCHUP       M " +
				"     JOIN PCKM_SEASON_WEEK   SW " +
				"       ON M.SEASON_WEEK_ID = SW.SEASON_WEEK_ID " +
				"     JOIN PCKM_SEASON        S " +
				"       ON SW.SEASON_ID     = S.SEASON_ID " +
				"     JOIN PCKM_TEAM          T1 " +
				"       ON M.HOME_TEAM_ID   = T1.TEAM_ID " +
				"     JOIN PCKM_TEAM          T2 " +
				"       ON M.AWAY_TEAM_ID   = T2.TEAM_ID " +
				"     JOIN PCKM_VENUE         V " +
				"       ON M.VENUE_ID       = V.VENUE_ID " +
				"LEFT JOIN PCKM_RIVALRY       R " +
				"       ON(M.HOME_TEAM_ID   = R.TEAM_ID1 " +
				"      AND M.AWAY_TEAM_ID   = R.TEAM_ID2) " +
				"       OR(M.HOME_TEAM_ID   = R.TEAM_ID2 " +
				"      AND M.AWAY_TEAM_ID   = R.TEAM_ID1) " +
				"LEFT JOIN PCKM_USER_PICK     UP " +
				"       ON M.MATCHUP_ID     = UP.MATCHUP_ID " +
				"      AND UP.USER_GUID     = :userId " +
				"LEFT JOIN PCKM_USER          U " +
				"       ON UP.USER_GUID     = U.USER_GUID " +
				"    WHERE S.SEASON_ID      = :seasonId  " +
				" ORDER BY S.SEASON " +
				"        , SW.WEEK_NUMBER " +
				"        , UP.RANK        DESC NULLS LAST" +
				"        , M.MATCHUP_DATE  " +
				"        , T1.TEAM_NAME";
		
		Query query = getEntityManager().createNativeQuery(sql);
		//int pos = 1;
		
		return NativeQueryResultsMapper.map(MatchupUserPick.class, query.setParameter("seasonId", seasonId).setParameter("userId", userId).getResultList());
	}
	
	public List<UserScore> getLeaderBoardForSeason(Long seasonId) {
		List<UserScore> userScores = new ArrayList<UserScore>();
		List<User> users = userBean.findAll();
		
		for (User user : users) {
			Long score = 0L;
			List<MatchupUserPick> userPicks = getMatchupUserPicksByUserSeason(user.getId(), seasonId);
			
			for (MatchupUserPick userPick : userPicks) {
				score += userPick.getScore();
			}
			
			userScores.add(new UserScore(user, score));
		}
		
		Collections.sort(userScores);
		
		return userScores;
	}
	
	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Matchup> root) {
		Join<Matchup, SeasonWeek> seasonWeekRoot = root.join(Matchup_.seasonWeek);
		Join<SeasonWeek, Season> seasonRoot = seasonWeekRoot.join(SeasonWeek_.season);
		Join<Matchup, Team> homeTeamRoot = root.join(Matchup_.homeTeam);
		
		return new Order[] {
				criteriaBuilder.asc(seasonRoot.get(Season_.season)),
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
