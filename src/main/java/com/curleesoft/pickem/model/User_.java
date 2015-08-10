package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-08-10T14:04:31.860-0500")
@StaticMetamodel(User.class)
public class User_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<User, Long> id;
	public static volatile SingularAttribute<User, String> emailAddr;
	public static volatile SingularAttribute<User, String> firstName;
	public static volatile SingularAttribute<User, String> lastName;
	public static volatile SingularAttribute<User, String> userId;
	public static volatile SingularAttribute<User, String> userPass;
	public static volatile SingularAttribute<User, String> nickName;
	public static volatile SingularAttribute<User, Theme> theme;
	public static volatile SetAttribute<User, Group> groups;
}
