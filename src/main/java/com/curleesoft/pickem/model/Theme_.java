package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.608-0500")
@StaticMetamodel(Theme.class)
public class Theme_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Theme, Long> id;
	public static volatile SingularAttribute<Theme, Boolean> active;
	public static volatile SingularAttribute<Theme, String> themeName;
	public static volatile SingularAttribute<Theme, String> themePath;
}
