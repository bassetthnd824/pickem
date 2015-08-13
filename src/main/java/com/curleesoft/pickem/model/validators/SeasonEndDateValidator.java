package com.curleesoft.pickem.model.validators;

import java.util.Calendar;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.constraints.ValidSeasonEndDate;

public class SeasonEndDateValidator implements ConstraintValidator<ValidSeasonEndDate, Season> {
	
	@Override
	public void initialize(ValidSeasonEndDate constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(Season season, ConstraintValidatorContext context) {
		
		boolean isValid = true;
		
		if (season == null) {
			isValid = false;
		}
		
		if (season.getEndDate() == null) {
			isValid = false;
		}
		
		if (isValid) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(season.getEndDate());
			int endYear = cal.get(Calendar.YEAR);
			int seasonInt = 0;
			
			try {
				seasonInt = Integer.parseInt(season.getSeason());
				
				if (endYear == seasonInt || endYear == seasonInt + 1) {
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
			context.buildConstraintViolationWithTemplate("season end date is invalid")
					.addNode("endDate").addConstraintViolation();
		}
		
		return isValid;
	}
}
