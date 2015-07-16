package com.curleesoft.pickem.converter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.util.StrutsTypeConverter;

import com.opensymphony.xwork2.conversion.TypeConversionException;

public class DateConverter extends StrutsTypeConverter {
	
	private static final String SHORT_FORMAT_STRING = "yyyy-MM-dd";
	private static final String LONG_FORMAT_STRING = "yyyy-MM-dd hh:mm:ss a";
	
	
	@Override
	@SuppressWarnings("rawtypes")
	public Object convertFromString(Map context, String[] values, Class toClass) {
		
		String inputString = values[0];
		String formatString = SHORT_FORMAT_STRING;
		Date newDate = null;
		
		if (StringUtils.length(inputString) > 10) {
			formatString = LONG_FORMAT_STRING;
		}
		
		try {
			newDate = (StringUtils.isNotBlank(inputString)) ? new SimpleDateFormat(formatString).parse(inputString) : null;
		} catch (ParseException e) {
			throw new TypeConversionException(e);
		}
		
		return newDate;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String convertToString(Map context, Object o) {
		String formatString = SHORT_FORMAT_STRING;
		Date inputDate = (Date) o;
		Calendar cal = Calendar.getInstance();
		cal.setTime(inputDate);
		
		if (cal.get(Calendar.HOUR) > 0 || cal.get(Calendar.MINUTE) > 0 || cal.get(Calendar.SECOND) > 0 || cal.get(Calendar.MILLISECOND) > 0) {
			formatString = LONG_FORMAT_STRING;
		}
		
		return new SimpleDateFormat(formatString).format(inputDate);
	}

}
