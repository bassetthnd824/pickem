package com.curleesoft.pickem.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NativeQueryResultsMapper {
	
	private static Log log = LogFactory.getLog(NativeQueryResultsMapper.class);
	
	public static <T> List<T> map(Class<T> entityType, List<Object[]> objectArrayList) {
		List<T> results = new ArrayList<T>();
		List<Field> mappingFields = getNativeQueryResultColumnAnnotatedFields(entityType);
		LongConverter longConverter = new LongConverter(null);
		BeanUtilsBean beanUtilsBean = new BeanUtilsBean();
		beanUtilsBean.getConvertUtils().register(longConverter, Long.class);
		
		try {
			for (Object[] objectArray : objectArrayList) {
				T t = entityType.newInstance();
				int i = 0;
				
				for (Object o : objectArray) {
					
					beanUtilsBean.setProperty(t, mappingFields.get(i).getName(), o);
					i++;
				}
				
				results.add(t);
			}
		} catch (InstantiationException e) {
			log.debug("Cannot instantiate: ", e);
			results.clear();
			
		} catch (IllegalAccessException e) {
			log.debug("Illegal access: ", e);
			results.clear();
			
		} catch (InvocationTargetException e) {
			log.debug("Cannot invoke method: ", e);
			results.clear();
		}
		
		return results;
	}
	
	private static <T> List<Field> getNativeQueryResultColumnAnnotatedFields(Class<T> entityType) {
		Field[] fields = entityType.getDeclaredFields();
		List<Field> orderedFields = Arrays.asList(new Field[fields.length]);
		
		for (Field field : fields) {
			if (field.isAnnotationPresent(NativeQueryResultColumn.class)) {
				NativeQueryResultColumn nqrc = field.getAnnotation(NativeQueryResultColumn.class);
				orderedFields.set(nqrc.index(), field);
			}
		}
		
		return orderedFields;
	}
}
