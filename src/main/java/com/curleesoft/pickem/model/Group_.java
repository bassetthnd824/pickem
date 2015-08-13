package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-08-12T14:59:42.138-0500")
@StaticMetamodel(Group.class)
public class Group_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Group, Long> id;
	public static volatile SingularAttribute<Group, String> groupName;
	public static volatile ListAttribute<Group, User> users;
}
