<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/struts-tags" prefix="s" %>

<h2><s:property value="welcomeMessage"/></h2>

<s:form>
	<input type="hidden" id="numberOfConferenceTeams" value="${numberOfConferenceTeams}"/>
	
	<c:forEach items="${seasonWeeks }" var="seasonWeek">
		<h3>Week ${seasonWeek.weekNumber}</h3>
		
		<div class="clearfix column-container column-header-container">
			<div class="image-column"></div>
			<div class="matchup-date text-center hidden-xs hidden-sm"><strong>Date</strong></div>
			<div class="rivalry-name text-center hidden-xs hidden-sm hidden-md"><strong>Rivalry Name</strong></div>
			<div class="team-name text-center"><strong>Away Team</strong></div>
			<div class="team-score text-center"><strong>Score</strong></div>
			<div class="matchup-ranktext-center"><strong>Rank</strong></div>
			<div class="team-score text-center"><strong>Score</strong></div>
			<div class="team-name text-center"><strong>Home Team</strong></div>
			<div class="venue-name text-center hidden-xs hidden-sm"><strong>Venue</strong></div>
			<div class="sort-buttons"></div>
		</div>
		
		<div class="weekly-matchups">
			<c:set var="rank" value="${numberOfConferenceTeams}"/>
			<c:set var="matchupsList" value="${matchupUserPicks[seasonWeek.id]}"/>
			<c:set var="i" value="0"/>
			
			<c:forEach items="${matchupsList}" var="matchup">
				<div class="weekly-matchup">
					<input type="hidden" name="model[${i}].matchup.id" value="${matchup.matchupId}"/>
					<input type="hidden" name="model[${i}].id" value="${matchup.userPickId}"/>
					
					<div class="well well-sm column-container clearfix">
						<div class="image-column">
							<c:choose>
								<c:when test="${matchup.pickedTeamId != null && matchup.winningTeamId != -1 && matchup.winningTeamId == matchup.pickedTeamId}">
									<img alt="" src="<c:url value="/resources/img/check-24-ns.png" />"/>
								</c:when>
								
								<c:when test="${matchup.pickedTeamId != null && matchup.winningTeamId != -1 && matchup.winningTeamId != matchup.pickedTeamId}">
									<img alt="" src="<c:url value="/resources/img/cross-24-ns.png" />"/>
								</c:when>
							</c:choose>
						</div>
						
						<div class="matchup-date hidden-xs hidden-sm"><fmt:formatDate value="${matchup.matchupDate}" pattern="yyyy-MM-dd"/></div>
						<div class="rivalry-name hidden-xs hidden-sm hidden-md">${matchup.rivalryName}</div>
						
						<div class="team-name">
							<label class="radio-inline">
								<input type="radio" name="model[${i}].pickedTeam.id" value="${matchup.awayTeamId}" ${(matchup.pickedTeamId != null && matchup.awayTeamId == matchup.pickedTeamId) ? ' checked="checked"' : '' }/>
								${matchup.awayTeamName}
								<span class="hidden-xs hidden-sm">${matchup.awaySquadName}</span>
							</label>
						</div>
						
						<div class="team-score">${matchup.awayTeamScore}</div>
						<div class="matchup-rank"><span>${(matchup.rank != null) ? matchup.rank : rank}</span><input type="hidden" name="model[${i}].rank" value="${(matchup.rank != null) ? matchup.rank : rank}"/></div>
						<div class="team-score">${matchup.homeTeamScore}</div>
						
						<div class="team-name">
							<label class="radio-inline">
								<input type="radio" name="model[${i}].pickedTeam.id" value="${matchup.homeTeamId}" ${(matchup.pickedTeamId != null && matchup.homeTeamId == matchup.pickedTeamId) ? ' checked="checked"' : '' }/>
								${matchup.homeTeamName}
								<span class="hidden-xs hidden-sm">${matchup.homeSquadName}</span>
							</label>
						</div>
						
						<div class="venue-name hidden-xs hidden-sm">${matchup.venueName} ${matchup.cityState}</div>
						
						<div class="sort-buttons">
							<div><button type="button" class="up-button btn btn-xs btn-default btn-block"><span class="glyphicon glyphicon-triangle-top"></span></button></div>
							<div><button type="button" class="down-button btn btn-xs btn-default btn-block"><span class="glyphicon glyphicon-triangle-bottom"></span></button></div>
						</div>
					</div>
				</div>
				
				<c:set var="rank" value="${rank - 1}"/>
				<c:set var="i" value="${i + 1}"/>
			</c:forEach>
		</div>
	</c:forEach>
	
	<div class="button-container pull-right">
		<s:submit type="button" action="main" cssClass="btn btn-default">Cancel</s:submit>
		<s:submit type="button" action="main_save" cssClass="btn btn-default btn-primary">Save</s:submit>
	</div>
</s:form>