package com.curleesoft.pickem.rest.dto;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;

public class UserDTO extends AbstractBaseDTO implements DataTransferObject {
	
	private String emailAddr;
	private String firstName;
	private String lastName;
	private String userId;
	private ThemeDTO theme;
	private Set<UserGroupDTO> userGroups;
	//private Set<UserPickDTO> userPicks;
	
	public UserDTO() {}
	
	public UserDTO(final User entity) {
		super(entity);
		
		if (entity != null) {
			this.emailAddr = entity.getEmailAddr();
			this.firstName = entity.getFirstName();
			this.lastName = entity.getLastName();
			this.userId = entity.getUserId();
			this.theme = new ThemeDTO(entity.getTheme());
			this.userGroups = new HashSet<UserGroupDTO>();
			
			for (UserGroup userGroup : entity.getUserGroups()) {
				this.userGroups.add(new UserGroupDTO(userGroup));
			}
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
	
	public ThemeDTO getTheme() {
		return theme;
	}

	public void setTheme(ThemeDTO theme) {
		this.theme = theme;
	}

	public Set<UserGroupDTO> getUserGroups() {
		return userGroups;
	}
	
	public void setUserGroups(Set<UserGroupDTO> userGroups) {
		this.userGroups = userGroups;
	}
	
	public User fromDTO(User entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new User();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		populateEntityCollection(entity, UserGroup.class, "userGroups", entityManager);
		
		entity.setEmailAddr(this.emailAddr);
		entity.setFirstName(this.firstName);
		entity.setLastName(this.lastName);
		entity.setUserId(this.userId);
		
		if (this.theme != null) {
			entity.setTheme(this.theme.fromDTO(entity.getTheme(), entityManager));
		}
		
		entity = entityManager.merge(entity);
		return entity;
	}
}
