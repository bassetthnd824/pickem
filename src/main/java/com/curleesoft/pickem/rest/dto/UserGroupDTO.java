package com.curleesoft.pickem.rest.dto;

import com.curleesoft.pickem.model.UserGroup;

public class UserGroupDTO {
	
	private Long id;
	private NestedUserDTO user;
	private NestedGroupDTO group;
	
	public UserGroupDTO() {}
	
	public UserGroupDTO(final UserGroup entity) {
		if (entity != null) {
			this.id = entity.getId();
			this.user = new NestedUserDTO(entity.getUser());
			this.group = new NestedGroupDTO(entity.getGroup());
		}
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
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

}
