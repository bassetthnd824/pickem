<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<h2>Your Account</h2>
		
		<s:if test="hasActionErrors()">
			<s:actionerror/>
		</s:if>
		
		<s:form cssClass="form-horizontal clearfix margin-before">
			<s:textfield label="Email Address" name="model.emailAddr"/>
			<s:textfield label="First Name" name="model.firstName"/>
			<s:textfield label="Last Name" name="model.lastName"/>
			<s:textfield label="Screen Name" name="model.nickName"/>
			
			<p class="alert alert-warning">If you do not wish to change your password, leave it blank.</p>
			
			<s:password label="Old Password" name="model.oldPass"/>
			<s:password label="New Password" name="model.userPass"/>
			<s:password label="Confirm Password" name="model.confirmPass"/>
			
			<div class="button-container pull-right">
				<s:submit type="button" action="account" cssClass="btn btn-default">Cancel</s:submit>
				<s:submit type="button" action="account_save" cssClass="btn btn-default btn-primary">Save</s:submit>
			</div>
		</s:form>
	</div>
</div>