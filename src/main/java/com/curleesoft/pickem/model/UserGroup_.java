package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.612-0500")
@StaticMetamodel(UserGroup.class)
public class UserGroup_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<UserGroup, Long> id;
	public static volatile SingularAttribute<UserGroup, User> user;
	public static volatile SingularAttribute<UserGroup, Group> group;
}
