package com.curleesoft.pickem.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import com.curleesoft.pickem.model.constraints.AssertThemePathDoesNotBeginWithSlash;

/**
 * The persistent class for the PCKM_THEME database table.
 * 
 */
@Entity
@Table(name = "PCKM_THEME")
public class Theme extends AbstractBaseEntity implements Serializable, PickemEntity {
	
	private static final long serialVersionUID = 1L;
	private Long id;
	private boolean active;
	private String themeName;
	private String themePath;
	
	public Theme() {
	}
	
	@Id
	@SequenceGenerator(name = "PCKM_THEME_ID_GENERATOR", sequenceName = "PCKM_THEME_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_THEME_ID_GENERATOR")
	@Column(name = "THEME_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(nullable = false, length = 1)
	@NotNull
	@Type(type = "yes_no")
	public boolean getActive() {
		return this.active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	@Column(name = "THEME_NAME", nullable = false, unique = true, length = 40)
	@NotNull(message = "theme name cannot be blank")
	public String getThemeName() {
		return this.themeName;
	}
	
	public void setThemeName(String themeName) {
		this.themeName = themeName;
	}
	
	@Column(name = "THEME_PATH", length = 100)
	@AssertThemePathDoesNotBeginWithSlash
	public String getThemePath() {
		return this.themePath;
	}
	
	public void setThemePath(String themePath) {
		this.themePath = themePath;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Theme that = (Theme) o;

		if (themeName != null ? !themeName.equals(that.themeName) : that.themeName != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return themeName.hashCode();
	}

	@Override
	public String toString() {
		return themeName;
	}
	
}