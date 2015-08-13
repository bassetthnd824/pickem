<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form cssClass="form-horizontal">
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<div class="form-header clearfix">
					<h2 class="pull-left">Season Week</h2>
					
					<s:if test="formMode == 'edit'">
						<div class="pull-right">
							<s:submit type="button" action="seasonWeeks_delete" cssClass="btn btn-default btn-danger"><span class="glyphicon glyphicon-minus"></span></s:submit>
						</div>
					</s:if>
				</div>
				
				<s:if test="hasActionErrors()">
					<s:actionerror/>
				</s:if>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden key="id" name="id"/>
			
			<s:select label="Season" id="seasonWeek_season_id" name="season.id" list="seasons" listKey="id" listValue="season" headerKey="" headerValue="Please select a Season"/>
			<s:textfield label="Week Number" id="seasonWeek_weekNumber" name="weekNumber"/>
			<s:textfield label="Begin Date" name="beginDate" cssClass="date begin-date"/>
			<s:textfield label="End Date" name="endDate" cssClass="date end-date" data-duration="days"/>
			
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
					<s:submit type="button" action="seasonWeeks_cancel" cssClass="btn btn-default">Cancel</s:submit>
					<s:submit type="button" action="seasonWeeks_save" cssClass="btn btn-default btn-primary">Save</s:submit>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit type="button" action="seasonWeeks_search" cssClass="btn btn-default btn-primary">Search</s:submit>
				</div>
			</s:else>
		</s:form>
	</div>
</div>