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

import org.apache.commons.lang.StringUtils;

/**
 * The persistent class for the PCKM_VENUE database table.
 * 
 */
@Entity
@Table(name = "PCKM_VENUE")
public class Venue extends AbstractBaseEntity implements Serializable, PickemEntity {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String cityState;
	private String venueName;

	public Venue() {
	}

	@Id
	@SequenceGenerator(name = "PCKM_VENUE_ID_GENERATOR", sequenceName = "PCKM_VENUE_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PCKM_VENUE_ID_GENERATOR")
	@Column(name = "VENUE_ID", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "CITY_STATE", nullable = false, length = 60)
	@NotNull(message = "city/state cannot be blank")
	public String getCityState() {
		return this.cityState;
	}

	public void setCityState(String cityState) {
		this.cityState = cityState;
	}

	@Column(name = "VENUE_NAME", nullable = false, length = 60)
	@NotNull(message = "venue name cannot be blank")
	public String getVenueName() {
		return this.venueName;
	}

	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Venue that = (Venue) o;

		if (venueName != null ? !venueName.equals(that.venueName) : that.venueName != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		return StringUtils.defaultString(venueName).hashCode();
	}

	@Override
	public String toString() {
		return StringUtils.defaultString(venueName);
	}
	
}