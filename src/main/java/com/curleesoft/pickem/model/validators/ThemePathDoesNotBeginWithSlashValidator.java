package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.constraints.AssertThemePathDoesNotBeginWithSlash;

public class ThemePathDoesNotBeginWithSlashValidator implements ConstraintValidator<AssertThemePathDoesNotBeginWithSlash, String> {

	@Override
	public void initialize(AssertThemePathDoesNotBeginWithSlash constraintAnnotation) {
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
		if (value == null) {
			return false;
		}
		
		boolean isValid = !value.startsWith("/");
		
		return isValid;
	}
	
}
