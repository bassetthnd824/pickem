package com.curleesoft.pickem.model.constraints;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.curleesoft.pickem.model.validators.MatchupTeamsNotEqualValidator;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = MatchupTeamsNotEqualValidator.class)
@Documented
public @interface AssertMatchupTeamsNotEqual {
	String message() default "teams cannot be the same";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
	
}
