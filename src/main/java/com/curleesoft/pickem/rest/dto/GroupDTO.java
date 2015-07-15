package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Group;

public class GroupDTO extends AbstractBaseDTO<Group> implements DataTransferObject<Group> {
	
	private String groupName;
	
	public GroupDTO() {}
	
	public GroupDTO(final Group entity) {
		super(entity);
		this.groupName = entity.getGroupName();
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
		
		return entity;
	}
}
