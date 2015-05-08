package com.curleesoft.pickem.rest.dto;

import com.curleesoft.pickem.model.Group;

public class NestedGroupDTO {
	
	private Long id;
	private String groupName;
	
	public NestedGroupDTO() {}
	
	public NestedGroupDTO(final Group entity) {
		if (entity != null) {
			this.id = entity.getId();
			this.groupName = entity.getGroupName();
		}
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

}
