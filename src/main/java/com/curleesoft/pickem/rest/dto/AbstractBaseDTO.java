package com.curleesoft.pickem.rest.dto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;

import com.curleesoft.pickem.model.PickemEntity;

public abstract class AbstractBaseDTO implements DataTransferObject {
	
	private Long id;
	private Date lastUpdateDate;
	private String lastUpdateUser;
	private Date createDate;
	private String createUser;
	private Long sysModCount;
	
	public AbstractBaseDTO() {}
	
	public AbstractBaseDTO(final PickemEntity entity) {
		if (entity != null) {
			this.id = entity.getId();
			this.lastUpdateDate = entity.getLastUpdateDate();
			this.lastUpdateUser = entity.getLastUpdateUser();
			this.createDate = entity.getCreateDate();
			this.createUser = entity.getCreateUser();
			this.sysModCount = entity.getSysModCount();
		}
	}
	
	public <E extends PickemEntity> E fromDTO(E entity, EntityManager entityManager) {
		entity.setId(this.id);
		entity.setLastUpdateDate(this.lastUpdateDate);
		entity.setLastUpdateUser(this.lastUpdateUser);
		entity.setCreateDate(this.createDate);
		entity.setCreateUser(this.createUser);
		entity.setSysModCount(this.sysModCount);
		
		return entity;
	}
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
	
	@SuppressWarnings("unchecked")
	public <E1 extends PickemEntity, E2 extends PickemEntity, D extends AbstractBaseDTO> void populateEntityCollection(E1 entity, Class<E2> collectionClass, String collectionName, EntityManager entityManager) {
		try {
			String methodName = "get" + StringUtils.capitalize(collectionName);
			Method m1 = entity.getClass().getMethod(methodName);
			Method m2 = this.getClass().getMethod(methodName);
			
			Iterator<E2> it1 = ((Collection<E2>) m1.invoke(entity)).iterator();
			
			while (it1.hasNext()) {
				boolean found = false;
				E2 setEntity = it1.next();
				Iterator<D> it2 = ((Collection<D>) m2.invoke(this)).iterator();
				
				while (it2.hasNext()) {
					D dto = it2.next();
					
					if (dto.getId().equals(setEntity.getId())) {
						found = true;
						break;
					}
				}
				
				if (found == false) {
					it1.remove();
				}
			}
			
			Iterator<D> it2 = ((Collection<D>) m2.invoke(this)).iterator();
			
			while (it2.hasNext()) {
				boolean found = false;
				D dto = it2.next();
				it1 = ((Collection<E2>) m1.invoke(entity)).iterator();
				
				while (it1.hasNext()) {
					E2 setEntity = it1.next();
					
					if (dto.getId().equals(setEntity.getId())) {
						found = true;
						break;
					}
				}
				
				if (found == false) {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<E2> criteriaQuery = criteriaBuilder.createQuery(collectionClass);
					Root<E2> root = criteriaQuery.from(collectionClass);
					criteriaQuery.select(criteriaQuery.getSelection()).distinct(true);
					criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
					TypedQuery<E2> query = entityManager.createQuery(criteriaQuery);
					
					Iterator<E2> resultIter = query.getResultList().iterator();
					
					while (resultIter.hasNext()) {
						E2 result = resultIter.next();
						
						if (result.getId().equals(dto.getId())) {
							((Collection<E2>) m1.invoke(entity)).add(result);
							break;
						}
					}
				}
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

	}
}
