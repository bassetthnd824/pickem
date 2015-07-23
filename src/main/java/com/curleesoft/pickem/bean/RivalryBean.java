package com.curleesoft.pickem.bean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.Rivalry;
import com.curleesoft.pickem.model.Rivalry_;
import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Team_;

@Stateless
public class RivalryBean extends GenericHibernateBean<Rivalry, Long> {
	
	public RivalryBean() {
		super(Rivalry.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Rivalry> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Rivalry_.rivalryName))
		};
	}

	@Override
	public Predicate[] extractPredicates(Rivalry exampleInstance, CriteriaBuilder criteriaBuilder, Root<Rivalry> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getRivalryName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Rivalry_.rivalryName)), StringUtils.lowerCase("%" + exampleInstance.getRivalryName() + "%")));
		}
		
		if (exampleInstance.getTeam1() != null && exampleInstance.getTeam1().getId() != null) {
			Join<Rivalry, Team> team1Root = root.join(Rivalry_.team1);
			Join<Rivalry, Team> team2Root = root.join(Rivalry_.team2);
			predicates.add(
					criteriaBuilder.or(
							criteriaBuilder.equal(team1Root.get(Team_.id), exampleInstance.getTeam1().getId()), 
							criteriaBuilder.equal(team2Root.get(Team_.id), exampleInstance.getTeam1().getId())
					)
			);
		}
		
		if (exampleInstance.getTeam2() != null && exampleInstance.getTeam2().getId() != null) {
			Join<Rivalry, Team> teamRoot = root.join(Rivalry_.team2);
			predicates.add(criteriaBuilder.equal(teamRoot.get(Team_.id), exampleInstance.getTeam2().getId()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
