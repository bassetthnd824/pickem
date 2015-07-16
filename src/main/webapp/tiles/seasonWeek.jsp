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
			</s:if>
			
			<input type="hidden" id="formMode" name="formMode" value="${formMode}"/>
			<input type="hidden" id="modelId" name="modelId" value="${model.id}"/>
			
			<div class="form-group">
				<label class="col-sm-3 control-label">Season</label>
				<div class="col-sm-9">
					<select id="season.id" name="season.id" class="form-control">
						<s:if test="formMode != 'edit'">
							<option value="">Please select a Season</option>
						</s:if>
						
						<c:forEach items="${seasons}" var="season">
							<option value="${season.id}"${(season.id == model.season.id) ? ' selected="selected"' : '' }>${season.season}</option>
						</c:forEach>
					</select>
				</div>
			</div>
			
			<div class="form-group">
				<label class="col-sm-3 control-label">Week Number</label>
				<div class="col-sm-9">
					<input type="text" id="weekNumber" name="weekNumber" class="form-control" value="${model.weekNumber }"/>
				</div>
			</div>
			
			<div class="form-group">
				<label class="col-sm-3 control-label">Begin Date</label>
				<div class="col-sm-9">
					<input type="text" id="beginDate" name="beginDate" class="form-control" value="${model.beginDate }"/>
				</div>
			</div>
			
			<div class="form-group">
				<label class="col-sm-3 control-label">End Date</label>
				<div class="col-sm-9">
					<input type="text" id="endDate" name="endDate" class="form-control" value="${model.endDate }" data-duration="days"/>
				</div>
			</div>
			
			<s:if test="formMode == 'edit'">
				<div class="form-group">
					<label class="col-sm-3 control-label">Last Updated On</label>
					<div class="col-sm-9">
						<p class="form-control-static"><s:date name="model.lastUpdateDate" format="yyyy-MM-dd hh:mm:ss a"/></p>
					</div>
				</div>
				
				<div class="form-group">
					<label class="col-sm-3 control-label">Last Updated By</label>
					<div class="col-sm-9">
						<p class="form-control-static"><s:property value="model.lastUpdateUser" /></p>
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