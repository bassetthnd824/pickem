package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.UserGroup;

public class UserGroupDTO extends AbstractBaseDTO<UserGroup> implements DataTransferObject<UserGroup> {
	
	private NestedUserDTO user;
	private NestedGroupDTO group;
	
	public UserGroupDTO() {}
	
	public UserGroupDTO(final UserGroup entity) {
		super(entity);
		
		if (entity != null) {
			this.user = new NestedUserDTO(entity.getUser());
			this.group = new NestedGroupDTO(entity.getGroup());
		}
	}
	
	public NestedUserDTO getUser() {
		return user;
	}
	
	public void setUser(NestedUserDTO user) {
		this.user = user;
	}
	
	public NestedGroupDTO getGroup() {
		return group;
	}
	
	public void setGroup(NestedGroupDTO group) {
		this.group = group;
	}
	
	public UserGroup fromDTO(UserGroup entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new UserGroup();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setUser(this.user.fromDTO(entity.getUser(), entityManager));
		entity.setGroup(this.group.fromDTO(entity.getGroup(), entityManager));
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
