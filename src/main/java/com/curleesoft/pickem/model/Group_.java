package com.curleesoft.pickem.model;

import javax.annotation.Generated;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.597-0500")
@StaticMetamodel(Group.class)
public class Group_ extends AbstractBaseEntity_ {
	public static volatile SingularAttribute<Group, Long> id;
	public static volatile SingularAttribute<Group, String> groupName;
	public static volatile SetAttribute<Group, UserGroup> userGroups;
}
