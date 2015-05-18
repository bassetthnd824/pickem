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

/**
 * The persistent class for the PCKM_USER_GROUP database table.
 * 
 */
@Entity
@Table(name = "PCKM_USER_GROUP")
public class UserGroup extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private User user;
	private Group group;
	
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
	
	// bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name = "USER_GUID", nullable = false)
	public User getUser() {
		return this.user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}

	// bi-directional many-to-one association to User
	@ManyToOne
	@JoinColumn(name = "GROUP_ID", nullable = false)
	public Group getGroup() {
		return this.group;
	}
	
	public void setGroup(Group group) {
		this.group = group;
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
		
		if (group != null ? !group.equals(that.group) : that.group != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = user != null ? user.hashCode() : 0;
		result = 31 * result + (group != null ? group.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return user.toString() + " " + group.toString();
	}
	
}