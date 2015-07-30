package com.curleesoft.pickem.form;

import com.curleesoft.pickem.model.User;

public class UserScore implements Comparable<UserScore> {
	
	private User user;
	private Long score;
	
	public UserScore(User user, Long score) {
		this.user = user;
		this.score = score;
	}
	
	public User getUser() {
		return user;
	}
	
	public Long getScore() {
		return score;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		
		UserScore that = (UserScore) o;
		
		if (user != null ? !user.equals(that.user) : that.user != null) {
			return false;
		}
		
		if (score != null ? !score.equals(that.score) : that.score != null) {
			return false;
		}
		
		return true;
	}

	@Override
	public int hashCode() {
		int result = user != null ? user.hashCode() : 0;
		result = 31 * result + (score != null ? score.hashCode() : 0);
		return result;
	}

	@Override
	public int compareTo(UserScore o) {
		int result = 0;
		
		if (this == o) {
			return result;
		}
		
		result = o.score.compareTo(this.score);
		
		if (result == 0) {
			result = this.user.compareTo(o.user);
		}
		
		return result;
	}
}
