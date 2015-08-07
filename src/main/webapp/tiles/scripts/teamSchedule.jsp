<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<script type="text/javascript" src="<c:url value="/resources/js/app/teamSchedule.js"/>"></script>
<script id="tableTemplate" type="text/template">
	<table class="table table-hover margin-before">
		<thead>
			<tr>
				<th>Date</th>
				<th>Opponent</th>
				<th>Score</th>
				<th>Result</th>
			</tr>
		</thead>
		<tbody>
			{{ _.each(matchups, function(matchup) { }}
				<tr>
					<td>{{=matchup.matchupDate}}</td>
					<td>{{=matchup.opponentName}}</td>
					<td>{{=(matchup.scoreResult) ? matchup.scoreResult : ''}}</td>
					<td>{{=(matchup.winLoss) ? matchup.winLoss : ''}}</td>
				</tr>
			{{ }); }}
		</tbody>
	</table>
</script>