package com.curleesoft.pickem.rest.dto;

import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Group;
import com.curleesoft.pickem.model.UserGroup;

public class GroupDTO extends AbstractBaseDTO<Group> implements DataTransferObject<Group> {
	
	private String groupName;
	private Set<UserGroupDTO> userGroups;
	
	public GroupDTO() {}
	
	public GroupDTO(final Group entity) {
		super(entity);
		this.groupName = entity.getGroupName();
		
		for (UserGroup userGroup : entity.getUserGroups()) {
			this.userGroups.add(new UserGroupDTO(userGroup));
		}
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public Set<UserGroupDTO> getUserGroups() {
		return userGroups;
	}
	
	public void setUserGroups(Set<UserGroupDTO> userGroups) {
		this.userGroups = userGroups;
	}
	
	public Group fromDTO(Group entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Group();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, UserGroup.class, "userGroups", entityManager);
		
		entity.setGroupName(this.groupName);
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
