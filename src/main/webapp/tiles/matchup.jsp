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
				
				<s:if test="hasActionErrors()">
					<s:actionerror/>
				</s:if>
			</s:if>
			
			<s:hidden key="formMode" id="formMode"/>
			<s:hidden key="id" name="id"/>
			<s:hidden name="hiddenSeasonWeekId" id="hiddenSeasonWeekId" value="%{seasonWeek.id}" /> 
			
			<s:select label="Season" name="seasonWeek.season.id" list="seasons" listKey="id" listValue="season" headerKey="" headerValue="Please select a Season" cssClass="double-select double-select-master" data-action-name="seasonWeeks_getSeasonWeeksBySeason.action" data-parameter-name="season.id" data-slave-id="matchups_seasonWeek_id"/>
			<s:select label="Week Number" name="seasonWeek.id" id="matchups_seasonWeek_id" list="seasonWeeks" listKey="id" listValue="weekNumber" headerKey="" headerValue="Please select a Week Number" cssClass="double-select double-select-slave" data-value-prop="id" data-text-prop="weekNumber" data-hidden-value-holder="hiddenSeasonWeekId"/>
			<s:textfield label="Date" id="matchups_matchupDate" name="matchupDate" cssClass="date"/>
			
			<s:if test="formMode == 'edit' || formMode == 'add'">
				<s:select label="Home Team" id="matchups_homeTeam_id" name="homeTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
				<s:textfield label="Home Team Score" name="homeTeamScore"/>
				<s:select label="Away Team" name="awayTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
				<s:textfield label="Away Team Score" name="awayTeamScore"/>
			</s:if>
			<s:else>
				<s:select label="Team" name="homeTeam.id" list="teams" listKey="id" listValue="teamName + ' ' + squadName" headerKey="" headerValue="Please select a Team"/>
			</s:else>
			
			<s:select label="Venue" id="matchups_venue_id" name="venue.id" list="venues" listKey="id" listValue="venueName" headerKey="" headerValue="Please select a Venue"/>
			
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