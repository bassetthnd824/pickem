<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form action="seasons.action" cssClass="form-horizontal">
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
			
			<input type="hidden" id="formMode" name="formMode" value="${formMode}"/>
			<input type="hidden" id="modelId" name="modelId" value="${model.id}"/>
			
			<div class="form-group">
				<label class="col-sm-3 control-label">Season</label>
				<div class="col-sm-9">
					<input type="text" id="season" name="season" class="form-control" value="${model.season }"/>
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
					<input type="text" id="endDate" name="endDate" class="form-control" value="${model.endDate }" data-duration="months"/>
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
			
			<s:if test="formMode != 'search'">
				<div class="pull-right">
					<s:submit action="seasons_cancel" cssClass="btn btn-default" key="Cancel"/>
					<s:submit action="seasons_save" cssClass="btn btn-default btn-primary" key="Save"/>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit action="seasons_search" cssClass="btn btn-default btn-primary" key="Search"/>
				</div>
			</s:else>
		</s:form>
	</div>
</div>