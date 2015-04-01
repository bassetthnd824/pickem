package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.constraints.AssertMatchupTeamsNotEqual;

public class MatchupTeamsNotEqualValidator implements ConstraintValidator<AssertMatchupTeamsNotEqual, Matchup> {
	
	@Override
	public void initialize(AssertMatchupTeamsNotEqual constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(Matchup matchup, ConstraintValidatorContext context) {
		
		boolean isValid = false;
		
		if (matchup == null) {
			return true;
		}
		
		isValid = (!matchup.getHomeTeam().equals(matchup.getAwayTeam()));
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("teams cannot be equal")
					.addPropertyNode("teamId1").addConstraintViolation();
		}
		
		return isValid;
	}
	
}
