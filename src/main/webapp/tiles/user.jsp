<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form cssClass="form-horizontal">
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<div class="form-header clearfix">
					<h2 class="pull-left">User</h2>
					
					<s:if test="formMode == 'edit'">
						<div class="pull-right">
							<s:submit type="button" action="users_delete" cssClass="btn btn-default btn-danger"><span class="glyphicon glyphicon-minus"></span></s:submit>
						</div>
					</s:if>
				</div>
				
				<s:if test="hasActionErrors()">
					<s:actionerror/>
				</s:if>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden key="id" name="id"/>
			
			<s:textfield label="Email Address" name="emailAddr"/>
			<s:textfield label="First Name" name="firstName"/>
			<s:textfield label="Last Name" name="lastName"/>
			<s:textfield label="Nickname" name="nickName"/>
			<s:select label="Theme" name="theme.id" list="themes" listKey="id" listValue="themeName" headerKey="" headerValue="Please select a Theme"/>
			
			<s:if test="formMode == 'edit'">
				<div class="form-group">
					<label class="col-sm-3 control-label">Groups</label>
					<div class="col-sm-9 controls">
						<c:set var="i" value="0"/>
						<c:forEach items="${grps}" var="group">
							<c:set var="checkedText" value=""/>
							<c:forEach items="${selectedGroups}" var="groupId">
								<c:if test="${group.id == groupId}">
									<c:set var="checkedText" value='checked="checked"'/>
								</c:if>
							</c:forEach>
							
							<div class="checkbox">
								<label for="selectedGroups[${i}]">
									<input type="checkbox" id="selectedGroups[${i}]" name="selectedGroups[${i}]" value="${group.id}" ${checkedText}/>
									${group.groupName}
								</label>
							</div>
							<c:set var="i" value="${i + 1}"/>
						</c:forEach>
					</div>
				</div>
				
				<div class="form-group">
					<label class="col-sm-3 control-label">Last Updated On</label>
					<div class="col-sm-9">
						<p class="form-control-static"><s:date name="lastUpdateDate" format="yyyy-MM-dd hh:mm:ss a"/></p>
					</div>
				</div>
				
				<div class="form-group">
					<label class="col-sm-3 control-label">Last Updated By</label>
					<div class="col-sm-9">
						<p class="form-control-static"><s:property value="lastUpdateUser" /></p>
					</div>
				</div>
			</s:if>
			
			<s:if test="formMode != 'search' && formMode != 'init'">
				<div class="pull-right">
					<s:submit type="button" action="users_cancel" cssClass="btn btn-default">Cancel</s:submit>
					<s:submit type="button" action="users_save" cssClass="btn btn-default btn-primary">Save</s:submit>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit type="button" action="users_search" cssClass="btn btn-default btn-primary">Search</s:submit>
				</div>
			</s:else>
		</s:form>
	</div>
</div>