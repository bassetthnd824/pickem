<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form cssClass="form-horizontal">
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<div class="form-header clearfix">
					<h2 class="pull-left">Season</h2>
					
					<s:if test="formMode == 'edit'">
						<div class="pull-right">
							<s:submit type="button" action="seasons_delete" cssClass="btn btn-default btn-danger"><span class="glyphicon glyphicon-minus"></span></s:submit>
						</div>
					</s:if>
				</div>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden name="modelId" value="%{model.id}"/>
			
			<s:textfield label="Season" name="model.season"/>
			<s:textfield label="Begin Date" name="model.beginDate" cssClass="begin-date" />
			<s:textfield label="End Date" name="model.endDate" cssClass="end-date" data-duration="months"/>
			 
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
					<s:submit type="button" action="seasons_cancel" cssClass="btn btn-default">Cancel</s:submit>
					<s:submit type="button" action="seasons_save" cssClass="btn btn-default btn-primary">Save</s:submit>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit type="button" action="seasons_search" cssClass="btn btn-default btn-primary">Search</s:submit>
				</div>
			</s:else>
		</s:form>
	</div>
</div>