<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-6 col-sm-offset-3">
		<s:form cssClass="form-horizontal">
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<div class="form-header clearfix">
					<h2 class="pull-left">Matchup</h2>
					
					<s:if test="formMode == 'edit'">
						<div class="pull-right">
							<s:submit type="button" action="matchups_delete" cssClass="btn btn-default btn-danger"><span class="glyphicon glyphicon-minus"></span></s:submit>
						</div>
					</s:if>
				</div>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden name="modelId" value="%{model.id}"/>
			<s:hidden name="hiddenSeasonWeekId" id="hiddenSeasonWeekId" value="%{model.seasonWeek.id}" /> 
			
			<s:select label="Season" name="model.seasonWeek.season.id" list="seasons" listKey="id" listValue="season" headerKey="" headerValue="Please select a Season" cssClass="double-select double-select-master" data-action-name="seasonWeeks_getSeasonWeeksBySeason.action" data-parameter-name="model.season.id" data-slave-id="seasonWeek"/>
			<s:select label="Week Number" name="model.seasonWeek.id" id="seasonWeek" list="seasonWeeks" listKey="id" listValue="weekNumber" headerKey="" headerValue="Please select a Week Number" cssClass="double-select double-select-slave" data-value-prop="id" data-text-prop="weekNumber" data-hidden-value-holder="hiddenSeasonWeekId"/>
			<s:textfield label="Date" name="model.matchupDate" cssClass="date"/>
			
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<s:select label="Home Team" name="model.homeTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
				<s:textfield label="Home Team Score" name="model.homeTeamScore"/>
				<s:select label="Away Team" name="model.awayTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
				<s:textfield label="Away Team Score" name="model.awayTeamScore"/>
			</s:if>
			<s:else>
				<s:select label="Team" name="model.homeTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
			</s:else>
			
			<s:select label="Venue" name="model.venue.id" list="venues" listKey="id" listValue="venueName" headerKey="" headerValue="Please select a Venue"/>
			
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
					<s:submit type="button" action="matchups_cancel" cssClass="btn btn-default">Cancel</s:submit>
					<s:submit type="button" action="matchups_save" cssClass="btn btn-default btn-primary">Save</s:submit>
				</div>
			</s:if>
				
			<s:else>
				<div class="pull-right">
					<s:submit type="button" action="matchups_search" cssClass="btn btn-default btn-primary">Search</s:submit>
				</div>
			</s:else>
		</s:form>
	</div>
</div>