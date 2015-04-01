package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.constraints.AssertUserPassProperlyFormed;

public class UserPassProperlyFormedValidator implements ConstraintValidator<AssertUserPassProperlyFormed, String> {
	
	@Override
	public void initialize(AssertUserPassProperlyFormed constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
		boolean result = false;
		
		if (value == null) {
			return true;
		}
		
		boolean containsUpper = false;
		boolean containsNumber = false;
		
		for (int i = 0; i < value.length(); i++) {
			String thisChar = StringUtils.mid(value, i, 1);
			
			if (CharSet.ASCII_ALPHA_UPPER.contains(CharUtils.toChar(thisChar))) {
				containsUpper = true;
			}
			
			if (CharSet.ASCII_NUMERIC.contains(CharUtils.toChar(thisChar))) {
				containsNumber = true;
			}
		}
		
		result = (containsUpper && containsNumber);
		
		return result;
	}
	
}
