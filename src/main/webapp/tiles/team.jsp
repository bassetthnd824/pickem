<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form cssClass="form-horizontal">
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<div class="form-header clearfix">
					<h2 class="pull-left">Team</h2>
					
					<s:if test="formMode == 'edit'">
						<div class="pull-right">
							<s:submit type="button" action="teams_delete" cssClass="btn btn-default btn-danger"><span class="glyphicon glyphicon-minus"></span></s:submit>
						</div>
					</s:if>
				</div>
				
				<s:if test="hasActionErrors()">
					<s:actionerror/>
				</s:if>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden key="id" name="id"/>
			
			<s:textfield label="Name" name="teamName"/>
			<s:textfield label="Squad Name" name="squadName"/>
			
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<s:checkbox label="Conference Member" name="conferenceMember" />
			</s:if>
			
			<s:select label="Home Venue" name="homeVenue.id" list="venues" listKey="id" listValue="venueName" headerKey="" headerValue="Please select a Home Venue"/>
			
			<s:if test="formMode == 'edit'">
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
					<s:submit type="button" action="teams_cancel" cssClass="btn btn-default">Cancel</s:submit>
					<s:submit type="button" action="teams_save" cssClass="btn btn-default btn-primary">Save</s:submit>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit type="button" action="teams_search" cssClass="btn btn-default btn-primary">Search</s:submit>
				</div>
			</s:else>
		</s:form>
	</div>
</div>