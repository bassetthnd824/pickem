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
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.constraints.AssertUserHasAtLeastOneGroup;
import com.curleesoft.pickem.model.constraints.AssertUserPassProperlyFormed;

/**
 * The persistent class for the PCKM_USER database table.
 * 
 */
@Entity
@Table(name = "PCKM_USER")
@AssertUserHasAtLeastOneGroup
public class User extends AbstractBaseEntity implements Comparable<User>, Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private String emailAddr;
	private String firstName;
	private String lastName;
	private String userId;
	private String userPass;
	private String nickName;
	private Theme theme;
	private Set<Group> groups;
	
	public User() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_USER_ID_GENERATOR", sequenceName = "PCKM_USER_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_USER_ID_GENERATOR")
	@Column(name = "USER_GUID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name = "EMAIL_ADDR", nullable = false, length = 200)
	@NotNull(message = "email address cannot be blank")
	public String getEmailAddr() {
		return this.emailAddr;
	}
	
	public void setEmailAddr(String emailAddr) {
		this.emailAddr = emailAddr;
	}
	
	@Column(name = "FIRST_NAME", nullable = false, length = 40)
	@NotNull(message = "first name cannot be blank")
	public String getFirstName() {
		return this.firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	@Column(name = "LAST_NAME", nullable = false, length = 40)
	@NotNull(message = "last name cannot be blank")
	public String getLastName() {
		return this.lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	@Column(name = "USER_ID", nullable = false, unique = true, length = 200)
	@NotNull(message = "user id cannot be blank")
	public String getUserId() {
		return this.userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@Column(name = "USER_PASS", nullable = false, length = 128)
	@NotNull(message = "password cannot be blank")
	@AssertUserPassProperlyFormed
	public String getUserPass() {
		return this.userPass;
	}
	
	public void setUserPass(String userPass) {
		this.userPass = userPass;
	}
	
	@Column(name = "NICK_NAME", length = 40)
	public String getNickName() {
		return this.nickName;
	}
	
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
	// uni-directional many-to-one association to Theme
	@ManyToOne
	@JoinColumn(name = "THEME_ID")
	public Theme getTheme() {
		return this.theme;
	}
	
	public void setTheme(Theme theme) {
		this.theme = theme;
	}
	
	@ManyToMany(cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
	@JoinTable(name = "PCKM_USER_GROUP", 
			joinColumns = { @JoinColumn(name = "USER_GUID") },
			inverseJoinColumns = { @JoinColumn(name = "GROUP_ID") })
	public Set<Group> getGroups() {
		if (this.groups == null) {
			this.groups = new HashSet<Group>();
		}
		
		return this.groups;
	}
	
	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	@Override
	public String toString() {
		return userId + " " + firstName + " " + lastName;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		User that = (User) o;

		if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return StringUtils.defaultString(userId).hashCode();
	}

	@Override
	public int compareTo(User o) {
		if (this == o) {
			return 0;
		}
		
		return this.userId.compareTo(o.userId);
	}
	
	@Transient
	public boolean isInGroup(Long groupId) {
		boolean result = false;
		
		for (Group group : groups) {
			if (group.getId().equals(groupId)) {
				result = true;
				break;
			}
		}
		
		return result;
	}
}