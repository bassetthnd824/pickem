package com.curleesoft.pickem.rest.dto;

import java.util.Date;

import com.curleesoft.pickem.model.AbstractBaseEntity;

public abstract class AbstractBaseDTO {
	
	private Date lastUpdateDate;
	private String lastUpdateUser;
	private Date createDate;
	private String createUser;
	private Long sysModCount;
	
	public AbstractBaseDTO() {}
	
	public AbstractBaseDTO(final AbstractBaseEntity entity) {
		if (entity != null) {
			this.lastUpdateDate = entity.getLastUpdateDate();
			this.lastUpdateUser = entity.getLastUpdateUser();
			this.createDate = entity.getCreateDate();
			this.createUser = entity.getCreateUser();
			this.sysModCount = entity.getSysModCount();
		}
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}

	public String getLastUpdateUser() {
		return lastUpdateUser;
	}

	public void setLastUpdateUser(String lastUpdateUser) {
		this.lastUpdateUser = lastUpdateUser;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Long getSysModCount() {
		return sysModCount;
	}

	public void setSysModCount(Long sysModCount) {
		this.sysModCount = sysModCount;
	}
	
}
