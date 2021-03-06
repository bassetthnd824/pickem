package com.curleesoft.pickem.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;

/**
 * Entity implementation class for Entity: Group
 *
 */
@Entity
@Table(name = "PCKM_GROUP")
public class Group extends AbstractBaseEntity<Long> implements Serializable, PickemEntity {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String groupName;
	private List<User> users;

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
	@NotNull
	@Size(max = 20)
	public String getGroupName() {
		return this.groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	@ManyToMany(mappedBy = "groups")
	public List<User> getUsers() {
		return this.users;
	}
	
	public void setUsers(List<User> users) {
		this.users = users;
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
		return StringUtils.defaultString(groupName).hashCode();
	}

	@Override
	public String toString() {
		return StringUtils.defaultString(groupName);
	}
	
}
