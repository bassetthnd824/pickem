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

import com.curleesoft.pickem.model.Team;
import com.curleesoft.pickem.model.Team_;
import com.curleesoft.pickem.model.Venue;
import com.curleesoft.pickem.model.Venue_;

@Stateless
public class TeamBean extends GenericHibernateBean<Team, Long> {
	
	public TeamBean() {
		super(Team.class);
	}

	@Override
	protected Order[] getDefaultOrder(CriteriaBuilder criteriaBuilder, Root<Team> root) {
		return new Order[] {
				criteriaBuilder.asc(root.get(Team_.teamName))
		};
	}

	@Override
	public Predicate[] extractPredicates(Team exampleInstance, CriteriaBuilder criteriaBuilder, Root<Team> root) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		
		if (StringUtils.isNotBlank(exampleInstance.getTeamName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Team_.teamName)), StringUtils.lowerCase("%" + exampleInstance.getTeamName() + "%")));
		}
		
		if (StringUtils.isNotBlank(exampleInstance.getSquadName())) {
			predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(Team_.squadName)), StringUtils.lowerCase("%" + exampleInstance.getSquadName() + "%")));
		}
		
		if (exampleInstance.getHomeVenue() != null && exampleInstance.getHomeVenue().getId() != null) {
			Join<Team, Venue> venueRoot = root.join(Team_.homeVenue);
			predicates.add(criteriaBuilder.equal(venueRoot.get(Venue_.id), exampleInstance.getHomeVenue().getId()));
		}
		
		return predicates.toArray(new Predicate[predicates.size()]);
	}

}
