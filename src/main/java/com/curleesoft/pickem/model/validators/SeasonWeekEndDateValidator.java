package com.curleesoft.pickem.model.validators;

import java.util.Calendar;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.constraints.ValidSeasonWeekEndDate;

public class SeasonWeekEndDateValidator implements ConstraintValidator<ValidSeasonWeekEndDate, SeasonWeek> {
	
	@Override
	public void initialize(ValidSeasonWeekEndDate constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(SeasonWeek seasonWeek, ConstraintValidatorContext context) {
		
		boolean isValid = true;
		
		if (seasonWeek == null) {
			isValid = false;
		}
		
		if (seasonWeek.getEndDate() == null) {
			isValid = false;
		}
		
		if (isValid) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(seasonWeek.getEndDate());
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			
			if (dayOfWeek != Calendar.WEDNESDAY) {
				isValid = false;
			}
			
			if (isValid) {
				cal.setTime(seasonWeek.getBeginDate());
				cal.add(Calendar.DATE, 6);
				
				if (!cal.getTime().equals(seasonWeek.getEndDate())) {
					isValid = false;
				}
			}
		}
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("season week end date is invalid")
					.addPropertyNode("endDate").addConstraintViolation();
		}
		
		return isValid;
	}
	
}
