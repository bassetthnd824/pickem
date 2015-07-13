package com.curleesoft.pickem.util;

import org.jboss.security.auth.spi.Util;

public class Globals {
	
	public static final String ACTIVE_USER = "com.curleesoft.pickem.ActiveUser";
	public static final String CURRENT_SEASON = "com.curleesoft.pickem.CurrentSeason";
	
	public static String generateHash(final String stringToHash) {
		return Util.createPasswordHash("SHA-256", Util.BASE64_ENCODING, null, null, stringToHash);
	}
}
