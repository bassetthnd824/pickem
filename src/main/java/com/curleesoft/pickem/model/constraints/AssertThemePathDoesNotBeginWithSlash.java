package com.curleesoft.pickem.model.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.curleesoft.pickem.model.validators.ThemePathDoesNotBeginWithSlashValidator;

@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ThemePathDoesNotBeginWithSlashValidator.class)
@Documented
public @interface AssertThemePathDoesNotBeginWithSlash {
	String message() default "theme path must not begin with a slash";
	
	Class<?>[] groups() default {};
	
	Class<? extends Payload>[] payload() default {};
}
