package com.curleesoft.pickem.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * The persistent class for the PCKM_USER_GROUP database table.
 * 
 */
@Entity
@Table(name = "PCKM_USER_GROUP")
public class UserGroup extends AbstractBaseEntity implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private String userGroup;
	private User user;
	
	public UserGroup() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_USER_GROUP_ID_GENERATOR", sequenceName = "PCKM_USER_GROUP_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_USER_GROUP_ID_GENERATOR")
	@Column(name = "USER_GROUP_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "USER_GROUP", nullable = false, length = 20)
	@NotNull(message = "user group cannot be blank")
	public String getUserGroup() {
		return this.userGroup;
	}
	
	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}
	
	// bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name = "USER_GUID", nullable = false)
	public User getUser() {
		return this.user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		UserGroup that = (UserGroup) o;

		if (user != null ? !user.equals(that.user) : that.user != null) {
			return false;
		}
		
		if (userGroup != null ? !userGroup.equals(that.userGroup) : that.userGroup != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = user != null ? user.hashCode() : 0;
		result = 31 * result + (userGroup != null ? userGroup.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return user.toString() + " " + userGroup;
	}
	
}