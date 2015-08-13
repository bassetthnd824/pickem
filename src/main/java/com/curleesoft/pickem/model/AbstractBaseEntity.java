package com.curleesoft.pickem.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

@MappedSuperclass
public abstract class AbstractBaseEntity<ID> {
	
	private Date lastUpdateDate;
	private String lastUpdateUser;
	private Date createDate;
	private String createUser;
	private Long sysModCount;
	
	@Transient
	public abstract ID getId();
	@Transient
	public abstract void setId(ID id);
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LAST_UPDATE_DATE", nullable = false)
	public Date getLastUpdateDate() {
		return this.lastUpdateDate;
	}
	
	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}
	
	@Column(name = "LAST_UPDATE_USER", nullable = false, length = 200)
	public String getLastUpdateUser() {
		return this.lastUpdateUser;
	}
	
	public void setLastUpdateUser(String lastUpdateUser) {
		this.lastUpdateUser = lastUpdateUser;
	}
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATE_DATE", nullable = false)
	public Date getCreateDate() {
		return this.createDate;
	}
	
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	@Column(name = "CREATE_USER", nullable = false, length = 200)
	public String getCreateUser() {
		return this.createUser;
	}
	
	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}
	
	@Version
	@Column(name = "SYS_MOD_COUNT", updatable = false, nullable = false)
	public Long getSysModCount() {
		return this.sysModCount;
	}
	
	public void setSysModCount(Long sysModCount) {
		this.sysModCount = sysModCount;
	}
	
	@PrePersist
	public void preInsert() {
		this.createDate = this.lastUpdateDate = new Date();
		this.sysModCount = 0L;
	}
	
	@PreUpdate
	public void preUpdate() {
		this.lastUpdateDate = new Date();
	}
}
