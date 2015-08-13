package com.curleesoft.pickem.model.validators;

import java.util.Calendar;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.Season;
import com.curleesoft.pickem.model.SeasonWeek;
import com.curleesoft.pickem.model.constraints.ValidSeasonWeekBeginDate;

public class SeasonWeekBeginDateValidator implements ConstraintValidator<ValidSeasonWeekBeginDate, SeasonWeek> {
	
	@Override
	public void initialize(ValidSeasonWeekBeginDate constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(SeasonWeek seasonWeek, ConstraintValidatorContext context) {
		
		boolean isValid = true;
		
		if (seasonWeek == null) {
			isValid = false;
		}
		
		if (seasonWeek.getBeginDate() == null) {
			isValid = false;
		}
		
		if (isValid) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(seasonWeek.getBeginDate());
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			
			if (dayOfWeek != Calendar.THURSDAY) {
				isValid = false;
			}
		}
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("season week begin date invalid")
					.addNode("beginDate").addConstraintViolation();
		}
		
		return isValid;
	}
}
