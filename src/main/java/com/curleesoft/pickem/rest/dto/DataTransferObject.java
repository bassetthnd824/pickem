package com.curleesoft.pickem.rest.dto;

import java.util.Date;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.PickemEntity;

public interface DataTransferObject {
	
	public Long getId();
	public void setId(Long id);
	
	public Date getLastUpdateDate();
	public void setLastUpdateDate(Date lastUpdateDate);
	
	public String getLastUpdateUser();
	public void setLastUpdateUser(String lastUpdateUser);

	public Date getCreateDate();
	public void setCreateDate(Date createDate);

	public String getCreateUser();
	public void setCreateUser(String createUser);

	public Long getSysModCount();
	public void setSysModCount(Long sysModCount);
	
	public <E extends PickemEntity> E fromDTO(E entity, EntityManager entityManager);
}
