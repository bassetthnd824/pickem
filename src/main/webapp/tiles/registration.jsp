<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="/struts-bootstrap-tags" prefix="sb" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<h2>Register</h2>
		
		<s:if test="hasActionErrors()">
			<s:actionerror/>
		</s:if>
		
		<s:form action="register_save" cssClass="form-horizontal clearfix margin-before">
			<s:textfield label="Email Address" name="emailAddr" requiredLabel="true"/>
			<s:password label="Password" name="userPass" requiredLabel="true"/>
			<s:password label="Confirm Password" name="confirmPass" requiredLabel="true"/>
			<s:textfield label="First Name" name="firstName" requiredLabel="true"/>
			<s:textfield label="Last Name" name="lastName" requiredLabel="true"/>
			<s:textfield label="Screen Name" name="nickName" requiredLabel="true"/>
			
			<div class="pull-right">
				<button type="button" id="resetButton" class="btn btn-default">Reset</button>
				<button type="submit" id="submitButton" class="btn btn-default btn-primary">Submit</button>
			</div>
		</s:form>
	</div>
</div>