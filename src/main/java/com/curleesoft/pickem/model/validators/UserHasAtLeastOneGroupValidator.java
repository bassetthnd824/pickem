package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;
import com.curleesoft.pickem.model.constraints.AssertUserHasAtLeastOneGroup;

public class UserHasAtLeastOneGroupValidator implements ConstraintValidator<AssertUserHasAtLeastOneGroup, User> {
	
	@Override
	public void initialize(AssertUserHasAtLeastOneGroup constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(User user, ConstraintValidatorContext context) {
		
		boolean result = false;
		
		if (user == null) {
			return false;
		}
		
		if (user.getUserGroups().size() < 1) {
			return false;
		}
		
		for (UserGroup userGroup : user.getUserGroups()) {
			if (userGroup != null) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
}
