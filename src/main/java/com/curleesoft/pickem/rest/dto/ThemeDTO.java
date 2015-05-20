package com.curleesoft.pickem.rest.dto;

import javax.persistence.EntityManager;

import com.curleesoft.pickem.model.Theme;

public class ThemeDTO extends AbstractBaseDTO implements DataTransferObject {
	
	private boolean active;
	private String themeName;
	private String themePath;
	
	public ThemeDTO() {}
	
	public ThemeDTO(final Theme entity) {
		super(entity);
		
		if (entity != null) {
			this.active = entity.getActive();
			this.themeName = entity.getThemeName();
			this.themePath = entity.getThemePath();
		}
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getThemeName() {
		return themeName;
	}

	public void setThemeName(String themeName) {
		this.themeName = themeName;
	}

	public String getThemePath() {
		return themePath;
	}

	public void setThemePath(String themePath) {
		this.themePath = themePath;
	}

	public Theme fromDTO(Theme entity, EntityManager entityManager) {
		if (entity == null) {
			entity = new Theme();
		}
		
		entity = super.fromDTO(entity, entityManager);
		
		entity.setActive(this.active);
		entity.setThemeName(this.themeName);;
		entity.setThemePath(this.themePath);
		
		entity = entityManager.merge(entity);
		return entity;
	}
	
}
