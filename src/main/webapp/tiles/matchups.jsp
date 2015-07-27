<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<div class="row">
	<div class="col-sm-12">
		<table class="table table-hover">
			<tr>
				<th>Season</th>
				<th>Week Number</th>
				<th>Date</th>
				<th>Home Team</th>
				<th>Home Team Score</th>
				<th>Away Team</th>
				<th>Away Team Score</th>
				<th>Venue</th>
				<th>Last Updated On</th>
				<th>Last Updated By</th>
				<th>Actions</th>
			</tr>
			
			<s:iterator value="modelList">
				<s:url action="matchups_edit" var="editUrl">
					<s:param name="modelId" value="id"/>
				</s:url>
				<tr>
					<td><s:property value="seasonWeek.season.season"/></td>
					<td><s:property value="seasonWeek.weekNumber"/></td>
					<td><s:date name="matchupDate" format="yyyy-MM-dd"/></td>
					<td><s:property value="homeTeam.teamName + ' ' + homeTeam.squadName"/></td>
					<td><s:property value="homeTeamScore"/></td>
					<td><s:property value="awayTeam.teamName + ' ' + awayTeam.squadName"/></td>
					<td><s:property value="awayTeamScore"/></td>
					<td><s:property value="venue.venueName"/></td>
					<td><s:date name="lastUpdateDate" format="yyyy-MM-dd hh:mm:ss a"/></td>
					<td><s:property value="lastUpdateUser"/></td>
					<td>
						<a href="<s:property value="#editUrl"/>" class="btn btn-small btn-default edit"><span class="glyphicon glyphicon-pencil"></span></a>
					</td>
				</tr>
			</s:iterator>
		</table>
		
		<div>
			<a href="<s:url action="matchups_add"/>" class="btn btn-small btn-primary add"><span class="glyphicon glyphicon-plus"></span></a>
		</div>
	</div>
</div>