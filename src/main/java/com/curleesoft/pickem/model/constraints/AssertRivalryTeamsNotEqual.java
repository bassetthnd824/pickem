package com.curleesoft.pickem.model.constraints;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.curleesoft.pickem.model.validators.RivalryTeamsNotEqualValidator;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = RivalryTeamsNotEqualValidator.class)
@Documented
public @interface AssertRivalryTeamsNotEqual {
	String message() default "teams cannot be the same";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
	
}
