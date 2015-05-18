package com.curleesoft.pickem.rest.dto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.User;
import com.curleesoft.pickem.model.UserGroup;

public class UserDTO extends AbstractBaseDTO {
	
	private Long id;
	private String emailAddr;
	private String firstName;
	private String lastName;
	private String userId;
	//private ThemeDTO theme;
	private Set<UserGroupDTO> userGroups;
	//private Set<UserPickDTO> userPicks;
	
	public UserDTO() {}
	
	public UserDTO(final User entity) {
		super(entity);
		
		if (entity != null) {
			this.id = entity.getId();
			this.emailAddr = entity.getEmailAddr();
			this.firstName = entity.getFirstName();
			this.lastName = entity.getLastName();
			this.userId = entity.getUserId();
			this.userGroups = new HashSet<UserGroupDTO>();
			
			for (UserGroup userGroup : entity.getUserGroups()) {
				this.userGroups.add(new UserGroupDTO(userGroup));
			}
		}
	}
	
	public User fromDTO(User entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new User();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		Iterator<UserGroup> it = entity.getUserGroups().iterator();
		
		while (it.hasNext()) {
			boolean found = false;
			UserGroup userGroup = it.next();
			Iterator<UserGroupDTO> itDto = this.getUserGroups().iterator();
			
			while (itDto.hasNext()) {
				UserGroupDTO userGroupDTO = itDto.next();
				
				if (userGroupDTO.getId().equals(userGroup.getId())) {
					found = true;
					break;
				}
			}
			
			if (found == false) {
				it.remove();
			}
		}
		
		Iterator<UserGroupDTO> itDto = this.getUserGroups().iterator();
		
		while (itDto.hasNext()) {
			boolean found = false;
			UserGroupDTO userGroupDTO = itDto.next();
			it = entity.getUserGroups().iterator();
			
			while (it.hasNext()) {
				UserGroup userGroup = it.next();
				
				if (userGroupDTO.getId().equals(userGroup.getId())) {
					found = true;
					break;
				}
			}
			
			if (found == false) {
				Iterator<UserGroup> resultIter = entityManager.createQuery("SELECT DISTINCT u FROM UserGroup u", UserGroup.class).getResultList().iterator();
				
				while (resultIter.hasNext()) {
					UserGroup result = resultIter.next();
					
					if (result.getId().equals(userGroupDTO.getId())) {
						entity.getUserGroups().add(result);
						break;
					}
				}
			}
		}
		
		entity.setEmailAddr(this.emailAddr);
		entity.setFirstName(this.firstName);
		entity.setLastName(this.lastName);
		
//		if (this.theme != null) {
//			entity.setTheme(this.theme.fromDTO(entity.getTheme(), entityManager));
//		}
		
		entity = entityManager.merge(entity);
		return entity;
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
	
	public Set<UserGroupDTO> getUserGroups() {
		return userGroups;
	}
	
	public void setUserGroups(Set<UserGroupDTO> userGroups) {
		this.userGroups = userGroups;
	}
	
}
