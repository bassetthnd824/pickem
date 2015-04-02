package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Entity implementation class for Entity: Group
 *
 */
@Entity
@Table(name = "PCKM_GROUP")
public class Group extends AbstractBaseEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String groupName;
	private Set<UserGroup> userGroups;

	@Id
	@SequenceGenerator(name = "PCKM_GROUP_ID_GENERATOR", sequenceName = "PCKM_GROUP_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_GROUP_ID_GENERATOR")
	@Column(name = "GROUP_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "GROUP_NAME", nullable = false, length = 20)
	@NotNull(message = "group name cannot be blank")
	public String getGroupName() {
		return this.groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	// bi-directional many-to-one association to UserGroup
	@OneToMany(mappedBy = "group", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.EAGER)
	public Set<UserGroup> getUserGroups() {
		if (this.userGroups == null) {
			this.userGroups = new HashSet<UserGroup>();
		}
		
		return this.userGroups;
	}
	
	public void setUserGroups(Set<UserGroup> userGroups) {
		this.userGroups = userGroups;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Group that = (Group) o;

		if (groupName != null ? !groupName.equals(that.groupName) : that.groupName != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return groupName.hashCode();
	}

	@Override
	public String toString() {
		return groupName;
	}
	
}
