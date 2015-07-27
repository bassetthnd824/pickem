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

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.SeasonWeek_;
import com.curleesoft.pickem.model.Season_;

@Stateless
public class SeasonWeekBean extends GenericHibernateBean<SeasonWeek, Long> {
	
	public SeasonWeekBean() {
		super(SeasonWeek.class);
	}
	
	public List<SeasonWeek> getSeasonWeeksBySeason(Long seasonId) {
		final CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
		final CriteriaQuery<SeasonWeek> criteriaQuery = criteriaBuilder.createQuery(SeasonWeek.class);
		Root<SeasonWeek> root = criteriaQuery.from(SeasonWeek.class);
		Join<SeasonWeek, Season> seasonRoot = root.join(SeasonWeek_.season);
		
		criteriaQuery.select(criteriaQuery.getSelection()).where(criteriaBuilder.equal(seasonRoot.get(Season_.id), seasonId));
		criteriaQuery.orderBy(getDefaultOrder(criteriaBuilder, root));
		TypedQuery<SeasonWeek> query = getEntityManager().createQuery(criteriaQuery);
		
		return query.getResultList();
	}
	
	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<SeasonWeek> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(SeasonWeek_.season)),
				criteriaBuilder.asc(root.get(SeasonWeek_.weekNumber))
		};
	}

	@Override
	public Predicate[] extractPredicates(SeasonWeek exampleInstance, CriteriaBuilder criteriaBuilder, Root<SeasonWeek> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (exampleInstance.getSeason() != null && exampleInstance.getSeason().getId() != null) {
			Join<SeasonWeek, Season> seasonRoot = root.join(SeasonWeek_.season);
			predicates.add(criteriaBuilder.equal(seasonRoot.get(Season_.id), exampleInstance.getSeason().getId()));
		}
		
		if (exampleInstance.getWeekNumber() != null) {
			predicates.add(criteriaBuilder.equal(root.get(SeasonWeek_.weekNumber), exampleInstance.getWeekNumber()));
		}
		
		if (exampleInstance.getBeginDate() != null) {
			predicates.add(criteriaBuilder.equal(root.get(SeasonWeek_.beginDate), exampleInstance.getBeginDate()));
		}
		
		if (exampleInstance.getEndDate() != null) {
			predicates.add(criteriaBuilder.equal(root.get(SeasonWeek_.endDate), exampleInstance.getEndDate()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
