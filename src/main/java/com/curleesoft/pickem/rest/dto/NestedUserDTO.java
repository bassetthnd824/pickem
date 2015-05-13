package com.curleesoft.pickem.rest.dto;

import com.curleesoft.pickem.model.User;


public class NestedUserDTO extends AbstractBaseDTO {
	
	private Long id;
	private String emailAddr;
	private String firstName;
	private String lastName;
	private String userId;
	//private ThemeDTO theme;
	
	public NestedUserDTO() {}
	
	public NestedUserDTO(final User entity) {
		super(entity);
		
		if (entity != null) {
			this.id = entity.getId();
			this.emailAddr = entity.getEmailAddr();
			this.firstName = entity.getFirstName();
			this.lastName = entity.getLastName();
			this.userId = entity.getUserId();
		}
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getEmailAddr() {
		return emailAddr;
	}
	
	public void setEmailAddr(String emailAddr) {
		this.emailAddr = emailAddr;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}
