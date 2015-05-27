package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.User;

public class NestedUserDTO extends AbstractBaseDTO<User> implements DataTransferObject<User> {
	
	private String emailAddr;
	private String firstName;
	private String lastName;
	private String userId;
	//private ThemeDTO theme;
	
	public NestedUserDTO() {}
	
	public NestedUserDTO(final User entity) {
		super(entity);
		
		if (entity != null) {
			this.emailAddr = entity.getEmailAddr();
			this.firstName = entity.getFirstName();
			this.lastName = entity.getLastName();
			this.userId = entity.getUserId();
		}
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
	
	public User fromDTO(User entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new User();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setEmailAddr(this.emailAddr);
		entity.setFirstName(this.firstName);
		entity.setLastName(this.lastName);
		entity.setUserId(this.userId);
		
//		if (this.theme != null) {
//			entity.setTheme(this.theme.fromDTO(entity.getTheme(), entityManager));
//		}
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
