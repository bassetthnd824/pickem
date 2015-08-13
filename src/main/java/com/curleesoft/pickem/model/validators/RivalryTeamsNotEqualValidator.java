package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Rivalry;
import com.curleesoft.pickem.model.constraints.AssertRivalryTeamsNotEqual;

public class RivalryTeamsNotEqualValidator implements ConstraintValidator<AssertRivalryTeamsNotEqual, Rivalry> {
	
	@Override
	public void initialize(AssertRivalryTeamsNotEqual constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(Rivalry rivalry, ConstraintValidatorContext context) {
		
		boolean isValid = false;
		
		if (rivalry == null) {
			return true;
		}
		
		isValid = (!rivalry.getTeam1().getId().equals(rivalry.getTeam2().getId()));
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("teams cannot be equal")
					.addNode("teamId1").addConstraintViolation();
		}
		
		return isValid;
	}
	
}
