package com.curleesoft.pickem.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2015-07-09T09:57:25.549-0500")
@StaticMetamodel(AbstractBaseEntity.class)
public class AbstractBaseEntity_ {
	public static volatile SingularAttribute<AbstractBaseEntity, Date> lastUpdateDate;
	public static volatile SingularAttribute<AbstractBaseEntity, String> lastUpdateUser;
	public static volatile SingularAttribute<AbstractBaseEntity, Date> createDate;
	public static volatile SingularAttribute<AbstractBaseEntity, String> createUser;
	public static volatile SingularAttribute<AbstractBaseEntity, Long> sysModCount;
}
