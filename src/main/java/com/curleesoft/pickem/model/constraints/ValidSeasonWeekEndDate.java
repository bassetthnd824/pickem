package com.curleesoft.pickem.model.constraints;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.curleesoft.pickem.model.validators.SeasonWeekEndDateValidator;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = SeasonWeekEndDateValidator.class)
@Documented
public @interface ValidSeasonWeekEndDate {
	String message() default "season week end date is invalid";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
	
}
