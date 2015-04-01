package com.curleesoft.pickem.dao;

import java.io.Serializable;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

public interface GenericDAO<T, ID extends Serializable> {
	
	public T findById(ID id, boolean lock);
	
	public List<T> findAll();
	
	public List<T> findAll(MultivaluedMap<String, String> queryParameters);
	
	public List<T> findByExample(T exampleInstance, String... excludeProperty);
	
	public T makePersistent(T entity);
	
	public void makeTransient(T entity);
	
	public void flush();
	
	public void clear();
}
