package com.curleesoft.pickem.model.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.curleesoft.pickem.form.PasswordConfirmationForm;
import com.curleesoft.pickem.model.constraints.AssertPasswordsMatch;

public class PasswordsMatchValidator implements ConstraintValidator<AssertPasswordsMatch, PasswordConfirmationForm> {
	
	private static final Log log = LogFactory.getLog(PasswordsMatchValidator.class);
	
	@Override
	public void initialize(AssertPasswordsMatch constraintAnnotation) {
		
	}

	@Override
	public boolean isValid(PasswordConfirmationForm passwordConfirmationForm, ConstraintValidatorContext context) {
		
		log.debug("In isValid method of " + this.getClass().getName());
		boolean isValid = false;
		
		if (passwordConfirmationForm == null || passwordConfirmationForm.getUserPass() == null || passwordConfirmationForm.getConfirmPass() == null) {
			log.debug("registerForm == null.  Returning true");
			return true;
		}
		
		isValid = (passwordConfirmationForm.getUserPass().equals(passwordConfirmationForm.getConfirmPass()));
		
		if (!isValid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("passwords do not match")
					.addNode("userPass").addConstraintViolation();
		}
		
		log.debug("Returning result of " + isValid);
		
		return isValid;
	}
	
}
