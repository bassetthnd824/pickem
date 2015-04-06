package com.curleesoft.pickem.service;

public class UserRegistration {

	private String emailAddress;
	private String userPass;
	private String confirmPass;
	private String firstName;
	private String lastName;

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getUserPass() {
		return userPass;
	}

	public void setUserPass(String userPass) {
		this.userPass = userPass;
	}

	public String getConfirmPass() {
		return confirmPass;
	}

	public void setConfirmPass(String confirmPass) {
		this.confirmPass = confirmPass;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

}
