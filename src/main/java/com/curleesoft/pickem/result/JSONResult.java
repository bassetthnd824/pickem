package com.curleesoft.pickem.result;

import java.io.PrintWriter;
import java.util.Date;

import org.apache.struts2.ServletActionContext;

import com.curleesoft.pickem.converter.DateSerializerDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.util.ValueStack;

public class JSONResult implements Result {

	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_PARAM = "resultModelName";
	
	private String resultModelName;
	
	public String getResultModelName() {
		return resultModelName;
	}
	
	public void setResultModelName(String resultModelName) {
		this.resultModelName = resultModelName;
	}
	
	@Override
	public void execute(ActionInvocation invocation) throws Exception {
		
		ServletActionContext.getResponse().setContentType("application/json");
		PrintWriter responseStream = ServletActionContext.getResponse().getWriter();
		
		ValueStack valueStack = invocation.getStack();
		Object jsonModel = valueStack.findValue(resultModelName);
		
		Gson gson = new GsonBuilder()
				.registerTypeAdapter(Date.class, new DateSerializerDeserializer())
				.serializeNulls()
				.create();
		String result = gson.toJson(jsonModel);
		
		responseStream.println(result);
	}

}
