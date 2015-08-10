package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-08-10T14:11:12.812-0500")
@StaticMetamodel(Group.class)
public class Group_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Group, Long> id;
	public static volatile SingularAttribute<Group, String> groupName;
	public static volatile SetAttribute<Group, User> users;
}
