package com.curleesoft.pickem.model;

import java.util.Date;

public interface PickemEntity {
	
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
	
}
