package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Group;

public class NestedGroupDTO extends AbstractBaseDTO<Group> implements DataTransferObject<Group> {
	
	private String groupName;
	
	public NestedGroupDTO() {}
	
	public NestedGroupDTO(final Group entity) {
		super(entity);
		
		if (entity != null) {
			this.groupName = entity.getGroupName();
		}
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public Group fromDTO(Group entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Group();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setGroupName(this.groupName);
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
