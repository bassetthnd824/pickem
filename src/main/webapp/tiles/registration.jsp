<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<%@ taglib uri="/struts-bootstrap-tags" prefix="sb" %>

<div class="row">
	<div class="col-sm-4 col-sm-offset-4">
		<h2>Register</h2>
		
		<s:form action="register_add" cssClass="clearfix">
			<s:textfield label="Email Address" name="emailAddress"/>
			<s:password label="Password" name="userPass"/>
			<s:password label="Confirm Password" name="confirmPass"/>
			<s:textfield label="First Name" name="firstName"/>
			<s:textfield label="Last Name" name="lastName"/>
			<s:textfield label="Screen Name" name="nickName"/>
			
			<div class="pull-right">
				<button type="button" id="resetButton" class="btn btn-default">Reset</button>
				<button type="submit" id="submitButton" class="btn btn-default btn-primary">Submit</button>
			</div>
		</s:form>
	</div>
</div>