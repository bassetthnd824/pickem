package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Matchup;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.constraints.ValidMatchupDate;

public class MatchupDateValidator implements ConstraintValidator<ValidMatchupDate, Matchup> {
	
	@Override
	public void initialize(ValidMatchupDate constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(Matchup matchup, ConstraintValidatorContext context) {
		
		boolean isValid = true;
		SeasonWeek seasonWeek = null;
		
		if (isValid && matchup == null) {
			isValid = false;
		}
		
		if (isValid && matchup.getMatchupDate() == null) {
			isValid = false;
		}
		
		if (isValid && matchup.getSeasonWeek() == null) {
			isValid = false;
		}
		
		if (isValid) {
			seasonWeek = matchup.getSeasonWeek();
		}
		
		if (isValid 
				&& (matchup.getMatchupDate().compareTo(seasonWeek.getBeginDate()) < 0
						|| matchup.getMatchupDate().compareTo(seasonWeek.getEndDate()) > 0)) {
			isValid = false;
		}
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("matchup date is invalid")
					.addPropertyNode("matchupDate").addConstraintViolation();
		}
		
		return isValid;
	}
}
