package com.curleesoft.pickem.model.validators;

import java.util.Calendar;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.constraints.ValidSeasonBeginDate;

public class SeasonBeginDateValidator implements ConstraintValidator<ValidSeasonBeginDate, Season> {
	
	@Override
	public void initialize(ValidSeasonBeginDate constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(Season season, ConstraintValidatorContext context) {
		
		boolean isValid = true;
		
		if (season == null) {
			isValid = false;
		}
		
		if (season.getBeginDate() == null) {
			isValid = false;
		}
		
		if (isValid) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(season.getBeginDate());
			int begYear = cal.get(Calendar.YEAR);
			int seasonInt = 0;
			
			try {
				seasonInt = Integer.parseInt(season.getSeason());
				
				if (seasonInt == begYear) {
					isValid = true;
				} else {
					isValid = false;
				}
				
			} catch (NumberFormatException e) {
				isValid = false;
			}
		}
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("season begin date is invalid")
					.addNode("beginDate").addConstraintViolation();
		}
		
		return isValid;
	}
}
