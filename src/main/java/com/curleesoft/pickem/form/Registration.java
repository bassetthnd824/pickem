package com.curleesoft.pickem.form;

import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.curleesoft.pickem.model.constraints.AssertPasswordsMatch;
import com.curleesoft.pickem.model.constraints.AssertUserPassProperlyFormed;

@AssertPasswordsMatch
public class Registration implements PasswordConfirmationForm {
	
	private String emailAddr;
	private String userPass;
	private String confirmPass;
	private String firstName;
	private String lastName;
	private String nickName;
	
	@NotBlank
	@Size(max = 200)
	@Email
	public String getEmailAddr() {
		return emailAddr;
	}
	
	public void setEmailAddr(String emailAddr) {
		this.emailAddr = emailAddr;
	}
	
	@NotBlank
	@Size(min = 6)
	@AssertUserPassProperlyFormed
	@Override
	public String getUserPass() {
		return userPass;
	}
	
	@Override
	public void setUserPass(String userPass) {
		this.userPass = userPass;
	}
	
	@NotBlank
	@Size(min = 6)
	@AssertUserPassProperlyFormed
	@Override
	public String getConfirmPass() {
		return confirmPass;
	}
	
	@Override
	public void setConfirmPass(String confirmPass) {
		this.confirmPass = confirmPass;
	}
	
	@NotBlank
	@Size(max = 40)
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	@NotBlank
	@Size(max = 40)
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@NotBlank
	@Size(max = 40)
	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
}
